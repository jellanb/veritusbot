package com.example.veritusbot.exception;

/**
 * Excepción lanzada cuando se intenta crear un usuario con un email ya registrado.
 */
public class UsuarioYaExisteException extends RuntimeException {

    public UsuarioYaExisteException(String email) {
        super("Ya existe un usuario registrado con el email: " + email);
    }
}

