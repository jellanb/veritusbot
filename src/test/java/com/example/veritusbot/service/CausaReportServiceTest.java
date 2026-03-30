package com.example.veritusbot.service;

import com.example.veritusbot.dto.CausaReportResponseDTO;
import com.example.veritusbot.model.Causa;
import com.example.veritusbot.repository.CausaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CausaReportServiceTest {

    @Mock
    private CausaRepository causaRepository;

    private CausaReportService causaReportService;

    @BeforeEach
    void setUp() {
        causaReportService = new CausaReportService(causaRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReporteShouldMapContentAndPagination() {
        Causa causa = new Causa();
        causa.setId(UUID.randomUUID());
        causa.setPersonaId(UUID.randomUUID());
        causa.setRol("C-123-2025");
        causa.setAnio(2025);
        causa.setCaratula("PEREZ CON DEMANDADO");
        causa.setTribunal("1 JUZGADO CIVIL");

        when(causaRepository.findAll((Specification<Causa>) any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(causa)));

        CausaReportResponseDTO response = causaReportService.getReporte(2020, 2026, "civil", "asc", 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(2025, response.getContent().get(0).getAnio());
        assertEquals("anio", response.getSortBy());
        assertEquals("asc", response.getSortDir());
        assertEquals(1, response.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(causaRepository, times(1))
                .findAll((Specification<Causa>) any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals("anio: ASC", pageable.getSort().toString());
    }

    @Test
    void getReporteShouldThrowWhenYearRangeIsInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> causaReportService.getReporte(2026, 2020, null, "desc", 0, 50)
        );

        assertEquals("El filtro anioDesde no puede ser mayor que anioHasta", exception.getMessage());
    }

    @Test
    void getReporteShouldThrowWhenSortDirIsInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> causaReportService.getReporte(null, null, null, "up", 0, 50)
        );

        assertEquals("El parametro sortDir solo acepta 'asc' o 'desc'", exception.getMessage());
    }

    @Test
    void getReporteShouldThrowWhenSizeIsOutOfRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> causaReportService.getReporte(null, null, null, "desc", 0, 0)
        );

        assertEquals("El parametro size debe estar entre 1 y 200", exception.getMessage());
    }
}
