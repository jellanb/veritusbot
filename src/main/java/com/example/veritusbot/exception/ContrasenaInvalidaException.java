package com.example.veritusbot.exception;

/**
 * Excepción lanzada cuando la contraseña proporcionada es inválida.
 * Se utiliza durante la validación de credenciales.
 */
public class ContrasenaInvalidaException extends RuntimeException {

    private static final String MENSAJE_POR_DEFECTO = "Contraseña inválida";

    public ContrasenaInvalidaException() {
        super(MENSAJE_POR_DEFECTO);
    }

    public ContrasenaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public ContrasenaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

