#!/usr/bin/env bash
#
# Linea Base 1.0 - corrida completa y reproducible sobre POST /search.
#
#   ./perf/run-baseline.sh 100
#   ./perf/run-baseline.sh 1000
#
# Variables opcionales:
#   VU_LEVELS="1 5 10 20"   niveles de concurrencia a medir
#   DURATION=60s            duracion de cada nivel
#   MIN_LEVEL=50            nivel minimo de coincidencia
#   SHOW_DETAILS=false      'true' agrega la lectura de detalle contra Mongo
#
# Requisitos: Docker, los jar compilados (mvn clean package), ./cert y ./.env.

set -euo pipefail

# BuildKit intenta usar una consola interactiva para el progreso y falla
# ("failed to get console") cuando la salida del build se redirige.
export BUILDKIT_PROGRESS=plain

DATASET="${1:-1000}"
VU_LEVELS="${VU_LEVELS:-1 5 10 20}"
DURATION="${DURATION:-60s}"
MIN_LEVEL="${MIN_LEVEL:-50}"
SHOW_DETAILS="${SHOW_DETAILS:-false}"

AUTH_LOCAL="https://localhost:8081"
INGESTOR_LOCAL="https://localhost:8086"
SEARCHER_LOCAL="https://localhost:8083"

cd "$(dirname "$0")/.."
mkdir -p perf/results

step() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

wait_for() {                      # wait_for <url> <descripcion> [intentos]
  # Alcanza con que conteste HTTP: 401 (sin token) y 405 (metodo equivocado)
  # tambien significan que el servicio esta arriba. curl devuelve 000 cuando
  # no hubo respuesta.
  local url="$1" what="$2" tries="${3:-60}" code
  for ((i = 1; i <= tries; i++)); do
    code="$(curl -ks -o /dev/null -w '%{http_code}' "$url" || true)"
    if [[ -n "$code" && "$code" != "000" ]]; then
      echo "    $what responde (HTTP $code)"
      return 0
    fi
    sleep 2
  done
  echo "    ERROR: $what no respondio tras $((tries * 2))s" >&2
  return 1
}

login() {
  curl -ks -X POST "$AUTH_LOCAL/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${PERF_USERNAME:-admin}\",\"password\":\"${PERF_PASSWORD:-admin}\"}" \
    | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"$//'
}

mongo_eval() { docker compose exec -T mongo mongosh "$1" --quiet --eval "$2"; }

# ---------------------------------------------------------------- dataset
step "Dataset de $DATASET registros"
if [[ ! -f "mock-provider/MO-${DATASET}.csv" ]]; then
  python3 perf/generate-dataset.py "$DATASET"
else
  echo "    mock-provider/MO-${DATASET}.csv ya existe"
fi

# ------------------------------------------------- reset del indice Lucene
# El ingestor abre el IndexWriter en CREATE_OR_APPEND y nunca borra: sin este
# reset, una segunda corrida deja el indice con la suma de ambos datasets y la
# medicion deja de corresponder al volumen declarado.
step "Reset del indice Lucene y de la coleccion listas"
PROJECT="$(docker compose config --format json | python3 -c 'import sys,json; print(json.load(sys.stdin)["name"])')"
docker compose stop ms-searcher ms-screener ms-screener-searcher ms-ingestor >/dev/null 2>&1 || true
docker compose rm -fs ms-searcher ms-screener ms-screener-searcher ms-ingestor >/dev/null 2>&1 || true
docker volume rm "${PROJECT}_lucene-index" >/dev/null 2>&1 \
  && echo "    volumen ${PROJECT}_lucene-index eliminado" \
  || echo "    volumen ${PROJECT}_lucene-index no existia"

docker compose up -d mongo >/dev/null
sleep 3
mongo_eval systechlist 'db.listas.deleteMany({})' >/dev/null 2>&1 \
  && echo "    coleccion listas vaciada" || echo "    coleccion listas ya vacia"

