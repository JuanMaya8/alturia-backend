package co.edu.ucc.alturia.auth;

import co.edu.ucc.alturia.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Autenticación sencilla con token opaco. Funciona sin internet porque los usuarios
 * están en la base de datos local (se sincronizan desde la nube).
 */
@Service
public class SesionService {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final String tokenKds;

    public SesionService(UsuarioRepository usuarios, @Value("${alturia.kds.token}") String tokenKds) {
        this.usuarios = usuarios;
        this.tokenKds = tokenKds;
    }

    public Sesion iniciar(String usuario, String contrasena) {
        Usuario u = usuarios.findByUsuario(usuario)
                .filter(Usuario::isActivo)
                .filter(x -> encoder.matches(contrasena, x.getContrasenaHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        Sesion s = new Sesion(UUID.randomUUID().toString(), u.getId(), u.getNombre(), u.getRol());
        sesiones.put(s.token(), s);
        return s;
    }

    public Sesion validar(String token) {
        if (token == null) return null;
        if (token.equals(tokenKds)) return new Sesion(token, null, "Pantalla de cocina", Rol.COCINA);
        return sesiones.get(token);
    }

    public void cerrar(String token) { sesiones.remove(token); }

    public String hash(String contrasena) { return encoder.encode(contrasena); }
}
