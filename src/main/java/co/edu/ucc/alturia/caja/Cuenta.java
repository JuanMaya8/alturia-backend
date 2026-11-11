package co.edu.ucc.alturia.caja;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Cuenta {
    public enum EstadoCuenta { ABIERTA, PAGADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long mesaId;
    public int mesaNumero;
    @Enumerated(EnumType.STRING)
    public EstadoCuenta estado = EstadoCuenta.ABIERTA;
    public BigDecimal total = BigDecimal.ZERO;
    public LocalDateTime fechaApertura = LocalDateTime.now();
    public LocalDateTime fechaCierre;
}
