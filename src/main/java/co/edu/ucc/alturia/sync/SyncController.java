package co.edu.ucc.alturia.sync;

import co.edu.ucc.alturia.common.ApiException;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** API central (perfil cloud): recibe los eventos del servidor local sin duplicarlos. */
@RestController
@RequestMapping("/api/sync")
@Profile("cloud")
public class SyncController {
    @Entity
    public static class EventoRecibido {
        @Id public String idEvento;
        public String tipo;
        public String entidad;
        public Long idEntidad;
        @Column(length = 8000) public String contenido;
        public LocalDateTime fechaOrigen;
        public LocalDateTime fechaRecepcion = LocalDateTime.now();
    }

    public interface Repo extends JpaRepository<EventoRecibido, String> {}

    private final Repo repo;
    private final String clave;

    public SyncController(Repo repo, @Value("${alturia.sync.clave:alturia-sync-dev}") String clave) {
        this.repo = repo; this.clave = clave;
    }

    @PostMapping("/eventos")
    public Map<String, Object> recibir(@RequestHeader("X-Sync-Key") String key, @RequestBody List<EventoSincronizacion> eventos) {
        if (!clave.equals(key)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Clave de sincronización inválida");
        int nuevos = 0;
        for (EventoSincronizacion e : eventos) {
            if (repo.existsById(e.idEvento)) continue;
            EventoRecibido r = new EventoRecibido();
            r.idEvento = e.idEvento; r.tipo = e.tipo; r.entidad = e.entidad; r.idEntidad = e.idEntidad;
            r.contenido = e.contenido; r.fechaOrigen = e.fecha;
            repo.save(r);
            nuevos++;
        }
        // TODO (Juan, HU-17): proyectar los eventos en las tablas centrales para reportes.
        return Map.of("recibidos", eventos.size(), "nuevos", nuevos);
    }
}
