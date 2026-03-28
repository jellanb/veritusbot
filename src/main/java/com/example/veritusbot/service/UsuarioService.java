package com.example.veritusbot.service;

import com.example.veritusbot.dto.CreateUsuarioRequestDTO;
import com.example.veritusbot.dto.UsuarioDTO;
import com.example.veritusbot.exception.UsuarioNoEncontradoException;
import com.example.veritusbot.exception.UsuarioYaExisteException;
import com.example.veritusbot.model.EstadoUsuario;
import com.example.veritusbot.model.RolUsuario;
import com.example.veritusbot.model.Usuario;
import com.example.veritusbot.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios del sistema.
 * Maneja creación, consulta y administración de usuarios.
 *
 * SOLID Principle - Single Responsibility:
 * Solo gestiona operaciones CRUD de usuarios, no autenticación.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==================== CREAR USUARIO ====================

    /**
     * Crea un nuevo usuario en el sistema.
     *
     * Flujo:
     * 1. Valida que el request tenga los datos mínimos requeridos
     * 2. Normaliza el email (lowercase + trim)
     * 3. Verifica que el email no esté ya registrado
     * 4. Valida y parsea el rol (por defecto VIEWER si no se especifica)
     * 5. Hashea la contraseña con BCrypt
     * 6. Persiste el usuario en la base de datos
     * 7. Retorna DTO con datos del usuario creado (sin contraseña)
     *
     * @param request DTO con datos del nuevo usuario
     * @return DTO con información del usuario creado
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws UsuarioYaExisteException si el email ya está registrado
     */
    @Transactional
    public UsuarioDTO crearUsuario(CreateUsuarioRequestDTO request) {

        // 1. Validar campos obligatorios
        if (!request.isValid()) {
            throw new IllegalArgumentException(
                    "Datos inválidos: email, password (mínimo 6 caracteres) y nombreCompleto son obligatorios"
            );
        }

        // 2. Normalizar email
        String emailNormalizado = request.getEmail().trim().toLowerCase();

        // 3. Verificar que el email no exista ya
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new UsuarioYaExisteException(emailNormalizado);
        }

        // 4. Parsear rol (VIEWER por defecto si no se especifica)
        RolUsuario rol = parsearRol(request.getRol());

        // 5. Hashear contraseña con BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 6. Crear y persistir la entidad
        Usuario nuevoUsuario = new Usuario(
                emailNormalizado,
                passwordHash,
                request.getNombreCompleto().trim(),
                rol
        );
        nuevoUsuario.setEstado(EstadoUsuario.ACTIVO);

        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        System.out.println("✅ Usuario creado: " + guardado.getEmail() + " | Rol: " + guardado.getRol());

        // 7. Retornar DTO (nunca el hash)
        return convertToDTO(guardado);
    }

    // ==================== LISTAR USUARIOS ====================

    /**
     * Retorna la lista de todos los usuarios del sistema.
     *
     * @return lista de UsuarioDTO
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== BUSCAR POR ID ====================

    /**
     * Busca un usuario por su UUID.
     *
     * @param id UUID del usuario
     * @return UsuarioDTO si existe
     * @throws UsuarioNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "No existe usuario con id: " + id
                ));
        return convertToDTO(usuario);
    }

    // ==================== PRIVADOS ====================

    /**
     * Parsea el string de rol al enum RolUsuario.
     * Si el valor es nulo o inválido, asigna VIEWER por defecto.
     *
     * @param rolString string del rol ("ADMIN", "OPERADOR", "VIEWER")
     * @return RolUsuario correspondiente
     */
    private RolUsuario parsearRol(String rolString) {
        if (rolString == null || rolString.trim().isEmpty()) {
            return RolUsuario.VIEWER;
        }
        try {
            return RolUsuario.valueOf(rolString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Rol inválido: '" + rolString + "'. Valores permitidos: ADMIN, OPERADOR, VIEWER"
            );
        }
    }

    /**
     * Convierte una entidad Usuario a DTO (nunca expone el hash).
     *
     * @param usuario entidad
     * @return DTO con datos públicos
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

