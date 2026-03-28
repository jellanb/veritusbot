package com.example.veritusbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad y CORS para la aplicación.
 * Define el codificador de contraseñas y políticas de CORS.
 *
 * SOLID Principle - Single Responsibility:
 * Solo configura componentes de seguridad, no implementa lógica.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Define el codificador de contraseñas BCrypt.
     * BCrypt es un algoritmo seguro para hashear contraseñas.
     *
     * @return PasswordEncoder configurado con BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 12 rounds de hashing
    }

    /**
     * Configura CORS (Cross-Origin Resource Sharing).
     * Permite que el frontend (en otro puerto) acceda a esta API.
     *
     * @return CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",      // React en desarrollo
                "http://localhost:4200",      // Angular en desarrollo
                "http://localhost:8080",      // Frontend en desarrollo
                "http://localhost:5173",      // Vite en desarrollo
                "http://127.0.0.1:3000",
                "http://127.0.0.1:4200"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // Headers que el cliente puede leer
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        // Credenciales permitidas (cookies, headers de auth)
        configuration.setAllowCredentials(true);

        // Tiempo de caché para preflight requests (en segundos)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

