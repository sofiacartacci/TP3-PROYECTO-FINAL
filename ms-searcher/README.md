# Searcher

### Reference Documentation
Servicio que busca en listas a partir de un texto y varios parámetros. Luego calcula e imputa al usuario el costo de la búsqueda invocando al servicio Biller

Hay un proceso que detecta si los índices fueron modificados y, en ese caso, los reabre. Ese proceso está programado para correr con la frecuencia determinada por la expresión cron de matcher.look-for-changes-in-index.cron (${MATCHER_LOOK_FOR_CHANGES_IN_INDEX_CRON})


### Detalle de parámetros


 - text: texto (nombre o documento) a buscar
 - minLevel: nivel de coincidencia mínimo a devolver (0-100)
 - updatedAfter: solo devolver registros que fueron actualizados después de esa fecha
 - includedCategories: lista de categorías a considerar
 - includedCategories: lista de subcategorías a considerar
 - includedKeywords:  lista de keywords a considerar
 - excludedCategories: lista de categorías a excluir de la búsqueda
 - excludedSubCategories : lista de subcategorías a excluir de la búsqueda
 - excludedKeywords: lista de keywords a excluir de la búsqueda
 - includeIfDeprecatedAfter: si el registro fue dado de bajo, incluirlo solamente si la fecha de baja es posterior a este valor
 - searchAliases: buscar también en aliases
 - showDetails: mostrar detalles en los resultados. Si este valor es false, solo se devuelven los ui de cada coincidencia (lo cual además es más rápido)



### Health check: 


* [https://localhost:8083/actuator](https://localhost:8083/actuator)

### Swagger: 
* [https://localhost:8083/v3/api-docs/](https://localhost:8083/v3/api-docs/)
* [https://localhost:8083/swagger-ui/index.html#/](https://localhost:8083/swagger-ui/index.html#/)

### Base de datos
Los datos de leen de la collection systechlist.listas

### Indices
Requiere de los índices generados por el servicio Ingestor



