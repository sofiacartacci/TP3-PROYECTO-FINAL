# Core

### Reference Documentation
Librería de uso general para listas

### Calculador de costo de una búsqueda individual

Se buscó un cálculo que no necesite acceder a datos cada vez que se ejecute y de esta manera sea muy rápido.

Se estima que el costo de una consulta es proporcional a la cantidad de registros que tiene que analizar
La cantidad de registros a analizar, si desconsideramos otros filtros para simplificar es, (aproximadamente) proporcional a la cantidad de días trascurridos
entre la fecha actual y el valor del parámetro updatedAfter

El costo de una búsqueda es un entero entre 1 y 120

 - El costo es 1 por cada 30 días entre la fecha actual y la fecha updatedAfter, (que debería coincidir con la del ultimo cruce)
 - Si la cantidad de meses es mayor a 120, el costo es 120
 - Si la cantidad de meses da negativa (porque updatedAfter es futuro, es decir está mal) el costo es 1
 
 
### Test unitarios

La clase MatcherTest es de especial importancia porque en la misma se están verificando los resultados del agloritmo de cruce. En principio se incluyeron unos pocos casos con la intención de que sean tomados como ejemplo para hacer un testing bien completo.
