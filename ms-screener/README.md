# Screener

Servicio que cruza un lote de personas contra las listas


Este servicio se apoya fuertemente en el servicio ms-screener-searcher. Este último debe ser ejecutado en varias instancias paralelas (con procesadores separados) y es la clave para la buena performance del screener. Se recomienda configurar por lo menos 8 pods, 16 sería mucho mejor, pero es importante que realmente usen procesadores independientes, de lo contrario no solo no se ganará performance sino que se podría degradar.

La variable de entorno SCREENER_SEARCHER_FORK_SIZE hace referencia a cuántas llamadas paralelas se harán al screener-searcher. El valor debe ser similar a la cantidad de instancias disponibles.

Screener procesa de a un archivo a la vez mientrs que el resto que reciba quedarán encolados y se procesarán después. 
El proceso de un archivo consiste en armar lotes de búsquedas del tamaño especificado en la variable BATCH_SIZE y generar SCREENER_SEARCHER_FORK_SIZE hilos de ejecución paralela. Cada uno de estos hilos envía un lote al screener-searcher. 
 
### Formato del archivo de entrada
Archivo en formato gzip (.gz). Adentro contiene un archivo .csv delimitado por tabuladores con las siguientes columnas:

El primer registro debe contener los nombres de las columnas

 - UID: identificador único de la persona
 - NAME: nombre completo
 - ID1: número de documento 1
 - ID2: número de documento 2	
 - ID3: número de documento 3
 - ID4: número de documento 4
 - DOB: fecha de nacimiento en formato yyyy-MM-dd (previsto para funcionalidades futuras, ahora no se usa y pude quedar vacío)
 - SCREENED: fecha de último cruce en formato yyyy-MM-dd. Esta fecha hace que solo se cruce contra novedades posteriores y es clave para la velocidad de procesamiento

Notar que el endpoint /screen se espera recibir un request multipart donde una parte es el archivo mencionado previamente y la otra es un json con los filtros  de búsqueda

### Formato del archivo de salida
Archivo en formato gzip (.gz). Adentro contiene un archivo .csv delimitado por tabuladores con las siguientes columnas:

 - 1) UID recibido en archivo de entrada
 - 2) Cantidad de coincidencias 
 - 3) Si cantidad de coincidencias=0 queda vacío. Si no, detalle de coinciencias en formato json (inline). El detalle contiene:


 - text: texto buscado (nombre o documentos)
 - coincs: array de coincidencias, para cada una se especifica: ranking, level, ui y tipo:

   - ranking: posición según nivel de coincidencia
   - level: nivel de coincidencia 0 a 100
   - ui: identificador del registro (para obtener con ms-retriever)
   - tipo: tipo de dato que coincidió: nam (nombre), als (alias), ass (alternative spelling), ids (ids)



El primer registro NO tiene nombres de columnas, es decir, no tienen nombre

Los lotes paralelos son procesados en simultáneo pero no se puede garantizar el orden de finalización, por lo que el archivo con el resultado no mantendrá el orden del archivo original.
 
### Colecciones
El servicio utiliza las siguientes colecciones:
screener
 

### Health check: 
* [https://localhost:8087/actuator](https://localhost:8087/actuator)

### Swagger: 
* [https://localhost:8087/v3/api-docs/](https://localhost:8087/v3/api-docs/)
* [https://localhost:8087/swagger-ui/index.html#/](https://localhost:8087/swagger-ui/index.html#/)





