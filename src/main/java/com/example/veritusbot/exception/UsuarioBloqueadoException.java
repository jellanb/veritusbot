package com.example.veritusbot.exception;

/**
 * Excepción lanzada cuando un usuario está bloqueado.
 * Se utiliza durante la validación del estado de usuario.
 */
public class UsuarioBloqueadoException extends RuntimeException {

    private static final String MENSAJE_POR_DEFECTO = "Usuario bloqueado. Contacte al administrador";

    public UsuarioBloqueadoException() {
        super(MENSAJE_POR_DEFECTO);
    }

    public UsuarioBloqueadoException(String mensaje) {
        super(mensaje);
    }

    public UsuarioBloqueadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

