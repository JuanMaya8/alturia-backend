package co.edu.ucc.alturia.mesas;

import jakarta.persistence.*;

/** Zona del restaurante (salón, terraza, segundo piso…). Agrupa las mesas: zona → mesa → capacidad. */
@Entity
public class Zona {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public String nombre;
    public boolean activa = true;
}
