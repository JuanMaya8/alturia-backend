package co.edu.ucc.alturia.pedidos;

import co.edu.ucc.alturia.auth.AuthInterceptor;
import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.auth.Sesion;
import co.edu.ucc.alturia.pedidos.PedidoDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) { this.service = service; }

    @PostMapping
    @RequiereRol({Rol.MESERO, Rol.ADMINISTRADOR})
    public PedidoDto crear(@RequestBody @Valid PedidoRequest r, @RequestAttribute(AuthInterceptor.ATTR) Sesion s) {
        return service.crear(r, s);
    }

    @GetMapping("/activos")
    public List<PedidoDto> activos() { return service.activos(); }

    @GetMapping("/{id}")
    public PedidoDto uno(@PathVariable Long id) { return service.dto(service.buscar(id)); }

    @PutMapping("/{id}/lineas")
    @RequiereRol({Rol.MESERO, Rol.ADMINISTRADOR})
    public PedidoDto modificar(@PathVariable Long id, @RequestBody List<@Valid LineaRequest> lineas,
                               @RequestAttribute(AuthInterceptor.ATTR) Sesion s) {
        return service.modificar(id, lineas, s);
    }

    @PostMapping("/{id}/entregar")
    @RequiereRol({Rol.MESERO, Rol.ADMINISTRADOR})
    public PedidoDto entregar(@PathVariable Long id, @RequestAttribute(AuthInterceptor.ATTR) Sesion s) {
        return service.entregar(id, s);
    }
}
