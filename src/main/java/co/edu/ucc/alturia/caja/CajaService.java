package co.edu.ucc.alturia.caja;

import co.edu.ucc.alturia.auth.Sesion;
import co.edu.ucc.alturia.caja.CajaDtos.*;
import co.edu.ucc.alturia.common.ApiException;
import co.edu.ucc.alturia.common.BitacoraService;
import co.edu.ucc.alturia.mesas.EstadoMesa;
import co.edu.ucc.alturia.mesas.Mesa;
import co.edu.ucc.alturia.mesas.MesaRepository;
import co.edu.ucc.alturia.pedidos.Enums;
import co.edu.ucc.alturia.pedidos.LineaPedido;
import co.edu.ucc.alturia.pedidos.Pedido;
import co.edu.ucc.alturia.pedidos.PedidoRepositories;
import co.edu.ucc.alturia.sync.OutboxService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Servicio de caja (RF-033 y RF-037): cuenta de la mesa, pago presencial y comprobante interno. */
@Service
public class CajaService {
    private final CajaRepositories.Cuentas cuentas;
    private final CajaRepositories.Pagos pagos;
    private final CajaRepositories.Comprobantes comprobantes;
    private final PedidoRepositories.Pedidos pedidos;
    private final MesaRepository mesas;
    private final OutboxService outbox;
    private final BitacoraService bitacora;
    private final SimpMessagingTemplate ws;

    public CajaService(CajaRepositories.Cuentas cuentas, CajaRepositories.Pagos pagos, CajaRepositories.Comprobantes comprobantes,
                       @Lazy PedidoRepositories.Pedidos pedidos, MesaRepository mesas, OutboxService outbox,
                       BitacoraService bitacora, SimpMessagingTemplate ws) {
        this.cuentas = cuentas; this.pagos = pagos; this.comprobantes = comprobantes; this.pedidos = pedidos;
        this.mesas = mesas; this.outbox = outbox; this.bitacora = bitacora; this.ws = ws;
    }

    /** Devuelve la cuenta abierta de la mesa o abre una nueva. */
    public Cuenta cuentaAbierta(Mesa mesa) {
        return cuentas.findFirstByMesaIdAndEstado(mesa.id, Cuenta.EstadoCuenta.ABIERTA).orElseGet(() -> {
            Cuenta c = new Cuenta();
            c.mesaId = mesa.id;
            c.mesaNumero = mesa.numero;
            return cuentas.save(c);
        });
    }

    /** HU-31: detalle de lo consumido (incluye modificaciones, excluye anulados). */
    public CuentaDto cuentaDeMesa(Long mesaId) {
        Mesa mesa = mesas.findById(mesaId).orElseThrow(() -> ApiException.noEncontrado("La mesa"));
        Optional<Cuenta> abierta = cuentas.findFirstByMesaIdAndEstado(mesaId, Cuenta.EstadoCuenta.ABIERTA);
        if (abierta.isEmpty()) throw ApiException.noEncontrado("La mesa no tiene consumos registrados. La cuenta");
        return detalle(abierta.get(), mesa.numero);
    }

    public List<CuentaDto> cuentasAbiertas() {
        return cuentas.findByEstado(Cuenta.EstadoCuenta.ABIERTA).stream().map(c -> detalle(c, c.mesaNumero))
                .filter(d -> !d.items().isEmpty()).toList();
    }

