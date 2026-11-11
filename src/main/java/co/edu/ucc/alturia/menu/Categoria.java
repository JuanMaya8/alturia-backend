package co.edu.ucc.alturia.menu;

import jakarta.persistence.*;

@Entity
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public String nombre;
}
