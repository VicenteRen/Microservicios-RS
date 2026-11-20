package cl.rinconsur.observaciones.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@Table(name = "registros_observacion")
public class RegistroObservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @NotBlank(message = "La clasificación es obligatoria")
    @Column(name = "clasificacion", nullable = false, length = 50)
    private String clasificacion;

    @NotBlank(message = "El rango etario es obligatorio")
    @Column(name = "rango_etario", nullable = false, length = 50)
    private String rangoEtario;

    @NotBlank(message = "La compañía es obligatoria")
    @Column(name = "compania", nullable = false, length = 50)
    private String compania;

    @NotNull
    @Column(name = "compras_otra_tienda", nullable = false)
    private Boolean tieneComprasExternas = false;

    @Column(name = "acceso_destino", length = 100)
    private String accesoDestino;

    @Column(name = "fecha_hora", nullable = false, length = 30, updatable = false)
    private String fechaHora;

    @NotNull(message = "El ID del usuario es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    private Long usuarioId;

    @NotBlank(message = "El nombre del observador es obligatorio")
    @Column(name = "nombre_observador", nullable = false, length = 150)
    private String nombreObservador;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;


    @Column(name = "anulado_por")
    private Long anuladoPor;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;


    @PrePersist
    protected void antesDeGuardar() {
        if (this.fechaHora == null || this.fechaHora.isBlank()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            this.fechaHora = LocalDateTime.now().format(formatter);
        }
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.tieneComprasExternas == null) {
            this.tieneComprasExternas = false;
        }
    }
}