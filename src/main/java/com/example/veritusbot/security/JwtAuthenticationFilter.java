package com.example.veritusbot.security;

import com.example.veritusbot.exception.TokenInvalidoException;
import com.example.veritusbot.service.auth.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Filtro para validar JWT en cada request.
 * Extrae el token del header Authorization e invalida credenciales.
 *
 * SOLID Principle - Single Responsibility:
 * Solo valida JWTs, no implementa lógica de negocio.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Filtra cada request para validar el JWT.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain FilterChain
     * @throws ServletException en caso de error
     * @throws IOException en caso de error
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Obtener el token del header
            String jwt = extractJwtFromRequest(request);

            // Si hay token y es válido, establecer autenticación
            if (jwt != null && jwtTokenProvider.isTokenValid(jwt)) {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);

                // Construir autenticación
                String userId = claims.getSubject();
                String rol = (String) claims.get("rol");

                Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (rol != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (TokenInvalidoException e) {
            // Token inválido o expirado - continuar sin autenticación
            // Spring Security rechazará el request si es necesario autenticación
        } catch (Exception e) {
            // Error al procesar token - continuar sin autenticación
            logger.debug("Error al procesar JWT: " + e.getMessage());
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el JWT del header Authorization.
     * Formato esperado: Authorization: Bearer <token>
     *
     * @param request HttpServletRequest
     * @return token sin el prefijo Bearer, o null si no existe
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}

