package com.example.veritusbot.service.auth;

import com.example.veritusbot.exception.TokenInvalidoException;
import com.example.veritusbot.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Componente responsable de generar y validar tokens JWT.
 * Maneja la autenticación basada en tokens.
 *
 * SOLID Principle - Single Responsibility:
 * Solo es responsable de crear y validar JWTs, no de autenticación.
 */
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:cambia-este-secret-en-produccion-minimo-64-bytes-para-hs512-por-seguridad}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")  // 24 horas por defecto
    private long jwtExpiration;

    /**
     * HS512 requiere una clave HMAC de al menos 64 bytes (512 bits).
     * Fallamos rápido al iniciar para evitar errores en runtime durante login.
     */
    @PostConstruct
    public void validateSecretAtStartup() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 64) {
            throw new IllegalStateException(
                    "Configuración inválida: app.jwt.secret debe tener al menos 64 bytes para HS512. " +
                    "Longitud actual: " + secretBytes.length + " bytes"
            );
        }
    }

    /**
     * Genera un token JWT para un usuario.
     *
     * @param usuario usuario para el cual generar el token
     * @return token JWT en forma de String
     */
    public String generateToken(Usuario usuario) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().toString())
                .claim("nombreCompleto", usuario.getNombreCompleto())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extrae los claims (datos) de un token JWT válido.
     *
     * @param token token JWT
     * @return claims del token
     * @throws TokenInvalidoException si el token es inválido o expiró
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new TokenInvalidoException("Token inválido o expirado: " + e.getMessage(), e);
        }
    }

    /**
     * Extrae el UUID del usuario del token.
     *
     * @param token token JWT
     * @return UUID del usuario
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extrae el email del usuario del token.
     *
     * @param token token JWT
     * @return email del usuario
     */
    public String getEmailFromToken(String token) {
        return (String) getClaimsFromToken(token).get("email");
    }

    /**
     * Extrae el rol del usuario del token.
     *
     * @param token token JWT
     * @return rol del usuario
     */
    public String getRolFromToken(String token) {
        return (String) getClaimsFromToken(token).get("rol");
    }

    /**
     * Verifica si un token es válido.
     *
     * @param token token JWT
     * @return true si es válido, false si expiró o es inválido
     */
    public boolean isTokenValid(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (TokenInvalidoException e) {
            return false;
        }
    }

    /**
     * Obtiene la clave para firmar y validar JWTs.
     *
     * @return SecretKey para operaciones criptográficas
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

