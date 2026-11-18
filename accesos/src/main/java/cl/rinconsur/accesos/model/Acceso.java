package cl.rinconsur.accesos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "accesos")
public class Acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acceso")
    private Long id;

    @NotBlank(message = "El código del acceso es obligatorio")
    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @NotBlank(message = "El nombre del acceso es obligatorio")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @PrePersist
    @PreUpdate
    protected void normalizarDatos() {
        if (this.codigo != null) {
            this.codigo = this.codigo.trim().toUpperCase();
        }
        if (this.nombre != null) {
            this.nombre = this.nombre.trim();
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }
}