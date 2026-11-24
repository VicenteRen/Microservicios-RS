package cl.rinconsur.accesos.service;

import cl.rinconsur.accesos.model.Acceso;
import cl.rinconsur.accesos.repository.AccesoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccesoService {

    private static final int MAX_ACTIVOS = 5;

    private final AccesoRepository repository;

    public AccesoService(AccesoRepository repository) {
        this.repository = repository;
    }

    // GET: Todos los accesos (Vista Administrador)
    public List<Acceso> listarTodos() {
        return repository.findAllByOrderByCodigoAsc();
    }

    // GET: Solo accesos habilitados (Vista Observador)
    public List<Acceso> listarActivos() {
        return repository.findByActivoTrueOrderByCodigoAsc();
    }

    // POST: Crear un nuevo acceso (Nace deshabilitado si ya hay 5 activos)
    public Acceso guardar(Acceso acceso) {
        if (acceso.getCodigo() == null || acceso.getNombre() == null || acceso.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código y el nombre son obligatorios");
        }

        String codigoLimpio = acceso.getCodigo().trim().toUpperCase();

        if (!codigoLimpio.matches("^[A-ZÑ][0-9]$")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El código debe tener una letra (A-Z o Ñ) seguida de un número (0-9), ej: A5"
            );
        }

        if (repository.existsByCodigoIgnoreCase(codigoLimpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe el acceso " + codigoLimpio);
        }

        boolean puedeActivarse = repository.countByActivoTrue() < MAX_ACTIVOS;

        acceso.setId(null);
        acceso.setCodigo(codigoLimpio);
        acceso.setNombre(acceso.getNombre().trim());
        acceso.setActivo(puedeActivarse);
        return repository.save(acceso);
    }

    // PUT: Habilitar / Deshabilitar un acceso (invierte true <-> false)
    public Acceso cambiarEstado(Long id) {
        Acceso acceso = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acceso no encontrado"));

        boolean nuevoEstado = !Boolean.TRUE.equals(acceso.getActivo());

        if (nuevoEstado && repository.countByActivoTrue() >= MAX_ACTIVOS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Máximo " + MAX_ACTIVOS + " accesos activos simultáneamente. Deshabilita otro primero."
            );
        }

        acceso.setActivo(nuevoEstado);
        return repository.save(acceso);
    }
}