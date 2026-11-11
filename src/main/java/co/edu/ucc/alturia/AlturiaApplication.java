package co.edu.ucc.alturia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada del servidor local (perfil "local") y de la API central (perfil "cloud"). */
@SpringBootApplication
@EnableScheduling
// Los repositorios se declaran como interfaces anidadas junto a su entidad o servicio
@EnableJpaRepositories(considerNestedRepositories = true)
public class AlturiaApplication {
    public static void main(String[] args) {
        new java.io.File("data").mkdirs(); // carpeta del archivo SQLite
        SpringApplication.run(AlturiaApplication.class, args);
    }
}
