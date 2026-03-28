package com.example.veritusbot.service.auth;

import com.example.veritusbot.dto.LoginRequestDTO;
import com.example.veritusbot.dto.LoginResponseDTO;
import com.example.veritusbot.dto.UsuarioDTO;
import com.example.veritusbot.exception.ContrasenaInvalidaException;
import com.example.veritusbot.exception.UsuarioBloqueadoException;
import com.example.veritusbot.exception.UsuarioNoEncontradoException;
import com.example.veritusbot.model.Usuario;
import com.example.veritusbot.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación.
 * Maneja la lógica de validación de credenciales y generación de sesiones.
 *
 * SOLID Principle - Single Responsibility:
 * Solo es responsable de la lógica de autenticación, no de gestión de datos.
 */
@Service
public class AuthenticationService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Autentica un usuario con sus credenciales.
     * Realiza validaciones completas antes de generar el token.
     *
     * @param loginRequest DTO con email y password
     * @return DTO con token y datos del usuario autenticado
     * @throws UsuarioNoEncontradoException si no existe usuario con ese email
     * @throws ContrasenaInvalidaException si la contraseña no coincide
     * @throws UsuarioBloqueadoException si el usuario está bloqueado
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {

        // 1. Validar que los datos no estén vacíos
        if (!loginRequest.isValid()) {
            throw new IllegalArgumentException("Email y contraseña son requeridos");
        }

        // 2. Buscar usuario por email
        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "Email o contraseña incorrectos"
                ));

        // 3. Validar que el usuario no esté bloqueado
        if (usuario.isBloqueado()) {
            throw new UsuarioBloqueadoException(
                    "Cuenta bloqueada. Contacte al administrador"
            );
        }

        // 4. Validar que el usuario esté activo
        if (!usuario.isActivo()) {
            throw new UsuarioNoEncontradoException(
                    "Usuario inactivo. Contacte al administrador"
            );
        }

        // 5. Validar la contraseña (comparar con hash BCrypt)
        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
            throw new ContrasenaInvalidaException(
                    "Email o contraseña incorrectos"
            );
        }

        // 6. Generar token JWT
        String token = jwtTokenProvider.generateToken(usuario);

        // 7. Actualizar último acceso
        usuario.setLastLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // 8. Construir respuesta exitosa
        UsuarioDTO usuarioDTO = convertToDTO(usuario);
        return new LoginResponseDTO(token, usuarioDTO);
    }

    /**
     * Valida si un token JWT es válido.
     *
     * @param token token JWT a validar
     * @return true si el token es válido
     */
    public boolean isTokenValid(String token) {
        return jwtTokenProvider.isTokenValid(token);
    }

    /**
     * Convierte una entidad Usuario a DTO.
     * Asegura que no se exponga información sensible.
     *
     * @param usuario entidad Usuario
     * @return DTO con información pública
     */
    private UsuarioDTO convertToDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNombreCompleto(usuario.getNombreCompleto());
        dto.setRol(usuario.getRol().toString());
        dto.setEstado(usuario.getEstado().toString());
        dto.setCreatedAt(usuario.getCreatedAt());
        dto.setLastLogin(usuario.getLastLogin());
        return dto;
    }
}

