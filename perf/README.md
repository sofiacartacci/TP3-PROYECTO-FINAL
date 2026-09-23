# Línea Base 1.0 — medición de `POST /search`

Medición inicial del tiempo de respuesta de `ms-searcher` bajo un volumen
definido, para tener contra qué comparar las mejoras posteriores (incluido el
matching probabilístico con RAG).

## El volumen medido

**Registros en el índice.** Dos escenarios, generados de forma reproducible con
`perf/generate-dataset.py`:

| Escenario | Registros | Archivo |
|---|---|---|
| L-100 | 100 | `mock-provider/MO-100.csv` |
| L-1000 | 1000 | `mock-provider/MO-1000.csv` |

El generador respeta el esquema exacto de `mock-provider/MO.csv` (28 columnas,
delimitador `;`, todos los campos entrecomillados, UTF-8, LF), así que
`ms-ingestor` los procesa sin tocar el mapeo. Los nombres no se repiten dentro
de un dataset: cada registro es una combinación única de nombre y apellido.

`mock-provider/MO.csv` (el mock funcional original) queda intacto.

**Consultas.** Cada corrida usa un mix fijo, derivado del propio dataset y
guardado en `perf/queries-<N>.json`:

| Tipo | Peso | Qué mide |
|---|---|---|
| Presente | 60% | Camino con coincidencias |
| Fuzzy | 20% | Variante con error tipográfico o sin acentos |
| Ausente | 20% | Camino sin coincidencias |

Los nombres rotan en cada iteración para no medir el efecto de cachés.

**Concurrencia.** 1, 5, 10 y 20 usuarios virtuales, 60 segundos por nivel, cada
nivel en su propia corrida (no escalonado), para que el p95 de cada nivel sea
limpio y no una mezcla.

**Parámetros fijos.** `minLevel = 50`, `showDetails = false`, `searchAliases = true`,
sin filtros de categoría ni keywords.

## Cómo correr la medición

Requisitos: Docker, los jar compilados (`mvn clean package` en cada módulo),
`./cert` con los certificados, `.env` con los valores del equipo, y `./stopwords`
con `stopwords.txt`.

```bash
./perf/run-baseline.sh 100
./perf/run-baseline.sh 1000
```

El script hace todo el ciclo: genera el dataset si falta, **resetea el índice
Lucene**, levanta lo necesario, ingesta, arranca el searcher, corre un warm-up
que descarta, mide cada nivel de concurrencia y verifica contra el biller.

Variables opcionales:

```bash
VU_LEVELS="1 10" DURATION=120s SHOW_DETAILS=true ./perf/run-baseline.sh 1000
```

Los resultados quedan en `perf/results/medicion-d<N>-vu<C>.json`, con las
condiciones del test embebidas en cada archivo.

Para correr un solo nivel a mano:

```bash
docker compose --profile perf run --rm \
  -e DATASET=1000 -e VUS=10 -e DURATION=60s \
  k6 run /perf/baseline.js
```

## Métricas que se registran

| Métrica | De dónde sale |
|---|---|
| Latencia total (avg, p95, p99, máx) | `http_req_duration` — lo que ve el cliente |
| Tiempo de matching (avg, p95, máx) | `matching_time` — el `elapsedTime` del cuerpo |
| Throughput | `http_reqs` (req/s) |
| Tasa de error | `search_errors` |
| Búsquedas OK | `searches_ok` |

**Por qué se miden dos latencias.** `SearcherService.check()` calcula su
`elapsedTime` *antes* de llamar al biller:

```java
result.setElapsedTime(getElapsedTime(dde));           // solo el matching
...
restTemplate.exchange(billerUrl, HttpMethod.POST, ...); // llamada bloqueante
```

Entonces el `elapsedTime` de la respuesta mide solo el matching, mientras que la
latencia observada incluye además la facturación, TLS y serialización. **La
diferencia entre ambas es el costo de facturar**, y como esa llamada es
sincrónica y bloqueante, es el primer candidato a cuello de botella. Separarlas
permite demostrar después si una mejora atacó el matching o la facturación.

