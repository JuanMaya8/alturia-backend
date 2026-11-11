package co.edu.ucc.alturia.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Column(unique = true, nullable = false)
    private String usuario;
    @JsonIgnore
    private String contrasenaHash;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    private boolean activo = true;

    protected Usuario() {}

    public Usuario(String nombre, String usuario, String contrasenaHash, Rol rol) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsuario() { return usuario; }
    public String getContrasenaHash() { return contrasenaHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setRol(Rol rol) { this.rol = rol; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setContrasenaHash(String h) { this.contrasenaHash = h; }
}
