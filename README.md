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
GestorPedidos.procesarPedido() mezcla 6 responsabilidades (validación
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

## Cómo ejecutar
```
$ mvn spring-boot:run
$ mvn test
```

## Herramientas utilizadas
- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code / IntelliJ IDEA, Git, GitHub
