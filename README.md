# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño
de Software — Sexto Semestre. Un único proyecto Spring Boot
(pedidos-service/) con dos partes: diagnóstico y refactorización de
un antipatrón combinado en GestorPedidos, y diagnóstico y corrección
de un segundo antipatrón introducido al hacer crecer el mismo
proyecto con tres campañas de descuento.

## Decisiones de diseño

### Parte 1 — GestorPedidos
**Antipatrón identificado:** God Object y Spaghetti Code combinados.
GestorPedidos.procesarPedido() mezclaba 6 responsabilidades (validación
de stock, validación de cliente/mora con hasta 3 niveles de
anidamiento, cálculo de subtotal, cálculo de descuento, persistencia
vía JDBC embebido y notificación) en un único método de más de 100
líneas.

Evidencia (líneas de GestorPedidos.java en el primer commit, el código
original):

- Líneas 31–44: validación de stock, con un `SELECT stock FROM inventario`
  dentro del `for`.
- Líneas 46–65: validación de cliente y mora. Llega a 3 niveles de
  anidamiento: `else if (tipoCliente.equals("MOROSO"))` (línea 52),
  `if (deudaPendiente != null && deudaPendiente > 0)` (línea 56) e
  `if (ahora.isBefore(LocalTime.of(20, 0)))` (línea 58).
- Líneas 67–73: cálculo del subtotal, con una consulta SQL por cada ítem.
- Líneas 75–93: cálculo del descuento con `if/else` anidados por tipo de
  cliente y por monto (2 niveles), y una consulta SQL dentro de la rama
  FRECUENTE (línea 86).
- Líneas 98–113: persistencia con JDBC directo (`INSERT` del pedido,
  `CALL IDENTITY()`, `INSERT` del detalle y `UPDATE` del inventario).
- Líneas 115–129: armado del correo con `StringBuilder` y envío.

La clase tiene 6 razones distintas para cambiar (God Object) y el método
trabaja al mismo tiempo con SQL, reglas de negocio y formato de texto,
con 4 `return` intercalados en medio de la lógica (Spaghetti Code).
Para agregar un tipo de cliente nuevo habría que meter otro `else if`
entre las líneas 77 y 93 y revisar también las líneas 46–65, porque la
misma variable `tipoCliente` decide la validación de mora.

**Patrón aplicado:** Chain of Responsibility para las validaciones
(dependencia real de orden y corte anticipado) y Strategy para el
cálculo de descuento por tipo de cliente (sin dependencia de orden).
Alternativa descartada: una lista de predicados booleanos para las
validaciones, sin corte anticipado real.

- Se eligió Chain of Responsibility porque si ValidadorStock rechaza el
  pedido, ValidadorCliente no debe ejecutarse. Con un `validarTodo()`
  sobre una lista de `Predicate<ContextoPedido>` se evaluarían todos los
  predicados aunque el primero ya hubiera fallado.
- Se eligió Strategy para el descuento, y no otro eslabón de la cadena,
  porque siempre se aplica una sola regla según el tipo de cliente, sin
  orden ni corte. SelectorEstrategiaDescuento la elige con un `Map` y
  reemplaza el `if/else` anidado.
- La persistencia quedó en PedidoRepository y el correo en
  NotificacionPedidoService. GestorPedidos solo coordina las cuatro
  partes, y los pedidos de prueba (GestorPedidosTest) dan la misma
  salida que con el GestorPedidos original.

### Parte 2 — Crecimiento del proyecto
**Antipatrón identificado:** Golden Hammer. Las tres campañas de
descuento (Black Friday, Corporativo, Volumen) se implementaron como
eslabones adicionales de la cadena de validación existente, aunque
no tenían ninguna dependencia de orden entre sí ni necesidad de corte
anticipado — la propiedad que sí justificaba la cadena en
ValidadorStock y ValidadorCliente. Se reutilizó Chain of
Responsibility porque "ya funcionó" en la Parte 1, sin evaluar si
correspondía al nuevo problema.

Evidencia:

- PromocionBlackFriday, PromocionCorporativo y PromocionVolumen
  extienden ValidadorPedido, pero ninguna llama a `contexto.rechazar(...)`.
  Solo llaman a `contexto.aplicarDescuentoCampana(...)`, es decir, no
  validan nada.
- No hay dependencia de orden: ejecutar PromocionVolumen antes o después
  de PromocionCorporativo da el mismo resultado, porque
  `aplicarDescuentoCampana` se queda con el mayor valor. En cambio,
  ValidadorStock sí debe ir antes que ValidadorCliente.
- Para que los eslabones se coordinaran se agregó el campo mutable
  `descuentoCampana` a ContextoPedido. Si dos campañas tuvieran que
  sumarse en vez de competir, habría que cambiar ese método compartido y
  el resultado dependería de qué eslabón escribe primero.
- GestorPedidos encadena 5 eslabones y después hace
  `Math.max(descuentoTipoCliente, contexto.getDescuentoCampana())`: el
  descuento quedó repartido entre la cadena y el Strategy.

**Patrón aplicado:** Strategy, extendiendo SelectorEstrategiaDescuento
con CalculadorDescuentoFinal. Los tres eslabones mal aplicados y el
campo descuentoCampana se eliminaron del código (no se comentaron,
para no dejar un Lava Flow) y su historial queda documentado
únicamente en los commits de este repositorio.

- Las campañas se modelaron como EstrategiaDescuento porque, igual que
  DescuentoVip y DescuentoFrecuente, calculan un porcentaje sin depender
  de un orden ni cortar el flujo del pedido. Mantenerlas en la cadena se
  descartó porque era la causa del antipatrón.
- CalculadorDescuentoFinal toma el mayor entre el descuento por tipo de
  cliente y el de las campañas, la misma regla que antes pero en un solo
  lugar.
- Las pruebas de las campañas (GestorPedidosTest y BlackFridayTest) dan
  los mismos totales con los eslabones y con las estrategias.

## Cómo ejecutar
```
$ mvn spring-boot:run
$ mvn test
```

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code / IntelliJ IDEA, Git, GitHub

## Conclusiones
Antes de aplicar un patrón hay que diagnosticar con evidencia: al
revisar las líneas de GestorPedidos se vio que había dos problemas
juntos y que cada responsabilidad pedía una solución distinta, una
cadena para las validaciones y una estrategia para los descuentos. Las
pruebas escritas antes de refactorizar sirvieron para confirmar que la
salida no cambió. En la Parte 2 aprendí que conocer un patrón no
garantiza usarlo bien: la cadena había funcionado y por eso se usó para
las campañas, que no validan nada ni dependen de un orden. Corregirlo
fue sencillo porque el Strategy ya existía, y el código descartado se
borró en vez de dejarlo comentado.
