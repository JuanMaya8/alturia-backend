package co.edu.ucc.alturia.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxService {
    public interface Repo extends JpaRepository<EventoSincronizacion, Long> {
        List<EventoSincronizacion> findTop100BySincronizadoFalseOrderById();
        long countBySincronizadoFalse();
        List<EventoSincronizacion> findTop500BySincronizadoTrueOrderById();
    }

    private final Repo repo;
    private final ObjectMapper json;

    public OutboxService(Repo repo, ObjectMapper json) { this.repo = repo; this.json = json; }

    public void registrar(String tipo, String entidad, Long id, Object contenido) {
        EventoSincronizacion e = new EventoSincronizacion();
        e.tipo = tipo;
        e.entidad = entidad;
        e.idEntidad = id;
        try { e.contenido = json.writeValueAsString(contenido); } catch (Exception ex) { e.contenido = "{}"; }
        repo.save(e);
    }

    public long pendientes() { return repo.countBySincronizadoFalse(); }
}
