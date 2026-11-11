package co.edu.ucc.alturia.config;

import co.edu.ucc.alturia.auth.Rol;
import co.edu.ucc.alturia.auth.SesionService;
import co.edu.ucc.alturia.auth.Usuario;
import co.edu.ucc.alturia.auth.UsuarioRepository;
import co.edu.ucc.alturia.menu.*;
import co.edu.ucc.alturia.mesas.Mesa;
import co.edu.ucc.alturia.mesas.MesaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos iniciales de demostración. El menú es un EJEMPLO: reemplácelo por el menú real del Restaurante Alturia.
 * Usuarios: admin/admin12345, mesero/mesero12345, caja/caja12345. KDS: token configurado en alturia.kds.token.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final SesionService sesiones;
    private final MenuRepositories.Estaciones estaciones;
    private final MenuRepositories.Categorias categorias;
    private final MenuRepositories.Productos productos;
    private final MesaRepository mesas;

    public DataSeeder(UsuarioRepository u, SesionService s, MenuRepositories.Estaciones e, MenuRepositories.Categorias c,
                      MenuRepositories.Productos p, MesaRepository m) {
        this.usuarios = u; this.sesiones = s; this.estaciones = e; this.categorias = c; this.productos = p; this.mesas = m;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) return;
        usuarios.save(new Usuario("Administrador", "admin", sesiones.hash("admin12345"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Mesero de prueba", "mesero", sesiones.hash("mesero12345"), Rol.MESERO));
        usuarios.save(new Usuario("Cajera de prueba", "caja", sesiones.hash("caja12345"), Rol.CAJERO));

        Estacion cocina = estacion("Cocina", true);
        Estacion bar = estacion("Bar", false);
        Categoria platos = categoria("Platos fuertes");
        Categoria entradas = categoria("Entradas");
        Categoria bebidas = categoria("Bebidas");
        producto("Cuy asado mediano", "48000", platos, cocina, 35, List.of("cuy mediano", "cuyecito"), List.of());
        producto("Cuy asado grande", "58000", platos, cocina, 40, List.of("cuy grande"), List.of());
        producto("Trucha frita", "32000", platos, cocina, 20, List.of("trucha"), List.of("pescado"));
        producto("Hamburguesa", "22000", platos, cocina, 15, List.of("burger"), List.of("gluten"));
        producto("Ceviche", "26000", entradas, cocina, 10, List.of(), List.of("mariscos"));
        producto("Empanadas", "9000", entradas, cocina, 10, List.of("empanaditas"), List.of());
        producto("Jugo de lulo", "7000", bebidas, bar, 5, List.of("lulo"), List.of());
        producto("Gaseosa", "5000", bebidas, bar, 1, List.of("cola", "soda"), List.of());
        producto("Cerveza", "7000", bebidas, bar, 1, List.of("pola", "cerveza nacional"), List.of());
        producto("Hervido de maracuyá", "8000", bebidas, bar, 5, List.of("hervido"), List.of());
        for (int i = 1; i <= 10; i++) {
            Mesa m = new Mesa();
            m.numero = i;
            m.capacidad = i <= 6 ? 4 : 6;
            mesas.save(m);
        }
    }

    private Estacion estacion(String nombre, boolean porDefecto) {
        Estacion e = new Estacion();
        e.nombre = nombre;
        e.porDefecto = porDefecto;
        return estaciones.save(e);
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.nombre = nombre;
        return categorias.save(c);
    }

    private void producto(String nombre, String precio, Categoria c, Estacion e, int min, List<String> sin, List<String> alerg) {
        Producto p = new Producto();
        p.nombre = nombre;
        p.precio = new BigDecimal(precio);
        p.categoria = c;
        p.estacion = e;
        p.tiempoPreparacionMin = min;
        p.sinonimos = new java.util.ArrayList<>(sin);
        p.alergenos = new java.util.ArrayList<>(alerg);
        productos.save(p);
    }
}
