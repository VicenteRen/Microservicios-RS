package cl.rinconsur.accesos.service;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.repository.AccesoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccesoServiceTest {

    @Mock
    private AccesoRepository repository;

    @InjectMocks
    private AccesoService service;

    @Test
    void guardar_conDatosValidosYMenosDe5Activos_guardaAccesoHabilitado() {
        // preparar
        Acceso nuevo = new Acceso();
        nuevo.setCodigo("a5");
        nuevo.setNombre("Acceso Boulevard");

        when(repository.existsByCodigoIgnoreCase("A5")).thenReturn(false);
        when(repository.countByActivoTrue()).thenReturn(4L);
        when(repository.save(any(Acceso.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        Acceso resultado = service.guardar(nuevo);

        // comprobar
        assertEquals("A5", resultado.getCodigo());
        assertEquals("Acceso Boulevard", resultado.getNombre());
        assertTrue(resultado.getActivo());
        verify(repository, times(1)).save(any(Acceso.class));
    }

    @Test
    void guardar_con5ActivosExistentes_guardaAccesoDeshabilitado() {
        // preparar
        Acceso nuevo = new Acceso();
        nuevo.setCodigo("B1");
        nuevo.setNombre("Acceso Terraza");

        when(repository.existsByCodigoIgnoreCase("B1")).thenReturn(false);
        when(repository.countByActivoTrue()).thenReturn(5L);
        when(repository.save(any(Acceso.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        Acceso resultado = service.guardar(nuevo);

        // comprobar
        assertEquals("B1", resultado.getCodigo());
        assertFalse(resultado.getActivo());
    }

    @Test
    void guardar_conCodigoFormatoInvalido_lanzaExcepcionBadRequest() {
        // preparar
        Acceso invalido = new Acceso();
        invalido.setCodigo("PEPITO100");
        invalido.setNombre("Acceso Malo");

        // ejecucion y comprobar
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.guardar(invalido)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void guardar_conCodigoDuplicado_lanzaExcepcionConflict() {
        // preparar
        Acceso duplicado = new Acceso();
        duplicado.setCodigo("A1");
        duplicado.setNombre("Acceso Norte");

        when(repository.existsByCodigoIgnoreCase("A1")).thenReturn(true);

        // ejecucion y comprobar
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.guardar(duplicado)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void cambiarEstado_intentarActivarSextoAcceso_lanzaExcepcionBadRequest() {
        // preparar
        Acceso inactivo = new Acceso();
        inactivo.setId(6L);
        inactivo.setCodigo("A6");
        inactivo.setNombre("Acceso Sur 2");
        inactivo.setActivo(false);

        when(repository.findById(6L)).thenReturn(Optional.of(inactivo));
        when(repository.countByActivoTrue()).thenReturn(5L);

        // ejecucion y comprobar
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.cambiarEstado(6L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }
}