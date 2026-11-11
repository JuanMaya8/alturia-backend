package co.edu.ucc.alturia.auth;

import co.edu.ucc.alturia.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-02: gestionar usuarios (solo administrador). */
@RestController
@RequestMapping("/api/usuarios")
@RequiereRol(Rol.ADMINISTRADOR)
public class UsuarioController {
    public record NuevoUsuario(@NotBlank String nombre, @NotBlank String usuario, @NotNull Rol rol,
                               @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String contrasena) {}

    private final UsuarioRepository repo;
    private final SesionService sesiones;

    public UsuarioController(UsuarioRepository repo, SesionService sesiones) {
        this.repo = repo;
        this.sesiones = sesiones;
    }

    @GetMapping
    public List<Usuario> listar() { return repo.findAll(); }

    @PostMapping
    public Usuario crear(@RequestBody @Valid NuevoUsuario n) {
        if (repo.findByUsuario(n.usuario()).isPresent()) throw ApiException.conflicto("El nombre de usuario ya existe");
        return repo.save(new Usuario(n.nombre(), n.usuario(), sesiones.hash(n.contrasena()), n.rol()));
    }

    @PatchMapping("/{id}/activo")
    public Usuario activar(@PathVariable Long id, @RequestParam boolean valor) {
        Usuario u = repo.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario"));
        if (!valor && u.getRol() == Rol.ADMINISTRADOR && repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1)
            throw ApiException.conflicto("No se puede desactivar al único administrador activo");
        u.setActivo(valor);
        return repo.save(u);
    }
}
