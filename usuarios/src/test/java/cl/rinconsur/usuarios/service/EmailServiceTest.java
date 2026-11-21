package cl.rinconsur.usuarios.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void enviarContrasenaTemporal_configurado_enviaCorreoConDatosCorrectos() {
        EmailService emailService = new EmailService(mailSender, "rinconsur@gmail.com");

        emailService.enviarContrasenaTemporal("ana@duoc.cl", "Ana Pérez", "111111111", "Temp123*");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage enviado = captor.getValue();
        assertEquals("rinconsur@gmail.com", enviado.getFrom());
        assertArrayEquals(new String[]{"ana@duoc.cl"}, enviado.getTo());
        assertNotNull(enviado.getText());
        assertTrue(enviado.getText().contains("Ana Pérez"));
        assertTrue(enviado.getText().contains("111111111"));
        assertTrue(enviado.getText().contains("Temp123*"));
    }

    @Test
    void enviarContrasenaTemporal_sinRemitente_lanzaInternalServerError() {
        EmailService emailService = new EmailService(mailSender, "");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.enviarContrasenaTemporal("ana@duoc.cl", "Ana", "111111111", "Temp123*")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void enviarContrasenaTemporal_errorSmtp_lanzaBadGateway() {
        EmailService emailService = new EmailService(mailSender, "rinconsur@gmail.com");
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.enviarContrasenaTemporal("ana@duoc.cl", "Ana", "111111111", "Temp123*")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }
}