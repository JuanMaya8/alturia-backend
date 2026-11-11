package co.edu.ucc.alturia.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Las tablets consultan este endpoint para saber si hay conexión con el servidor local. */
@RestController
public class SaludController {
    @GetMapping("/api/salud")
    public Map<String, Object> salud() { return Map.of("estado", "ok", "servidor", "alturia-local"); }
}
