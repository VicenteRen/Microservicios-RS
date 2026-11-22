package cl.rinconsur.observaciones.service;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.repository.RegistroObservacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroObservacionLogicaAdicionalTest {

    @Mock private RegistroObservacionRepository repository;
    private RegistroObservacionService service;
    private RestClient usuariosClient;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void configurar() {
        service = new RegistroObservacionService(repository);
        usuariosClient = mock(RestClient.class);
        ReflectionTestUtils.setField(service, "usuariosClient", usuariosClient);
    }

    private void prepararPeticionLogin() {
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        when(usuariosClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/usuarios/login")).thenReturn(bodySpec);
        when(bodySpec.body(any(Map.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
    }

    private void login(Map<String, Object> respuesta) {
        prepararPeticionLogin();
        when(responseSpec.body(Map.class)).thenReturn(respuesta);
    }

    private void loginError(RuntimeException error) {
        prepararPeticionLogin();
        when(responseSpec.body(Map.class)).thenThrow(error);
    }

    private RegistroObservacion registro(long propietario, boolean activo) {
        RegistroObservacion r = new RegistroObservacion();
        r.setId(9L);
        r.setUsuarioId(propietario);
        r.setActivo(activo);
        return r;
    }

    private void verificarError(int codigo, String rut, String clave, String motivo) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.anular(9L, rut, clave, motivo));
        assertEquals(codigo, e.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void listarTodos_devuelveLosActivosDelRepositorio() {
        RegistroObservacion obs = registro(4L, true);
        when(repository.findByActivoTrueOrderByIdDesc()).thenReturn(List.of(obs));
        assertEquals(List.of(obs), service.listarTodos());
        verify(repository).findByActivoTrueOrderByIdDesc();
    }

    @Test
    void guardar_transitoAlternativo_reiniciaIdentificadorYEstado() {
        RegistroObservacion obs = registro(3L, false);
        obs.setClasificacion("tRaNsItO");
        obs.setTieneComprasExternas(true);
        when(repository.save(obs)).thenReturn(obs);
        RegistroObservacion resultado = service.guardar(obs);
        assertSame(obs, resultado);
        assertNull(obs.getId());
        assertTrue(obs.getActivo());
        assertFalse(obs.getTieneComprasExternas());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void anular_rutInvalido_rechaza(String rut) {
        verificarError(400, rut, "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_motivoNulo_rechaza() {
        verificarError(400, "12345678-9", "Clave123!", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"          ", "1234567890", "Registro\nincorrecto"})
    void anular_motivoInvalido_rechaza(String motivo) {
        verificarError(400, "12345678-9", "Clave123!", motivo);
    }

    @Test
    void anular_motivoDemasiadoLargo_rechaza() {
        verificarError(400, "12345678-9", "Clave123!", "A".repeat(256));
    }

    @Test
    void anular_credencialesIncorrectas_retorna403() {
        loginError(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        verificarError(403, "12345678-9", "MalaClave", "Registro duplicado");
    }

    @Test
    void anular_errorServidorUsuarios_retorna503() {
        loginError(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        verificarError(503, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_falloConexionUsuarios_retorna503() {
        loginError(new IllegalStateException("Servicio desconectado"));
        verificarError(503, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_loginSinRespuesta_retorna403() {
        login(null);
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_loginSinId_retorna403() {
        login(Map.of("rolId", 1, "activo", true));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_loginSinRol_retorna403() {
        login(Map.of("id", 5, "activo", true));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_usuarioInactivo_retorna403() {
        login(Map.of("id", 5, "rolId", 1, "activo", false));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_rolNoAutorizado_retorna403() {
        login(Map.of("id", 5, "rolId", 3, "activo", true));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
        verify(repository, never()).findById(anyLong());
    }

    @Test
    void anular_registroNoExiste_retorna404() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        when(repository.findById(9L)).thenReturn(Optional.empty());
        verificarError(404, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_registroYaAnulado_retorna409() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        when(repository.findById(9L)).thenReturn(Optional.of(registro(5, false)));
        verificarError(409, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_observacionAjenaYNoAdmin_retorna403() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        when(repository.findById(9L)).thenReturn(Optional.of(registro(8, true)));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_propietario_desactivaRegistroSinBorrarlo() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        RegistroObservacion obs = registro(5, true);
        when(repository.findById(9L)).thenReturn(Optional.of(obs));
        service.anular(9L, " 12345678-9 ", "Clave123!", "  Registro duplicado  ");
        assertFalse(obs.getActivo());
        assertEquals(Long.valueOf(5), obs.getAnuladoPor());
        assertEquals("Registro duplicado", obs.getMotivoAnulacion());
        assertNotNull(obs.getFechaAnulacion());
        assertFalse(obs.getFechaAnulacion().isAfter(LocalDateTime.now()));
        verify(repository).save(obs);
        verify(repository, never()).delete(any(RegistroObservacion.class));
    }

    @Test
    void anular_administrador_puedeAnularRegistroAjeno() {
        login(Map.of("id", 2, "rolId", 2, "activo", true));
        RegistroObservacion obs = registro(8, true);
        when(repository.findById(9L)).thenReturn(Optional.of(obs));
        service.anular(9L, "12345678-9", "Clave123!", "Registro duplicado");
        assertFalse(obs.getActivo());
        assertEquals(Long.valueOf(2), obs.getAnuladoPor());
        verify(repository).save(obs);
    }

    @Test
    void listarPorUsuario_sinObservaciones_devuelveListaVacia() {
        when(repository.findByUsuarioIdAndActivoTrueOrderByIdDesc(42L))
                .thenReturn(List.of());
        assertTrue(service.listarPorUsuario(42L).isEmpty());
        verify(repository).findByUsuarioIdAndActivoTrueOrderByIdDesc(42L);
    }

    @Test
    void guardar_clasificacionSinEspecificarNoCambiaLasCompras() {
        RegistroObservacion obs = registro(7L, false);
        obs.setClasificacion(null);
        obs.setTieneComprasExternas(true);
        when(repository.save(obs)).thenReturn(obs);
        assertSame(obs, service.guardar(obs));
        assertTrue(obs.getTieneComprasExternas());
        assertNull(obs.getId());
        assertTrue(obs.getActivo());
    }

    @Test
    void anular_motivoMinimoValido_deDiezCaracteresEsAceptado() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        RegistroObservacion obs = registro(5, true);
        when(repository.findById(9L)).thenReturn(Optional.of(obs));
        service.anular(9L, "12345678-9", "Clave123!", "abcdefghij");
        assertEquals("abcdefghij", obs.getMotivoAnulacion());
        assertFalse(obs.getActivo());
        verify(repository).save(obs);
    }

    @Test
    void anular_motivoMaximoValido_de255CaracteresEsAceptado() {
        login(Map.of("id", 5, "rolId", 1, "activo", true));
        RegistroObservacion obs = registro(5, true);
        when(repository.findById(9L)).thenReturn(Optional.of(obs));
        String motivo = "A".repeat(255);
        service.anular(9L, "12345678-9", "Clave123!", motivo);
        assertEquals(motivo, obs.getMotivoAnulacion());
        verify(repository).save(obs);
    }

    @Test
    void anular_loginConIdNoNumerico_retorna403() {
        login(Map.of("id", "cinco", "rolId", 1, "activo", true));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_loginConRolNoNumerico_retorna403() {
        login(Map.of("id", 5, "rolId", "admin", "activo", true));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

    @Test
    void anular_loginSinCampoActivo_retorna403() {
        login(Map.of("id", 5, "rolId", 1));
        verificarError(403, "12345678-9", "Clave123!", "Registro duplicado");
    }

}