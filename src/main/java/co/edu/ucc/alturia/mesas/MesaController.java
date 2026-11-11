package co.edu.ucc.alturia.mesas;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-06 (registrar mesas) y HU-07 (consultar y actualizar su estado). */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {
    public record MesaRequest(@Min(1) int numero, @Min(1) int capacidad, @NotNull(message = "Seleccione la zona de la mesa") Long zonaId) {}

    private final MesaRepository repo;
    private final ZonaRepository zonas;
    private final SimpMessagingTemplate ws;

    public MesaController(MesaRepository repo, ZonaRepository zonas, SimpMessagingTemplate ws) {
        this.repo = repo; this.zonas = zonas; this.ws = ws;
    }

    @GetMapping
    public List<Mesa> listar() { return repo.findAllByOrderByNumero(); }

    @PostMapping
    @RequiereRol(Rol.ADMINISTRADOR)
    public Mesa crear(@RequestBody @Valid MesaRequest r) {
        if (repo.existsByNumero(r.numero())) throw ApiException.conflicto("Ya existe una mesa con ese número");
        Mesa m = new Mesa();
        m.numero = r.numero();
        m.capacidad = r.capacidad();
        m.zona = zona(r.zonaId());
        return repo.save(m);
    }

    /** Cambia la mesa de zona (por ejemplo, las mesas creadas antes de existir las zonas). */
    @PatchMapping("/{id}/zona")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Mesa cambiarZona(@PathVariable Long id, @RequestParam Long zonaId) {
        Mesa m = repo.findById(id).orElseThrow(() -> ApiException.noEncontrado("La mesa"));
        m.zona = zona(zonaId);
        repo.save(m);
        ws.convertAndSend("/topic/mesas", m);
        return m;
    }

    private Zona zona(Long id) { return zonas.findById(id).orElseThrow(() -> ApiException.noEncontrado("La zona")); }

    @PatchMapping("/{id}/estado")
    @RequiereRol({Rol.MESERO, Rol.ADMINISTRADOR})
    public Mesa estado(@PathVariable Long id, @RequestParam EstadoMesa valor) {
        Mesa m = repo.findById(id).orElseThrow(() -> ApiException.noEncontrado("La mesa"));
        m.estado = valor;
        repo.save(m);
        ws.convertAndSend("/topic/mesas", m);
        return m;
    }
}
