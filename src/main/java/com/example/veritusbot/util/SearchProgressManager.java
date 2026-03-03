package com.example.veritusbot.util;

import com.example.veritusbot.dto.PersonaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Maneja la persistencia del estado de búsqueda
 * Permite recuperar el progreso si un navegador se cierra inesperadamente
 */
@Component
public class SearchProgressManager {

    private static final String PROGRESS_FILE = "search_progress.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Estructura para guardar el estado de búsqueda
     */
    public static class SearchProgress {
        public String persona; // "nombres|apellidoPaterno|apellidoMaterno"
        public int anio;
        public int ultimoTribunalProcesado; // Último tribunal completado
        public long timestamp;

        public SearchProgress() {}

        public SearchProgress(String persona, int anio, int ultimoTribunal) {
            this.persona = persona;
            this.anio = anio;
            this.ultimoTribunalProcesado = ultimoTribunal;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("%s|%d|%d", persona, anio, ultimoTribunalProcesado);
        }
    }

    /**
     * Guarda el progreso actual de búsqueda
     */
    public void saveProgress(PersonaDTO persona, int anio, int ultimoTribunal) {
        try {
            String personaKey = persona.getNombres() + "|" +
                                persona.getApellidoPaterno() + "|" +
                                persona.getApellidoMaterno();

            SearchProgress progress = new SearchProgress(personaKey, anio, ultimoTribunal);

            // Leer progreso existente
            Map<String, SearchProgress> allProgress = loadAllProgress();

            // Actualizar o agregar nuevo
            String key = personaKey + ":" + anio;
            allProgress.put(key, progress);

            // Guardar
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(PROGRESS_FILE), allProgress);

        } catch (IOException e) {
            // Silent fail - no detener búsqueda por error de persistencia
        }
    }

    /**
     * Obtiene el último tribunal procesado para una persona y año específico
     * Retorna 1 si no hay progreso guardado (comienza desde el principio)
     */
    public int getLastProcessedTribunal(PersonaDTO persona, int anio) {
        try {
            String personaKey = persona.getNombres() + "|" +
                                persona.getApellidoPaterno() + "|" +
                                persona.getApellidoMaterno();
            String key = personaKey + ":" + anio;

            Map<String, SearchProgress> allProgress = loadAllProgress();
            SearchProgress progress = allProgress.get(key);

            if (progress != null) {
                return progress.ultimoTribunalProcesado + 1; // Comenzar desde el siguiente
            }
        } catch (Exception e) {
            // Silent fail - continuar con valor por defecto
        }

        return 2; // Comenzar desde el primer tribunal (índice 2)
    }

    /**
     * Limpia el progreso para una persona y año (cuando se completa exitosamente)
     */
    public void clearProgress(PersonaDTO persona, int anio) {
        try {
            String personaKey = persona.getNombres() + "|" +
                                persona.getApellidoPaterno() + "|" +
                                persona.getApellidoMaterno();
            String key = personaKey + ":" + anio;

            Map<String, SearchProgress> allProgress = loadAllProgress();
            allProgress.remove(key);

            if (!allProgress.isEmpty()) {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(PROGRESS_FILE), allProgress);
            } else {
                new File(PROGRESS_FILE).delete();
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Limpia todo el progreso (cuando se completa la búsqueda de la persona)
     */
    public void clearAllProgress(PersonaDTO persona) {
        try {
            String personaKey = persona.getNombres() + "|" +
                                persona.getApellidoPaterno() + "|" +
                                persona.getApellidoMaterno();

            Map<String, SearchProgress> allProgress = loadAllProgress();
            allProgress.entrySet().removeIf(entry -> entry.getKey().startsWith(personaKey));

            if (!allProgress.isEmpty()) {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(PROGRESS_FILE), allProgress);
            } else {
                new File(PROGRESS_FILE).delete();
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Carga todo el progreso guardado
     */
    private Map<String, SearchProgress> loadAllProgress() {
        try {
            File file = new File(PROGRESS_FILE);
            if (file.exists()) {
                return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructMapType(
                        HashMap.class, String.class, SearchProgress.class));
            }
        } catch (IOException e) {
            // Silent fail
        }
        return new HashMap<>();
    }

    /**
     * Obtiene un resumen del progreso actual
     */
    public String getProgressSummary() {
        try {
            Map<String, SearchProgress> allProgress = loadAllProgress();
            if (allProgress.isEmpty()) {
                return "Sin progreso guardado";
            }
            return String.format("%d búsquedas en progreso", allProgress.size());
        } catch (Exception e) {
            return "Error al leer progreso";
        }
    }
}

