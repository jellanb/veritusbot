package com.example.veritusbot.exception;

/**
 * Excepción lanzada cuando no se encuentra un usuario en la base de datos.
 * Se utiliza durante el proceso de autenticación.
 */
public class UsuarioNoEncontradoException extends RuntimeException {

    private static final String MENSAJE_POR_DEFECTO = "Usuario no encontrado";

    public UsuarioNoEncontradoException() {
        super(MENSAJE_POR_DEFECTO);
    }

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public UsuarioNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

