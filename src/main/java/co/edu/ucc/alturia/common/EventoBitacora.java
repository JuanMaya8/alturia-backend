package co.edu.ucc.alturia.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro de eventos relevantes para diagnóstico (RNF-17). */
@Entity
public class EventoBitacora {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accion;
    private String entidad;
    private String usuario;
    private LocalDateTime fechaHora = LocalDateTime.now();

    protected EventoBitacora() {}

    public EventoBitacora(String accion, String entidad, String usuario) {
        this.accion = accion;
        this.entidad = entidad;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getUsuario() { return usuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}
