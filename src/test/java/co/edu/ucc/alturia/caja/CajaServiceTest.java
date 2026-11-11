package co.edu.ucc.alturia.caja;

import co.edu.ucc.alturia.caja.CajaDtos.CalculoPago;
import co.edu.ucc.alturia.caja.CajaDtos.PagoRequest;
import co.edu.ucc.alturia.common.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Escenarios de la HU-32 (registrar el pago en caja). */
class CajaServiceTest {
    private static BigDecimal $(long v) { return BigDecimal.valueOf(v); }

    @Test
    void efectivoConCambio() {
        CalculoPago c = CajaService.calcular($(45000), new PagoRequest($(50000), null, null));
        assertEquals(0, c.cambio().compareTo($(5000)));
        assertEquals(0, c.saldoPendiente().signum());
        assertEquals(Pago.MedioPago.EFECTIVO, c.medio());
    }

    @Test
    void valorInsuficiente() {
        CalculoPago c = CajaService.calcular($(45000), new PagoRequest($(40000), null, null));
        assertEquals(0, c.saldoPendiente().compareTo($(5000)));
    }

    @Test
    void transferenciaCompleta() {
        CalculoPago c = CajaService.calcular($(45000), new PagoRequest(null, $(45000), "REF123"));
        assertEquals(Pago.MedioPago.TRANSFERENCIA, c.medio());
        assertEquals(0, c.saldoPendiente().signum());
    }

    @Test
    void pagoMixtoConCambio() {
        CalculoPago c = CajaService.calcular($(80000), new PagoRequest($(40000), $(50000), "REF9"));
        assertEquals(Pago.MedioPago.MIXTO, c.medio());
        assertEquals(0, c.cambio().compareTo($(10000)));
        assertEquals(0, c.saldoPendiente().signum());
    }

    @Test
    void transferenciaMayorAlTotalSeRechaza() {
        assertThrows(ApiException.class, () -> CajaService.calcular($(10000), new PagoRequest(null, $(20000), "X")));
    }
}
