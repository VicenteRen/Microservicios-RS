package cl.rinconsur.usuarios.service;

import cl.rinconsur.usuarios.model.Usuario;
import cl.rinconsur.usuarios.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String SAL = "RinconSur2026*";

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public String encriptarConSal(String texto) {
        try {
            String textoConSal = SAL + texto.trim();
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(textoConSal.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar", e);
        }
    }

    @PostConstruct
    public void precargarUsuarios() {
        if (repository.count() == 0) {
            Usuario obs = new Usuario();
            obs.setRut("214297353");
            obs.setContrasena(encriptarConSal("Observador123*"));
            obs.setNombreCompleto("Vicente Ponce");
            obs.setCorreo("v.ponce@rinconsur.cl");
            obs.setTelefono("987654321");
            obs.setPreguntaSeguridad("¿Cuál es el nombre de tu primera mascota?");
            obs.setRespuestaSeguridad(encriptarConSal("princesa"));
            obs.setRolId(1L);
            obs.setRolNombre("Observador");
            obs.setPrimerIngreso(false);
            obs.setActivo(true);
            repository.save(obs);

            Usuario admin = new Usuario();
            admin.setRut("228405566");
            admin.setContrasena(encriptarConSal("Admin123*"));
            admin.setNombreCompleto("Jefe Administrador");
            admin.setCorreo("admin@rinconsur.cl");
            admin.setTelefono("911223344");
            admin.setPreguntaSeguridad("¿En qué ciudad se conocieron tus padres?");
            admin.setRespuestaSeguridad(encriptarConSal("santiago"));
            admin.setRolId(2L);
            admin.setRolNombre("Administrador");
            admin.setPrimerIngreso(false);
            admin.setActivo(true);
            repository.save(admin);
        }
    }

    public List<Usuario> listarActivos() {
        return repository.findByActivoTrueOrderByNombreCompletoAsc();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario autenticar(String rut, String contrasena) {
        if (rut == null || contrasena == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUT y contraseña obligatorios");
        }
        Usuario usuario = repository.findByRutIgnoreCaseAndActivoTrue(rut.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RUT o contraseña incorrecta. Vuelva a intentarlo."));

        if (!usuario.getContrasena().equals(encriptarConSal(contrasena))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RUT o contraseña incorrecta. Vuelva a intentarlo.");
        }
        return usuario;
    }

    public Usuario guardar(Usuario nuevo) {
        String rutLimpio = nuevo.getRut().trim().toUpperCase();
        String correoLimpio = nuevo.getCorreo().trim().toLowerCase();
        String telefonoLimpio = nuevo.getTelefono().trim();

        Optional<Usuario> existenteOpt = repository.findByRutIgnoreCase(rutLimpio);

        if (existenteOpt.isPresent() && Boolean.TRUE.equals(existenteOpt.get().getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El RUT ya existe y está activo en el sistema");
        }
        if (repository.existsByCorreoIgnoreCaseAndActivoTrue(correoLimpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado por otro usuario activo");
        }
        if (repository.existsByTelefonoAndActivoTrue(telefonoLimpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El teléfono ya está registrado por otro usuario activo");
        }

        Long rolId = (nuevo.getRolId() != null && nuevo.getRolId() == 2L) ? 2L : 1L;
        String rolNombre = (rolId == 2L) ? "Administrador" : "Observador";
        String pregunta = (nuevo.getPreguntaSeguridad() != null && !nuevo.getPreguntaSeguridad().isBlank())
                ? nuevo.getPreguntaSeguridad() : "¿Cuál es el nombre de tu primera mascota?";
        String respuestaLimpia = (nuevo.getRespuestaSeguridad() != null && !nuevo.getRespuestaSeguridad().isBlank())
                ? nuevo.getRespuestaSeguridad().trim().toLowerCase() : "pendiente";

        if (existenteOpt.isPresent()) {
            Usuario existente = existenteOpt.get();
            existente.setContrasena(encriptarConSal(nuevo.getContrasena()));
            existente.setNombreCompleto(nuevo.getNombreCompleto().trim());
            existente.setCorreo(correoLimpio);
            existente.setTelefono(telefonoLimpio);
            existente.setPreguntaSeguridad(pregunta);
            existente.setRespuestaSeguridad(encriptarConSal(respuestaLimpia));
            existente.setRolId(rolId);
            existente.setRolNombre(rolNombre);
            existente.setPrimerIngreso(true);
            existente.setActivo(true);
            return repository.save(existente);
        }

        nuevo.setId(null);
        nuevo.setRut(rutLimpio);
        nuevo.setContrasena(encriptarConSal(nuevo.getContrasena()));
        nuevo.setCorreo(correoLimpio);
        nuevo.setTelefono(telefonoLimpio);
        nuevo.setPreguntaSeguridad(pregunta);
        nuevo.setRespuestaSeguridad(encriptarConSal(respuestaLimpia));
        nuevo.setRolId(rolId);
        nuevo.setRolNombre(rolNombre);
        nuevo.setPrimerIngreso(true);
        nuevo.setActivo(true);
        return repository.save(nuevo);
    }

    public Usuario actualizarPerfil(Long id, String nombre, String correo, String telefono) {
        Usuario usuario = buscarPorId(id);
        String correoLimpio = correo.trim().toLowerCase();
        String telefonoLimpio = telefono.trim();

        if (repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot(correoLimpio, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado por otro usuario");
        }
        if (repository.existsByTelefonoAndActivoTrueAndIdNot(telefonoLimpio, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El teléfono ya está registrado por otro usuario");
        }

        if (nombre != null && !nombre.isBlank()) {
            usuario.setNombreCompleto(nombre.trim());
        }
        usuario.setCorreo(correoLimpio);
        usuario.setTelefono(telefonoLimpio);
        return repository.save(usuario);
    }

    public Usuario actualizarDesdeAdmin(Long id, String correo, String telefono, Long rolId) {
        Usuario usuario = buscarPorId(id);
        String correoLimpio = correo.trim().toLowerCase();
        String telefonoLimpio = telefono.trim();

        if (repository.existsByCorreoIgnoreCaseAndActivoTrueAndIdNot(correoLimpio, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado por otro usuario");
        }
        if (repository.existsByTelefonoAndActivoTrueAndIdNot(telefonoLimpio, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El teléfono ya está registrado por otro usuario");
        }

        usuario.setCorreo(correoLimpio);
        usuario.setTelefono(telefonoLimpio);
        usuario.setRolId(rolId);
        usuario.setRolNombre((rolId != null && rolId == 2L) ? "Administrador" : "Observador");
        return repository.save(usuario);
    }

    public Usuario cambiarContrasenaDesdePerfil(Long id, String contrasenaActual, String nuevaContrasena) {
        Usuario usuario = buscarPorId(id);
        if (!usuario.getContrasena().equals(encriptarConSal(contrasenaActual))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        usuario.setContrasena(encriptarConSal(nuevaContrasena));
        usuario.setPrimerIngreso(false);
        return repository.save(usuario);
    }

    public Usuario configurarPrimerIngreso(Long id, String preguntaSeguridad, String respuestaSeguridad, String nuevaContrasena) {
        Usuario usuario = buscarPorId(id);
        if (preguntaSeguridad == null || preguntaSeguridad.isBlank()
                || respuestaSeguridad == null || respuestaSeguridad.isBlank()
                || nuevaContrasena == null || nuevaContrasena.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos los campos son obligatorios");
        }
        usuario.setPreguntaSeguridad(preguntaSeguridad.trim());
        usuario.setRespuestaSeguridad(encriptarConSal(respuestaSeguridad.trim().toLowerCase()));
        usuario.setContrasena(encriptarConSal(nuevaContrasena));
        usuario.setPrimerIngreso(false);
        return repository.save(usuario);
    }

    public boolean verificarIdentidadRecuperacion(String rut, String pregunta, String respuesta) {
        Optional<Usuario> usuarioOpt = repository.findByRutIgnoreCaseAndActivoTrue(rut.trim());
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();
        if (!usuario.getPreguntaSeguridad().equals(pregunta)) return false;
        return usuario.getRespuestaSeguridad().equals(encriptarConSal(respuesta.toLowerCase()));
    }

    public boolean restablecerContrasena(String rut, String nuevaContrasena) {
        Optional<Usuario> usuarioOpt = repository.findByRutIgnoreCaseAndActivoTrue(rut.trim());
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();
        usuario.setContrasena(encriptarConSal(nuevaContrasena));
        usuario.setPrimerIngreso(false);
        repository.save(usuario);
        return true;
    }

    public Usuario actualizarAvatar(Long id, byte[] avatar) {
        Usuario usuario = buscarPorId(id);
        usuario.setAvatar(avatar);
        return repository.save(usuario);
    }

    public boolean deshabilitar(Long id) {
        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return false;
        }
        if (usuario.getRolId() == 2L && repository.countByRolIdAndActivoTrue(2L) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar al único Administrador activo");
        }
        usuario.setActivo(false);
        repository.save(usuario);
        return true;
    }
}