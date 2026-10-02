INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (1, 'Valentina Rojas', 'VIP');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (2, 'Andres Perez', 'FRECUENTE');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (3, 'Jorge Castro', 'MOROSO');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (4, 'Camila Duarte', 'ESTANDAR');
INSERT INTO clientes (id, nombre, tipo_cliente) VALUES (5, 'Cliente sin registrar', NULL);

-- deuda pendiente del cliente moroso: 150000
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 100000, FALSE);
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 50000, FALSE);

INSERT INTO productos (id, nombre, precio) VALUES (1, 'Teclado', 100000);
INSERT INTO productos (id, nombre, precio) VALUES (2, 'Monitor', 250000);
INSERT INTO productos (id, nombre, precio) VALUES (3, 'Cuaderno', 5000);
INSERT INTO productos (id, nombre, precio) VALUES (4, 'Portatil', 400000);

INSERT INTO inventario (producto_id, stock) VALUES (1, 50);
INSERT INTO inventario (producto_id, stock) VALUES (2, 10);
INSERT INTO inventario (producto_id, stock) VALUES (3, 200);
INSERT INTO inventario (producto_id, stock) VALUES (4, 2);

-- 11 pedidos previos del cliente frecuente (para el descuento del 8%)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
SELECT 2, 10000, 0, 1900, 11900, TIMESTAMP '2026-01-15 10:00:00', 'CONFIRMADO' FROM SYSTEM_RANGE(1, 11);
