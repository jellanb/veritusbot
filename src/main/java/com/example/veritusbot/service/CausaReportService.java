package com.example.veritusbot.service;

import com.example.veritusbot.dto.CausaReportItemDTO;
import com.example.veritusbot.dto.CausaReportResponseDTO;
import com.example.veritusbot.model.Causa;
import com.example.veritusbot.repository.CausaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de reporte de causas con filtros y paginacion.
 */
@Service
public class CausaReportService {

    private static final String SORT_FIELD_ANIO = "anio";

    private final CausaRepository causaRepository;

    public CausaReportService(CausaRepository causaRepository) {
        this.causaRepository = causaRepository;
    }

    public CausaReportResponseDTO getReporte(Integer anioDesde,
                                             Integer anioHasta,
                                             String tribunal,
                                             String sortDir,
                                             int page,
                                             int size) {
        validateInputs(anioDesde, anioHasta, sortDir, page, size);

        Sort.Direction direction = parseDirection(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, SORT_FIELD_ANIO));

        Specification<Causa> specification = buildSpecification(anioDesde, anioHasta, tribunal);
        Page<Causa> causas = causaRepository.findAll(specification, pageable);

        List<CausaReportItemDTO> content = causas.getContent().stream()
                .map(this::toItemDto)
                .toList();

        return new CausaReportResponseDTO(
                content,
                causas.getNumber(),
                causas.getSize(),
                causas.getTotalElements(),
                causas.getTotalPages(),
                SORT_FIELD_ANIO,
                direction.name().toLowerCase(Locale.ROOT)
        );
    }

    private void validateInputs(Integer anioDesde, Integer anioHasta, String sortDir, int page, int size) {
        if (anioDesde != null && anioHasta != null && anioDesde > anioHasta) {
            throw new IllegalArgumentException("El filtro anioDesde no puede ser mayor que anioHasta");
        }

        if (page < 0) {
            throw new IllegalArgumentException("El parametro page debe ser mayor o igual a 0");
        }

        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("El parametro size debe estar entre 1 y 200");
        }

        if (sortDir != null && !("asc".equalsIgnoreCase(sortDir) || "desc".equalsIgnoreCase(sortDir))) {
            throw new IllegalArgumentException("El parametro sortDir solo acepta 'asc' o 'desc'");
        }
    }

    private Sort.Direction parseDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    private Specification<Causa> buildSpecification(Integer anioDesde, Integer anioHasta, String tribunal) {
        Specification<Causa> specification = Specification.where(null);

        if (anioDesde != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("anio"), anioDesde));
        }

        if (anioHasta != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("anio"), anioHasta));
        }

        if (tribunal != null && !tribunal.trim().isEmpty()) {
            String tribunalNormalized = tribunal.trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("tribunal")), "%" + tribunalNormalized + "%"));
        }

        return specification;
    }

    private CausaReportItemDTO toItemDto(Causa causa) {
        return new CausaReportItemDTO(
                causa.getId(),
                causa.getPersonaId(),
                causa.getRol(),
                causa.getAnio(),
                causa.getCaratula(),
                causa.getTribunal()
        );
    }
}

