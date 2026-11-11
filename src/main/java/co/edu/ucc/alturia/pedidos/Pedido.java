package co.edu.ucc.alturia.pedidos;

import co.edu.ucc.alturia.mesas.Mesa;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    /** UUID generado en la tablet: evita duplicados al reenviar sin conexión (idempotencia). */
    @Column(unique = true, nullable = false)
    public String idLocal;
    @ManyToOne(fetch = FetchType.EAGER)
    public Mesa mesa;
    public Long meseroId;
    public String meseroNombre;
    @Enumerated(EnumType.STRING)
    public Enums.OrigenPedido origen = Enums.OrigenPedido.MANUAL;
    @Enumerated(EnumType.STRING)
    public Enums.EstadoPedido estado = Enums.EstadoPedido.RECIBIDO;
    public LocalDateTime fechaCreacion = LocalDateTime.now();
    public LocalDateTime fechaConfirmacion;
    public LocalDateTime fechaListo;
    public LocalDateTime fechaEntrega;
    public boolean modificado;
    public Long cuentaId;
    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    public List<String> restricciones = new ArrayList<>();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    public List<LineaPedido> lineas = new ArrayList<>();

    public BigDecimal total() {
        return lineas.stream().map(LineaPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean tieneAlertas() {
        return !restricciones.isEmpty() || lineas.stream().anyMatch(l -> l.activa() && !l.producto.alergenos.isEmpty());
    }

    /** El estado general se deriva del avance de las líneas activas en todas las estaciones. */
    public void recalcularEstado() {
        if (estado == Enums.EstadoPedido.ENTREGADO || estado == Enums.EstadoPedido.ANULADO) return;
        List<LineaPedido> activas = lineas.stream().filter(LineaPedido::activa).toList();
        if (activas.isEmpty()) { estado = Enums.EstadoPedido.ANULADO; return; }
        if (activas.stream().allMatch(l -> l.avance == Enums.EstadoPedido.LISTO)) {
            if (estado != Enums.EstadoPedido.LISTO) fechaListo = LocalDateTime.now();
            estado = Enums.EstadoPedido.LISTO;
        } else if (activas.stream().allMatch(l -> l.avance == Enums.EstadoPedido.RECIBIDO)) {
            estado = Enums.EstadoPedido.RECIBIDO;
        } else {
            estado = Enums.EstadoPedido.EN_PREPARACION;
        }
    }
}
