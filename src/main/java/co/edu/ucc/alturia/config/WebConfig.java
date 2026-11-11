package co.edu.ucc.alturia.config;

import co.edu.ucc.alturia.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;

    public WebConfig(AuthInterceptor auth) { this.auth = auth; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auth).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/salud", "/api/sync/**");
    }

    /** Las tablets, el televisor y la caja están en la red local; en desarrollo Angular corre en :4200. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("*").allowedHeaders("*");
    }

    /** El servidor local sirve el frontend compilado (src/main/resources/static). Las rutas de Angular vuelven a index.html. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String r : new String[]{"/login", "/sala", "/sala/**", "/kds", "/kds/**", "/caja", "/caja/**", "/admin", "/admin/**"}) {
            registry.addViewController(r).setViewName("forward:/index.html");
        }
    }
}
