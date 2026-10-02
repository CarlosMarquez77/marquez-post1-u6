package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Pedidos de prueba. Cada prueba se revierte al terminar (@Transactional)
// para que el stock y los pedidos previos no cambien entre pruebas.
@SpringBootTest
@Transactional
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestor;

    private ResultadoPedido procesar(Long clienteId, Long productoId, int cantidad) {
        PedidoRequest request = new PedidoRequest(clienteId, "cliente@correo.com",
            List.of(new ItemPedido(productoId, cantidad)));
        return gestor.procesarPedido(request);
    }

    // Ejecuta el pedido simulando la hora indicada, para probar el horario de corte
    private ResultadoPedido procesarALas(LocalTime hora, Long clienteId, Long productoId, int cantidad) {
        try (MockedStatic<LocalTime> reloj = Mockito.mockStatic(LocalTime.class, Mockito.CALLS_REAL_METHODS)) {
            reloj.when(LocalTime::now).thenReturn(hora);
            return procesar(clienteId, productoId, cantidad);
        }
    }

    @Test
    void stockInsuficiente() {
        ResultadoPedido r = procesar(4L, 4L, 5); // solo hay 2 portatiles
        assertFalse(r.isConfirmado());
        assertEquals("Stock insuficiente: producto 4", r.getMotivoRechazo());
    }

    @Test
    void clienteMorosoDentroDelHorarioDeCorte() {
        ResultadoPedido r = procesarALas(LocalTime.of(10, 0), 3L, 3L, 2);
        assertFalse(r.isConfirmado());
        assertEquals("Cliente con deuda pendiente: $150000.0", r.getMotivoRechazo());
    }

    @Test
    void clienteMorosoFueraDelHorarioDeCorte() {
        ResultadoPedido r = procesarALas(LocalTime.of(21, 0), 3L, 3L, 2);
        assertTrue(r.isConfirmado());
        assertEquals(11900.0, r.getTotal(), 0.01); // 10000 + 19% de impuesto
    }

    @Test
    void clienteNoRegistrado() {
        ResultadoPedido r = procesar(5L, 1L, 1);
        assertFalse(r.isConfirmado());
        assertEquals("Cliente no registrado", r.getMotivoRechazo());
    }

    @Test
    void descuentoVip() {
        ResultadoPedido r = procesar(1L, 2L, 5); // subtotal 1250000, descuento 15%
        assertTrue(r.isConfirmado());
        assertEquals(1264375.0, r.getTotal(), 0.01);
    }

    @Test
    void descuentoFrecuente() {
        ResultadoPedido r = procesar(2L, 1L, 2); // subtotal 200000, 11 pedidos previos, descuento 8%
        assertTrue(r.isConfirmado());
        assertEquals(218960.0, r.getTotal(), 0.01);
    }
}
