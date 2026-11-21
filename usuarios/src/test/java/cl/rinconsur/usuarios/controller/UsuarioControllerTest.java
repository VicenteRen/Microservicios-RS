package cl.rinconsur.usuarios.controller;

import cl.rinconsur.usuarios.model.Usuario;
import cl.rinconsur.usuarios.service.AvatarStorageService;
import cl.rinconsur.usuarios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;
    @Mock
    private AvatarStorageService avatarStorageService;

    private UsuarioController controller;
    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        controller = new UsuarioController(service, avatarStorageService);
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1L);
        usuarioEjemplo.setRut("214297353");
        usuarioEjemplo.setNombreCompleto("Vicente Ponce");
        usuarioEjemplo.setCorreo("v.ponce@rinconsur.cl");
        usuarioEjemplo.setTelefono("987654321");
    }

    @Test
    void listarYBuscarPorId_devuelven200() {
        when(service.listarActivos()).thenReturn(List.of(usuarioEjemplo));
        when(service.buscarPorId(1L)).thenReturn(usuarioEjemplo);

        assertEquals(HttpStatus.OK, controller.listarActivos().getStatusCode());
        assertEquals(1, controller.listarActivos().getBody().size());
        assertEquals(HttpStatus.OK, controller.buscarPorId(1L).getStatusCode());
    }

    @Test
    void guardar_devuelve201Created() {
        when(service.guardar(usuarioEjemplo)).thenReturn(usuarioEjemplo);

        ResponseEntity<Usuario> resp = controller.guardar(usuarioEjemplo);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(usuarioEjemplo, resp.getBody());
    }

    @Test
    void login_okYBadRequest() {
        when(service.autenticar("214297353", "Clave1*")).thenReturn(usuarioEjemplo);

        ResponseEntity<Usuario> resp = controller.login(Map.of("rut", "214297353", "contrasena", "Clave1*"));
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        assertThrows(ResponseStatusException.class, () -> controller.login(Map.of("rut", "")));
    }

    @Test
    void actualizarPerfil_okYBadRequest() {
        when(service.actualizarPerfil(1L, "Vicente", "a@b.cl", "912345678")).thenReturn(usuarioEjemplo);

        ResponseEntity<Usuario> resp = controller.actualizarPerfil(1L, Map.of(
                "nombreCompleto", "Vicente", "correo", "a@b.cl", "telefono", "912345678"));
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        assertThrows(ResponseStatusException.class, () -> controller.actualizarPerfil(1L, Map.of("correo", "")));
    }

    @Test
    void cambiarContrasenaDesdePerfil_okYBadRequest() {
        when(service.cambiarContrasenaDesdePerfil(1L, "Vieja1*", "Nueva1*")).thenReturn(usuarioEjemplo);

        ResponseEntity<Usuario> resp = controller.cambiarContrasenaDesdePerfil(1L,
                Map.of("contrasenaActual", "Vieja1*", "nuevaContrasena", "Nueva1*"));
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        assertThrows(ResponseStatusException.class, () -> controller.cambiarContrasenaDesdePerfil(1L, null));
    }

    @Test
    void actualizarDesdeAdmin_okYErrores() {
        when(service.actualizarDesdeAdmin(1L, "a@b.cl", "912345678", 2L)).thenReturn(usuarioEjemplo);

        ResponseEntity<Usuario> resp = controller.actualizarDesdeAdmin(1L,
                Map.of("correo", "a@b.cl", "telefono", "912345678", "rolId", 2));
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        assertThrows(ResponseStatusException.class, () -> controller.actualizarDesdeAdmin(1L,
                Map.of("correo", "a@b.cl", "telefono", "912345678", "rolId", "no-numero")));
        assertThrows(ResponseStatusException.class, () -> controller.actualizarDesdeAdmin(1L, null));
    }

    @Test
    void verificarIdentidadYRecuperarContrasena() {
        when(service.verificarIdentidadRecuperacion("214297353", "p", "r")).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.verificarIdentidad(
                Map.of("rut", "214297353", "preguntaSeguridad", "p", "respuestaSeguridad", "r")).getStatusCode());

        when(service.verificarIdentidadRecuperacion("214297353", "p", "mal")).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.verificarIdentidad(
                Map.of("rut", "214297353", "preguntaSeguridad", "p", "respuestaSeguridad", "mal")).getStatusCode());

        assertThrows(ResponseStatusException.class, () -> controller.verificarIdentidad(null));

        assertEquals(HttpStatus.OK, controller.recuperarContrasena(Map.of(
                "rut", "214297353", "preguntaSeguridad", "p", "respuestaSeguridad", "r", "nuevaContrasena", "Clave123*")).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> controller.recuperarContrasena(null));
    }

    @Test
    void avatarYPrimerIngreso() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg", "image/jpeg", "img".getBytes());
        when(service.actualizarAvatar(1L, archivo)).thenReturn(usuarioEjemplo);
        assertEquals(HttpStatus.OK, controller.actualizarAvatar(1L, archivo).getStatusCode());

        Resource recurso = new ByteArrayResource("img".getBytes());
        when(avatarStorageService.cargar("a.jpg")).thenReturn(recurso);
        when(avatarStorageService.detectarMediaType("a.jpg")).thenReturn(MediaType.IMAGE_JPEG);
        ResponseEntity<Resource> imgResp = controller.obtenerAvatar("a.jpg");
        assertEquals(HttpStatus.OK, imgResp.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, imgResp.getHeaders().getContentType());

        when(service.configurarPrimerIngreso(1L, "p", "r", "Clave123*")).thenReturn(usuarioEjemplo);
        assertEquals(HttpStatus.OK, controller.configurarPrimerIngreso(1L,
                Map.of("preguntaSeguridad", "p", "respuestaSeguridad", "r", "nuevaContrasena", "Clave123*")).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> controller.configurarPrimerIngreso(1L, null));
    }

    @Test
    void eliminarYManejarError() {
        when(service.deshabilitar(1L)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminar(1L).getStatusCode());

        when(service.deshabilitar(2L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.eliminar(2L).getStatusCode());

        ResponseEntity<?> err = controller.manejarError(new ResponseStatusException(HttpStatus.CONFLICT, "Duplicado"));
        assertEquals(HttpStatus.CONFLICT, err.getStatusCode());
        assertEquals(Map.of("message", "Duplicado"), err.getBody());
    }
}