package cl.rinconsur.observaciones.repository;

import cl.rinconsur.observaciones.model.RegistroObservacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroObservacionRepository extends JpaRepository<RegistroObservacion, Long> {

    // Equivalente a listarTodosLosRegistros() (Solo devuelve registros con activo = true)
    List<RegistroObservacion> findByActivoTrueOrderByIdDesc();

    // Equivalente a listarRegistrosPorUsuario(idUsuario) (Solo devuelve activos de ese observador)
    List<RegistroObservacion> findByUsuarioIdAndActivoTrueOrderByIdDesc(Long usuarioId);
}