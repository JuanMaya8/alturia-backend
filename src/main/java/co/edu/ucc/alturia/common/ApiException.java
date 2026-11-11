package co.edu.ucc.alturia.common;

import org.springframework.http.HttpStatus;

/** Error de negocio con un mensaje pensado para mostrarse tal cual al usuario. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ApiException noEncontrado(String que) { return new ApiException(HttpStatus.NOT_FOUND, que + " no existe"); }
    public static ApiException invalido(String mensaje) { return new ApiException(HttpStatus.BAD_REQUEST, mensaje); }
    public static ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
    public static ApiException prohibido() { return new ApiException(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a esta función"); }
    public static ApiException noAutenticado() { return new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"); }
}
