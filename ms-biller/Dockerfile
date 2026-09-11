#docker run --name ms-biller --network myNetwork -v C:\$listas\logs:/var/logs -v C:\$listas\cert:/var/cert -p 8084:8080 -e JWT_SECRET_KEY -e KEYSTORE_PASS -e KEYSTORE_ALIAS -e TRUSTSTORE_PASS ms-biller

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