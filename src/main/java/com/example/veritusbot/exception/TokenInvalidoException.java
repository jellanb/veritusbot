package com.example.veritusbot.exception;

/**
 * Excepción lanzada cuando un token JWT es inválido o expirado.
 * Se utiliza durante la validación y procesamiento de tokens.
 */
public class TokenInvalidoException extends RuntimeException {

    private static final String MENSAJE_POR_DEFECTO = "Token inválido o expirado";

    public TokenInvalidoException() {
        super(MENSAJE_POR_DEFECTO);
    }

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }

    public TokenInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

