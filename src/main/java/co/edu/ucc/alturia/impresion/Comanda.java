package co.edu.ucc.alturia.impresion;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Comanda {
    public enum EstadoImpresion { PENDIENTE, IMPRESA, FALLIDA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long pedidoId;
    public String estacion;
    @Enumerated(EnumType.STRING)
    public TipoComanda tipo;
    @Column(length = 4000)
    public String contenido;
    @Enumerated(EnumType.STRING)
    public EstadoImpresion estadoImpresion = EstadoImpresion.PENDIENTE;
    public int intentos;
    public boolean diferida;
    public String host;
    public int puerto;
    public LocalDateTime fechaCreacion = LocalDateTime.now();
    public LocalDateTime fechaImpresion;
}
