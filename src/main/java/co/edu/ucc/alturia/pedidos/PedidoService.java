package co.edu.ucc.alturia.pedidos;

import co.edu.ucc.alturia.auth.Sesion;
import co.edu.ucc.alturia.caja.CajaService;
import co.edu.ucc.alturia.common.ApiException;
import co.edu.ucc.alturia.common.BitacoraService;
import co.edu.ucc.alturia.impresion.ImpresionService;
import co.edu.ucc.alturia.impresion.TipoComanda;
import co.edu.ucc.alturia.menu.Estacion;
import co.edu.ucc.alturia.menu.MenuRepositories;
import co.edu.ucc.alturia.menu.Producto;
import co.edu.ucc.alturia.mesas.EstadoMesa;
import co.edu.ucc.alturia.mesas.Mesa;
import co.edu.ucc.alturia.mesas.MesaRepository;
import co.edu.ucc.alturia.nlp.EjemploNlp;
import co.edu.ucc.alturia.pedidos.Enums.EstadoLinea;
import co.edu.ucc.alturia.pedidos.Enums.EstadoPedido;
import co.edu.ucc.alturia.pedidos.PedidoDtos.*;
import co.edu.ucc.alturia.sync.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recepción de pedidos, enrutamiento por estación y cambios de estado desde el KDS.
 * Componentes «Recepción de pedidos», «Enrutador por estación» y «Registro de tiempos» del diagrama C4 nivel 3.
 */
