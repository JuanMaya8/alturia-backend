package co.edu.ucc.alturia.menu;

import jakarta.persistence.*;

/** Estación de preparación (Cocina, Bar). Cada línea del pedido se enruta a una estación (RF-034). */
@Entity
public class Estacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public String nombre;
    /** IP de la comandera; si está vacía se usa la comandera por defecto. */
    public String ipComandera;
    public int puertoComandera = 9100;
    public boolean porDefecto;
}
