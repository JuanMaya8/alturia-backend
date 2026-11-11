package co.edu.ucc.alturia.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class BitacoraService {
    public interface Repo extends JpaRepository<EventoBitacora, Long> {}

    private final Repo repo;

    public BitacoraService(Repo repo) { this.repo = repo; }

    public void registrar(String accion, String entidad, String usuario) {
        repo.save(new EventoBitacora(accion, entidad, usuario));
    }
}
