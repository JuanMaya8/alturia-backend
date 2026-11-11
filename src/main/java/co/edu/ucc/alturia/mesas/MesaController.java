package co.edu.ucc.alturia.mesas;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-06 (registrar mesas) y HU-07 (consultar y actualizar su estado). */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {
    public record MesaRequest(@Min(1) int numero, @Min(1) int capacidad) {}

    private final MesaRepository repo;
    private final SimpMessagingTemplate ws;

    public MesaController(MesaRepository repo, SimpMessagingTemplate ws) { this.repo = repo; this.ws = ws; }

    @GetMapping
    public List<Mesa> listar() { return repo.findAllByOrderByNumero(); }

    @PostMapping
    @RequiereRol(Rol.ADMINISTRADOR)
    public Mesa crear(@RequestBody @Valid MesaRequest r) {
        if (repo.existsByNumero(r.numero())) throw ApiException.conflicto("Ya existe una mesa con ese número");
        Mesa m = new Mesa();
        m.numero = r.numero();
        m.capacidad = r.capacidad();
        return repo.save(m);
    }

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
