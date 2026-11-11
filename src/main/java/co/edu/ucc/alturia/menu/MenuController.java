package co.edu.ucc.alturia.menu;

import co.edu.ucc.alturia.auth.RequiereRol;
import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** HU-03 (administrar menú), HU-04 (disponibilidad) y HU-05 (consultar menú). */
@RestController
@RequestMapping("/api/menu")
public class MenuController {
    public record ProductoRequest(@NotBlank(message = "El nombre es obligatorio") String nombre,
                                  @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero") BigDecimal precio,
                                  @NotNull(message = "La categoría es obligatoria") Long categoriaId,
                                  @NotNull(message = "La estación de preparación es obligatoria") Long estacionId,
                                  @Min(1) int tiempoPreparacionMin, List<String> sinonimos, List<String> alergenos) {}

    private final MenuRepositories.Productos productos;
    private final MenuRepositories.Categorias categorias;
    private final MenuRepositories.Estaciones estaciones;
    private final SimpMessagingTemplate ws;

    public MenuController(MenuRepositories.Productos p, MenuRepositories.Categorias c, MenuRepositories.Estaciones e, SimpMessagingTemplate ws) {
        this.productos = p; this.categorias = c; this.estaciones = e; this.ws = ws;
    }

    @GetMapping("/productos")
    public List<Producto> productos() { return productos.findByActivoTrueOrderByNombre(); }

    @GetMapping("/categorias")
    public List<Categoria> categorias() { return categorias.findAll(); }

    @GetMapping("/estaciones")
    public List<Estacion> estaciones() { return estaciones.findAll(); }

    @PostMapping("/productos")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Producto crear(@RequestBody @Valid ProductoRequest r) { return guardar(new Producto(), r); }

    @PutMapping("/productos/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Producto actualizar(@PathVariable Long id, @RequestBody @Valid ProductoRequest r) {
        return guardar(productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El producto")), r);
    }

    @DeleteMapping("/productos/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public void desactivar(@PathVariable Long id) {
        Producto p = productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El producto"));
        p.activo = false;
        productos.save(p);
        ws.convertAndSend("/topic/menu", Map.of("productoId", id, "activo", false));
    }

    /** HU-04: marcar «Agotado» o «Disponible»; las tablets conectadas se enteran por WebSocket. */
    @PatchMapping("/productos/{id}/disponible")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Producto disponibilidad(@PathVariable Long id, @RequestParam boolean valor) {
        Producto p = productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El producto"));
        p.disponible = valor;
        productos.save(p);
        ws.convertAndSend("/topic/menu", Map.of("productoId", id, "disponible", valor));
        return p;
    }

    private Producto guardar(Producto p, ProductoRequest r) {
        p.nombre = r.nombre().trim();
        p.precio = r.precio();
        p.categoria = categorias.findById(r.categoriaId()).orElseThrow(() -> ApiException.noEncontrado("La categoría"));
        p.estacion = estaciones.findById(r.estacionId()).orElseThrow(() -> ApiException.noEncontrado("La estación"));
        p.tiempoPreparacionMin = r.tiempoPreparacionMin();
        p.sinonimos = r.sinonimos() == null ? List.of() : r.sinonimos();
        p.alergenos = r.alergenos() == null ? List.of() : r.alergenos();
        return productos.save(p);
    }
}
