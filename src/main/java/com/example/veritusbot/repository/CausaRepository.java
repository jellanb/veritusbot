package com.example.veritusbot.repository;

import com.example.veritusbot.model.Causa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CausaRepository extends JpaRepository<Causa, UUID> {

    /**
     * Find a causa by rol, anio and tribunal
     * Used to prevent duplicate causas from being saved
     */
    Optional<Causa> findByRolAndAnioAndTribunal(String rol, Integer anio, String tribunal);

    /**
     * Check if a causa exists by rol, anio and tribunal
     */
    boolean existsByRolAndAnioAndTribunal(String rol, Integer anio, String tribunal);
}
