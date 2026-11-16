package cl.prospin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import cl.prospin.model.Boleta;
import cl.prospin.model.DetalleBoleta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Correos de ProSpin por SMTP (Gmail con contraseña de aplicación).
 * Si el envío falla, solo se registra en el log: el inicio de sesión y el pago siguen funcionando.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean habilitado;
    private final String remitente;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.enabled:false}") boolean habilitado,
                       @Value("${spring.mail.username:}") String remitente) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remitente = remitente;
    }

    public void avisoInicioSesion(String para, LocalDateTime cuando) {
        enviar(para, "ProSpin: se inició sesión en tu cuenta",
                "Hola,\n\nSe inició sesión en tu cuenta de ProSpin el " + cuando.format(FORMATO) + ".\n\n"
                + "Si no fuiste tú, cambia tu contraseña.\n\nProSpin Tenis de Mesa");
    }

    public void confirmacionCompra(String para, Long boletaId, Integer total) {
        enviar(para, "ProSpin: pedido realizado (boleta N° " + boletaId + ")",
                "Hola,\n\nTu pedido fue pagado con Webpay.\n\n"
                + "Número de boleta: " + boletaId + "\n"
                + "Total pagado: $" + String.format("%,d", total).replace(',', '.') + " CLP\n\n"
                + "Puedes revisar el detalle en la sección Pedidos de la tienda.\n\nProSpin Tenis de Mesa");
    }

    // Boleta pagada: incluye cada producto, cantidad y subtotal
    public void boletaPagada(Boleta boleta) {
        StringBuilder lineas = new StringBuilder();
        for (DetalleBoleta d : boleta.getDetalles()) {
            lineas.append("- ").append(d.getProducto().getNombre())
                  .append(" x ").append(d.getCantidad())
                  .append(" = ").append(formato(d.getSubtotal())).append("\n");
        }
        enviar(boleta.getUsuario().getEmail(), "ProSpin: boleta N° " + boleta.getId() + " pagada",
                "Hola,\n\nTu pago fue aprobado por Webpay. Este es el detalle de tu compra:\n\n"
                + lineas + "\nTotal pagado: " + formato(boleta.getTotal()) + " CLP\n"
                + "Fecha: " + boleta.getFecha().format(FORMATO) + "\n\n"
                + "Gracias por comprar en ProSpin Tenis de Mesa.");
    }

    private String formato(Integer monto) {
        return "$" + String.format("%,d", monto).replace(',', '.');
    }

    private void enviar(String para, String asunto, String texto) {
        if (!habilitado) {
            log.info("Correo desactivado (app.mail.enabled=false): {}", asunto);
            return;
        }
        JavaMailSender servidor = mailSender.getIfAvailable();
        if (servidor == null) {
            log.warn("Correo no enviado: falta configurar spring.mail.*");
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            servidor.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        } catch (RuntimeException e) {
            // Cualquier otro error del correo no debe afectar al pago ni al inicio de sesión
            log.warn("Error inesperado al enviar correo a {}: {}", para, e.getMessage());
        }
    }
}
