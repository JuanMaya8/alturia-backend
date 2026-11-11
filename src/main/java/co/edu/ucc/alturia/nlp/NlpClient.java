package co.edu.ucc.alturia.nlp;

import co.edu.ucc.alturia.common.ApiException;
import co.edu.ucc.alturia.menu.Producto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Cliente del servicio NLP local (FastAPI en el mismo computador). Espera máximo 3 s; si falla,
 * el mesero pasa al registro manual sin perder lo escrito (RNF-20).
 */
@Component
public class NlpClient {
    private final RestClient http;

    public NlpClient(@Value("${alturia.nlp.url}") String url, @Value("${alturia.nlp.timeout-ms}") int timeout) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeout);
        f.setReadTimeout(timeout);
        this.http = RestClient.builder().baseUrl(url).requestFactory(f).build();
    }

    public JsonNode interpretar(String frase, List<Producto> catalogo) {
        List<Map<String, Object>> cat = catalogo.stream().map(p -> Map.<String, Object>of(
                "id", p.id, "nombre", p.nombre, "sinonimos", p.sinonimos, "disponible", p.disponible)).toList();
        try {
            return http.post().uri("/interpretar").body(Map.of("frase", frase, "catalogo", cat)).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible interpretar el pedido. Regístrelo manualmente");
        }
    }
}
