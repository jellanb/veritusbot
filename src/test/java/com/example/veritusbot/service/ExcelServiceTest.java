package com.example.veritusbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.veritusbot.dto.PersonaDTO;

@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    @Mock
    private PersonPersistenceService personPersistenceService;

    @Mock
    private PersonaProcesadaPersistenceService personaProcesadaPersistenceService;

    @TempDir
    Path tempDir;

    @Test
    void readClientFromCSVShouldReturnEmptyWhenFileDoesNotExist() {
        ExcelService excelService = new ExcelService(personPersistenceService, personaProcesadaPersistenceService);

        List<PersonaDTO> result = excelService.readClientFromCSV(tempDir.resolve("missing.csv").toString());

        assertTrue(result.isEmpty());
        verify(personPersistenceService, never()).personExists(any());
        verify(personPersistenceService, never()).saveOrGetExisting(any());
        verify(personaProcesadaPersistenceService, never()).saveNewPersonaProcesada(any());
    }

    @Test
    void readClientFromCSVShouldLoadValidRowsHandleDuplicatesAndSkipInvalidRows() throws IOException {
        Path csv = tempDir.resolve("personas.csv");
        Files.writeString(csv, String.join("\n",
                "Nombres;ApellidoPaterno;ApellidoMaterno;AnioInicio;AnioFin",
                "Ana;Perez;Diaz;2020;2021",
                "Luis;Gomez;Rojas;2022;2023",
                "Malo;Dato;Invalido;no-num;2024",
                "Solo;Dos;Columnas",
                " ; ; ;2020;2021"
        ), StandardCharsets.UTF_8);

        ExcelService excelService = new ExcelService(personPersistenceService, personaProcesadaPersistenceService);
        when(personPersistenceService.personExists(any(PersonaDTO.class))).thenReturn(false, true);

        List<PersonaDTO> result = excelService.readClientFromCSV(csv.toString());

        assertEquals(2, result.size());
        assertEquals("Ana", result.get(0).getNombres());
        assertEquals("Luis", result.get(1).getNombres());

        verify(personPersistenceService, times(2)).personExists(any(PersonaDTO.class));
        verify(personPersistenceService, times(1)).saveOrGetExisting(any(PersonaDTO.class));
        verify(personaProcesadaPersistenceService, times(1)).saveNewPersonaProcesada(any(PersonaDTO.class));
    }
}

