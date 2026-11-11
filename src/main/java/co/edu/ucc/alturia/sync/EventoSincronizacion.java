package co.edu.ucc.alturia.sync;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Bandeja de salida del servidor local: cada cambio se envía a la nube en orden y con identificador único. */
@Entity
public class EventoSincronizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public String idEvento = UUID.randomUUID().toString();
    public String tipo;
    public String entidad;
    public Long idEntidad;
    @Column(length = 8000)
    public String contenido;
    public boolean sincronizado;
    public int intentos;
    public LocalDateTime fecha = LocalDateTime.now();
}
