package cl.rinconsur.accesos.service;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.repository.AccesoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccesoServiceAdicionalTest {

    @Mock private AccesoRepository repository;
    @InjectMocks private AccesoService service;

    private Acceso acceso(long id, String codigo, String nombre, Boolean activo) {
        Acceso a = new Acceso();
        a.setId(id);
        a.setCodigo(codigo);
        a.setNombre(nombre);
        a.setActivo(activo);
        return a;
    }

    @Test
    void listarTodos_devuelveLaListaDelRepositorio() {
        Acceso a = acceso(1L, "A1", "Norte", true);
        Acceso b = acceso(2L, "B1", "Sur", false);
        when(repository.findAllByOrderByCodigoAsc()).thenReturn(List.of(a, b));

        assertEquals(List.of(a, b), service.listarTodos());
        verify(repository).findAllByOrderByCodigoAsc();
    }

    @Test
    void listarActivos_devuelveSoloLosHabilitadosDelRepositorio() {
        Acceso a = acceso(1L, "A1", "Norte", true);
        when(repository.findByActivoTrueOrderByCodigoAsc()).thenReturn(List.of(a));

        assertEquals(List.of(a), service.listarActivos());
        verify(repository).findByActivoTrueOrderByCodigoAsc();
    }

    @Test
    void cambiarEstado_desactivarAccesoActivo_guardaEstadoFalsoSinContarCupos() {
        Acceso a = acceso(1L, "A1", "Norte", true);
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        when(repository.save(a)).thenAnswer(inv -> inv.getArgument(0));

        Acceso resultado = service.cambiarEstado(1L);

        assertSame(a, resultado);
        assertFalse(resultado.getActivo());
        verify(repository, never()).countByActivoTrue();
        verify(repository).save(a);
    }

    @Test
    void cambiarEstado_activarAccesoConCupo_guardaEstadoVerdadero() {
        Acceso a = acceso(2L, "B2", "Poniente", false);
        when(repository.findById(2L)).thenReturn(Optional.of(a));
        when(repository.countByActivoTrue()).thenReturn(4L);
        when(repository.save(a)).thenAnswer(inv -> inv.getArgument(0));

        Acceso resultado = service.cambiarEstado(2L);

        assertTrue(resultado.getActivo());
        verify(repository).countByActivoTrue();
        verify(repository).save(a);
    }

    @Test
    void cambiarEstado_idInexistente_retornaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cambiarEstado(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(repository, never()).save(any());
        verify(repository, never()).countByActivoTrue();
    }

    @Test
    void cambiarEstado_estadoNulo_seConsideraInactivoYSePuedeHabilitar() {
        Acceso a = acceso(3L, "C3", "Oriente", null);
        when(repository.findById(3L)).thenReturn(Optional.of(a));
        when(repository.countByActivoTrue()).thenReturn(0L);
        when(repository.save(a)).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.cambiarEstado(3L).getActivo());
        verify(repository).save(a);
    }

    @Test
    void guardar_codigoNulo_retornaBadRequestSinConsultarRepositorio() {
        Acceso nuevo = acceso(7L, null, "Norte", true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardar(nuevo));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void guardar_nombreNulo_retornaBadRequestSinConsultarRepositorio() {
        Acceso nuevo = acceso(7L, "D4", null, true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardar(nuevo));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void guardar_nombreAusenteOEnBlanco_retornaBadRequest(String nombre) {
        Acceso nuevo = acceso(7L, "D4", nombre, true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardar(nuevo));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void guardar_normalizaCodigoYNombreYDescartaIdIngresado() {
        Acceso nuevo = acceso(999L, " c3 ", "  Acceso Oriente  ", false);
        when(repository.existsByCodigoIgnoreCase("C3")).thenReturn(false);
        when(repository.countByActivoTrue()).thenReturn(2L);
        when(repository.save(nuevo)).thenAnswer(inv -> inv.getArgument(0));

        Acceso resultado = service.guardar(nuevo);

        assertNull(resultado.getId());
        assertEquals("C3", resultado.getCodigo());
        assertEquals("Acceso Oriente", resultado.getNombre());
        assertTrue(resultado.getActivo());
        verify(repository).save(nuevo);
    }
}