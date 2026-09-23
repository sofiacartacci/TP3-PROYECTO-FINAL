FROM nginx:alpine
RUN apk add --no-cache zip
# LIST_FILE elige el dataset a servir. Por defecto MO.csv (100 registros,
# mockeo funcional). Para la Linea Base se pasa MO-100.csv o MO-1000.csv,
# generados con perf/generate-dataset.py.
ARG LIST_FILE=MO.csv
COPY ${LIST_FILE} /tmp/MO.csv
RUN cd /tmp && zip -j /usr/share/nginx/html/mock-list.zip MO.csv
