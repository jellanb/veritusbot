package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    /**
     * Lee un archivo CSV y retorna una lista de PersonaDTO
     * El archivo debe estar en la raíz del proyecto
     * Formato CSV: Nombres;Apellido Paterno;Apellido Materno;AnoInit;AnoFin
     * (Usa punto y coma como separador)
     *
     * @param fileName nombre del archivo CSV (ej: "personas.csv")
     * @return lista de PersonaDTO
     */
    public List<PersonaDTO> leerPersonasDelExcel(String fileName) {
        List<PersonaDTO> personas = new ArrayList<>();

        try {
            // Ruta del archivo en la raíz del proyecto
            File file = new File(fileName);

            if (!file.exists()) {
                System.err.println("❌ El archivo no existe: " + fileName);
                System.err.println("   Ruta buscada: " + file.getAbsolutePath());
                return personas;
            }

            System.out.println("📖 Leyendo archivo: " + fileName);
            System.out.println("   Ruta: " + file.getAbsolutePath());

            // Leer el archivo CSV
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String linea;
                int numeroLinea = 0;

                while ((linea = reader.readLine()) != null) {
                    numeroLinea++;

                    // Saltar encabezado
                    if (numeroLinea == 1) {
                        System.out.println("📊 Encabezado encontrado: " + linea);
                        continue;
                    }

                    try {
                        // Parsear la línea CSV
                        String[] valores = parsearCSV(linea);

                        if (valores.length >= 5) {
                            String nombres = valores[0].trim();
                            String apellidoPaterno = valores[1].trim();
                            String apellidoMaterno = valores[2].trim();
                            int anioInit;
                            int anioFin;

                            try {
                                anioInit = Integer.parseInt(valores[3].trim());
                            } catch (NumberFormatException e) {
                                System.err.println("⚠ Error en AnoInit (línea " + numeroLinea + "): " + e.getMessage());
                                continue;
                            }

                            try {
                                anioFin = Integer.parseInt(valores[4].trim());
                            } catch (NumberFormatException e) {
                                System.err.println("⚠ Error en AnoFin (línea " + numeroLinea + "): " + e.getMessage());
                                continue;
                            }

                            if (!nombres.isEmpty() && !apellidoPaterno.isEmpty() && !apellidoMaterno.isEmpty() && anioInit > 0 && anioFin > 0) {
                                PersonaDTO persona = new PersonaDTO(nombres, apellidoPaterno, apellidoMaterno, anioInit, anioFin);
                                personas.add(persona);
                                System.out.println("✓ Persona cargada: " + persona);
                            } else {
                                System.err.println("⚠ Fila " + numeroLinea + " incompleta o con años inválidos");
                            }
                        } else {
                            System.err.println("⚠ Fila " + numeroLinea + " con formato incorrecto (esperaba 5 columnas, encontró " + valores.length + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error procesando fila " + numeroLinea + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("\n✓ Total de personas cargadas: " + personas.size());

        } catch (IOException e) {
            System.err.println("❌ Error leyendo archivo: " + e.getMessage());
        }

        return personas;
    }

    /**
     * Parsea una línea CSV simple usando punto y coma como separador
     * Formato: Nombres;Apellido Paterno;Apellido Materno;AnoInit;AnoFin
     */
    private String[] parsearCSV(String linea) {
        // Separar por punto y coma (;)
        String[] valores = linea.split(";");

        // Trim a cada valor
        for (int i = 0; i < valores.length; i++) {
            valores[i] = valores[i].trim();
        }

        return valores;
    }
}
