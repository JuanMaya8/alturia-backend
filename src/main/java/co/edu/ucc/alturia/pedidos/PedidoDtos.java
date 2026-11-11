package co.edu.ucc.alturia.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDtos {
    private PedidoDtos() {}

    public record LineaRequest(@NotNull Long productoId, @Min(1) @Max(99) int cantidad,
                               @Size(max = 200) String observacion) {}

    public record PedidoRequest(@NotBlank String idLocal, @NotNull Long mesaId, Enums.OrigenPedido origen,
                                @NotEmpty(message = "El pedido debe tener al menos un producto") List<@Valid LineaRequest> lineas,
                                List<String> restricciones,
                                /* Para el aprendizaje del NLP (HU-24): frase original y si el mesero corrigió la propuesta. */
                                String fraseOriginal, Boolean corregidoPorMesero) {}

    public record LineaDto(Long id, Long productoId, String producto, int cantidad, String observacion, String estacion,
                           Enums.EstadoLinea estado, Enums.EstadoPedido avance, BigDecimal subtotal, List<String> alergenos) {
        static LineaDto de(LineaPedido l) {
            return new LineaDto(l.id, l.producto.id, l.producto.nombre, l.cantidad, l.observacion,
                    l.estacion == null ? "Sin estación" : l.estacion.nombre, l.estado, l.avance, l.subtotal(), l.producto.alergenos);
        }
    }

    public record PedidoDto(Long id, String idLocal, Long mesaId, int mesa, String mesero, Enums.EstadoPedido estado,
                            Enums.OrigenPedido origen, LocalDateTime fechaConfirmacion, long minutos, boolean atrasado,
                            boolean modificado, boolean alertas, List<String> restricciones, BigDecimal total, List<LineaDto> lineas) {
        public static PedidoDto de(Pedido p, int minutosLimite) {
            long min = p.fechaConfirmacion == null ? 0 : Duration.between(p.fechaConfirmacion, LocalDateTime.now()).toMinutes();
            boolean activo = p.estado == Enums.EstadoPedido.RECIBIDO || p.estado == Enums.EstadoPedido.EN_PREPARACION;
            return new PedidoDto(p.id, p.idLocal, p.mesa.id, p.mesa.numero, p.meseroNombre, p.estado, p.origen, p.fechaConfirmacion,
                    min, activo && min > minutosLimite, p.modificado, p.tieneAlertas(), p.restricciones, p.total(),
                    p.lineas.stream().map(LineaDto::de).toList());
        }
    }

    /** Fila del consolidado de producción (RF-036): producto + observación, cantidad pendiente. */
    public record ConsolidadoItem(String producto, String variante, int cantidad) {}
}
