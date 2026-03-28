package com.example.veritusbot.service.auth;

import com.example.veritusbot.model.EstadoUsuario;
import com.example.veritusbot.model.RolUsuario;
import com.example.veritusbot.model.Usuario;
import com.example.veritusbot.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario administrador por defecto al iniciar la aplicación,
 * solo si no existe ningún admin en la base de datos.
 *
 * Esto soluciona el problema de los hashes BCrypt ficticios en los scripts SQL.
 * El hash se genera en tiempo de ejecución con el PasswordEncoder real de Spring.
 */
@Component
public class AdminInitializationService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initializeAdminUser() {
        if (!usuarioRepository.existsByEmail("admin@veritus.com")) {
            Usuario admin = new Usuario(
                    "admin@veritus.com",
                    passwordEncoder.encode("admin123"),
                    "Administrador Sistema",
                    RolUsuario.ADMIN
            );
            admin.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(admin);

            System.out.println("╔══════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ USUARIO ADMIN CREADO AUTOMÁTICAMENTE              ║");
            System.out.println("║  Email:    admin@veritus.com                          ║");
            System.out.println("║  Password: admin123                                   ║");
            System.out.println("║  Rol:      ADMIN                                      ║");
            System.out.println("╚══════════════════════════════════════════════════════╝");
        }
    }
}

