package co.edu.ucc.alturia.nlp;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.common.ApiException;
import co.edu.ucc.alturia.menu.MenuRepositories;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

/** HU-22/HU-23: la tablet envía la frase y el servidor local la reenvía al NLP con el catálogo vigente. */
@RestController
@RequestMapping("/api/nlp")
@RequiereRol({Rol.MESERO, Rol.ADMINISTRADOR})
public class NlpController {
    public record FraseRequest(String frase) {}

    private final NlpClient nlp;
    private final MenuRepositories.Productos productos;

    public NlpController(NlpClient nlp, MenuRepositories.Productos productos) { this.nlp = nlp; this.productos = productos; }

    @PostMapping("/interpretar")
    public JsonNode interpretar(@RequestBody FraseRequest r) {
        if (r.frase() == null || r.frase().isBlank()) throw ApiException.invalido("Escriba o dicte el pedido");
        return nlp.interpretar(r.frase(), productos.findByActivoTrueOrderByNombre());
    }
}