## Comportamientos del código que condicionan la medición

Todo esto está contemplado en el runner; se documenta porque explica por qué los
pasos van en ese orden.

1. **El searcher no arranca sin índice.** `Matcher.init()` abre el directorio
   Lucene en `@PostConstruct`. Si `/var/lucene-index` está vacío, el contexto de
   Spring no levanta. Orden obligatorio: ingestar primero, arrancar el searcher
   después.

2. **El índice acumula entre corridas.** `MergeIndex` y `FastloadItemWriter`
   abren el `IndexWriter` con el `OpenMode` por defecto (`CREATE_OR_APPEND`) y
   nunca borran. Sin resetear el volumen, medir 1000 después de 100 daría un
   índice de 1100 documentos y el volumen declarado sería falso. El runner
   elimina el volumen `<proyecto>_lucene-index` y vacía `listas` antes de cada
   corrida.

3. **El índice se recarga cada 30 segundos**
   (`MATCHER_LOOK_FOR_CHANGES_IN_INDEX_CRON`). El runner espera 35s tras la
   ingesta. Ese reopen periódico puede aparecer como un pico aislado en el p95.

4. **`minLevel` no filtra a nivel Lucene.** En `Matcher.check()`,
   `search.getMinLevel() / 100` es división entera sobre un `Integer`: da 0 para
   cualquier `minLevel` menor a 100. El filtrado real ocurre después en
   `Matcher.filter()`, que sí usa `/100F`. El resultado devuelto es correcto,
   pero el costo de `review()` se paga sobre todos los hits igual. Consecuencia
   práctica: **subir `minLevel` no reduce el tiempo de respuesta**, así que no
   sirve como variable de carga. Es una mejora concreta para una iteración
   posterior.

5. **`showDetails: true` agrega una consulta a Mongo** (`repo.findAllById`). Es
   la variable que más mueve la latencia, por eso está fija y declarada.

6. **El warm-up no es opcional.** Entre el JIT de la JVM y el page cache de
   Lucene, las primeras decenas de requests son mucho más lentas. Si no se
   descartan, la línea base queda inflada y toda comparación futura contra ella
   queda viciada.

## Verificación cruzada con el biller

Cada búsqueda factura contra `ms-biller`. El runner consulta
`POST /getUserTotal` antes y después: la diferencia tiene que coincidir con la
suma de `searches_ok` de todas las corridas. Es una comprobación independiente
de que las búsquedas se procesaron de verdad y de que la tasa de error reportada
es real.

## Qué documentar en Confluence

Los números sin las condiciones no son comparables. Para cada corrida:

- **Hardware**: CPU, RAM, y si corre sobre WSL2, cuánta RAM tiene asignada
  Docker Desktop. Cambia los resultados de forma significativa.
- **Config**: versiones de las imágenes, `matcher.maxres` (100),
  `matcher.maxhits` (100000), threads de Tomcat (200 por defecto), pool de
  conexiones de Mongo, `minLevel`, `showDetails`.
- **Dataset**: cuál de los dos escenarios, cómo se generó, y el mix de consultas.
- **Topología**: todos los servicios sobre un único host Docker, compitiendo por
  la misma CPU. Es una limitación real de la medición y hay que declararla.
- **Resultados**: la tabla por nivel de concurrencia, con latencia total y
  matching separados.
- **Conteo del biller** como verificación cruzada.

## Limitaciones conocidas

- Un solo host: no hay aislamiento entre el generador de carga y los servicios
  medidos. Con 20 VUs, k6 compite por CPU con la JVM.
- El dataset es sintético y regular (nombres de un pool acotado, sin ruido real
  ni duplicados parciales), así que el comportamiento del matcher es más
  favorable que con listas reales.
- No se mide el screener ni el flujo batch, solo `POST /search`.
