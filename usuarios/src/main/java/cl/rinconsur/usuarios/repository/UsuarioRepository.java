package cl.rinconsur.usuarios.repository;

import cl.rinconsur.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByActivoTrueOrderByNombreCompletoAsc();
    Optional<Usuario> findByRutIgnoreCaseAndActivoTrue(String rut);
    Optional<Usuario> findByRutIgnoreCase(String rut);
    long countByRolIdAndActivoTrue(Long rolId);
    boolean existsByCorreoIgnoreCaseAndActivoTrue(String correo);
    boolean existsByTelefonoAndActivoTrue(String telefono);
    boolean existsByCorreoIgnoreCaseAndActivoTrueAndIdNot(String correo, Long id);
    boolean existsByTelefonoAndActivoTrueAndIdNot(String telefono, Long id);
}