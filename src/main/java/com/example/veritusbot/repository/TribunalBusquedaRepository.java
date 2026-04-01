package com.example.veritusbot.repository;

import com.example.veritusbot.model.TribunalBusqueda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribunalBusquedaRepository extends JpaRepository<TribunalBusqueda, Long> {

    /**
     * Find all tribunal searches for a person in a specific request and phase
     */
    List<TribunalBusqueda> findByPersonaProcessadaIdAndSolicitudIdAndFase(
            Integer personaProcessadaId,
            String solicitudId,
            String fase
    );

    /**
     * Find all tribunal searches for a person in a specific request, year and phase.
     */
    List<TribunalBusqueda> findByPersonaProcessadaIdAndSolicitudIdAndAnnoAndFase(
            Integer personaProcessadaId,
            String solicitudId,
            Integer anno,
            String fase
    );

    /**
     * Find specific tribunal search
     */
    Optional<TribunalBusqueda> findByPersonaProcessadaIdAndSolicitudIdAndTribunalNombreAndAnnoAndFase(
            Integer personaProcessadaId,
            String solicitudId,
            String tribunalNombre,
            Integer anno,
            String fase
    );

    /**
     * Count tribunal searches with specific status for a request.
     */
    long countByPersonaProcessadaIdAndSolicitudIdAndFaseAndEstado(
            Integer personaProcessadaId,
            String solicitudId,
            String fase,
            String estado
    );

    /**
     * Get all tribunals with errors for analysis
     */
    @Query("""
        SELECT tb FROM TribunalBusqueda tb
        WHERE tb.personaProcessadaId = :personaId
        AND tb.solicitudId = :solicitudId
        AND tb.fase = :fase
        AND (tb.estado = 'ERROR_SCRAPER' OR tb.estado = 'ERROR_CONEXION')
    """)
    List<TribunalBusqueda> findErroredTribunals(
            @Param("personaId") Integer personaProcessadaId,
            @Param("solicitudId") String solicitudId,
            @Param("fase") String fase
    );
}

