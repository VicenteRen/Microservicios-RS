package cl.rinconsur.usuarios.controller;

import cl.rinconsur.usuarios.model.Usuario;
import cl.rinconsur.usuarios.service.AvatarStorageService;
import cl.rinconsur.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@OpenAPIDefinition(
    info = @Info(
        title = "API de Gestión de Usuarios - Rincón Sur",
        version = "1.0",
        description = "Microservicio encargado de la administración de usuarios de Rincón Sur app."
    )
)
@Tag(name = "Usuarios", description = "Operaciones para autenticar, registrar, actualizar, recuperar contraseña y administrar usuarios")
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService service;
    private final AvatarStorageService avatarStorageService;

    public UsuarioController(UsuarioService service, AvatarStorageService avatarStorageService) {
        this.service = service;
        this.avatarStorageService = avatarStorageService;
    }

    @Operation(summary = "Listar usuarios activos", description = "Devuelve todos los usuarios con estado activo = true ordenados alfabéticamente por nombre completo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de usuarios activos obtenida correctamente")
    })
    @GetMapping
    public ResponseEntity<List<Usuario>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @Operation(summary = "Buscar usuario por ID", description = "Obtiene los datos completos de un usuario a partir de su ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(
        summary = "Crear o reactivar un usuario",
        description = "Registra un nuevo usuario generando automáticamente una contraseña temporal cifrada en SHA-256 y enviándola por correo electrónico. Valida unicidad de RUT, correo y teléfono; si el RUT estaba inactivo, lo reactiva."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado o reactivado exitosamente y correo temporal enviado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o campos obligatorios faltantes"),
        @ApiResponse(responseCode = "409", description = "RUT, correo o teléfono ya registrado por un usuario activo"),
        @ApiResponse(responseCode = "502", description = "No fue posible enviar el correo con la contraseña temporal")
    })
    @PostMapping
    public ResponseEntity<Usuario> guardar(@Valid @RequestBody Usuario usuario) {
        Usuario guardado = service.guardar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica a un usuario activo validando su RUT y contraseña cifrada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
        @ApiResponse(responseCode = "400", description = "RUT o contraseña faltantes"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo")
    })
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody Map<String, String> credenciales) {
        String rut = credenciales != null ? credenciales.get("rut") : null;
        String contrasena = credenciales != null ? credenciales.get("contrasena") : null;
        if (rut == null || rut.isBlank() || contrasena == null || contrasena.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUT y contraseña obligatorios");
        }
        Usuario autenticado = service.autenticar(rut, contrasena);
        return ResponseEntity.ok(autenticado);
    }

    @Operation(summary = "Actualizar datos de contacto del perfil", description = "Modifica el nombre completo, correo electrónico y teléfono del usuario autenticado, validando que correo y teléfono no pertenezcan a otro usuario activo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Correo o teléfono faltantes"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Correo o teléfono ya registrado por otro usuario")
    })
    @PutMapping("/{id}/contacto")
    public ResponseEntity<Usuario> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos
    ) {
        String correo = datos != null ? datos.get("correo") : null;
        String telefono = datos != null ? datos.get("telefono") : null;
        if (correo == null || correo.isBlank() || telefono == null || telefono.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo y teléfono son obligatorios");
        }
        Usuario actualizado = service.actualizarPerfil(
                id,
                datos.get("nombreCompleto"),
                correo,
                telefono
        );
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Cambiar contraseña desde Mi Perfil", description = "Valida la contraseña actual del usuario y actualiza su clave aplicando el cifrado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o campos faltantes"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}/cambiar-contrasena")
    public ResponseEntity<Usuario> cambiarContrasenaDesdePerfil(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos
    ) {
        String actual = datos != null ? datos.get("contrasenaActual") : null;
        String nueva = datos != null ? datos.get("nuevaContrasena") : null;
        if (actual == null || actual.isBlank() || nueva == null || nueva.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar la contraseña actual y la nueva");
        }
        Usuario actualizado = service.cambiarContrasenaDesdePerfil(id, actual, nueva);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Actualizar usuario desde Gestión de Usuarios", description = "Permite al Administrador modificar el correo, teléfono y rol (Observador o Administrador) de un usuario, validando unicidad de correo y teléfono.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos faltantes o rol inválido"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Correo o teléfono ya registrado por otro usuario")
    })
    @PutMapping("/{id}/admin")
    public ResponseEntity<Usuario> actualizarDesdeAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, Object> datos
    ) {
        if (datos == null || datos.get("correo") == null || datos.get("telefono") == null || datos.get("rolId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo, teléfono y rol son obligatorios");
        }
        String correo = datos.get("correo").toString();
        String telefono = datos.get("telefono").toString();
        Long rolId;
        try {
            rolId = Long.valueOf(datos.get("rolId").toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol especificado es inválido");
        }
        Usuario actualizado = service.actualizarDesdeAdmin(id, correo, telefono, rolId);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Verificar identidad para recuperación de contraseña", description = "Comprueba que el RUT, la pregunta de seguridad y la respuesta cifrada coincidan para autorizar el restablecimiento de clave.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identidad verificada correctamente"),
        @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes"),
        @ApiResponse(responseCode = "401", description = "Los datos de seguridad no coinciden")
    })
    @PostMapping("/verificar-identidad")
    public ResponseEntity<Void> verificarIdentidad(@RequestBody Map<String, String> datos) {
        String rut = datos != null ? datos.get("rut") : null;
        String pregunta = datos != null ? datos.get("preguntaSeguridad") : null;
        String respuesta = datos != null ? datos.get("respuestaSeguridad") : null;
        if (rut == null || rut.isBlank() || pregunta == null || pregunta.isBlank() || respuesta == null || respuesta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUT, pregunta y respuesta son obligatorios");
        }
        boolean valido = service.verificarIdentidadRecuperacion(rut, pregunta, respuesta);
        return valido ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @Operation(summary = "Restablecer contraseña olvidada", description = "Actualiza la contraseña del usuario identificado por su RUT con cifrado y marca primerIngreso en false.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente"),
        @ApiResponse(responseCode = "400", description = "RUT o nueva contraseña faltantes"),
        @ApiResponse(responseCode = "404", description = "Usuario activo no encontrado para el RUT indicado")
    })
    @PutMapping("/recuperar-contrasena")
    public ResponseEntity<Void> recuperarContrasena(
            @RequestBody Map<String, String> datos
    ) {
        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes completar todos los campos"
            );
        }

        service.restablecerContrasena(
                datos.get("rut"),
                datos.get("preguntaSeguridad"),
                datos.get("respuestaSeguridad"),
                datos.get("nuevaContrasena")
        );

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Subir foto de perfil (avatar)", description = "Recibe una imagen en multipart/form-data, la guarda en la carpeta uploads/avatares del servidor y almacena su URL en MySQL.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avatar actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no permitido"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Usuario> actualizarAvatar(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo
    ) {
        Usuario actualizado = service.actualizarAvatar(id, archivo);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Obtener foto de perfil (avatar)", description = "Devuelve la imagen almacenada en la carpeta del servidor para mostrarla en la app.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagen obtenida correctamente"),
        @ApiResponse(responseCode = "404", description = "Avatar no encontrado")
    })
    @GetMapping("/avatares/{nombreArchivo:.+}")
    public ResponseEntity<Resource> obtenerAvatar(@PathVariable String nombreArchivo) {
        Resource recurso = avatarStorageService.cargar(nombreArchivo);
        MediaType mediaType = avatarStorageService.detectarMediaType(nombreArchivo);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(recurso);
    }

    @Operation(summary = "Completar configuración de primer ingreso", description = "Guarda la pregunta y respuesta de seguridad cifrada, reemplaza la contraseña temporal por la definitiva y cambia primerIngreso a false.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Primer ingreso configurado correctamente"),
        @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}/primer-ingreso")
    public ResponseEntity<Usuario> configurarPrimerIngreso(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos
    ) {
        if (datos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos los campos son obligatorios");
        }
        Usuario actualizado = service.configurarPrimerIngreso(
                id,
                datos.get("preguntaSeguridad"),
                datos.get("respuestaSeguridad"),
                datos.get("nuevaContrasena")
        );
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Deshabilitar usuario", description = "Realiza soft delete cambiando activo a false para preservar su historial. Impide eliminar al único Administrador activo del sistema.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuario deshabilitado correctamente"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar al único Administrador activo"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado o ya inactivo")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boolean deshabilitado = service.deshabilitar(id);
        if (!deshabilitado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> manejarError(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : "Error en la solicitud";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Datos inválidos en la solicitud");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", mensaje));
    }
}