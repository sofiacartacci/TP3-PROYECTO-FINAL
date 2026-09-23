// Linea Base 1.0 - medicion de POST /search (ms-searcher)
//
// Se ejecuta una vez por nivel de concurrencia; run-baseline.sh recorre los
// niveles y guarda un resumen por corrida. Ver perf/README.md.
//
// Variables de entorno:
//   DATASET     cantidad de registros en el indice (elige el archivo de queries)
//   VUS         usuarios virtuales concurrentes
//   DURATION    duracion de la corrida (ej: 60s)
//   LABEL       etiqueta del archivo de salida (ej: warmup, medicion)
//   AUTH_URL    base del authenticator
//   SEARCH_URL  base del searcher
//   USERNAME / PASSWORD   credenciales del authenticator
//   MIN_LEVEL   nivel minimo de coincidencia
//   SHOW_DETAILS  'true' agrega la lectura de detalle contra Mongo

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const DATASET = __ENV.DATASET || '1000';
const LABEL = __ENV.LABEL || 'medicion';
const VUS = parseInt(__ENV.VUS || '1', 10);
const DURATION = __ENV.DURATION || '60s';

const AUTH_URL = __ENV.AUTH_URL || 'https://ms-authenticator:8080';
const SEARCH_URL = __ENV.SEARCH_URL || 'https://ms-searcher:8080';
const USERNAME = __ENV.USERNAME || 'admin';
const PASSWORD = __ENV.PASSWORD || 'admin';
const MIN_LEVEL = parseInt(__ENV.MIN_LEVEL || '50', 10);
const SHOW_DETAILS = (__ENV.SHOW_DETAILS || 'false') === 'true';

// open() solo corre en el contexto de init.
const queries = JSON.parse(open(`./queries-${DATASET}.json`));

// elapsedTime del cuerpo de la respuesta: mide SOLO el matching en el servidor,
// porque SearcherService lo calcula antes de llamar al biller. La diferencia
// contra http_req_duration es el costo de facturacion + TLS + serializacion.
const matchingTime = new Trend('matching_time', true);
const searchErrors = new Rate('search_errors');
const searchesOk = new Counter('searches_ok');

export const options = {
  vus: VUS,
  duration: DURATION,
  insecureSkipTLSVerify: true,          // certificado self-signed del ecosistema
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    search_errors: ['rate<0.01'],
  },
};

export function setup() {
  const res = http.post(`${AUTH_URL}/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } });

  if (res.status !== 200) {
    throw new Error(`Login fallido (${res.status}). Revisar credenciales y que ms-authenticator este arriba.`);
  }
  // JWT_EXP_TIME esta en 3000000 ms (50 min), alcanza para toda la corrida.
  return { token: res.json('token') };
}

// Mix declarado en el README: 60% presente, 20% fuzzy, 20% ausente.
function pickQuery(n) {
  const bucket = n % 10;
  if (bucket < 6) {
    return { type: 'exact', text: queries.exact[n % queries.exact.length] };
  }
  if (bucket < 8) {
    return { type: 'fuzzy', text: queries.fuzzy[n % queries.fuzzy.length] };
  }
  return { type: 'absent', text: queries.absent[n % queries.absent.length] };
}

export default function (data) {
  const n = __ITER * VUS + __VU;
  const query = pickQuery(n);

  const res = http.post(`${SEARCH_URL}/search`, JSON.stringify({
    text: query.text,
    minLevel: MIN_LEVEL,
    searchAliases: true,
    showDetails: SHOW_DETAILS,
  }), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
    tags: { query_type: query.type },
  });

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'cuerpo con returnCode': (r) => {
      try {
        return r.json('returnCode') !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  searchErrors.add(!ok);
  if (ok) {
    searchesOk.add(1);
    matchingTime.add(res.json('elapsedTime'), { query_type: query.type });
  }
}

export function handleSummary(data) {
  const name = `/perf/results/${LABEL}-d${DATASET}-vu${VUS}`;
  return {
    stdout: textSummary(data),
    [`${name}.json`]: JSON.stringify({
      condiciones: {
        dataset_registros: parseInt(DATASET, 10),
        vus: VUS,
        duracion: DURATION,
        min_level: MIN_LEVEL,
        show_details: SHOW_DETAILS,
        mix: '60% presente / 20% fuzzy / 20% ausente',
      },
      metricas: data.metrics,
    }, null, 2) + '\n',
  };
}

// Resumen de texto minimo: evita depender de modulos remotos (jslib.k6.io),
// que no estarian disponibles si la corrida se hace sin salida a internet.
function textSummary(data) {
  const m = data.metrics;
  const stat = (metric, key) => (m[metric] && m[metric].values[key] != null
    ? m[metric].values[key].toFixed(2) : 'n/d');

  return [
    '',
    `  dataset ${DATASET} registros | ${VUS} VUs | ${DURATION} | minLevel ${MIN_LEVEL} | showDetails ${SHOW_DETAILS}`,
    '',
    `  latencia total (cliente)   avg ${stat('http_req_duration', 'avg')} ms  p95 ${stat('http_req_duration', 'p(95)')} ms  max ${stat('http_req_duration', 'max')} ms`,
    `  matching (servidor)        avg ${stat('matching_time', 'avg')} ms  p95 ${stat('matching_time', 'p(95)')} ms  max ${stat('matching_time', 'max')} ms`,
    `  throughput                 ${stat('http_reqs', 'rate')} req/s`,
    `  busquedas OK               ${stat('searches_ok', 'count')}`,
    `  tasa de error              ${stat('search_errors', 'rate')}`,
    '',
  ].join('\n');
}
