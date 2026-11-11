package co.edu.ucc.alturia.mesas;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Zonas del restaurante: cada zona agrupa ciertas mesas y su capacidad total es la suma de sus mesas. */
@RestController
@RequestMapping("/api/zonas")
public class ZonaController {
    public record ZonaRequest(@NotBlank String nombre) {}
    public record ZonaDto(Long id, String nombre, boolean activa, int mesas, int capacidad) {}

    private final ZonaRepository zonas;
    private final MesaRepository mesas;

    public ZonaController(ZonaRepository zonas, MesaRepository mesas) { this.zonas = zonas; this.mesas = mesas; }

    @GetMapping
    public List<ZonaDto> listar() {
        List<Mesa> todas = mesas.findAll();
        return zonas.findAllByOrderByNombre().stream().map(z -> {
            List<Mesa> deZona = todas.stream().filter(m -> m.zona != null && m.zona.id.equals(z.id)).toList();
            return new ZonaDto(z.id, z.nombre, z.activa, deZona.size(), deZona.stream().mapToInt(m -> m.capacidad).sum());
        }).toList();
    }

    @PostMapping
    @RequiereRol(Rol.ADMINISTRADOR)
    public Zona crear(@RequestBody @Valid ZonaRequest r) {
        String nombre = r.nombre().trim();
        if (zonas.findByNombreIgnoreCase(nombre).isPresent()) throw ApiException.conflicto("Ya existe una zona con ese nombre");
        Zona z = new Zona();
        z.nombre = nombre;
        return zonas.save(z);
    }
}
