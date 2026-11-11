package co.edu.ucc.alturia.pedidos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class PedidoRepositories {
    private PedidoRepositories() {}

    public interface Pedidos extends JpaRepository<Pedido, Long> {
        Optional<Pedido> findByIdLocal(String idLocal);
        List<Pedido> findByEstadoInOrderByFechaConfirmacion(Collection<Enums.EstadoPedido> estados);
        List<Pedido> findByCuentaId(Long cuentaId);
        List<Pedido> findByMesaIdAndCuentaId(Long mesaId, Long cuentaId);
    }

    public interface Tiempos extends JpaRepository<RegistroTiempo, Long> {
        Optional<RegistroTiempo> findFirstByPedidoIdAndEstacionOrderByIdDesc(Long pedidoId, String estacion);
        List<RegistroTiempo> findByPedidoIdOrderById(Long pedidoId);
    }
}
