package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.exception.InvalidClientFileException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ClientFileParserService {

    private static final Logger logger = LoggerFactory.getLogger(ClientFileParserService.class);

    private static final String COL_NOMBRES = "NOMBRES";
    private static final String COL_APELLIDO_PATERNO = "APELLIDO_PATERNO";
    private static final String COL_APELLIDO_MATERNO = "APELLIDO_MATERNO";
    private static final String COL_ANIO_INI = "ANIOINI";
    private static final String COL_ANIO_FIN = "ANIOFIN";

    private final PersonPersistenceService personPersistenceService;
    private final PersonaProcesadaPersistenceService personaProcesadaPersistenceService;

    public ClientFileParserService(PersonPersistenceService personPersistenceService,
                                   PersonaProcesadaPersistenceService personaProcesadaPersistenceService) {
        this.personPersistenceService = personPersistenceService;
        this.personaProcesadaPersistenceService = personaProcesadaPersistenceService;
    }

    public List<PersonaDTO> parseAndValidate(MultipartFile file) {
        validateFilePresence(file);
        String extension = getExtension(file.getOriginalFilename());

        List<PersonaDTO> people;
        try {
            if ("csv".equals(extension)) {
                people = parseCsv(file.getInputStream());
            } else if ("xlsx".equals(extension) || "xls".equals(extension)) {
                people = parseExcel(file.getInputStream());
            } else {
                throw new InvalidClientFileException("Formato de archivo no soportado. Solo se permite .csv, .xlsx o .xls");
            }
        } catch (IOException e) {
            throw new InvalidClientFileException("No fue posible leer el archivo de clientes");
        }

        if (people.isEmpty()) {
            throw new InvalidClientFileException("El archivo no contiene registros validos para procesar");
        }

        persistPeople(people);
        logger.info("Archivo de clientes parseado correctamente. Registros: {}", people.size());
        return people;
    }

    private void validateFilePresence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidClientFileException("El archivo es obligatorio y debe enviarse en el campo 'file'");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!"csv".equals(extension) && !"xlsx".equals(extension) && !"xls".equals(extension)) {
            throw new InvalidClientFileException("Extension invalida. Solo se permite .csv, .xlsx o .xls");
        }
    }

    private List<PersonaDTO> parseCsv(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new InvalidClientFileException("El archivo CSV no contiene cabecera");
            }

            char delimiter = detectDelimiter(headerLine);
            List<String> headerValues = parseDelimitedLine(headerLine, delimiter);
            HeaderMapping mapping = buildHeaderMapping(headerValues);

            List<PersonaDTO> people = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseDelimitedLine(line, delimiter);
                PersonaDTO person = buildPersonFromValues(values, mapping, lineNumber);
                if (person != null) {
                    people.add(person);
                }
            }
            return people;
        }
    }

    private List<PersonaDTO> parseExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new InvalidClientFileException("El archivo Excel no contiene hojas");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidClientFileException("El archivo Excel no contiene cabecera");
            }

            List<String> headers = new ArrayList<>();
            int maxColumn = headerRow.getLastCellNum();
            for (int i = 0; i < maxColumn; i++) {
                headers.add(getCellStringValue(headerRow.getCell(i)));
            }
            HeaderMapping mapping = buildHeaderMapping(headers);

            List<PersonaDTO> people = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                List<String> values = new ArrayList<>();
                for (int col = 0; col < maxColumn; col++) {
                    values.add(getCellStringValue(row.getCell(col)));
                }

                PersonaDTO person = buildPersonFromValues(values, mapping, rowIndex + 1);
                if (person != null) {
                    people.add(person);
                }
            }
            return people;
        }
    }

    private HeaderMapping buildHeaderMapping(List<String> headers) {
        Map<String, Integer> indexes = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String canonical = canonicalHeader(headers.get(i));
            if (canonical != null && !indexes.containsKey(canonical)) {
                indexes.put(canonical, i);
            }
        }

        validateRequiredHeader(indexes, COL_NOMBRES, "NOMBRES");
        validateRequiredHeader(indexes, COL_APELLIDO_PATERNO, "APELIDO PATERNO / APELLIDO PATERNO");
        validateRequiredHeader(indexes, COL_APELLIDO_MATERNO, "APELLIDO MATERNO");
        validateRequiredHeader(indexes, COL_ANIO_INI, "AÑOINI");
        validateRequiredHeader(indexes, COL_ANIO_FIN, "AÑOFIN");

        return new HeaderMapping(
                indexes.get(COL_NOMBRES),
                indexes.get(COL_APELLIDO_PATERNO),
                indexes.get(COL_APELLIDO_MATERNO),
                indexes.get(COL_ANIO_INI),
                indexes.get(COL_ANIO_FIN)
        );
    }

    private void validateRequiredHeader(Map<String, Integer> indexes, String key, String readableName) {
        if (!indexes.containsKey(key)) {
            throw new InvalidClientFileException("Cabecera requerida no encontrada: " + readableName);
        }
    }

    private PersonaDTO buildPersonFromValues(List<String> values, HeaderMapping mapping, int lineNumber) {
        String nombres = getColumn(values, mapping.nombresIdx());
        String apellidoPaterno = getColumn(values, mapping.apellidoPaternoIdx());
        String apellidoMaterno = getColumn(values, mapping.apellidoMaternoIdx());
        String anioIniRaw = getColumn(values, mapping.anioIniIdx());
        String anioFinRaw = getColumn(values, mapping.anioFinIdx());

        if (isBlank(nombres) && isBlank(apellidoPaterno) && isBlank(apellidoMaterno)
                && isBlank(anioIniRaw) && isBlank(anioFinRaw)) {
            return null;
        }

        if (isBlank(nombres)) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": la columna NOMBRES es obligatoria");
        }
        if (isBlank(apellidoPaterno)) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": la columna APELLIDO PATERNO es obligatoria");
        }
        if (isBlank(apellidoMaterno)) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": la columna APELLIDO MATERNO es obligatoria");
        }

        int anioIni = parseYear(anioIniRaw, lineNumber, "AÑOINI");
        int anioFin = parseYear(anioFinRaw, lineNumber, "AÑOFIN");

        if (anioIni > anioFin) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": AÑOINI no puede ser mayor que AÑOFIN");
        }

        return new PersonaDTO(
                nombres.trim(),
                apellidoPaterno.trim(),
                apellidoMaterno.trim(),
                anioIni,
                anioFin
        );
    }

    private int parseYear(String raw, int lineNumber, String columnName) {
        if (isBlank(raw)) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": la columna " + columnName + " es obligatoria");
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidClientFileException("Fila " + lineNumber + ": la columna " + columnName + " debe ser numerica");
        }
    }

    private void persistPeople(List<PersonaDTO> people) {
        int nuevos = 0;
        int duplicados = 0;

        for (PersonaDTO person : people) {
            if (personPersistenceService.personExists(person)) {
                duplicados++;
                continue;
            }
            personPersistenceService.saveOrGetExisting(person);
            personaProcesadaPersistenceService.saveNewPersonaProcesada(person);
            nuevos++;
        }

        logger.info("Persistencia de clientes finalizada. Nuevos: {}, duplicados: {}", nuevos, duplicados);
    }

    private char detectDelimiter(String headerLine) {
        int semicolonCount = countDelimiterOccurrences(headerLine, ';');
        int commaCount = countDelimiterOccurrences(headerLine, ',');
        return semicolonCount >= commaCount ? ';' : ',';
    }

    private int countDelimiterOccurrences(String line, char delimiter) {
        boolean inQuotes = false;
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                count++;
            }
        }
        return count;
    }

    private List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String getColumn(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private String canonicalHeader(String rawHeader) {
        if (rawHeader == null) {
            return null;
        }

        String normalized = normalize(rawHeader);
        String compact = normalized.replace(" ", "");

        return switch (compact) {
            case "NOMBRES" -> COL_NOMBRES;
            case "APELIDOPATERNO", "APELLIDOPATERNO" -> COL_APELLIDO_PATERNO;
            case "APELIDOMATERNO", "APELLIDOMATERNO" -> COL_APELLIDO_MATERNO;
            case "ANOINI", "ANIOINI", "AOINI" -> COL_ANIO_INI;
            case "ANOFIN", "ANIOFIN", "AOFIN" -> COL_ANIO_FIN;
            default -> null;
        };
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                // Tolera cabeceras con mojibake de Ñ, por ejemplo A�OINI.
                .replace('�', 'N')
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double numeric = cell.getNumericCellValue();
                long rounded = Math.round(numeric);
                if (Math.abs(numeric - rounded) < 0.0000001d) {
                    yield Long.toString(rounded);
                }
                yield Double.toString(numeric);
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private String getExtension(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return "";
        }

        return originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record HeaderMapping(int nombresIdx,
                                 int apellidoPaternoIdx,
                                 int apellidoMaternoIdx,
                                 int anioIniIdx,
                                 int anioFinIdx) {
    }
}

