package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.exception.InvalidClientFileException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientFileParserServiceTest {

    @Mock
    private PersonPersistenceService personPersistenceService;

    @Mock
    private PersonaProcesadaPersistenceService personaProcesadaPersistenceService;

    @Test
    void parseAndValidateCsvShouldLoadRowsAndAcceptHeaderAlias() {
        String csv = String.join("\n",
                " nombres ; APELLIDO PATERNO ; apellido materno ; añoini ; añofin ",
                "ALEX ALEJANDRO;BREVIS;CATALAN;2019;2025",
                "ANA MARIA;REYES;PEDREROS;2020;2026"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        when(personPersistenceService.personExists(any(PersonaDTO.class))).thenReturn(false);
        ClientFileParserService parserService = new ClientFileParserService(personPersistenceService, personaProcesadaPersistenceService);

        List<PersonaDTO> result = parserService.parseAndValidate(file);

        assertEquals(2, result.size());
        assertEquals("ALEX ALEJANDRO", result.get(0).getNombres());
        assertEquals(2019, result.get(0).getAnioInit());
        verify(personPersistenceService, times(2)).personExists(any(PersonaDTO.class));
        verify(personPersistenceService, times(2)).saveOrGetExisting(any(PersonaDTO.class));
        verify(personaProcesadaPersistenceService, times(2)).saveNewPersonaProcesada(any(PersonaDTO.class));
    }

    @Test
    void parseAndValidateExcelShouldReadFirstSheetByColumns() throws IOException {
        byte[] workbookBytes = buildWorkbook(
                new String[]{"NOMBRES", "APELIDO PATERNO", "APELLIDO MATERNO", "AÑOINI", "AÑOFIN"},
                new String[]{"ALEX ALEJANDRO", "BREVIS", "CATALAN", "2019", "2025"}
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        when(personPersistenceService.personExists(any(PersonaDTO.class))).thenReturn(false);
        ClientFileParserService parserService = new ClientFileParserService(personPersistenceService, personaProcesadaPersistenceService);

        List<PersonaDTO> result = parserService.parseAndValidate(file);

        assertEquals(1, result.size());
        assertEquals("BREVIS", result.get(0).getApellidoPaterno());
        assertEquals(2025, result.get(0).getAnioFin());
    }

    @Test
    void parseAndValidateCsvShouldAutoDetectCommaDelimiter() {
        String csv = String.join("\n",
                "NOMBRES,APELIDO PATERNO,APELLIDO MATERNO,AÑOINI,AÑOFIN",
                "ALEX ALEJANDRO,BREVIS,CATALAN,2019,2025"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        when(personPersistenceService.personExists(any(PersonaDTO.class))).thenReturn(false);
        ClientFileParserService parserService = new ClientFileParserService(personPersistenceService, personaProcesadaPersistenceService);

        List<PersonaDTO> result = parserService.parseAndValidate(file);

        assertEquals(1, result.size());
        assertEquals("ALEX ALEJANDRO", result.get(0).getNombres());
    }

    @Test
    void parseAndValidateShouldFailWhenHeaderIsMissing() {
        String csv = String.join("\n",
                "NOMBRES;APELIDO PATERNO;AÑOINI;AÑOFIN",
                "ALEX ALEJANDRO;BREVIS;2019;2025"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ClientFileParserService parserService = new ClientFileParserService(personPersistenceService, personaProcesadaPersistenceService);

        InvalidClientFileException exception = assertThrows(
                InvalidClientFileException.class,
                () -> parserService.parseAndValidate(file)
        );

        assertEquals("Cabecera requerida no encontrada: APELLIDO MATERNO", exception.getMessage());
        verify(personPersistenceService, never()).saveOrGetExisting(any());
    }

    @Test
    void parseAndValidateShouldFailWhenYearRangeIsInvalid() {
        String csv = String.join("\n",
                "NOMBRES;APELIDO PATERNO;APELLIDO MATERNO;AÑOINI;AÑOFIN",
                "ALEX ALEJANDRO;BREVIS;CATALAN;2026;2025"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ClientFileParserService parserService = new ClientFileParserService(personPersistenceService, personaProcesadaPersistenceService);

        InvalidClientFileException exception = assertThrows(
                InvalidClientFileException.class,
                () -> parserService.parseAndValidate(file)
        );

        assertEquals("Fila 2: AÑOINI no puede ser mayor que AÑOFIN", exception.getMessage());
    }

    private byte[] buildWorkbook(String[] headers, String[] rowValues) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Clientes");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Row row = sheet.createRow(1);
            for (int i = 0; i < rowValues.length; i++) {
                row.createCell(i).setCellValue(rowValues[i]);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}

