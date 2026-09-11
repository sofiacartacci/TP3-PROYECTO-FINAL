# Ingestor

Servicio que descarga listas, las descomprime, detecta cambios, carga en base de datos y actualiza índices
El servicio se compone de un job con 4 steps:

-   Download
-   Unzip
-   Fastload
-   Merge

Se proveen endpoints para iniciar y detener job, consultar estado de job y steps y consultar estado de listas cargadas en último proceso

El servicio funciona con una única lista, pero el sistema como conjunto soporta múltiples listas.
Para trabajar con múltiples listas se debe crear una instancia del servicio ingestor para cada lista. Por ejemplo, si se desea trabajar con worldcheck y una lista propia, se deben crear dos servicios ingestor, uno configurado para WC y otro para la lista propia.
Ambos ingestores trabajarán sobre la misma base de datos e índices, sin interferir uno con el otro. Sin embargo sí sería recomendable configurarlos para que procesen en diferentes momentos del día

### Formato del archivo de listas

Las listas deben ser provistas en un archivo de texto delimitado (csv) comprimido dentro de un zip o gzip y respetando los siguientes requisitos:

-   El primer registro corresponde al encabezado con los nombres de los campos
-   El delimitador de campos debe ser un carcter único como por ejemplo tab, "|", "," o ";". Por ejemplo WC usa un tab
-   Los campos que contienen listas o arreglos de datos, como por ejemplo aliases, alt spellings, ids, countries, passports, etc deben separar cada elemento con un separador común a todo el archivo. Por ejemplo WC usa ";"
-   Hay campos que son obligatorios tales como UI o updated (ver aparte consideraciones sobre este último)
-   Cada UI debe ser un identificador único de cada persona y nunca, jamás en la existencia de la lista deberá reutilizarse para otra
-   Cada campo de fecha puede tener su propio formato, pero este formato debe respetarse para todos los registros

### Campo Updated

Gran parte de la lógica que hace que el proceso sea rápido se basa en este campo. El mismo debe cumplir los siguientes requisitos:

-   El campo se requiere siempre, si viene vacío se tomará el valor del campo "entered", si este último también está vacío se considerará un error
-   Los cambios de fecha de este campo deben ser cronológicamente coherentes, es decir que siempre que se modifique, deberá ser para una fecha posterior a la que tenía anteriormente.
-   Los valores de este campo deben se coherentes con la fecha de publicación de la lista. Si para una fecha de publicación dada, el máximo valor
    de este campo (para todos los registros) fue X, en publicaciones posteriores nunca podrá cambiarse el valor de este campo (para ningún registro) por una fecha anterior a X
-   El servicio confía en la integridad de este campo y lo utilizará para detectar cambios. Es decir que solo se considerará que un registro fue modificado cuando cambia el valor del campo "updated".

El proceso cargará solamente los registros cuya fecha updated sea posterior a la última fecha updated cargada para esa lista. Los registros cuya fecha updated no cumpla con estas características no serán considerados como modificados

Dados los requisitos anteriores, si bien se supone que los proveedores de listas deben cumplir con esas caracterísiticas, es algo difícil de asegurar. El servicio provee un mecanismo para mitigar el riesgo de no detectar un cambio, esto se logra mediante un parámetro que indica correr el proceso comparando todos los valores de este campo, en lugar de buscar solo los posteriores al último.
En esa modalidad, el proceso demorará más tiempo.

La configuración recomendada sería correr el proceso normalmente (comparing=false) todos los días y, una vez al mes correrlo en modo de comparar cada fecha (comparing=true).

### Parámetros

-   batch.size: indica el tamaño de lote, o commitcount

-   batch.cron.nocompare: expresión corn que indica la periodicidad de ejecución automática del proceso sin comparar fechas (default). Por ejemplo 0 0 0 2-31 \* \*, indica las 00:00:00 de todos los días excepto el primero de mes
-   batch.cron.comparing: expresión corn que indica la periodicidad de ejecución automática del proceso comparando fechas.Por ejemplo 0 0 0 1 \* \* indica todos los primeros de mes a las 00:00:00

-   download.buffersize: tamaño del buffer de descarga
-   download.timeout: timeout en milisegundos para descargas

-   index.folder: ruta a carpeta de índices
-   matcher.stopwords-file: ruta al archivo stopwords

-   mapping.id: identificador del proveedor de listas (se recomienda utilizar dos letras mayúsculas)
-   mapping.src-url: url de descarga de la lista. La misma puede incluir usuario y password
-   mapping.working-dir: ruta a carpeta de trabajo a utilizar. En la misma se realizarán las descargas y se descomprimirán los archivos
-   mapping.file-name: nombre del archivo a utilizar dentro del .zip. Por ejemplo para la slistas Systech sería TB_WC_RECORDS.txt. Para WC, que viene el formato gzip, se recomienda utilizar el id del proveedor, por ejemplo WC.csv
-   mapping.name: nombre del mapeo, por ejemplo "world-check"
-   mapping.delimiter: caracter delimitador de campos (para tab usar \t)
-   mapping.quote: caracter englobador de textos si es que se utiliza alguno, por ejemplo "
-   mapping.null: indicador de null o vacío, por ejemplo en WC se usa "-"
-   mapping.separator: caracter separador de listas o arreglos, por ejemplo en WC se usa ";"
-   mapping.errors.ignore=si es true, se ignorarán errores hasta una cantidad máxima especificada por el parámetro mapping.errors.max
-   mapping.errors.max: cantidad máxima de errores tolerada cuando mapping.errors.ignore es true
-   mapping.save.history: indica con true o false si se debe guardar en la base de datos las versiones anteriores de los registros modificados
-   mapping.fields: Lista de campos del archivo y su correspondencia con los campos del modelo. El nombre de l aizquierda es el nombre del campo en el modelo del servicio,
    el nombre de la derecha es el nombre del campo en el archivo. Los campos fecha permiten indicar el formato mediante :formato:idioma, por ejemplo si
    el campo entered tuviera valores como
    Apr, 29 2022 00:00
    se podría configurar como  
    entered:entered:MMM dd yyyy hhmm:ENGLISH

Ejemplo: ver application-local.properties

### Colecciones

El servicio utiliza las siguientes colecciones:

-   listas: tiene el contenido de las listas cargadas
-   providers: detalla la última fecha updated de cada proveedor de listas, cantidad de registros y detalle de errores
-   updates: contiene la fecha updated para cada UI. Esto se registra por separado para mejorar el tiempo de lectura evitando leerlo de "listas"

### Health check:

-   [https://localhost:8086/actuator](https://localhost:8086/actuator)

### Swagger:

-   [https://localhost:8086/v3/api-docs/](https://localhost:8086/v3/api-docs/)
-   [https://localhost:8086/swagger-ui/index.html#/](https://localhost:8086/swagger-ui/index.html#/)

### Base de datos

Los datos de guardan en la collection systechlist.ingestor
