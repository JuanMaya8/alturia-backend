package co.edu.ucc.alturia.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);
    long countByRolAndActivoTrue(Rol rol);
}
