package cl.rinconsur.observaciones.controller;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.service.RegistroObservacionService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@OpenAPIDefinition(
    info = @Info(
        title = "API de Registro de Observaciones - Rincón Sur",
        version = "1.0",
        description = "Microservicio encargado del registro de flujo de personas, consultas por observador y borrado lógico para dashboards."
    )
)
@Tag(name = "Observaciones", description = "Operaciones para registrar, consultar y deshabilitar observaciones en terreno")
@RestController
@RequestMapping("/api/observaciones")
@CrossOrigin(origins = "*")
public class RegistroObservacionController {

    private final RegistroObservacionService service;

    public RegistroObservacionController(RegistroObservacionService service) {
        this.service = service;
    }

    @Operation(summary = "Registrar nueva observación", description = "Guarda un nuevo conteo de persona en terreno con estado activo.")
    @PostMapping
    public ResponseEntity<RegistroObservacion> guardar(@Valid @RequestBody RegistroObservacion registro) {
        RegistroObservacion guardado = service.guardar(registro);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @Operation(summary = "Listar todas las observaciones activas", description = "Devuelve todos los registros con activo = true para el Dashboard e Historial del Administrador.")
    @GetMapping
    public List<RegistroObservacion> listarTodos() {
        return service.listarTodos();
    }

    @Operation(summary = "Listar observaciones por usuario", description = "Devuelve las observaciones activas registradas por un observador específico.")
    @GetMapping("/usuario/{usuarioId}")
    public List<RegistroObservacion> listarPorUsuario(@PathVariable Long usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @Operation(summary = "Deshabilitar observación (Borrado lógico)", description = "Cambia el estado activo a false sin eliminar físicamente el registro de MySQL.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLogicamente(@PathVariable Long id) {
        boolean deshabilitado = service.deshabilitar(id);
        if (!deshabilitado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}