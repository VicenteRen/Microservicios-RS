package cl.rinconsur.observaciones.service;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.repository.RegistroObservacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroObservacionServiceTest {

    @Mock
    private RegistroObservacionRepository repository;

    @InjectMocks
    private RegistroObservacionService service;

    @Test
    void guardar_conClasificacionTransito_fuerzaComprasExternasEnFalse() {
        // preparar
        RegistroObservacion registro = new RegistroObservacion();
        registro.setClasificacion("Tránsito");
        registro.setTieneComprasExternas(true);
        registro.setUsuarioId(1L);

        when(repository.save(any(RegistroObservacion.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        RegistroObservacion guardado = service.guardar(registro);

        // comprobar
        assertTrue(guardado.getActivo());
        assertFalse(guardado.getTieneComprasExternas());
        verify(repository, times(1)).save(registro);
    }

    @Test
    void guardar_conClasificacionVisita_mantieneComprasExternasEnTrue() {
        // preparar
        RegistroObservacion registro = new RegistroObservacion();
        registro.setClasificacion("Visita");
        registro.setTieneComprasExternas(true);
        registro.setUsuarioId(1L);

        when(repository.save(any(RegistroObservacion.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        RegistroObservacion guardado = service.guardar(registro);

        // comprobar
        assertTrue(guardado.getActivo());
        assertTrue(guardado.getTieneComprasExternas());
    }

    @Test
    void listarPorUsuario_conUsuarioExistente_retornaRegistrosActivos() {
        // preparar
        RegistroObservacion obs = new RegistroObservacion();
        obs.setId(10L);
        obs.setUsuarioId(2L);
        obs.setActivo(true);

        when(repository.findByUsuarioIdAndActivoTrueOrderByIdDesc(2L)).thenReturn(List.of(obs));

        // ejecucion
        List<RegistroObservacion> resultado = service.listarPorUsuario(2L);

        // comprobar
        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getUsuarioId());
    }

   

    @Test
    void anular_conMotivoCorto_retornaBadRequest() {
        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> service.anular(
                                1L,
                                "214297353",
                                "Clave123!",
                                "Error"
                        )
                );

        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void anular_sinContrasena_retornaBadRequest() {
        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> service.anular(
                                1L,
                                "214297353",
                                "",
                                "Registro duplicado por error"
                        )
                );

        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    
}