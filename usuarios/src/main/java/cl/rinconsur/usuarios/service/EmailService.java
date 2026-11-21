package cl.rinconsur.usuarios.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String remitente
    ) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviarContrasenaTemporal(
            String destinatario,
            String nombreCompleto,
            String rut,
            String contrasenaTemporal
    ) {
        if (remitente == null || remitente.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "El servicio de correo no está configurado en el servidor (falta MAIL_USERNAME)"
            );
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject("Rincón Sur - Credenciales de acceso temporal");
            mensaje.setText(
                    "Hola " + nombreCompleto + ",\n\n"
                    + "Se ha creado tu cuenta en la aplicación Rincón Sur.\n\n"
                    + "Tus credenciales temporales de ingreso son:\n"
                    + "- RUT: " + rut + "\n"
                    + "- Contraseña temporal: " + contrasenaTemporal + "\n\n"
                    + "Al iniciar sesión por primera vez, la aplicación te pedirá configurar tu pregunta de seguridad y establecer una contraseña definitiva.\n\n"
                    + "Si no reconoces este correo, comunícate con un Administrador."
            );

            mailSender.send(mensaje);
        } catch (MailException ex) {
            log.error("Error enviando correo temporal a {}: {}", destinatario, ex.getMessage(), ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible enviar el correo con la contraseña temporal. Verifica la dirección de correo o intenta nuevamente."
            );
        }
    }
}