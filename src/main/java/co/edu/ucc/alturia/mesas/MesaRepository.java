package co.edu.ucc.alturia.mesas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findAllByOrderByNumero();
    boolean existsByNumero(int numero);
    List<Mesa> findByZonaIsNull();
}
