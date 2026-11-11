package co.edu.ucc.alturia.auth;

import co.edu.ucc.alturia.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/** Valida el token "Authorization: Bearer ..." y los roles de {@link RequiereRol}. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String ATTR = "alturia.sesion";
    private final SesionService sesiones;

    public AuthInterceptor(SesionService sesiones) { this.sesiones = sesiones; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm) || "OPTIONS".equals(req.getMethod())) return true;
        String h = req.getHeader("Authorization");
        Sesion s = sesiones.validar(h != null && h.startsWith("Bearer ") ? h.substring(7) : null);
        if (s == null) throw ApiException.noAutenticado();
        RequiereRol r = hm.getMethodAnnotation(RequiereRol.class);
        if (r == null) r = hm.getBeanType().getAnnotation(RequiereRol.class);
        if (r != null && Arrays.stream(r.value()).noneMatch(x -> x == s.rol())) throw ApiException.prohibido();
        req.setAttribute(ATTR, s);
        return true;
    }
}
