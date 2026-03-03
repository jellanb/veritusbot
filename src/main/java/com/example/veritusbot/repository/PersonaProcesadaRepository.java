package com.example.veritusbot.repository;

import com.example.veritusbot.model.PersonaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaProcesadaRepository extends JpaRepository<PersonaProcesada, Integer> {

    /**
     * Busca una persona procesada por sus datos
     */
    Optional<PersonaProcesada> findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
            String primerNombre, String segundoNombre, String apellidoPaterno, String apellidoMaterno);

    /**
     * Obtiene todas las personas para filtrar en Java
     */
    List<PersonaProcesada> findAll();
}



