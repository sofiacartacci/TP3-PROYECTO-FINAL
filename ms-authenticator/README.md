# Authenticator

### Reference Documentation
Servicio que permite autenticar a un usuario y devolver un jwt y su refresh token para proveer autorización para otros servicios
Permite además generar un jwt en modo APIKey para consumo m2m (machine to machine) en el que, al crearlo se debe especificar la fecha de vencimiento. Para crear una APIKey se requiere la authority "admin"


### Health check: 


* [https://localhost:8081/actuator](https://localhost:8081/actuator)

### Swagger: 
* [https://localhost:8081/v3/api-docs/](https://localhost:8081/v3/api-docs/)
* [https://localhost:8081/swagger-ui/index.html#/](https://localhost:8081/swagger-ui/index.html#/)

### Base de datos
Los datos de autenticación se guardan en la collection "users"
Las passwords están encriptadas con bcrypt
Las authorities se guardan en un array y pueden tener cualquier valor que se desee utilizar. El único valor reservado es "admin" ya que es la auhtority necesaria para administrar el servicio.

La collection refreshtoken, como su nombre lo indica, es utilizada para guardar los refreshtoken que se generan con cada login exitoso

### Inicialización
Cuando el servicio se conecte a una base sin la collection "users" (o con la misma vacía) dará de alta un registro con el usuario y password del primer llamado al endpoint /login. Ese usuario tendrá además la authority "admin" que lo habilitará a dar de alta otros usuarios desde el endpoint /save.


