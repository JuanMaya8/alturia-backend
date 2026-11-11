package co.edu.ucc.alturia.nlp;

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

/** Frase original y pedido confirmado: ejemplos para reentrenar el modelo (HU-24). */
@Entity
public class EjemploNlp {
    public interface Repo extends JpaRepository<EjemploNlp, Long> {}

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(length = 500)
    public String fraseOriginal;
    @Column(length = 2000)
    public String fraseCorregida;
    public boolean corregido;
    public LocalDateTime fecha = LocalDateTime.now();

    protected EjemploNlp() {}

    public EjemploNlp(String fraseOriginal, String fraseCorregida, boolean corregido) {
        this.fraseOriginal = fraseOriginal;
        this.fraseCorregida = fraseCorregida;
        this.corregido = corregido;
    }
}
