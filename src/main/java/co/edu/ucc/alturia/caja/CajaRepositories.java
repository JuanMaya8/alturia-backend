package co.edu.ucc.alturia.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class CajaRepositories {
    private CajaRepositories() {}

    public interface Cuentas extends JpaRepository<Cuenta, Long> {
        Optional<Cuenta> findFirstByMesaIdAndEstado(Long mesaId, Cuenta.EstadoCuenta estado);
        List<Cuenta> findByEstado(Cuenta.EstadoCuenta estado);
    }

    public interface Pagos extends JpaRepository<Pago, Long> {
        Optional<Pago> findFirstByCuentaId(Long cuentaId);
        List<Pago> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    }

    public interface Comprobantes extends JpaRepository<Comprobante, Long> {
        Optional<Comprobante> findTopByOrderByConsecutivoDesc();
        Optional<Comprobante> findByCuentaId(Long cuentaId);
    }
}
