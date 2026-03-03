package com.example.veritusbot.repository;

import com.example.veritusbot.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    /**
     * Buscar una persona por nombre, apellidos
     */
    Optional<Persona> findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
        String primerNombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno
    );
}
