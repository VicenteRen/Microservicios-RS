package cl.rinconsur.usuarios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @NotBlank(message = "El RUT es obligatorio")
    @Column(name = "rut", nullable = false, unique = true, length = 20)
    private String rut;

    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Pattern(regexp = "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}\\s*$", message = "Formato de correo inválido")
    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @NotBlank(message = "El teléfono es obligatorio")
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "pregunta_seguridad", nullable = false)
    private String preguntaSeguridad = "¿Cuál es el nombre de tu primera mascota?";

    @Column(name = "respuesta_seguridad", nullable = false)
    private String respuestaSeguridad = "pendiente";

    @NotNull(message = "El rol es obligatorio")
    @Column(name = "id_rol", nullable = false)
    private Long rolId = 1L;

    @Column(name = "nombre_rol", nullable = false, length = 50)
    private String rolNombre = "Observador";

    @Lob
    @Column(name = "avatar", columnDefinition = "LONGBLOB")
    private byte[] avatar;

    @Column(name = "primer_ingreso", nullable = false)
    private Boolean primerIngreso = true;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @PrePersist
    @PreUpdate
    protected void normalizarDatos() {
        if (this.rut != null) {
            this.rut = this.rut.trim().toUpperCase();
        }
        if (this.nombreCompleto != null) {
            this.nombreCompleto = this.nombreCompleto.trim();
        }
        if (this.correo != null) {
            this.correo = this.correo.trim().toLowerCase();
        }
        if (this.telefono != null) {
            this.telefono = this.telefono.trim();
        }
        if (this.rolId == null) {
            this.rolId = 1L;
        }
        this.rolNombre = (this.rolId == 2L) ? "Administrador" : "Observador";
        if (this.primerIngreso == null) {
            this.primerIngreso = true;
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }
}