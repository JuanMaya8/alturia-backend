package co.edu.ucc.alturia.caja;

import co.edu.ucc.alturia.auth.AuthInterceptor;
import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.auth.Sesion;
import co.edu.ucc.alturia.caja.CajaDtos.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Módulo de caja: lo usa el cajero(a) en el computador de la caja (funciona sin internet). */
@RestController
@RequestMapping("/api/caja")
@RequiereRol({Rol.CAJERO, Rol.ADMINISTRADOR})
public class CajaController {
    private final CajaService service;

    public CajaController(CajaService service) { this.service = service; }

    @GetMapping("/cuentas")
    public List<CuentaDto> abiertas() { return service.cuentasAbiertas(); }

    @GetMapping("/mesas/{mesaId}/cuenta")
    public CuentaDto cuenta(@PathVariable Long mesaId) { return service.cuentaDeMesa(mesaId); }

    @PostMapping("/mesas/{mesaId}/calcular")
    public CalculoPago calcular(@PathVariable Long mesaId, @RequestBody PagoRequest r) {
        return CajaService.calcular(service.cuentaDeMesa(mesaId).total(), r);
    }

    @PostMapping("/mesas/{mesaId}/pagos")
    public PagoDto pagar(@PathVariable Long mesaId, @RequestBody PagoRequest r, @RequestAttribute(AuthInterceptor.ATTR) Sesion s) {
        return service.registrarPago(mesaId, r, s);
    }
}