    CuentaDto detalle(Cuenta c, int mesa) {
        Map<String, ItemCuenta> items = new LinkedHashMap<>();
        boolean sinEntregar = false;
        for (Pedido p : pedidos.findByCuentaId(c.id)) {
            if (p.estado == Enums.EstadoPedido.ANULADO) continue;
            boolean entregado = p.estado == Enums.EstadoPedido.ENTREGADO;
            if (!entregado) sinEntregar = true;
            for (LineaPedido l : p.lineas) {
                if (!l.activa()) continue;
                String clave = l.producto.nombre + "|" + l.precioUnitario + "|" + entregado;
                ItemCuenta prev = items.get(clave);
                int cant = l.cantidad + (prev == null ? 0 : prev.cantidad());
                items.put(clave, new ItemCuenta(l.producto.nombre, cant, l.precioUnitario,
                        l.precioUnitario.multiply(BigDecimal.valueOf(cant)), entregado));
            }
        }
        BigDecimal total = items.values().stream().map(ItemCuenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuentaDto(c.id, c.mesaId, mesa, c.estado.name(), new ArrayList<>(items.values()), total, sinEntregar,
                comprobantes.findByCuentaId(c.id).orElse(null));
    }

    /** Cálculo puro del pago: lo usa la vista previa y el registro. */
    public static CalculoPago calcular(BigDecimal total, PagoRequest r) {
        BigDecimal transf = nz(r.valorTransferencia());
        BigDecimal efectivo = nz(r.efectivoRecibido());
        if (transf.signum() < 0 || efectivo.signum() < 0) throw ApiException.invalido("Los valores no pueden ser negativos");
        if (transf.compareTo(total) > 0) throw ApiException.invalido("La transferencia no puede superar el total de la cuenta");
        BigDecimal restante = total.subtract(transf);
        BigDecimal cubierto = transf.add(efectivo.min(restante));
        BigDecimal saldo = total.subtract(cubierto).max(BigDecimal.ZERO);
        BigDecimal cambio = efectivo.subtract(restante).max(BigDecimal.ZERO);
        Pago.MedioPago medio = transf.signum() > 0 && restante.signum() > 0 ? Pago.MedioPago.MIXTO
                : transf.signum() > 0 ? Pago.MedioPago.TRANSFERENCIA : Pago.MedioPago.EFECTIVO;
        return new CalculoPago(total, cubierto, saldo, cambio, medio);
    }

    /** HU-32: registra el pago, genera el comprobante y libera la mesa. */
    @Transactional
    public PagoDto registrarPago(Long mesaId, PagoRequest r, Sesion s) {
        Cuenta c = cuentas.findFirstByMesaIdAndEstado(mesaId, Cuenta.EstadoCuenta.ABIERTA).orElseThrow(() -> {
            Optional<Comprobante> previo = cuentas.findAll().stream().filter(x -> x.mesaId.equals(mesaId))
                    .max(Comparator.comparing(x -> x.id)).flatMap(x -> comprobantes.findByCuentaId(x.id));
            return previo.isPresent() ? ApiException.conflicto("La cuenta ya está pagada (comprobante N.º " + previo.get().consecutivo + ")")
                    : ApiException.noEncontrado("La mesa no tiene consumos registrados. La cuenta");
        });
        CuentaDto d = detalle(c, c.mesaNumero);
        if (d.items().isEmpty()) throw ApiException.invalido("La mesa no tiene consumos registrados");
        CalculoPago calc = calcular(d.total(), r);
        if (nz(r.valorTransferencia()).signum() > 0 && (r.referenciaTransferencia() == null || r.referenciaTransferencia().isBlank()))
            throw ApiException.invalido("Digite la referencia de la transferencia");
        if (calc.saldoPendiente().signum() > 0)
            throw ApiException.invalido("Saldo pendiente: $" + calc.saldoPendiente().toPlainString());

        Pago p = new Pago();
        p.cuentaId = c.id;
        p.medio = calc.medio();
        p.valorTransferencia = nz(r.valorTransferencia());
        p.referenciaTransferencia = r.referenciaTransferencia();
        p.valorRecibido = nz(r.efectivoRecibido());
        p.valorEfectivo = d.total().subtract(p.valorTransferencia);
        p.cambio = calc.cambio();
        p.cajeroId = s.usuarioId();
        p.cajeroNombre = s.nombre();
        pagos.save(p);

        Comprobante comp = new Comprobante();
        comp.consecutivo = comprobantes.findTopByOrderByConsecutivoDesc().map(x -> x.consecutivo + 1).orElse(1L);
        comp.pagoId = p.id;
        comp.cuentaId = c.id;
        comp.total = d.total();
        comprobantes.save(comp);

        c.total = d.total();
        c.estado = Cuenta.EstadoCuenta.PAGADA;
        c.fechaCierre = LocalDateTime.now();
        cuentas.save(c);
        Mesa mesa = mesas.findById(mesaId).orElseThrow();
        mesa.estado = EstadoMesa.DISPONIBLE;
        mesas.save(mesa);
        ws.convertAndSend("/topic/mesas", mesa);
        outbox.registrar("PAGO_REGISTRADO", "Pago", p.id, new PagoDto(p, comp));
        bitacora.registrar("Pago " + p.medio + " mesa " + mesa.numero, "Pago#" + p.id, s.nombre());
        return new PagoDto(p, comp);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
