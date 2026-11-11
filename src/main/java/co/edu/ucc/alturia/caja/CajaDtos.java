package co.edu.ucc.alturia.caja;

import java.math.BigDecimal;
import java.util.List;

public final class CajaDtos {
    private CajaDtos() {}

    public record ItemCuenta(String producto, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, boolean entregado) {}

    public record CuentaDto(Long cuentaId, Long mesaId, int mesa, String estado, List<ItemCuenta> items, BigDecimal total,
                            boolean hayPedidosSinEntregar, Comprobante comprobante) {}

    /**
     * Pago presencial: efectivoRecibido (billetes que entrega el cliente), valorTransferencia y su referencia.
     * Un pago mixto lleva ambos valores.
     */
    public record PagoRequest(BigDecimal efectivoRecibido, BigDecimal valorTransferencia, String referenciaTransferencia) {}

    /** Vista previa para la pantalla de caja: saldo pendiente y cambio mientras el cajero(a) digita. */
    public record CalculoPago(BigDecimal total, BigDecimal cubierto, BigDecimal saldoPendiente, BigDecimal cambio, Pago.MedioPago medio) {}

    public record PagoDto(Pago pago, Comprobante comprobante) {}
}
