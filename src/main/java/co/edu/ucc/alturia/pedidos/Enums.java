package co.edu.ucc.alturia.pedidos;

/** Enumeraciones del pedido (diagrama de clases, nivel 4). */
public final class Enums {
    private Enums() {}
    public enum EstadoPedido { PENDIENTE_ENVIO, RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO, ANULADO, REQUIERE_REVISION }
    public enum EstadoLinea { ACTIVA, MODIFICADA, ANULADA }
    public enum OrigenPedido { MANUAL, LENGUAJE_NATURAL }
}
