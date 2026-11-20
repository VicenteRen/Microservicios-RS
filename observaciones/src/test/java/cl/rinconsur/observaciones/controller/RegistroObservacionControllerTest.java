package cl.rinconsur.observaciones.controller;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.service.RegistroObservacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistroObservacionController.class)
class RegistroObservacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean 
    private RegistroObservacionService observacionService;

    @Test
    void guardar_observacionValida_retornaCreated() throws Exception {
        RegistroObservacion obs = new RegistroObservacion();
        obs.setClasificacion("Tránsito");
        obs.setRangoEtario("18–25");
        obs.setCompania("Solo");
        obs.setTieneComprasExternas(false);
        obs.setAccesoDestino("A1 - Acceso Norte");
        obs.setFechaHora("18/11/2026 18:00");
        obs.setUsuarioId(1L);
        obs.setNombreObservador("Vicente");
        obs.setActivo(true);

        RegistroObservacion guardada = new RegistroObservacion();
        guardada.setId(1L);
        guardada.setClasificacion("Tránsito");
        guardada.setRangoEtario("18–25");
        guardada.setCompania("Solo");
        guardada.setTieneComprasExternas(false);
        guardada.setAccesoDestino("A1 - Acceso Norte");
        guardada.setFechaHora("18/11/2026 18:00");
        guardada.setUsuarioId(1L);
        guardada.setNombreObservador("Vicente");
        guardada.setActivo(true);

        when(observacionService.guardar(any(RegistroObservacion.class))).thenReturn(guardada);

        mockMvc.perform(post("/api/observaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(obs)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clasificacion").value("Tránsito"));
    }

    @Test
    void listarTodos_retornaListaDeObservaciones() throws Exception {
        RegistroObservacion obs = new RegistroObservacion();
        obs.setId(1L);
        obs.setClasificacion("Visita");
        obs.setRangoEtario("26–40");
        obs.setCompania("Pareja");
        obs.setTieneComprasExternas(true);
        obs.setAccesoDestino("A2 - Acceso Sur");
        obs.setFechaHora("18/11/2026 18:05");
        obs.setUsuarioId(1L);
        obs.setNombreObservador("Vicente");
        obs.setActivo(true);

        when(observacionService.listarTodos()).thenReturn(List.of(obs));

        mockMvc.perform(get("/api/observaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clasificacion").value("Visita"));
    }

    @Test
    void listarPorUsuario_retornaRegistrosDelUsuario() throws Exception {
        RegistroObservacion obs = new RegistroObservacion();
        obs.setId(2L);
        obs.setClasificacion("Tránsito");
        obs.setRangoEtario("41–60");
        obs.setCompania("Solo");
        obs.setTieneComprasExternas(false);
        obs.setAccesoDestino("A3 - Parking");
        obs.setFechaHora("18/11/2026 18:10");
        obs.setUsuarioId(1L);
        obs.setNombreObservador("Vicente");
        obs.setActivo(true);

        when(observacionService.listarPorUsuario(1L)).thenReturn(List.of(obs));

        mockMvc.perform(get("/api/observaciones/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }

    
    @Test
    void anular_conSolicitudValida_retornaNoContent() throws Exception {
        String solicitud = """
            {
                "rut": "214297353",
                "contrasena": "Clave123!",
                "motivo": "Registro duplicado por error"
            }
            """;

        mockMvc.perform(put("/api/observaciones/1/anular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(observacionService).anular(
                1L,
                "214297353",
                "Clave123!",
                "Registro duplicado por error"
        );
    }

    @Test
    void anular_sinDatos_retornaBadRequest() throws Exception {
        mockMvc.perform(put("/api/observaciones/1/anular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

}