package cl.rinconsur.accesos.repository;

import cl.rinconsur.accesos.model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    // Para el Administrador: lista todos ordenados por código (A1, A2, A3...)
    List<Acceso> findAllByOrderByCodigoAsc();

    // Para el Observador: lista únicamente los que están habilitados (activo = true)
    List<Acceso> findByActivoTrueOrderByCodigoAsc();

    // Para evitar códigos duplicados al crear un acceso nuevo
    boolean existsByCodigoIgnoreCase(String codigo);

    long countByActivoTrue();
}