@"
# SmartLists - Levantar el ecosistema con Docker

## Requisitos
1. Docker corriendo (WSL2/Ubuntu).
2. Proyecto compilado (mvn clean package en cada modulo - requiere ms_common-parent).
3. Certificados en ./cert (systech.p12 y truststore.p12).
4. .env creado desde .env.example con los valores del equipo.
5. Carpeta ./stopwords con stopwords.txt (copiar de ms-screener-searcher/src/main/resources/stopwords/).

## Comandos (en Ubuntu, parados en /mnt/c/Users/scart/SmartLists)
- Levantar todo: docker compose up -d --build
- Estado: docker compose ps
- Logs: docker compose logs -f ms-authenticator
- Bajar: docker compose down

## Puertos
authenticator 8081 | retriever 8082 | searcher 8083 | biller 8084
ingestor 8086 | screener 8087 | screener-searcher 8088 | Mongo 27017

## Notas
- Servicios en HTTPS con certificado self-signed: en Postman apagar SSL verification.
- authenticator usa base Mongo 'users'; el resto 'systechlist'.
- Orden de prueba: authenticator (token) -> ingestor (cargar lista) -> searcher (consultar).
  "@ | Out-File -Encoding utf8 README-docker.md