package com.example.veritusbot.exception;

/**
 * Excepcion para errores de validacion en archivos de carga de clientes.
 */
public class InvalidClientFileException extends RuntimeException {

    public InvalidClientFileException(String message) {
        super(message);
    }
}

