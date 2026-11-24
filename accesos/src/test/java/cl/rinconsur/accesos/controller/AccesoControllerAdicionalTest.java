package cl.rinconsur.accesos.controller;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.service.AccesoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccesoController.class)
class AccesoControllerAdicionalTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccesoService accesoService;

    private Acceso acceso(long id, String codigo, boolean activo) {
        Acceso a = new Acceso();
        a.setId(id);
        a.setCodigo(codigo);
        a.setNombre("Acceso " + codigo);
        a.setActivo(activo);
        return a;
    }

    @Test
    void listarActivos_devuelveSoloAccesosHabilitados() throws Exception {
        when(accesoService.listarActivos()).thenReturn(List.of(acceso(1L, "A1", true)));

        mockMvc.perform(get("/api/accesos/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("A1"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$.length()").value(1));
        verify(accesoService).listarActivos();
    }

    @Test
    void listarActivos_sinResultados_devuelveArregloVacio() throws Exception {
        when(accesoService.listarActivos()).thenReturn(List.of());

        mockMvc.perform(get("/api/accesos/activos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(accesoService).listarActivos();
    }

    @Test
    void cambiarEstado_idInexistente_retorna404YMensaje() throws Exception {
        when(accesoService.cambiarEstado(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Acceso no encontrado"));

        mockMvc.perform(put("/api/accesos/99/estado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Acceso no encontrado"));
    }

    @Test
    void cambiarEstado_superaLimite_retorna400YMensaje() throws Exception {
        when(accesoService.cambiarEstado(6L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 5 accesos activos simultáneamente. Deshabilita otro primero."));

        mockMvc.perform(put("/api/accesos/6/estado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Máximo 5 accesos activos simultáneamente. Deshabilita otro primero."));
    }

    @Test
    void cambiarEstado_siSeHabilita_retornaActivoVerdadero() throws Exception {
        when(accesoService.cambiarEstado(2L)).thenReturn(acceso(2L, "B2", true));

        mockMvc.perform(put("/api/accesos/2/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void guardar_sinNombre_retorna400PorValidacionDeBean() throws Exception {
        mockMvc.perform(post("/api/accesos")
                .contentType("application/json")
                .content("{\"codigo\":\"A1\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(accesoService, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cambiarEstado_errorSinMotivo_retornaMensajePredeterminado() throws Exception {
        when(accesoService.cambiarEstado(999L))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        mockMvc.perform(put("/api/accesos/999/estado"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error en la solicitud"));
    }
}

