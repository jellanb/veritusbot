package com.example.veritusbot.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Gestiona el rango horario de operación de la aplicación
 * La app solo trabaja entre 8 AM y 8 PM
 */
@Component
public class WorkingHoursManager {

    private static final LocalTime HORA_INICIO = LocalTime.of(8, 0);    // 8 AM
    private static final LocalTime HORA_FIN = LocalTime.of(20, 0);      // 8 PM

    /**
     * Verifica si la hora actual está dentro del rango de trabajo
     */
    public boolean estaEnRangoHorario() {
        LocalTime ahora = LocalDateTime.now().toLocalTime();
        return !ahora.isBefore(HORA_INICIO) && ahora.isBefore(HORA_FIN);
    }

    /**
     * Obtiene la hora de inicio del rango
     */
    public LocalTime getHoraInicio() {
        return HORA_INICIO;
    }

    /**
     * Obtiene la hora de fin del rango
     */
    public LocalTime getHoraFin() {
        return HORA_FIN;
    }

    /**
     * Calcula cuánto tiempo falta para el siguiente rango de trabajo
     * Si estamos en horario de trabajo, retorna 0
     * Si es después de 8 PM, calcula minutos hasta 8 AM del día siguiente
     * Si es antes de 8 AM, calcula minutos hasta 8 AM de hoy
     */
    public long getMinutosHastaSiguienteRango() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalTime horaActual = ahora.toLocalTime();

        // Si estamos en rango, retornar 0
        if (estaEnRangoHorario()) {
            return 0;
        }

        // Si es después de 8 PM
        if (horaActual.isAfter(HORA_FIN) || horaActual.equals(HORA_FIN)) {
            // Calcular minutos hasta 8 AM del día siguiente
            LocalDateTime proximoInicio = ahora.plusDays(1)
                .withHour(HORA_INICIO.getHour())
                .withMinute(HORA_INICIO.getMinute())
                .withSecond(0);

            return java.time.temporal.ChronoUnit.MINUTES.between(ahora, proximoInicio);
        }

        // Si es antes de 8 AM
        LocalDateTime proximoInicio = ahora
            .withHour(HORA_INICIO.getHour())
            .withMinute(HORA_INICIO.getMinute())
            .withSecond(0);

        return java.time.temporal.ChronoUnit.MINUTES.between(ahora, proximoInicio);
    }

    /**
     * Obtiene una descripción formateada del siguiente rango
     */
    public String getProximoRangoFormateado() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalTime horaActual = ahora.toLocalTime();

        if (estaEnRangoHorario()) {
            return String.format("En horario de trabajo (hasta las %02d:%02d)", HORA_FIN.getHour(), HORA_FIN.getMinute());
        }

        if (horaActual.isAfter(HORA_FIN) || horaActual.equals(HORA_FIN)) {
            LocalDateTime proximoInicio = ahora.plusDays(1)
                .withHour(HORA_INICIO.getHour())
                .withMinute(HORA_INICIO.getMinute());

            return String.format("Próximo horario: %02d:%02d %s",
                HORA_INICIO.getHour(),
                HORA_INICIO.getMinute(),
                proximoInicio.toLocalDate());
        }

        return String.format("Próximo horario: %02d:%02d hoy",
            HORA_INICIO.getHour(),
            HORA_INICIO.getMinute());
    }

    /**
     * Espera hasta el siguiente rango horario
     */
    public void esperarSiguienteRango() throws InterruptedException {
        long minutos = getMinutosHastaSiguienteRango();

        if (minutos == 0) {
            return;
        }

        long milisegundos = minutos * 60 * 1000;
        Thread.sleep(milisegundos);
    }
}

