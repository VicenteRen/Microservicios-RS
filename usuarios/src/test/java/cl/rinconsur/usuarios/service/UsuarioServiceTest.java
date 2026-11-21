package cl.rinconsur.usuarios.service;

import cl.rinconsur.usuarios.model.Usuario;
import cl.rinconsur.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;
    @Mock
    private EmailService emailService;
    @Mock
    private AvatarStorageService avatarStorageService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, emailService, avatarStorageService);
    }

    private Usuario crearUsuarioBase() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setRut("214297353");
        u.setContrasena(service.encriptarConSal("Observador123*"));
        u.setNombreCompleto("Vicente Ponce");
        u.setCorreo("v.ponce@rinconsur.cl");
        u.setTelefono("987654321");
        u.setPreguntaSeguridad("¿Cuál es el nombre de tu primera mascota?");
        u.setRespuestaSeguridad(service.encriptarConSal("princesa"));
        u.setRolId(1L);
        u.setRolNombre("Observador");
        u.setPrimerIngreso(false);
        u.setActivo(true);
        return u;
    }

    @Test
    void precargarUsuarios_cuandoTablaVacia_guardaDosUsuarios() {
        when(repository.count()).thenReturn(0L);

        service.precargarUsuarios();

        verify(repository, times(2)).save(any(Usuario.class));
    }

    @Test
    void precargarUsuarios_cuandoYaHayDatos_noInsertaNada() {
        when(repository.count()).thenReturn(2L);

        service.precargarUsuarios();

        verify(repository, never()).save(any());
    }

    @Test
    void listarActivos_devuelveListaOrdenada() {
        Usuario u = crearUsuarioBase();
        when(repository.findByActivoTrueOrderByNombreCompletoAsc()).thenReturn(List.of(u));

        List<Usuario> resultado = service.listarActivos();

        assertEquals(1, resultado.size());
        assertEquals("Vicente Ponce", resultado.get(0).getNombreCompleto());
    }

    @Test
    void buscarPorId_existente_devuelveUsuario() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));

        Usuario resultado = service.buscarPorId(1L);

        assertEquals("214297353", resultado.getRut());
    }

    @Test
    void buscarPorId_inexistente_lanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.buscarPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void autenticar_credencialesCorrectas_devuelveUsuario() {
        Usuario u = crearUsuarioBase();
        when(repository.findByRutIgnoreCaseAndActivoTrue("214297353")).thenReturn(Optional.of(u));

        Usuario autenticado = service.autenticar(" 214297353 ", "Observador123*");

        assertEquals(1L, autenticado.getId());
    }

    @Test
    void autenticar_camposNulos_lanzaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.autenticar(null, "123"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void autenticar_claveIncorrectaOUsuarioInexistente_lanzaUnauthorized() {
        Usuario u = crearUsuarioBase();
        when(repository.findByRutIgnoreCaseAndActivoTrue("214297353")).thenReturn(Optional.of(u));
        when(repository.findByRutIgnoreCaseAndActivoTrue("999999999")).thenReturn(Optional.empty());

        ResponseStatusException exClave = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("214297353", "ClaveMala1*"));
        ResponseStatusException exNoExiste = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("999999999", "Observador123*"));

        assertEquals(HttpStatus.UNAUTHORIZED, exClave.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exNoExiste.getStatusCode());
    }

    @Test
    void guardar_usuarioNuevo_normalizaCifraYEnviaCorreo() {
        Usuario nuevo = new Usuario();
        nuevo.setRut(" 12345678k ");
        nuevo.setNombreCompleto("  ana   maría  ");
        nuevo.setCorreo(" ANA@DUOC.CL ");
        nuevo.setTelefono(" 912345678 ");
        nuevo.setRolId(2L);

        when(repository.findByRutIgnoreCase("12345678K")).thenReturn(Optional.empty());
        when(repository.existsByCorreoIgnoreCaseAndActivoTrue("ana@duoc.cl")).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrue("912345678")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = service.guardar(nuevo);

        assertEquals("12345678K", guardado.getRut());
        assertEquals("Ana María", guardado.getNombreCompleto());
        assertEquals("ana@duoc.cl", guardado.getCorreo());
        assertEquals("Administrador", guardado.getRolNombre());
        assertTrue(guardado.getPrimerIngreso());
        assertTrue(guardado.getActivo());
        verify(emailService).enviarContrasenaTemporal(eq("ana@duoc.cl"), eq("Ana María"), eq("12345678K"), anyString());
    }

    @Test
    void guardar_usuarioInactivoExistente_reactivaCuenta() {
        Usuario inactivo = crearUsuarioBase();
        inactivo.setActivo(false);

        Usuario datos = new Usuario();
        datos.setRut("214297353");
        datos.setNombreCompleto("vicente ponce");
        datos.setCorreo("nuevo@duoc.cl");
        datos.setTelefono("900000001");
        datos.setRolId(1L);

        when(repository.findByRutIgnoreCase("214297353")).thenReturn(Optional.of(inactivo));
        when(repository.existsByCorreoIgnoreCaseAndActivoTrue("nuevo@duoc.cl")).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrue("900000001")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario reactivado = service.guardar(datos);

        assertTrue(reactivado.getActivo());
        assertTrue(reactivado.getPrimerIngreso());
        assertEquals("nuevo@duoc.cl", reactivado.getCorreo());
        verify(emailService).enviarContrasenaTemporal(eq("nuevo@duoc.cl"), eq("Vicente Ponce"), eq("214297353"), anyString());
    }

    @Test
    void guardar_duplicados_lanzaConflict() {
        Usuario activo = crearUsuarioBase();
        Usuario nuevo = crearUsuarioBase();

        when(repository.findByRutIgnoreCase("214297353")).thenReturn(Optional.of(activo));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> service.guardar(nuevo)).getStatusCode());

        when(repository.findByRutIgnoreCase("214297353")).thenReturn(Optional.empty());
        when(repository.existsByCorreoIgnoreCaseAndActivoTrue("v.ponce@rinconsur.cl")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> service.guardar(nuevo)).getStatusCode());

        when(repository.existsByCorreoIgnoreCaseAndActivoTrue("v.ponce@rinconsur.cl")).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrue("987654321")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> service.guardar(nuevo)).getStatusCode());
    }

    @Test
    void guardar_nombreInvalido_lanzaBadRequest() {
        Usuario nuevo = crearUsuarioBase();
        nuevo.setNombreCompleto("Ana123");

        when(repository.findByRutIgnoreCase("214297353")).thenReturn(Optional.empty());
        when(repository.existsByCorreoIgnoreCaseAndActivoTrue(anyString())).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrue(anyString())).thenReturn(false);

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.guardar(nuevo)).getStatusCode());
    }

    @Test
    void actualizarPerfil_valido_actualizaDatos() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot("nuevo@duoc.cl", 1L)).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrueAndIdNot("955555555", 1L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.actualizarPerfil(1L, "juan pérez", "NUEVO@DUOC.CL", "955555555");

        assertEquals("Juan Pérez", actualizado.getNombreCompleto());
        assertEquals("nuevo@duoc.cl", actualizado.getCorreo());
        assertEquals("955555555", actualizado.getTelefono());
    }

    @Test
    void actualizarPerfil_correoOTelefonoDuplicado_lanzaConflict() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot("otro@duoc.cl", 1L)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.actualizarPerfil(1L, "Juan", "otro@duoc.cl", "955555555")).getStatusCode());

        when(repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot("otro@duoc.cl", 1L)).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrueAndIdNot("955555555", 1L)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.actualizarPerfil(1L, "Juan", "otro@duoc.cl", "955555555")).getStatusCode());
    }

    @Test
    void actualizarDesdeAdmin_cambiaRolYContacto() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot("admin2@duoc.cl", 1L)).thenReturn(false);
        when(repository.existsByTelefonoAndActivoTrueAndIdNot("922222222", 1L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.actualizarDesdeAdmin(1L, "admin2@duoc.cl", "922222222", 2L);

        assertEquals(2L, actualizado.getRolId());
        assertEquals("Administrador", actualizado.getRolNombre());
    }

    @Test
    void cambiarContrasenaDesdePerfil_validaActualYActualiza() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.cambiarContrasenaDesdePerfil(1L, "Observador123*", "NuevaClave1*");

        assertEquals(service.encriptarConSal("NuevaClave1*"), actualizado.getContrasena());
        assertFalse(actualizado.getPrimerIngreso());
    }

    @Test
    void cambiarContrasenaDesdePerfil_actualIncorrectaONuevaDebil_lanzaBadRequest() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.cambiarContrasenaDesdePerfil(1L, "Mala123*", "NuevaClave1*")).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.cambiarContrasenaDesdePerfil(1L, "Observador123*", "debil")).getStatusCode());
    }

    @Test
    void configurarPrimerIngreso_valido_marcaPrimerIngresoFalse() {
        Usuario u = crearUsuarioBase();
        u.setPrimerIngreso(true);
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.configurarPrimerIngreso(1L, "¿Tu mascota?", "Rocky", "ClaveFuerte1*");

        assertFalse(actualizado.getPrimerIngreso());
        assertEquals("¿Tu mascota?", actualizado.getPreguntaSeguridad());
        assertEquals(service.encriptarConSal("rocky"), actualizado.getRespuestaSeguridad());
    }

    @Test
    void configurarPrimerIngreso_camposVacios_lanzaBadRequest() {
        Usuario u = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(u));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.configurarPrimerIngreso(1L, "", "Rocky", "ClaveFuerte1*")).getStatusCode());
    }

    @Test
    void verificarIdentidadYRestablecerContrasena_flujoCompleto() {
        Usuario u = crearUsuarioBase();
        when(repository.findByRutIgnoreCaseAndActivoTrue("214297353")).thenReturn(Optional.of(u));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.verificarIdentidadRecuperacion(
                "214297353", "¿Cuál es el nombre de tu primera mascota?", "PRINCESA"));
        assertFalse(service.verificarIdentidadRecuperacion(
                "214297353", "Otra pregunta", "princesa"));

        boolean resultado = service.restablecerContrasena(
                "214297353", "¿Cuál es el nombre de tu primera mascota?", "princesa", "Recuperada1*");

        assertTrue(resultado);
        assertEquals(service.encriptarConSal("Recuperada1*"), u.getContrasena());
    }

    @Test
    void restablecerContrasena_erroresValidacionEIdentidad() {
        Usuario u = crearUsuarioBase();
        when(repository.findByRutIgnoreCaseAndActivoTrue("214297353")).thenReturn(Optional.of(u));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.restablecerContrasena("", "p", "r", "Clave123*")).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.restablecerContrasena("214297353", "p", "r", "corta")).getStatusCode());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> service.restablecerContrasena("214297353", "¿Cuál es el nombre de tu primera mascota?", "otra", "Clave123*")).getStatusCode());
    }

    @Test
    void actualizarAvatar_guardaUrlYEliminaAnterior() {
        Usuario u = crearUsuarioBase();
        u.setAvatarUrl("/api/usuarios/avatares/viejo.jpg");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "img".getBytes());

        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(avatarStorageService.guardar(1L, archivo)).thenReturn("usuario_1_999.jpg");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.actualizarAvatar(1L, archivo);

        assertEquals("/api/usuarios/avatares/usuario_1_999.jpg", actualizado.getAvatarUrl());
        verify(avatarStorageService).eliminarSiExiste("/api/usuarios/avatares/viejo.jpg");
    }

    @Test
    void deshabilitar_observadorYProteccionUltimoAdmin() {
        Usuario obs = crearUsuarioBase();
        when(repository.findById(1L)).thenReturn(Optional.of(obs));
        assertTrue(service.deshabilitar(1L));
        assertFalse(obs.getActivo());

        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(service.deshabilitar(99L));

        Usuario unicoAdmin = crearUsuarioBase();
        unicoAdmin.setId(2L);
        unicoAdmin.setRolId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(unicoAdmin));
        when(repository.countByRolIdAndActivoTrue(2L)).thenReturn(1L);

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.deshabilitar(2L)).getStatusCode());
    }
}