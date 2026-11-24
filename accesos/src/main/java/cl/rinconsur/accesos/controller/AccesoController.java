package cl.rinconsur.accesos.controller;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.service.AccesoService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@OpenAPIDefinition(
    info = @Info(
        title = "API de Gestión de Accesos - Rincón Sur",
        version = "1.0",
        description = "Microservicio encargado de administrar y habilitar los puntos de acceso del centro comercial Rincón Sur."
    )
)
@Tag(name = "Puntos de Acceso", description = "Operaciones para listar, crear y cambiar el estado de los accesos")
@RestController
@RequestMapping("/api/accesos")
@CrossOrigin(origins = "*")
public class AccesoController {

    private final AccesoService service;

    public AccesoController(AccesoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar todos los accesos", description = "Devuelve todos los puntos de acceso (activos e inactivos) ordenados por código para la vista de Administrador.")
    @GetMapping
    public List<Acceso> listarTodos() {
        return service.listarTodos();
    }

    @Operation(summary = "Listar accesos habilitados", description = "Devuelve únicamente los puntos de acceso con estado activo = true para la vista del Observador.")
    @GetMapping("/activos")
    public List<Acceso> listarActivos() {
        return service.listarActivos();
    }

    @Operation(
        summary = "Crear un nuevo punto de acceso",
        description = "Valida el formato ^[A-ZÑ][0-9]$ y registra un nuevo acceso. Si ya existen 5 activos, se crea deshabilitado."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Acceso> guardar(@Valid @RequestBody Acceso acceso) {
        Acceso guardado = service.guardar(acceso);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @Operation(summary = "Habilitar o deshabilitar un acceso", description = "Invierte el estado activo/inactivo del acceso indicado por su ID, respetando el máximo de 5 activos simultáneos.")
    @PutMapping("/{id}/estado")
    public ResponseEntity<Acceso> cambiarEstado(@PathVariable Long id) {
        Acceso actualizado = service.cambiarEstado(id);
        return ResponseEntity.ok(actualizado);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> manejarError(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : "Error en la solicitud";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", mensaje));
    }
}