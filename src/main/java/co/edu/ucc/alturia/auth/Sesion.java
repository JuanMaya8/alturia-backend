package co.edu.ucc.alturia.auth;

/** Datos de la sesión activa. La pantalla de cocina usa una sesión de dispositivo (usuarioId nulo). */
public record Sesion(String token, Long usuarioId, String nombre, Rol rol) {
    public boolean esPantallaCocina() { return usuarioId == null && rol == Rol.COCINA; }
}
