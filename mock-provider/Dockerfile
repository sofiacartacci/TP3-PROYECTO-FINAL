FROM nginx:alpine
RUN apk add --no-cache zip
COPY MO.csv /tmp/MO.csv
RUN cd /tmp && zip -j /usr/share/nginx/html/mock-list.zip MO.csv