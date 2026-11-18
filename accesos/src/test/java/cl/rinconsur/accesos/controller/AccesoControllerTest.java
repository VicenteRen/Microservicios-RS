package cl.rinconsur.accesos.controller;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.service.AccesoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccesoController.class)
class AccesoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean 
    private AccesoService accesoService;

    @Test
    void listarTodos_retornaListaDeAccesos() throws Exception {
        Acceso a1 = new Acceso();
        a1.setId(1L);
        a1.setCodigo("A1");
        a1.setNombre("Acceso Norte");
        a1.setActivo(true);

        when(accesoService.listarTodos()).thenReturn(List.of(a1));

        mockMvc.perform(get("/api/accesos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("A1"))
                .andExpect(jsonPath("$[0].nombre").value("Acceso Norte"));
    }

    @Test
    void guardar_conDatosValidos_retornaCreated() throws Exception {
        Acceso nuevo = new Acceso();
        nuevo.setCodigo("A5");
        nuevo.setNombre("Acceso Boulevard");

        Acceso guardado = new Acceso();
        guardado.setId(5L);
        guardado.setCodigo("A5");
        guardado.setNombre("Acceso Boulevard");
        guardado.setActivo(true);

        when(accesoService.guardar(any(Acceso.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/accesos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.codigo").value("A5"));
    }

    @Test
    void guardar_conCodigoDuplicado_retornaConflict() throws Exception {
        Acceso duplicado = new Acceso();
        duplicado.setCodigo("A1");
        duplicado.setNombre("Acceso Norte");

        when(accesoService.guardar(any(Acceso.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe el acceso A1"));

        mockMvc.perform(post("/api/accesos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe el acceso A1"));
    }

    @Test
    void cambiarEstado_conIdExistente_retornaOk() throws Exception {
        Acceso actualizado = new Acceso();
        actualizado.setId(1L);
        actualizado.setCodigo("A1");
        actualizado.setNombre("Acceso Norte");
        actualizado.setActivo(false);

        when(accesoService.cambiarEstado(1L)).thenReturn(actualizado);

        mockMvc.perform(put("/api/accesos/1/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}