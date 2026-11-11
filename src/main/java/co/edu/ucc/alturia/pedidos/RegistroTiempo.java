package co.edu.ucc.alturia.pedidos;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Hora de cada cambio de estado por estación (RF-021). Sirve para medir tiempos y para «deshacer». */
@Entity
public class RegistroTiempo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long pedidoId;
    public String estacion;
    @Enumerated(EnumType.STRING)
    public Enums.EstadoPedido estado;
    @Enumerated(EnumType.STRING)
    public Enums.EstadoPedido estadoAnterior;
    public LocalDateTime fechaHora = LocalDateTime.now();
}
