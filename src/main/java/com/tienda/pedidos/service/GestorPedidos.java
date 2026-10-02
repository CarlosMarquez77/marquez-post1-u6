package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.PromocionBlackFriday;
import com.tienda.pedidos.validacion.PromocionCorporativo;
import com.tienda.pedidos.validacion.PromocionVolumen;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {
    private final ValidadorPedido primerValidador;
    private final SelectorEstrategiaDescuento selector;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;

    public GestorPedidos(ValidadorStock stock, ValidadorCliente cliente,
                          PromocionBlackFriday blackFriday, PromocionCorporativo corporativo,
                          PromocionVolumen volumen, SelectorEstrategiaDescuento selector,
                          PedidoRepository repository, NotificacionPedidoService notificacion) {
        // encadenar() devuelve el siguiente eslabon, por eso el inicio de la cadena es stock
        stock.encadenar(cliente)
            .encadenar(blackFriday).encadenar(corporativo).encadenar(volumen);
        this.primerValidador = stock;
        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) return ResultadoPedido.rechazado(contexto.getMotivoRechazo());

        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        double descuentoTipoCliente = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double descuento = Math.max(descuentoTipoCliente, contexto.getDescuentoCampana());
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (ItemPedido item : request.getItems()) {
            subtotal += repository.obtenerPrecio(item.getProductoId()) * item.getCantidad();
        }
        return subtotal;
    }
}
