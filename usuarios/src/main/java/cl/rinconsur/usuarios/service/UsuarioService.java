package cl.rinconsur.usuarios.service;

import cl.rinconsur.usuarios.model.Usuario;
import cl.rinconsur.usuarios.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String SAL = "RinconSur2026*";
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String ESPECIALES = "*@#$_-!";
    private static final String TODOS_CARACTERES = MAYUSCULAS + MINUSCULAS + DIGITOS + ESPECIALES;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository repository;
    private final EmailService emailService;
    private final AvatarStorageService avatarStorageService;

    public UsuarioService(
            UsuarioRepository repository,
            EmailService emailService,
            AvatarStorageService avatarStorageService
    ) {
        this.repository = repository;
        this.emailService = emailService;
        this.avatarStorageService = avatarStorageService;
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
        if (contrasena.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar los 64 caracteres");
        }
        Usuario usuario = repository.findByRutIgnoreCaseAndActivoTrue(rut.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RUT o contraseña incorrecta. Vuelva a intentarlo."));

        if (!usuario.getContrasena().equals(encriptarConSal(contrasena))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RUT o contraseña incorrecta. Vuelva a intentarlo.");
        }
        return usuario;
    }

    @Transactional
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

        String nombreNormalizado = validarYNormalizarNombre(nuevo.getNombreCompleto());
        String contrasenaTemporal = generarContrasenaTemporal();

        Long rolId = (nuevo.getRolId() != null && nuevo.getRolId() == 2L) ? 2L : 1L;
        String rolNombre = (rolId == 2L) ? "Administrador" : "Observador";
        String pregunta = (nuevo.getPreguntaSeguridad() != null && !nuevo.getPreguntaSeguridad().isBlank())
                ? nuevo.getPreguntaSeguridad() : "¿Cuál es el nombre de tu primera mascota?";
        String respuestaLimpia = (nuevo.getRespuestaSeguridad() != null && !nuevo.getRespuestaSeguridad().isBlank())
                ? nuevo.getRespuestaSeguridad().trim().toLowerCase() : "pendiente";

        Usuario guardado;

        if (existenteOpt.isPresent()) {
            Usuario existente = existenteOpt.get();
            existente.setContrasena(encriptarConSal(contrasenaTemporal));
            existente.setNombreCompleto(nombreNormalizado);
            existente.setCorreo(correoLimpio);
            existente.setTelefono(telefonoLimpio);
            existente.setPreguntaSeguridad(pregunta);
            existente.setRespuestaSeguridad(encriptarConSal(respuestaLimpia));
            existente.setRolId(rolId);
            existente.setRolNombre(rolNombre);
            existente.setPrimerIngreso(true);
            existente.setActivo(true);
            guardado = repository.saveAndFlush(existente);
        } else {
            nuevo.setId(null);
            nuevo.setRut(rutLimpio);
            nuevo.setContrasena(encriptarConSal(contrasenaTemporal));
            nuevo.setNombreCompleto(nombreNormalizado);
            nuevo.setCorreo(correoLimpio);
            nuevo.setTelefono(telefonoLimpio);
            nuevo.setPreguntaSeguridad(pregunta);
            nuevo.setRespuestaSeguridad(encriptarConSal(respuestaLimpia));
            nuevo.setRolId(rolId);
            nuevo.setRolNombre(rolNombre);
            nuevo.setPrimerIngreso(true);
            nuevo.setActivo(true);
            guardado = repository.saveAndFlush(nuevo);
        }

        emailService.enviarContrasenaTemporal(
                correoLimpio,
                guardado.getNombreCompleto(),
                rutLimpio,
                contrasenaTemporal
        );

        return guardado;
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

        usuario.setNombreCompleto(validarYNormalizarNombre(nombre));
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
        validarNuevaContrasena(nuevaContrasena);
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
        validarNuevaContrasena(nuevaContrasena);
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

    public boolean restablecerContrasena(
            String rut,
            String pregunta,
            String respuesta,
            String nuevaContrasena
    ) {
        if (rut == null || pregunta == null || respuesta == null
                || nuevaContrasena == null
                || rut.isBlank() || pregunta.isBlank()
                || respuesta.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Todos los campos son obligatorios"
            );
        }

        boolean valida = nuevaContrasena.length() >= 8
                && nuevaContrasena.length() <= 64
                && nuevaContrasena.chars().anyMatch(Character::isUpperCase)
                && nuevaContrasena.chars().anyMatch(Character::isDigit)
                && nuevaContrasena.chars().anyMatch(
                        c -> !Character.isLetterOrDigit(c)
                                && !Character.isWhitespace(c)
                );

        if (!valida) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener entre 8 y 64 caracteres, una mayúscula, un número y un carácter especial"
            );
        }

        if (!verificarIdentidadRecuperacion(rut, pregunta, respuesta)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Los datos de seguridad son incorrectos"
            );
        }

        Usuario usuario = repository
                .findByRutIgnoreCaseAndActivoTrue(rut.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No fue posible verificar la identidad"
                ));

        usuario.setContrasena(encriptarConSal(nuevaContrasena));
        usuario.setPrimerIngreso(false);
        repository.save(usuario);

        return true;
    }

    public Usuario actualizarAvatar(Long id, MultipartFile archivo) {
        Usuario usuario = buscarPorId(id);
        String avatarAnterior = usuario.getAvatarUrl();
        String nombreArchivo = avatarStorageService.guardar(id, archivo);
        usuario.setAvatarUrl("/api/usuarios/avatares/" + nombreArchivo);
        Usuario guardado = repository.save(usuario);
        avatarStorageService.eliminarSiExiste(avatarAnterior);
        return guardado;
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

    private String generarContrasenaTemporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(MAYUSCULAS.charAt(RANDOM.nextInt(MAYUSCULAS.length())));
        caracteres.add(MINUSCULAS.charAt(RANDOM.nextInt(MINUSCULAS.length())));
        caracteres.add(DIGITOS.charAt(RANDOM.nextInt(DIGITOS.length())));
        caracteres.add(ESPECIALES.charAt(RANDOM.nextInt(ESPECIALES.length())));

        while (caracteres.size() < 10) {
            caracteres.add(TODOS_CARACTERES.charAt(RANDOM.nextInt(TODOS_CARACTERES.length())));
        }

        Collections.shuffle(caracteres, RANDOM);
        StringBuilder sb = new StringBuilder(caracteres.size());
        for (char c : caracteres) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String validarYNormalizarNombre(String nombre) {
        if (nombre == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio"
            );
        }

        String nombreLimpio = nombre.trim().replaceAll(" +", " ");

        if (nombreLimpio.length() < 2
                || nombreLimpio.length() > 100
                || !nombreLimpio.matches("^[\\p{L}]+(?: [\\p{L}]+)*$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre debe tener entre 2 y 100 caracteres, solo letras y espacios"
            );
        }

        Locale locale = Locale.forLanguageTag("es-CL");
        String[] palabras = nombreLimpio.toLowerCase(locale).split(" ");
        for (int i = 0; i < palabras.length; i++) {
            palabras[i] = palabras[i].substring(0, 1).toUpperCase(locale)
                    + palabras[i].substring(1);
        }
        return String.join(" ", palabras);
    }

    private void validarNuevaContrasena(String contrasena) {
        if (contrasena == null
                || contrasena.length() < 8
                || contrasena.length() > 64
                || contrasena.chars().noneMatch(Character::isUpperCase)
                || contrasena.chars().noneMatch(Character::isDigit)
                || contrasena.chars().noneMatch(
                        c -> !Character.isLetterOrDigit(c)
                                && !Character.isWhitespace(c)
                )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener entre 8 y 64 caracteres, una mayúscula, un número y un carácter especial"
            );
        }
    }
}