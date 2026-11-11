package co.edu.ucc.alturia.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public final class MenuRepositories {
    private MenuRepositories() {}

    public interface Productos extends JpaRepository<Producto, Long> {
        List<Producto> findByActivoTrueOrderByNombre();
    }

    public interface Categorias extends JpaRepository<Categoria, Long> {
        Optional<Categoria> findByNombre(String nombre);
    }

    public interface Estaciones extends JpaRepository<Estacion, Long> {
        Optional<Estacion> findByNombre(String nombre);
        Optional<Estacion> findFirstByPorDefectoTrue();
    }
}