@Service
public class PedidoService {
    static final List<EstadoPedido> EN_COCINA = List.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION);
    static final List<EstadoPedido> ACTIVOS = List.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION,
            EstadoPedido.LISTO, EstadoPedido.REQUIERE_REVISION);

    private final PedidoRepositories.Pedidos pedidos;
    private final PedidoRepositories.Tiempos tiempos;
    private final MenuRepositories.Productos productos;
    private final MenuRepositories.Estaciones estaciones;
    private final MesaRepository mesas;
    private final ImpresionService impresion;
    private final CajaService caja;
    private final OutboxService outbox;
    private final EjemploNlp.Repo ejemplos;
    private final BitacoraService bitacora;
    private final SimpMessagingTemplate ws;
    private final int minutosLimite;
    private final int segundosDeshacer;

    public PedidoService(PedidoRepositories.Pedidos pedidos, PedidoRepositories.Tiempos tiempos,
                         MenuRepositories.Productos productos, MenuRepositories.Estaciones estaciones, MesaRepository mesas,
                         ImpresionService impresion, CajaService caja, OutboxService outbox, EjemploNlp.Repo ejemplos,
                         BitacoraService bitacora, SimpMessagingTemplate ws,
                         @Value("${alturia.pedidos.minutos-limite-espera}") int minutosLimite,
                         @Value("${alturia.pedidos.segundos-deshacer}") int segundosDeshacer) {
        this.pedidos = pedidos; this.tiempos = tiempos; this.productos = productos; this.estaciones = estaciones;
        this.mesas = mesas; this.impresion = impresion; this.caja = caja; this.outbox = outbox; this.ejemplos = ejemplos;
        this.bitacora = bitacora; this.ws = ws; this.minutosLimite = minutosLimite; this.segundosDeshacer = segundosDeshacer;
    }

    public PedidoDto dto(Pedido p) { return PedidoDto.de(p, minutosLimite); }

    /** HU-13/HU-14/HU-17: confirma un pedido. Si el idLocal ya existe, devuelve el existente (no duplica). */
    @Transactional
    public PedidoDto crear(PedidoRequest r, Sesion s) {
        Optional<Pedido> existente = pedidos.findByIdLocal(r.idLocal());
        if (existente.isPresent()) return dto(existente.get());

        Mesa mesa = mesas.findById(r.mesaId()).orElseThrow(() -> ApiException.noEncontrado("La mesa"));
        Pedido p = new Pedido();
        p.idLocal = r.idLocal();
        p.mesa = mesa;
        p.meseroId = s.usuarioId();
        p.meseroNombre = s.nombre();
        p.origen = r.origen() == null ? Enums.OrigenPedido.MANUAL : r.origen();
        p.restricciones = r.restricciones() == null ? new ArrayList<>() : new ArrayList<>(r.restricciones());
        p.fechaConfirmacion = LocalDateTime.now();
        boolean requiereRevision = false;
        for (LineaRequest lr : r.lineas()) {
            LineaPedido l = nuevaLinea(p, lr);
            if (!l.producto.disponible || !l.producto.activo) requiereRevision = true;
            p.lineas.add(l);
        }
        p.cuentaId = caja.cuentaAbierta(mesa).id;
        mesa.estado = EstadoMesa.OCUPADA;
        mesas.save(mesa);

        if (requiereRevision) {
            // HU-17: un producto se agotó mientras la tablet estaba sin conexión
            p.estado = EstadoPedido.REQUIERE_REVISION;
            pedidos.save(p);
            ws.convertAndSend("/topic/sala/avisos", Map.of("tipo", "REQUIERE_REVISION", "pedidoId", p.id,
                    "mesa", mesa.numero, "meseroId", String.valueOf(p.meseroId),
                    "mensaje", "El pedido de la mesa " + mesa.numero + " tiene productos no disponibles"));
            return dto(p);
        }
        pedidos.save(p);
        estacionesDe(p.lineas).forEach(e -> registrarTiempo(p.id, e, EstadoPedido.RECIBIDO, null));
        if (p.origen == Enums.OrigenPedido.LENGUAJE_NATURAL && r.fraseOriginal() != null) {
            ejemplos.save(new EjemploNlp(r.fraseOriginal(), resumen(p), Boolean.TRUE.equals(r.corregidoPorMesero())));
        }
        impresion.generar(p, TipoComanda.NUEVA, p.lineas);
        publicar(p);
        outbox.registrar("PEDIDO_CONFIRMADO", "Pedido", p.id, dto(p));
        bitacora.registrar("Pedido confirmado mesa " + mesa.numero, "Pedido#" + p.id, s.nombre());
        return dto(p);
    }

    /** HU-15: modifica un pedido ya enviado; imprime una comanda solo con los cambios. */
    @Transactional
    public PedidoDto modificar(Long id, List<LineaRequest> nuevas, Sesion s) {
        Pedido p = buscar(id);
        if (p.estado == EstadoPedido.LISTO || p.estado == EstadoPedido.ENTREGADO || p.estado == EstadoPedido.ANULADO)
            throw ApiException.conflicto("El pedido ya no puede modificarse");
        List<LineaRequest> pendientes = new ArrayList<>(nuevas);
        List<LineaPedido> cambios = new ArrayList<>();
        for (LineaPedido l : p.lineas) {
            if (!l.activa()) continue;
            Optional<LineaRequest> igual = pendientes.stream().filter(n -> n.productoId().equals(l.producto.id)
                    && n.cantidad() == l.cantidad && Objects.equals(vacio(n.observacion()), vacio(l.observacion))).findFirst();
            if (igual.isPresent()) pendientes.remove(igual.get());
            else { l.estado = EstadoLinea.ANULADA; cambios.add(l); }
        }
        for (LineaRequest n : pendientes) {
            LineaPedido l = nuevaLinea(p, n);
            l.estado = EstadoLinea.MODIFICADA;
            p.lineas.add(l);
            cambios.add(l);
        }
        if (cambios.isEmpty()) return dto(p);
        p.modificado = true;
        p.recalcularEstado();
        pedidos.save(p);
        impresion.generar(p, TipoComanda.MODIFICACION, cambios);
        publicar(p);
        outbox.registrar("PEDIDO_MODIFICADO", "Pedido", p.id, dto(p));
        bitacora.registrar("Pedido modificado", "Pedido#" + p.id, s.nombre());
        return dto(p);
    }

    /** HU-18/HU-26: avanza el estado de las líneas de una estación (tecla Enter del KDS). */
    @Transactional
    public PedidoDto avanzar(Long id, String estacion, boolean verificado) {
        Pedido p = buscar(id);
        List<LineaPedido> lineas = lineasDe(p, estacion);
        if (lineas.isEmpty()) throw ApiException.invalido("El pedido no tiene productos para la estación " + estacion);
        EstadoPedido actual = lineas.stream().map(l -> l.avance).min(Comparator.naturalOrder()).orElseThrow();
        EstadoPedido siguiente = switch (actual) {
            case RECIBIDO -> EstadoPedido.EN_PREPARACION;
            case EN_PREPARACION -> EstadoPedido.LISTO;
            default -> throw ApiException.conflicto("Los productos de esta estación ya están listos");
        };
        if (siguiente == EstadoPedido.LISTO && p.tieneAlertas() && !verificado)
            throw ApiException.conflicto("¿Verificó la restricción? Pulse + para confirmar o − para cancelar");
        lineas.forEach(l -> l.avance = siguiente);
        registrarTiempo(p.id, estacion, siguiente, actual);
        p.recalcularEstado();
        pedidos.save(p);
        publicar(p);
        if (p.estado == EstadoPedido.LISTO) {
            ws.convertAndSend("/topic/sala/avisos", Map.of("tipo", "PEDIDO_LISTO", "pedidoId", p.id,
                    "mesa", p.mesa.numero, "meseroId", String.valueOf(p.meseroId), "mensaje", "Pedido de la mesa " + p.mesa.numero + " listo"));
            outbox.registrar("PEDIDO_LISTO", "Pedido", p.id, dto(p));
        }
        return dto(p);
    }

    /** HU-18: tecla «0», deshace el último cambio de la estación dentro de los segundos permitidos. */
    @Transactional
    public PedidoDto deshacer(Long id, String estacion) {
        Pedido p = buscar(id);
        RegistroTiempo ultimo = tiempos.findFirstByPedidoIdAndEstacionOrderByIdDesc(id, estacion)
                .filter(t -> t.estadoAnterior != null)
                .filter(t -> Duration.between(t.fechaHora, LocalDateTime.now()).getSeconds() <= segundosDeshacer)
                .orElseThrow(() -> ApiException.conflicto("Ya no es posible deshacer este cambio"));
        lineasDe(p, estacion).forEach(l -> l.avance = ultimo.estadoAnterior);
        tiempos.delete(ultimo);
        if (p.estado == EstadoPedido.LISTO) p.fechaListo = null;
        p.estado = EstadoPedido.EN_PREPARACION;
        p.recalcularEstado();
        pedidos.save(p);
        publicar(p);
        return dto(p);
    }

    /** HU-19: el mesero marca el pedido como entregado. */
    @Transactional
    public PedidoDto entregar(Long id, Sesion s) {
        Pedido p = buscar(id);
        if (p.estado != EstadoPedido.LISTO) throw ApiException.conflicto("Solo se pueden entregar pedidos listos");
        p.estado = EstadoPedido.ENTREGADO;
        p.fechaEntrega = LocalDateTime.now();
        pedidos.save(p);
        publicar(p);
        outbox.registrar("PEDIDO_ENTREGADO", "Pedido", p.id, dto(p));
        return dto(p);
    }

    public List<PedidoDto> activos() {
        return pedidos.findByEstadoInOrderByFechaConfirmacion(ACTIVOS).stream().map(this::dto).toList();
    }

    /** Cola de una estación: solo las líneas que le corresponden y que aún no están listas (RF-028, RF-034). */
    public List<PedidoDto> cola(String estacion) {
        return pedidos.findByEstadoInOrderByFechaConfirmacion(EN_COCINA).stream()
                .filter(p -> lineasDe(p, estacion).stream().anyMatch(l -> l.avance != EstadoPedido.LISTO))
                .map(p -> {
                    PedidoDto d = dto(p);
                    List<LineaDto> propias = d.lineas().stream().filter(l -> l.estacion().equalsIgnoreCase(estacion)
                            && l.estado() != EstadoLinea.ANULADA).toList();
                    return new PedidoDto(d.id(), d.idLocal(), d.mesaId(), d.mesa(), d.mesero(), d.estado(), d.origen(),
                            d.fechaConfirmacion(), d.minutos(), d.atrasado(), d.modificado(), d.alertas(), d.restricciones(), d.total(), propias);
                }).toList();
    }

    /** HU-29: unidades pendientes de cada producto en la estación. */
    public List<ConsolidadoItem> consolidado(String estacion) {
        Map<String, Integer> suma = new TreeMap<>();
        pedidos.findByEstadoInOrderByFechaConfirmacion(EN_COCINA).forEach(p -> lineasDe(p, estacion).stream()
                .filter(l -> l.avance != EstadoPedido.LISTO)
                .forEach(l -> suma.merge(l.producto.nombre, l.cantidad, Integer::sum)));
        return suma.entrySet().stream().map(e -> new ConsolidadoItem(e.getKey(), null, e.getValue())).toList();
    }

    public Pedido buscar(Long id) { return pedidos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El pedido")); }

    private LineaPedido nuevaLinea(Pedido p, LineaRequest lr) {
        Producto prod = productos.findById(lr.productoId()).orElseThrow(() -> ApiException.noEncontrado("El producto " + lr.productoId()));
        LineaPedido l = new LineaPedido();
        l.pedido = p;
        l.producto = prod;
        l.estacion = prod.estacion != null ? prod.estacion : estaciones.findFirstByPorDefectoTrue().orElse(null);
        l.cantidad = lr.cantidad();
        l.precioUnitario = prod.precio;
        l.observacion = vacio(lr.observacion());
        return l;
    }

    private List<LineaPedido> lineasDe(Pedido p, String estacion) {
        return p.lineas.stream().filter(LineaPedido::activa)
                .filter(l -> l.estacion != null && l.estacion.nombre.equalsIgnoreCase(estacion)).toList();
    }

    private Set<String> estacionesDe(List<LineaPedido> lineas) {
        return lineas.stream().map(l -> l.estacion).filter(Objects::nonNull).map((Estacion e) -> e.nombre).collect(Collectors.toSet());
    }

    private void registrarTiempo(Long pedidoId, String estacion, EstadoPedido estado, EstadoPedido anterior) {
        RegistroTiempo t = new RegistroTiempo();
        t.pedidoId = pedidoId;
        t.estacion = estacion;
        t.estado = estado;
        t.estadoAnterior = anterior;
        tiempos.save(t);
    }

    private void publicar(Pedido p) { ws.convertAndSend("/topic/kds", dto(p)); }

    private static String vacio(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String resumen(Pedido p) {
        return p.lineas.stream().map(l -> l.cantidad + " x " + l.producto.nombre + (l.observacion == null ? "" : " (" + l.observacion + ")"))
                .collect(Collectors.joining("; "));
    }
}
