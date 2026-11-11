package co.edu.ucc.alturia.pedidos;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.impresion.ImpresionService;
import co.edu.ucc.alturia.pedidos.PedidoDtos.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints que usa la pantalla de cocina con el teclado numérico (ÉP-06 y ÉP-08). */
@RestController
@RequestMapping("/api/kds")
@RequiereRol({Rol.COCINA, Rol.ADMINISTRADOR})
public class KdsController {
    private final PedidoService service;
    private final ImpresionService impresion;

    public KdsController(PedidoService service, ImpresionService impresion) { this.service = service; this.impresion = impresion; }

    @GetMapping("/cola")
    public List<PedidoDto> cola(@RequestParam(defaultValue = "Cocina") String estacion) { return service.cola(estacion); }

    @GetMapping("/consolidado")
    public List<ConsolidadoItem> consolidado(@RequestParam(defaultValue = "Cocina") String estacion) { return service.consolidado(estacion); }

    @PostMapping("/pedidos/{id}/avanzar")
    public PedidoDto avanzar(@PathVariable Long id, @RequestParam String estacion, @RequestParam(defaultValue = "false") boolean verificado) {
        return service.avanzar(id, estacion, verificado);
    }

    @PostMapping("/pedidos/{id}/deshacer")
    public PedidoDto deshacer(@PathVariable Long id, @RequestParam String estacion) { return service.deshacer(id, estacion); }

    /** Tecla «.»: reimprime la comanda del pedido seleccionado (HU-27). */
    @PostMapping("/pedidos/{id}/reimprimir")
    public void reimprimir(@PathVariable Long id, @RequestParam String estacion) { impresion.reimprimir(service.buscar(id), estacion); }
}
