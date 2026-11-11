package co.edu.ucc.alturia.impresion;

import co.edu.ucc.alturia.menu.Estacion;
import co.edu.ucc.alturia.pedidos.LineaPedido;
import co.edu.ucc.alturia.pedidos.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agente de impresión ESC/POS (RF-035): una comanda por estación; si la comandera no responde,
 * alerta en el KDS y reintenta cada 10 s marcando la comanda como «IMPRESIÓN DIFERIDA».
 */
@Service
public class ImpresionService {
    public interface Repo extends JpaRepository<Comanda, Long> {
        List<Comanda> findByEstadoImpresionAndIntentosLessThan(Comanda.EstadoImpresion e, int intentos);
        List<Comanda> findByPedidoIdAndEstacionOrderByIdDesc(Long pedidoId, String estacion);
    }

    private static final Logger log = LoggerFactory.getLogger(ImpresionService.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final Repo repo;
    private final SimpMessagingTemplate ws;
    private final String modo;
    private final String hostPorDefecto;
    private final int puertoPorDefecto;

    public ImpresionService(Repo repo, SimpMessagingTemplate ws, @Value("${alturia.impresion.modo}") String modo,
                            @Value("${alturia.impresion.host}") String host, @Value("${alturia.impresion.puerto}") int puerto) {
        this.repo = repo; this.ws = ws; this.modo = modo; this.hostPorDefecto = host; this.puertoPorDefecto = puerto;
    }

    public void generar(Pedido p, TipoComanda tipo, List<LineaPedido> lineas) {
        Map<String, List<LineaPedido>> porEstacion = new LinkedHashMap<>();
        Map<String, Estacion> estaciones = new HashMap<>();
        for (LineaPedido l : lineas) {
            String nombre = l.estacion == null ? "Cocina" : l.estacion.nombre;
            porEstacion.computeIfAbsent(nombre, k -> new ArrayList<>()).add(l);
            if (l.estacion != null) estaciones.put(nombre, l.estacion);
        }
        porEstacion.forEach((estacion, ls) -> {
            Comanda c = new Comanda();
            c.pedidoId = p.id;
            c.estacion = estacion;
            c.tipo = tipo;
            Estacion e = estaciones.get(estacion);
            c.host = e != null && e.ipComandera != null && !e.ipComandera.isBlank() ? e.ipComandera : hostPorDefecto;
            c.puerto = e != null && e.puertoComandera > 0 ? e.puertoComandera : puertoPorDefecto;
            c.contenido = formatear(p, estacion, tipo, ls);
            repo.save(c);
            enviar(c);
        });
    }

    public void reimprimir(Pedido p, String estacion) {
        List<LineaPedido> lineas = p.lineas.stream().filter(LineaPedido::activa)
                .filter(l -> l.estacion != null && l.estacion.nombre.equalsIgnoreCase(estacion)).toList();
        generar(p, TipoComanda.REIMPRESION, lineas);
    }

    static String formatear(Pedido p, String estacion, TipoComanda tipo, List<LineaPedido> lineas) {
        StringBuilder sb = new StringBuilder();
        sb.append("== ").append(estacion.toUpperCase()).append(" ==\n");
        sb.append(switch (tipo) { case NUEVA -> "COMANDA"; case MODIFICACION -> "MODIFICACIÓN"; case REIMPRESION -> "REIMPRESIÓN"; }).append('\n');
        sb.append("Mesa ").append(p.mesa.numero).append("   Mesero: ").append(p.meseroNombre).append('\n');
        sb.append("Pedido #").append(p.id).append("   ").append(LocalDateTime.now().format(HORA)).append('\n');
        for (String r : p.restricciones) sb.append("!! RESTRICCIÓN: ").append(r).append('\n');
        lineas.stream().filter(l -> !l.producto.alergenos.isEmpty() && l.activa())
                .forEach(l -> sb.append("!! ").append(l.producto.nombre).append(" contiene: ").append(String.join(", ", l.producto.alergenos)).append('\n'));
        sb.append("--------------------------------\n");
        for (LineaPedido l : lineas) {
            sb.append(l.activa() ? "" : "ANULADO ").append(l.cantidad).append(" x ").append(l.producto.nombre).append('\n');
            if (l.observacion != null) sb.append("   > ").append(l.observacion).append('\n');
        }
        return sb.toString();
    }

    void enviar(Comanda c) {
        String texto = (c.diferida ? "!! IMPRESIÓN DIFERIDA\n" : "") + c.contenido;
        try {
            if ("red".equalsIgnoreCase(modo)) {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(c.host, c.puerto), 2000);
                    OutputStream out = s.getOutputStream();
                    out.write(EscPos.documento(texto));
                    out.flush();
                }
            } else {
                log.info("\n[COMANDERA {}:{}]\n{}", c.host, c.puerto, texto);
            }
            c.estadoImpresion = Comanda.EstadoImpresion.IMPRESA;
            c.fechaImpresion = LocalDateTime.now();
        } catch (Exception e) {
            c.estadoImpresion = Comanda.EstadoImpresion.FALLIDA;
            c.intentos++;
            c.diferida = true;
            log.warn("Comandera sin respuesta ({}:{}): {}", c.host, c.puerto, e.getMessage());
            ws.convertAndSend("/topic/kds/alertas", Map.of("tipo", "COMANDERA_SIN_RESPUESTA", "comandaId", c.id,
                    "pedidoId", c.pedidoId, "mensaje", "Comandera sin respuesta"));
        }
        repo.save(c);
    }

    @Scheduled(fixedDelay = 10_000)
    public void reintentar() {
        repo.findByEstadoImpresionAndIntentosLessThan(Comanda.EstadoImpresion.FALLIDA, 60).forEach(this::enviar);
    }
}
