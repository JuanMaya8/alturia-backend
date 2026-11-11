package co.edu.ucc.alturia.auth;

import java.lang.annotation.*;

/** Restringe un endpoint a los roles indicados. Sin anotación basta con estar autenticado. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereRol {
    Rol[] value();
}