# ------------------------------------------------------------ levantar base
step "Levantando mock-provider, authenticator y biller"
# El build escribe a la terminal: si se redirige, el escritor de progreso
# aborta con "failed to get console: provided file is not a console".
export MOCK_LIST_FILE="MO-${DATASET}.csv"
docker compose build mock-provider
docker compose up -d mock-provider >/dev/null
docker compose up -d ms-authenticator ms-biller >/dev/null
wait_for "$AUTH_LOCAL/login" "ms-authenticator"

TOKEN="$(login)"
[[ -n "$TOKEN" ]] || { echo "ERROR: no se pudo obtener token" >&2; exit 1; }
echo "    token obtenido"

# ------------------------------------------------------------- ingesta
step "Ingesta de la lista"
docker compose up -d ms-ingestor >/dev/null
wait_for "$INGESTOR_LOCAL/getJobStatus" "ms-ingestor"

curl -ks "$INGESTOR_LOCAL/startJob?compareChanges=false" -H "Authorization: Bearer $TOKEN" >/dev/null
echo "    job lanzado, esperando..."

for ((i = 1; i <= 150; i++)); do
  STATUS="$(curl -ks "$INGESTOR_LOCAL/getJobStatus" -H "Authorization: Bearer $TOKEN" \
            | grep -o '"status":"[A-Z]*"' | head -1 | sed 's/.*:"//;s/"//')"
  [[ "$STATUS" == "COMPLETED" || "$STATUS" == "FAILED" ]] && break
  sleep 2
done
echo "    job: ${STATUS:-desconocido}"
[[ "$STATUS" == "COMPLETED" ]] || { echo "ERROR: la ingesta no termino bien" >&2; exit 1; }

CARGADOS="$(mongo_eval systechlist 'db.listas.countDocuments({})' | tr -d '\r')"
echo "    registros en Mongo: $CARGADOS (esperado: $DATASET)"
[[ "$CARGADOS" == "$DATASET" ]] || echo "    AVISO: no coincide con el dataset declarado"

# ------------------------------------------------------------- searcher
# Matcher.init() abre el indice en @PostConstruct: si el searcher arranca antes
# de que exista, el contexto de Spring no levanta. Por eso va despues.
step "Levantando ms-searcher (despues de que el indice exista)"
docker compose up -d ms-searcher >/dev/null
wait_for "$SEARCHER_LOCAL/search" "ms-searcher"
echo "    esperando 35s por la recarga del indice (cron cada 30s)"
sleep 35

BILL_ANTES="$(curl -ks -X POST "https://localhost:8084/getUserTotal" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"user\":\"${PERF_USERNAME:-admin}\",\"from\":\"2000-01-01T00:00:00+00:00\",\"to\":\"2100-01-01T00:00:00+00:00\"}" || echo '{}')"

# ------------------------------------------------------------- medicion
run_k6() {                        # run_k6 <label> <vus> <duracion>
  docker compose --profile perf run --rm -T \
    -e "DATASET=$DATASET" -e "VUS=$2" -e "DURATION=$3" -e "LABEL=$1" \
    -e "MIN_LEVEL=$MIN_LEVEL" -e "SHOW_DETAILS=$SHOW_DETAILS" \
    k6 run /perf/baseline.js
}

step "Warm-up (30s, se descarta: JIT de la JVM y page cache de Lucene)"
run_k6 warmup 5 30s >/dev/null 2>&1 || true
echo "    warm-up terminado"

for VUS in $VU_LEVELS; do
  step "Medicion: $VUS usuarios concurrentes, $DURATION"
  run_k6 medicion "$VUS" "$DURATION"
done

# ------------------------------------------------------- verificacion cruzada
step "Verificacion contra el biller"
BILL_DESPUES="$(curl -ks -X POST "https://localhost:8084/getUserTotal" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"user\":\"${PERF_USERNAME:-admin}\",\"from\":\"2000-01-01T00:00:00+00:00\",\"to\":\"2100-01-01T00:00:00+00:00\"}" || echo '{}')"
echo "    antes:   $BILL_ANTES"
echo "    despues: $BILL_DESPUES"
echo "    (la diferencia debe coincidir con la suma de busquedas OK de los k6)"

step "Listo"
echo "    resultados en perf/results/"
ls -1 perf/results/medicion-d"${DATASET}"-vu*.json 2>/dev/null || true
