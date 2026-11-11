package co.edu.ucc.alturia.caja;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pago recibido en persona. El sistema no se conecta con bancos: solo guarda lo que confirma el cajero(a). */
@Entity
public class Pago {
    public enum MedioPago { EFECTIVO, TRANSFERENCIA, MIXTO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long cuentaId;
    @Enumerated(EnumType.STRING)
    public MedioPago medio;
    /** Parte de la cuenta cubierta con efectivo. */
    public BigDecimal valorEfectivo = BigDecimal.ZERO;
    /** Billetes que entregó el cliente. */
    public BigDecimal valorRecibido = BigDecimal.ZERO;
    public BigDecimal cambio = BigDecimal.ZERO;
    public BigDecimal valorTransferencia = BigDecimal.ZERO;
    public String referenciaTransferencia;
    public Long cajeroId;
    public String cajeroNombre;
    public LocalDateTime fecha = LocalDateTime.now();
}
