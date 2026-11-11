package co.edu.ucc.alturia.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Envía la bandeja de salida a la API central cuando hay internet (RNF-04, RNF-12).
 * Si la URL de la nube está vacía, el servidor trabaja solo en modo local.
 */
@Component
public class SincronizadorNube {
    private static final Logger log = LoggerFactory.getLogger(SincronizadorNube.class);
    private final OutboxService.Repo repo;
    private final String url;
    private final String clave;

    public SincronizadorNube(OutboxService.Repo repo, @Value("${alturia.sync.url-nube:}") String url,
                             @Value("${alturia.sync.clave:alturia-sync-dev}") String clave) {
        this.repo = repo; this.url = url; this.clave = clave;
    }

    @Scheduled(fixedDelayString = "${alturia.sync.intervalo-ms:15000}")
    public void sincronizar() {
        if (url == null || url.isBlank()) return;
        List<EventoSincronizacion> lote = repo.findTop100BySincronizadoFalseOrderById();
        if (lote.isEmpty()) return;
        try {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(5000);
            f.setReadTimeout(10000);
            RestClient.builder().baseUrl(url).requestFactory(f).build()
                    .post().uri("/api/sync/eventos").header("X-Sync-Key", clave).body(lote).retrieve().toBodilessEntity();
            lote.forEach(e -> e.sincronizado = true);
        } catch (Exception ex) {
            lote.forEach(e -> e.intentos++);
            log.info("Sin conexión con la nube; {} eventos quedan pendientes", lote.size());
        }
        repo.saveAll(lote);
    }
}
