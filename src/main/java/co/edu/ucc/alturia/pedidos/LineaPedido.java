package co.edu.ucc.alturia.pedidos;

import co.edu.ucc.alturia.menu.Estacion;
import co.edu.ucc.alturia.menu.Producto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class LineaPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
    public Pedido pedido;
    @ManyToOne(fetch = FetchType.EAGER)
    public Producto producto;
    @ManyToOne(fetch = FetchType.EAGER)
    public Estacion estacion;
    public int cantidad;
    public BigDecimal precioUnitario;
    @Column(length = 200)
    public String observacion;
    @Enumerated(EnumType.STRING)
    public Enums.EstadoLinea estado = Enums.EstadoLinea.ACTIVA;
    /** Avance de esta línea en su estación: RECIBIDO, EN_PREPARACION o LISTO. */
    @Enumerated(EnumType.STRING)
    public Enums.EstadoPedido avance = Enums.EstadoPedido.RECIBIDO;

    public BigDecimal subtotal() {
        return estado == Enums.EstadoLinea.ANULADA ? BigDecimal.ZERO : precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public boolean activa() { return estado != Enums.EstadoLinea.ANULADA; }
}
