package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Prueba con la campana Black Friday activa (usa otra base en memoria
// para no chocar con la de las demas pruebas)
@SpringBootTest(properties = {
    "promo.black-friday.activa=true",
    "spring.datasource.url=jdbc:h2:mem:blackfridaydb;MODE=LEGACY"
})
@Transactional
class BlackFridayTest {

    @Autowired
    private GestorPedidos gestor;

    @Test
    void campanaBlackFriday() {
        PedidoRequest request = new PedidoRequest(4L, "cliente@correo.com",
            List.of(new ItemPedido(1L, 1)));
        ResultadoPedido r = gestor.procesarPedido(request); // subtotal 100000, descuento 25%
        assertTrue(r.isConfirmado());
        assertEquals(89250.0, r.getTotal(), 0.01);
    }
}
