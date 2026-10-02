package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

// Imprime el correo en consola para no depender de un servidor SMTP
@Service
public class EmailServiceConsola implements EmailService {

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println(cuerpo);
    }
}
