package co.edu.ucc.alturia.mesas;

import jakarta.persistence.*;

@Entity
public class Mesa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public int numero;
    public int capacidad;
    @Enumerated(EnumType.STRING)
    public EstadoMesa estado = EstadoMesa.DISPONIBLE;
}
