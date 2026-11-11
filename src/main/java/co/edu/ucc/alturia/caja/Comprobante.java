package co.edu.ucc.alturia.caja;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante interno del pago. No reemplaza la factura del establecimiento. */
@Entity
public class Comprobante {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true)
    public Long consecutivo;
    public Long pagoId;
    public Long cuentaId;
    public BigDecimal total;
    public LocalDateTime fechaEmision = LocalDateTime.now();
    public String leyenda = "Documento interno. No válido como factura";
}
