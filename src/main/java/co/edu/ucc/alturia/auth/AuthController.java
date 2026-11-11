package co.edu.ucc.alturia.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/** HU-01: iniciar sesión y acceder según el rol. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String usuario, @NotBlank String contrasena) {}

    private final SesionService sesiones;

    public AuthController(SesionService sesiones) { this.sesiones = sesiones; }

    @PostMapping("/login")
    public Sesion login(@RequestBody @jakarta.validation.Valid LoginRequest r) {
        return sesiones.iniciar(r.usuario(), r.contrasena());
    }

    @GetMapping("/yo")
    public Sesion yo(@RequestAttribute(AuthInterceptor.ATTR) Sesion s) { return s; }

    @PostMapping("/logout")
    public void logout(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) sesiones.cerrar(h.substring(7));
    }
}
