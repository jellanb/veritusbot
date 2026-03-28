package com.example.veritusbot.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad para generar hashes BCrypt de contraseñas.
 * Útil para crear usuarios de prueba o en scripts de migración.
 *
 * Uso en línea de comandos:
 * java -cp ... com.example.veritusbot.util.PasswordEncoderUtil "miContraseña"
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java PasswordEncoderUtil <contraseña>");
            System.out.println("\nEjemplo:");
            System.out.println("  java PasswordEncoderUtil admin123");
            System.out.println("\nGenerará el hash BCrypt de la contraseña.");
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = args[0];
        String hash = encoder.encode(password);

        System.out.println("Contraseña: " + password);
        System.out.println("Hash BCrypt: " + hash);
        System.out.println("\nCopia el hash anterior en la columna 'password_hash' de la tabla usuarios.");
    }
}

