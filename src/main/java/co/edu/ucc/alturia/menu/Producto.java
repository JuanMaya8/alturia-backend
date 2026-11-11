package co.edu.ucc.alturia.menu;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String nombre;
    public BigDecimal precio;
    @ManyToOne(fetch = FetchType.EAGER)
    public Categoria categoria;
    @ManyToOne(fetch = FetchType.EAGER)
    public Estacion estacion;
    public int tiempoPreparacionMin = 15;
    /** Otros nombres con que lo piden los clientes (ej. «cola» para gaseosa). Los usa el NLP. */
    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    public List<String> sinonimos = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    public List<String> alergenos = new ArrayList<>();
    public boolean disponible = true;
    public boolean activo = true;
}
