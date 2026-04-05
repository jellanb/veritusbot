package com.example.veritusbot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.veritusbot.security.JwtAuthenticationFilter;

/**
 * Configuración principal de Spring Security.
 * Define qué endpoints requieren autenticación y cuáles son públicos.
 *
 * SOLID Principle - Single Responsibility:
 * Solo configura políticas de seguridad HTTP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Define la cadena de filtros de seguridad HTTP.
     * Especifica qué rutas requieren autenticación y cuáles son públicas.
     *
     * @param http HttpSecurity para configurar seguridad
     * @return SecurityFilterChain configurado
     * @throws Exception en caso de error
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Deshabilitar CSRF (para APIs stateless con JWT)
                .csrf(csrf -> csrf.disable())

                // Configurar políticas de sesión (stateless para JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configurar autorización de endpoints
                .authorizeHttpRequests(authz -> authz
                        // ✅ PÚBLICOS - Sin autenticación requerida
                        .requestMatchers(HttpMethod.POST, "/api/veritus-app/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/veritus-app/health-auth").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/veritus-app/usuarios/listado").permitAll()
                        .requestMatchers("/api/veritus-app/health-auth").permitAll()

                        // ✅ SWAGGER Y DOCS (si existen)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                        // 🔐 PROTEGIDOS - Solo ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/veritus-app/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/veritus-app/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/veritus-app/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/veritus-app/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/veritus-app/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/veritus-app/usuarios/**").hasRole("ADMIN")

                        // 🔐 PROTEGIDOS - Requieren autenticación
                        .requestMatchers(HttpMethod.POST, "/api/buscar-personas").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/buscar-personas/detener").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/buscar-personas/**").authenticated()

                        // Por defecto, todos los demás requieren autenticación
                        .anyRequest().authenticated()
                )

                // Agregar filtro JWT antes del filtro de autenticación por defecto
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Manejo de excepciones
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"No autenticado\",\"mensaje\":\"" +
                                    authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Acceso denegado\",\"mensaje\":\"" +
                                    accessDeniedException.getMessage() + "\"}");
                        })
                );

        return http.build();
    }
}

