#Ejemplo inicialización 
#docker run --name ms-searcher --network myNetwork -v C:\$listas\logs:/var/logs -v C:\$listas\cert:/var/cert -v C:\$listasfiles\lucene-index:/var/lucene-index -v C:\$listas\stopwords:/var/stopwords -p 8083:8080 -e JWT_SECRET_KEY -e KEYSTORE_PASS -e KEYSTORE_ALIAS -e TRUSTSTORE_PASS -e SYSTECH-API-KEY ms-searcher

FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
ENV LOG=/var/logs
ENV JWT_SECRET_KEY=${JWT_SECRET_KEY}
ENV JWT_EXP_TIME=60000
ENV JWT_REF_TIME=600000
ENV MONGO_PORT=27017
ENV MONGO_HOST=host.docker.internal
ENV MONGO_DATABASE=systechlist
ENV KEYSTORE_FILE=/var/cert/systech.p12
ENV KEYSTORE_PASS=${KEYSTORE_PASS}
ENV KEYSTORE_ALIAS=${KEYSTORE_ALIAS}
ENV TRUSTSTORE_FILE=/var/cert/truststore.p12
ENV TRUSTSTORE_PASS=${TRUSTSTORE_PASS}
ENV BILLER_URL=https://ms-biller:8080
ENV SYSTECH-API-KEY='${SYSTECH-API-KEY}'
ENV MATCHER_INDEXPATH=/var/lucene-index
ENV MATCHER_STOPWORDS_FILE=/var/stopwords/stopwords.txt
ENV MATCHER_LOOK_FOR_CHANGES_IN_INDEX_CRON='*/30 * * * * *'
