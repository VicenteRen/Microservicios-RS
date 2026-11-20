package cl.rinconsur.observaciones.service;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.repository.RegistroObservacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;


import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RegistroObservacionService {

    private final RegistroObservacionRepository repository;

    private final RestClient usuariosClient =
        RestClient.create("http://localhost:8082");

    public RegistroObservacionService(RegistroObservacionRepository repository) {
        this.repository = repository;
    }

    // POST: Guardar nueva observación
    public RegistroObservacion guardar(RegistroObservacion registro) {
        registro.setId(null);
        registro.setActivo(true);

        // Regla de negocio: si es Tránsito, no puede tener compras externas
        if ("Tránsito".equalsIgnoreCase(registro.getClasificacion()) ||
            "TRANSITO".equalsIgnoreCase(registro.getClasificacion())) {
            registro.setTieneComprasExternas(false);
        }

        return repository.save(registro);
    }

    // GET: Listar todos los registros activos (Para el Administrador / Dashboard)
    public List<RegistroObservacion> listarTodos() {
        return repository.findByActivoTrueOrderByIdDesc();
    }

    // GET: Listar registros activos de un observador específico
    public List<RegistroObservacion> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdAndActivoTrueOrderByIdDesc(usuarioId);
    }

    // DELETE / PUT: Deshabilitación lógica (Punto 6 del profesor: nunca borrar físicamente)
    
    @Transactional
    public void anular(
            Long idRegistro,
            String rut,
            String contrasena,
            String motivo
    ) {
        if (rut == null || rut.isBlank() ||
            contrasena == null || contrasena.isBlank() ||
            motivo == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "RUT, contraseña y motivo son obligatorios"
            );
        }

        String motivoValidado = motivo.trim();

        if (motivoValidado.length() < 10 ||
            motivoValidado.length() > 255 ||
            motivoValidado.chars().noneMatch(
                    Character::isLetter
            ) ||
            motivoValidado.chars().anyMatch(
                    Character::isISOControl
            )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo debe tener entre 10 y 255 caracteres, incluir letras y no contener caracteres de control"
            );
        }

        Map<?, ?> usuario;

        try {
            usuario = usuariosClient.post()
                    .uri("/api/usuarios/login")
                    .body(Map.of(
                            "rut", rut.trim(),
                            "contrasena", contrasena
                    ))
                    .retrieve()
                    .body(Map.class);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Contraseña o identidad incorrecta"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar la identidad"
            );

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con Usuarios"
            );
        }

        if (usuario == null ||
            !(usuario.get("id") instanceof Number) ||
            !(usuario.get("rolId") instanceof Number) ||
            !Boolean.TRUE.equals(usuario.get("activo"))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuario no autorizado"
            );
        }

        long idUsuario =
                ((Number) usuario.get("id")).longValue();

        long rolId =
                ((Number) usuario.get("rolId")).longValue();

        if (rolId != 1L && rolId != 2L) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Rol no autorizado"
            );
        }

        RegistroObservacion registro =
                repository.findById(idRegistro)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Observación no encontrada"
                                )
                        );

        if (!Boolean.TRUE.equals(registro.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta observación ya está anulada"
            );
        }

        boolean esAdministrador = rolId == 2L;

        boolean esPropietario =
                registro.getUsuarioId().equals(idUsuario);

        if (!esAdministrador && !esPropietario) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puede anular observaciones ajenas"
            );
        }

        registro.setActivo(false);
        registro.setAnuladoPor(idUsuario);
        registro.setMotivoAnulacion(motivoValidado);
        registro.setFechaAnulacion(LocalDateTime.now());

        repository.save(registro);
    }

}