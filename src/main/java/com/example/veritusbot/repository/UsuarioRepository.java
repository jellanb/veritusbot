package com.example.veritusbot.repository;

import com.example.veritusbot.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para la entidad Usuario.
 * Proporciona operaciones CRUD y búsquedas personalizadas.
 *
 * SOLID Principle - Dependency Inversion:
 * Define contratos sin implementación, la BD la proporciona Spring Data.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca un usuario por su email.
     *
     * @param email email del usuario
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email email del usuario
     * @return true si existe, false en caso contrario
     */
    boolean existsByEmail(String email);
}

