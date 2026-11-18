package cl.rinconsur.observaciones.service;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import cl.rinconsur.observaciones.repository.RegistroObservacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistroObservacionService {

    private final RegistroObservacionRepository repository;

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
    public boolean deshabilitar(Long id) {
        RegistroObservacion registro = repository.findById(id).orElse(null);
        if (registro == null || !Boolean.TRUE.equals(registro.getActivo())) {
            return false;
        }

        registro.setActivo(false);
        repository.save(registro);
        return true;
    }
}