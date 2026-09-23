# Parent

### Reference Documentation

Proyecto parent que incluye dependencias comunes a los demás proyectos.
Los proyectos "ms*parent" y "ms_core" han sido nombrados intencionalmente con con un guión bajo (en lugar de guión medio) luego del
prefijo, es decir "ms*". Con esto se consigue que en el explorador de proyectos de STS se posicionen al inicio, separados de los servicios.

### Requisitos de ejecución

-   Java versión 11 o posterior
-   Base de datos MongoDb

### Ambiente desarrollo local (pc del programador)

-   Jdk 11 o posterior
-   Maven 3.6.3 o posterior

-   Variables de entorno:

    -   JAVA_HOME apuntando a jdk, por ejemplo =C:\jdk-11
    -   SPRING_PROFILES_ACTIVE=local
    -   JWT_SECRET_KEY= valor de clave secreta, por ejemplo JhdXRob3JpdGllcyI6W3siYXV0aG9yaXR5IjoidXNlciJ9LHsiYXV0aG9yaXR5IjoiYWRtaW4ifV0s
    -   KEYSTORE_FILE=ruta al almacén de certificados, por ejemplo systech.p12
    -   KEYSTORE_PASS=password del almacén de certificados, en el certificado provisto es es "changeit"
    -   KEYSTORE_ALIAS=alias del certificado para SSL, en el certificado provisto es "systech"
    -   TRSUTSTORE_FILE=ruta al almacén de certificados de confianza, por ejemplo trustore.p12. Ahí deben estar los certificados de los sevicios a consumir
    -   TRUSTSTORE_PASS=password del almacén de certificados de confianza, en el certificado provisto es es "changeit"
    -   LOG=carpeta donde se guardarán los logs
    -   SYSTECH-API-KEY= api key utilizada por los servicios que consumen otros servicios del ecosistema, por ejemplo searcher consume biller. Las api key se generan con el ms-authenticator

-   La ejecución tomará el profile **local** (definido en la variable de entorno SPRING_PROFILES_ACTIVE), es decir que leerá las properties del archivo application-local.properties de cada servicio

### Generación de ejecutables

Posicionarse en ms_common-parent y ejecutar **"mvn clean install"**
Esto compilará todos los módulos definidos en el parent. También se pueden compilar por separado desde la carpeta de cada uno, con la restricción de haber compilado primero ms_common-core
El resultado de la compilación es un jar ejecutable

### Ejecución de servicios

Desde la carpeta de un servicio ejecutar mvn spring-boot:run

### Despliegue en servidores

El profile activo en los servidores (SPRING_PROFILES_ACTIVE) **no** debería ser **local**, de esta manera tomará como properties el archivo application.properties.

Los application.properties, como regla general no tendrán definidos los valores (salvo alguna excepción) sino que esperarán encontrarlos en variables de entorno del sistema operativo con los nombres indicados entre **llaves** (por ejemplo ${JWT_SECRET_KEY}), es decir que, **para que el servicio arranque es requisito que todas las variables de entorno que necesita, estén definidas**

### Docker

**Nota1**: el término host se refiere a la máquina donde corren los contenendores docker

**Nota2**: hay servicios que necesitan consumir otros servicios, por ejemplo searcher consume biller, para esto se debe crear una red de docker y agregar los contenedores a dicha red. Para crear la red:

docker create network myNetwork

**Nota3**: notar que dentro de la red creada, todos los servicios escuchan en el puerto 8080. Los puertos asignados (8081, 8082, etc) son para visibilidad en el host

**Nota4** El certificado generado debe incluir como Subject Alternative Name el nombre de cada uno de los servicios. Ver aparte ejemplo de generación de certificados

-   Como paso previo a la generación de una imagen de Docker se debe haber compilado el servicio
-   Para generar una imagen ejecutar lo siguiente **desde la carpeta del servicio**:

docker build -t ms-authenticator .

     - Notar el punto del final, que hace referencia a la carpeta actual
    - "ms-authenticator" es el nombre de la imagen a generar y que debería representar(o ser igual) al nombre del servicio. Reemplazar según cada servicio
     - La imagen se creará según las especificaciones del archivo Dockerfile ubicado en la carpeta de cada servicio.
     - Notar que los archivos Dockerfile de cada servicio son similares pero tienen algunas variables de entorno diferentes

-   Para iniciar el contenedor, ejecutar por lo siguiente **desde la carpeta del servicio**:

docker run --name ms-authenticator --network myNetwork -v C:\$listas\logs:/var/logs -v C:\$listas\cert:/var/cert -p 8081:8080 -e JWT_SECRET_KEY -e KEYSTORE_PASS -e KEYSTORE_ALIAS -e TRUSTSTORE_PASS ms-authenticator

     - --name ms-authenticator indica el nombre del container que, posteriormente, será utilizado como nombre de red para acceder desde otros containers
     - myNetwork hace referencia a la red creada (nota 2)
     - "C:\$listas\logs" se refiere a la carpeta del host donde se desean generar los logs y pude ser modificado según se desee
     - "8081" es el puerto del host donde se publicará el servicio. **Notar que cuando se levanten varios servicios, a cada uno se el deberá asignar un puerto diferente**
     - "8080" es el puerto en que incian los servicios dentro del contenedor. Al no disponer de la variable de
     entorno SPRING_PROFILES_ACTIVE=local utilizarán el archivo application.properties, que no especifica ningún
     puerto en particular, es decir usarán el puerto por defecto (8080)

     - "JWT_SECRET_KEY", "KEYSTORE_PASS", "KEYSTORE_ALIAS" son los  **nombres** (no el valor) de las variables de entorno del host que contienen los valores que necesita el servicio y por lo tanto se pasa al contenedor al generar la imagen

     - "ms-authenticator" es el nombre de la imagen a iniciar

### Generación de certificados y almacenes de confianza

Certificado:

keytool -genkeypair -keyalg RSA -keysize 2048 -alias systech -dname "CN=systech,C=AR" -ext "SAN:c=DNS:localhost,IP:127.0.0.1,DNS:ms-authenticator,DNS:ms-retriever,DNS:ms-searcher,DNS:ms-biller,DNS:ms-ingestor,DNS:ms-screener,DNS:ms-screener-searcher" -validity 3650 -keystore systech.p12 -storepass changeit -keypass changeit -deststoretype pkcs12

Truststore:

keytool -exportcert -keystore systech.p12 -alias systech -file cert.cer
keytool -import -file cert.cer -alias systech -keystore truststore.p12 -deststoretype pkcs12

Recordar que se deben usar los mismos certificados en Vigia y en cada MSs. De lo contrario, podrian haber errores de certificado y firmas.

### Postman

Cada servicio tiene, dentro de la carpeta src/test/resources una colección de postman para poder probarlo

### Swagger

Cada servicio tiene su autodocumentación de endpoints en la ruta /swagger-ui/index.html#/

### Actuator

Cada servicio tiene su health check en /actuator/health
