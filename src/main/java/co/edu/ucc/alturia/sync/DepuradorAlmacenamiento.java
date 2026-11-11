package co.edu.ucc.alturia.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RNF-18: cuando el almacenamiento local llega al 80 % de la capacidad asignada, borra primero lo más
 * antiguo que ya fue sincronizado. Nunca borra lo pendiente.
 */
@Component
public class DepuradorAlmacenamiento {
    private static final Logger log = LoggerFactory.getLogger(DepuradorAlmacenamiento.class);
    private final OutboxService.Repo repo;
    private final long max;
    private final double umbral;

    public DepuradorAlmacenamiento(OutboxService.Repo repo, @Value("${alturia.almacenamiento.max-registros-locales}") long max,
                                   @Value("${alturia.almacenamiento.umbral-depuracion}") double umbral) {
        this.repo = repo; this.max = max; this.umbral = umbral;
    }

    @Scheduled(fixedDelay = 600_000)
    public void depurar() {
        long total = repo.count();
        if (total < max * umbral) return;
        var viejos = repo.findTop500BySincronizadoTrueOrderById();
        repo.deleteAll(viejos);
        log.info("Depuración local: {} eventos sincronizados eliminados", viejos.size());
        // TODO (Juan): extender a pedidos, comandas y tiempos antiguos cuyos eventos ya estén sincronizados.
    }
}
