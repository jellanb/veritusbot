package com.example.veritusbot.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestiona el rango horario de operación de la aplicación
 * La app solo trabaja entre 8 AM y 8 PM (si está habilitado)
 *
 * Configuración runtime en memoria (inicia en true al levantar la app).
 *  - true: Respeta el rango horario (08:00-20:00)
 *  - false: Busca sin importar el rango horario (24/7)
 */
@Component
public class WorkingHoursManager {

    private static final LocalTime DEFAULT_HORA_INICIO = LocalTime.of(8, 0);    // 8 AM
    private static final LocalTime DEFAULT_HORA_FIN = LocalTime.of(20, 0);      // 8 PM

    private final AtomicBoolean workingHoursEnabled = new AtomicBoolean(true);
    private volatile LocalTime horaInicio = DEFAULT_HORA_INICIO;
    private volatile LocalTime horaFin = DEFAULT_HORA_FIN;

    /**
     * Verifica si la hora actual está dentro del rango de trabajo
     *
     * Si workingHoursEnabled = false, siempre retorna true (trabaja 24/7)
     * Si workingHoursEnabled = true, verifica si está entre 08:00 y 20:00
     */
    public boolean estaEnRangoHorario() {
        // Si el rango horario está deshabilitado, siempre está en rango (24/7)
        if (!workingHoursEnabled.get()) {
            return true;
        }

        LocalTime ahora = LocalDateTime.now().toLocalTime();
        return !ahora.isBefore(horaInicio) && ahora.isBefore(horaFin);
    }

    /**
     * Obtiene la hora de inicio del rango
     */
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    /**
     * Obtiene la hora de fin del rango
     */
    public LocalTime getHoraFin() {
        return horaFin;
    }

    /**
     * Verifica si el rango horario está habilitado
     * @return true si respeta el rango horario, false si trabaja 24/7
     */
    public boolean isWorkingHoursEnabled() {
        return workingHoursEnabled.get();
    }

    /**
     * Actualiza en runtime si el rango horario está habilitado.
     */
    public void setWorkingHoursEnabled(boolean enabled) {
        workingHoursEnabled.set(enabled);
    }

    /**
     * Actualiza en runtime el rango horario permitido para búsquedas.
     */
    public void setRangoHorario(LocalTime nuevaHoraInicio, LocalTime nuevaHoraFin) {
        if (nuevaHoraInicio == null || nuevaHoraFin == null) {
            throw new IllegalArgumentException("horaInicio y horaFin son obligatorias");
        }
        if (!nuevaHoraInicio.isBefore(nuevaHoraFin)) {
            throw new IllegalArgumentException("horaInicio debe ser menor a horaFin");
        }
        this.horaInicio = nuevaHoraInicio;
        this.horaFin = nuevaHoraFin;
    }

    /**
     * Obtiene una descripción del estado de configuración
     */
    public String getConfiguracionDescripcion() {
        if (workingHoursEnabled.get()) {
            return String.format("✓ Rango horario HABILITADO (%s-%s)", horaInicio, horaFin);
        } else {
            return "✓ Rango horario DESHABILITADO (Funciona 24/7)";
        }
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
        if (horaActual.isAfter(horaFin) || horaActual.equals(horaFin)) {
            // Calcular minutos hasta 8 AM del día siguiente
            LocalDateTime proximoInicio = ahora.plusDays(1)
                .withHour(horaInicio.getHour())
                .withMinute(horaInicio.getMinute())
                .withSecond(0);

            return java.time.temporal.ChronoUnit.MINUTES.between(ahora, proximoInicio);
        }

        // Si es antes de 8 AM
        LocalDateTime proximoInicio = ahora
            .withHour(horaInicio.getHour())
            .withMinute(horaInicio.getMinute())
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
            return String.format("En horario de trabajo (hasta las %02d:%02d)", horaFin.getHour(), horaFin.getMinute());
        }

        if (horaActual.isAfter(horaFin) || horaActual.equals(horaFin)) {
            LocalDateTime proximoInicio = ahora.plusDays(1)
                .withHour(horaInicio.getHour())
                .withMinute(horaInicio.getMinute());

            return String.format("Próximo horario: %02d:%02d %s",
                horaInicio.getHour(),
                horaInicio.getMinute(),
                proximoInicio.toLocalDate());
        }

        return String.format("Próximo horario: %02d:%02d hoy",
            horaInicio.getHour(),
            horaInicio.getMinute());
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

