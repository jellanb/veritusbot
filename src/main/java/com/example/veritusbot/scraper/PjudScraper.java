package com.example.veritusbot.scraper;

import com.example.veritusbot.config.ResourceCleanupManager;
import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.model.Causa;
import com.example.veritusbot.model.Persona;
import com.example.veritusbot.model.PersonaProcesada;
import com.example.veritusbot.repository.PersonaRepository;
import com.example.veritusbot.repository.CausaRepository;
import com.example.veritusbot.repository.PersonaProcesadaRepository;
import com.example.veritusbot.service.ExcelService;
import com.example.veritusbot.util.LoggerUtil;
import com.example.veritusbot.util.SearchProgressManager;
import com.example.veritusbot.util.WorkingHoursManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class PjudScraper {

    private static final String URL = "https://oficinajudicialvirtual.pjud.cl/indexN.php";
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_THREADS = 3; // Máximo 3 ventanas simultáneas

    @Autowired
    private ExcelService excelService;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private CausaRepository causaRepository;

    @Autowired
    private ResourceCleanupManager resourceCleanupManager;

    @Autowired
    private LoggerUtil logger;

    @Autowired
    private SearchProgressManager progressManager;

    @Autowired
    private PersonaProcesadaRepository personaProcesadaRepository;

    @Autowired
    private WorkingHoursManager workingHoursManager;

    /**
     * Método principal que lee personas del Excel y busca en el sitio web
     * Solo procesa personas con procesado = 0 o procesado = 1 hace más de 6 meses
     * Respeta el rango horario de 8 AM a 8 PM
     */
    public void processClientsFromCSV(String nombreArchivo) {
        LocalDateTime startTime = LocalDateTime.now();
        String HourInitStr = startTime.format(FORMATTER_HORA);

        logger.section("INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL - " + HourInitStr);
        logger.info("⏰ Rango horario de trabajo: 08:00 - 20:00 (8 AM - 8 PM)");
        logger.info("📋 La aplicación se pausará fuera de este rango y continuará al día siguiente");

        // Leer personas del Excel
        List<PersonaDTO> person = excelService.readClientFromCSV(nombreArchivo);

        if (person.isEmpty()) {
            logger.error("No se cargaron personas del Excel");
            return;
        }

        logger.info("Personas en Excel: " + person.size());
        for (PersonaDTO persona : person) {
            logger.debug("  • " + persona);
        }

        // Guardar todas las personas en tabla personas_procesadas (sin procesar aún)
        logger.info("Sincronizando personas con tabla personas_procesadas...");
        for (PersonaDTO persona : person) {
            String[] nombres = persona.getNombres().split(" ", 2);
            String primerNombre = nombres.length > 0 ? nombres[0] : "";
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            // Verificar si ya existe
            Optional<PersonaProcesada> existente = personaProcesadaRepository
                .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                    primerNombre, segundoNombre, persona.getApellidoPaterno(), persona.getApellidoMaterno());

            if (existente.isEmpty()) {
                // Crear nueva
                PersonaProcesada nuevaPersona = new PersonaProcesada(
                    primerNombre,
                    segundoNombre,
                    persona.getApellidoPaterno(),
                    persona.getApellidoMaterno()
                );
                personaProcesadaRepository.save(nuevaPersona);
                logger.debug("  ✓ Guardada: %s %s", primerNombre, persona.getApellidoPaterno());
            } else {
                logger.debug("  ℹ️  Ya existe: %s %s", primerNombre, persona.getApellidoPaterno());
            }
        }
        logger.info("✓ Personas sincronizadas");

        // Obtener personas pendientes: procesado=0 o procesado=1 hace más de 6 meses
        logger.info("Filtrando personas pendientes...");
        List<PersonaProcesada> todasLasPersonas = personaProcesadaRepository.findAll();
        List<PersonaProcesada> personasPendientes = new ArrayList<>();

        LocalDateTime ahora = LocalDateTime.now();
        for (PersonaProcesada persona : todasLasPersonas) {
            if (!persona.getProcesado()) {
                // No procesada nunca
                personasPendientes.add(persona);
            } else if (persona.getFechaProcesada() != null) {
                // Verificar si han pasado más de 6 meses
                LocalDateTime hace6Meses = ahora.minusMonths(6);
                if (persona.getFechaProcesada().isBefore(hace6Meses)) {
                    personasPendientes.add(persona);
                }
            }
        }

        logger.info("📊 Personas pendientes de procesar: " + personasPendientes.size());
        logger.info("   (procesado=0 O procesado=1 pero más de 6 meses)");

        for (PersonaProcesada p : personasPendientes) {
            String estado = p.getProcesado() ?
                String.format("(reproceso - procesado el %s)", p.getFechaProcesada()) :
                "(primera búsqueda)";
            logger.debug("  • %s %s %s", p.getPrimerNombre(), p.getApellidoPaterno(), estado);
        }

        if (personasPendientes.isEmpty()) {
            logger.info("✅ No hay personas pendientes por procesar");
            return;
        }

        // Buscar cada persona respetando rango horario para la fase 1
        int contador = 1;
        for (PersonaProcesada personaProcesada : personasPendientes) {
            // Verificar rango horario antes de procesar
            if (!workingHoursManager.estaEnRangoHorario()) {
                logger.section("⏸️  FUERA DE HORARIO DE TRABAJO");
                logger.info("Hora actual: " + LocalDateTime.now().format(FORMATTER_HORA));
                logger.info("Rango: 08:00 - 20:00");
                logger.info("Estado: " + workingHoursManager.getProximoRangoFormateado());

                long minutos = workingHoursManager.getMinutosHastaSiguienteRango();
                logger.info("⏳ Esperando %d minutos hasta el próximo rango...", minutos);
                logger.info("   (La aplicación continuará automáticamente)");

                try {
                    workingHoursManager.esperarSiguienteRango();
                    logger.info("✅ Reanudando búsqueda desde el rango horario");
                } catch (InterruptedException e) {
                    logger.error("Error esperando siguiente rango: %s", e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            LocalDateTime horaInicioPersiana = LocalDateTime.now();

            logger.info("════════════════════════════════════════════════════════════");
            logger.info("BUSCANDO PERSONA %d/%d | Progreso: %.1f%%",
                    contador, personasPendientes.size(), (contador - 1) * 100.0 / personasPendientes.size());
            logger.info("════════════════════════════════════════════════════════════");

            // Obtener los años de búsqueda del Excel
            PersonaDTO personaExcel = obtenerPersonaDelExcel(person, personaProcesada);

            if (personaExcel != null) {
                buscarPersonaFase1(personaExcel);
            } else {
                logger.warning("⚠️  No se encontraron datos de búsqueda en Excel para: %s %s",
                    personaProcesada.getPrimerNombre(), personaProcesada.getApellidoPaterno());
            }

            LocalDateTime horaFinPersona = LocalDateTime.now();
            long segundosTranscurridos = java.time.temporal.ChronoUnit.SECONDS.between(horaInicioPersiana, horaFinPersona);
            long minutos = segundosTranscurridos / 60;
            long segundos = segundosTranscurridos % 60;

            double porcentajeAvance = (contador * 100.0) / personasPendientes.size();

            logger.info("✅ CLIENTE %d COMPLETADO", contador);
            logger.info("   Tiempo de búsqueda: %dm %ds", minutos, segundos);
            logger.info("   Progreso total: %.1f%% (%d/%d)", porcentajeAvance, contador, personasPendientes.size());

            contador++;
        }

        // Buscar cada persona respetando rango horario para la fase 2
        contador = 1;
        for (PersonaProcesada personaProcesada : personasPendientes) {
            // Verificar rango horario antes de procesar
            if (!workingHoursManager.estaEnRangoHorario()) {
                logger.section("⏸️  FUERA DE HORARIO DE TRABAJO");
                logger.info("Hora actual: " + LocalDateTime.now().format(FORMATTER_HORA));
                logger.info("Rango: 08:00 - 20:00");
                logger.info("Estado: " + workingHoursManager.getProximoRangoFormateado());

                long minutos = workingHoursManager.getMinutosHastaSiguienteRango();
                logger.info("⏳ Esperando %d minutos hasta el próximo rango...", minutos);
                logger.info("   (La aplicación continuará automáticamente)");

                try {
                    workingHoursManager.esperarSiguienteRango();
                    logger.info("✅ Reanudando búsqueda desde el rango horario");
                } catch (InterruptedException e) {
                    logger.error("Error esperando siguiente rango: %s", e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            LocalDateTime horaInicioPersiana = LocalDateTime.now();

            logger.info("════════════════════════════════════════════════════════════");
            logger.info("BUSCANDO PERSONA %d/%d | Progreso: %.1f%%",
                    contador, personasPendientes.size(), (contador - 1) * 100.0 / personasPendientes.size());
            logger.info("════════════════════════════════════════════════════════════");

            // Obtener los años de búsqueda del Excel
            PersonaDTO personaExcel = obtenerPersonaDelExcel(person, personaProcesada);

            if (personaExcel != null) {
                buscarPersonaFase2(personaExcel);
                marcarPersonaComoProcesada(personaExcel, personaProcesada.getId());
            } else {
                logger.warning("⚠️  No se encontraron datos de búsqueda en Excel para: %s %s",
                        personaProcesada.getPrimerNombre(), personaProcesada.getApellidoPaterno());
            }

            LocalDateTime horaFinPersona = LocalDateTime.now();
            long segundosTranscurridos = java.time.temporal.ChronoUnit.SECONDS.between(horaInicioPersiana, horaFinPersona);
            long minutos = segundosTranscurridos / 60;
            long segundos = segundosTranscurridos % 60;

            double porcentajeAvance = (contador * 100.0) / personasPendientes.size();

            logger.info("✅ CLIENTE %d COMPLETADO", contador);
            logger.info("   Tiempo de búsqueda: %dm %ds", minutos, segundos);
            logger.info("   Progreso total: %.1f%% (%d/%d)", porcentajeAvance, contador, personasPendientes.size());

            contador++;
        }



        LocalDateTime horaFin = LocalDateTime.now();
        String horaFinStr = horaFin.format(FORMATTER_HORA);
        long tiempoTotalSegundos = java.time.temporal.ChronoUnit.SECONDS.between(startTime, horaFin);
        long tiempoMinutos = tiempoTotalSegundos / 60;
        long tiempoSegundos = tiempoTotalSegundos % 60;

        logger.section("BÚSQUEDA COMPLETADA");
        logger.info("Hora de inicio: " + HourInitStr);
        logger.info("Hora de fin:    " + horaFinStr);
        logger.info("Tiempo total:   %dm %ds", tiempoMinutos, tiempoSegundos);
        logger.info("Total personas procesadas: " + personasPendientes.size());
        logger.info("✅ TODAS LAS PERSONAS PENDIENTES HAN SIDO PROCESADAS");
    }

    /**
     * Obtiene los datos de búsqueda (años) de una persona procesada desde el Excel
     */
    private PersonaDTO obtenerPersonaDelExcel(List<PersonaDTO> personas, PersonaProcesada personaProcesada) {
        String nombreCompleto = personaProcesada.getPrimerNombre();
        if (personaProcesada.getSegundoNombre() != null && !personaProcesada.getSegundoNombre().isEmpty()) {
            nombreCompleto += " " + personaProcesada.getSegundoNombre();
        }

        for (PersonaDTO persona : personas) {
            if (persona.getNombres().equals(nombreCompleto) &&
                persona.getApellidoPaterno().equals(personaProcesada.getApellidoPaterno()) &&
                persona.getApellidoMaterno().equals(personaProcesada.getApellidoMaterno())) {
                return persona;
            }
        }
        return null;
    }

    /**
     * Busca una persona específica usando paralelismo (3 ventanas máximo)
     * Cada ventana busca un año diferente con delay escalonado
     */
    private void buscarPersonaFase1(PersonaDTO persona) {
        logger.info("🔍 Buscando: %s %s %s", persona.getNombres(), persona.getApellidoPaterno(), persona.getApellidoMaterno());
        logger.info("   Rango de años: %d a %d", persona.getAnioInit(), persona.getAnioFin());
        logger.info("   Modo: PARALELO (máximo %d ventanas simultáneas)", MAX_THREADS);
        logger.debug("   Delay entre navegadores: 5 segundos");

        // Primera fase: Tribunales de Santiago
        logger.section("FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)");
        buscarEnTribunalesConFiltro(persona, true, false);
    }

    private void buscarPersonaFase2(PersonaDTO persona) {
        logger.info("🔍 Buscando: %s %s %s", persona.getNombres(), persona.getApellidoPaterno(), persona.getApellidoMaterno());
        logger.info("   Rango de años: %d a %d", persona.getAnioInit(), persona.getAnioFin());
        logger.info("   Modo: PARALELO (máximo %d ventanas simultáneas)", MAX_THREADS);
        logger.debug("   Delay entre navegadores: 5 segundos");

        // Segunda fase: Todos los demás tribunales
        logger.section("FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)");
        buscarEnTribunalesConFiltro(persona, false, true);
    }

    /**
     * Busca una persona en tribunales con filtro (Santiago o no-Santiago)
     * soloSantiago: true busca SOLO tribunales de Santiago (1º-30º)
     * excluirSantiago: true busca TODO EXCEPTO tribunales de Santiago
     */
    private void buscarEnTribunalesConFiltro(PersonaDTO persona, boolean soloSantiago, boolean excluirSantiago) {
        // Lista sincronizada para resultados compartidos entre threads
        List<String[]> todosLosResultados = new CopyOnWriteArrayList<>();
        todosLosResultados.add(new String[]{"Nombres", "Apellido Paterno", "Apellido Materno", "Año", "Rol", "Fecha", "Caratulado", "Tribunal"});

        // Crear ThreadPool con máximo MAX_THREADS
        java.util.concurrent.ScheduledExecutorService executor =
            java.util.concurrent.Executors.newScheduledThreadPool(MAX_THREADS);

        resourceCleanupManager.registerExecutorService(executor);

        try {
            List<Future<?>> futures = new ArrayList<>();

            int currentDelay = 0;
            for (int anio = persona.getAnioInit(); anio <= persona.getAnioFin(); anio++) {
                final int anioFinal = anio;
                final int delaySeconds = currentDelay;

                logger.debug("   📅 Año %d - Se iniciará en: %d segundos", anioFinal, delaySeconds);

                futures.add(executor.schedule(() -> {
                    logger.debug("   ▶ [THREAD %s] Procesando año: %d", Thread.currentThread().getName(), anioFinal);
                    buscarPorNombreParaleloConFiltro(
                        persona.getNombres(),
                        persona.getApellidoPaterno(),
                        persona.getApellidoMaterno(),
                        anioFinal,
                        todosLosResultados,
                        soloSantiago,
                        excluirSantiago
                    );
                }, delaySeconds, TimeUnit.SECONDS));

                currentDelay += 5;
            }

            logger.debug("⏳ Esperando a que terminen todas las búsquedas...");
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    logger.warning("Error en búsqueda paralela: %s", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error durante la búsqueda paralela: %s", e.getMessage());
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.warning("Timeout esperando termination, forzando shutdown...");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Guardar resultados si es la última fase (excluirSantiago)
        if (excluirSantiago && todosLosResultados.size() > 1) {
            String filePath = "resultados_busqueda.csv";
            guardarEnCSV(filePath, new ArrayList<>(todosLosResultados));
            guardarEnBD(persona, new ArrayList<>(todosLosResultados));

            logger.info("✓ Total de causas para este cliente: %d", todosLosResultados.size() - 1);
            logger.info("✓ CSV actualizado: %s", filePath);
            logger.info("✓ Base de datos actualizada");

            // Limpiar progreso cuando se completa exitosamente
            progressManager.clearAllProgress(persona);
        }

        // Si completó la fase Santiago, marcar tribunal_principal_procesado como true
        if (soloSantiago && !todosLosResultados.isEmpty()) {
            marcarTribunalPrincipalProcesado(persona);
            logger.info("✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true");
        }
    }

    /**
     * Versión paralela del método buscarPorNombre con filtro de tribunales
     * Múltiples threads pueden llamar esto simultáneamente, cada uno con un año diferente
     * Los resultados se agregan a una lista compartida (thread-safe)
     * soloSantiago: solo procesa tribunales cuyo nombre contiene 'Santiago'
     * excluirSantiago: procesa todo excepto tribunales que contienen 'Santiago'
     * AHORA CON REINTENTOS MEJORADOS: Valida que el navegador anterior esté cerrado
     */
    private void buscarPorNombreParaleloConFiltro(String nombres, String apellidoPaterno, String apellidoMaterno,
                                        int anio, List<String[]> resultadosCompartidos,
                                        boolean soloSantiago, boolean excluirSantiago) {

        // Reintentos: máximo 3 intentos para completar la búsqueda de un año
        int maxReintentos = 3;
        int intento = 0;
        boolean busquedaCompletada = false;
        long tiempoUltimoIntento = 0;

        while (intento < maxReintentos && !busquedaCompletada) {
            intento++;

            if (intento > 1) {
                // Validar que ha pasado suficiente tiempo desde el último intento
                long tiempoTranscurrido = System.currentTimeMillis() - tiempoUltimoIntento;
                long tiempoMinimo = 8000; // 8 segundos mínimo para que el navegador se cierre

                if (tiempoTranscurrido < tiempoMinimo) {
                    long tiempoEspera = tiempoMinimo - tiempoTranscurrido;
                    logger.warning("   [%d] ⏳ Esperando %dms para que navegador anterior se cierre completamente...", anio, tiempoEspera);
                    try {
                        Thread.sleep(tiempoEspera);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                logger.warning("   [%d] 🔄 REINTENTO %d/%d - Reabriendo navegador después de fallo...", anio, intento, maxReintentos);
            }

            tiempoUltimoIntento = System.currentTimeMillis();

            busquedaCompletada = ejecutarBusquedaPorAno(
                nombres, apellidoPaterno, apellidoMaterno, anio,
                resultadosCompartidos, soloSantiago, excluirSantiago
            );

            if (!busquedaCompletada && intento < maxReintentos) {
                logger.warning("   [%d] ⚠️  Búsqueda falló en intento %d, esperando antes de reintentar...", anio, intento);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!busquedaCompletada) {
            logger.error("   [%d] ❌ FALLO PERMANENTE - Se agotaron los %d reintentos para el año %d", anio, maxReintentos, anio);
        } else {
            logger.info("   [%d] ✅ Año %d completado exitosamente (intento: %d)", anio, anio, intento);
        }
    }

    /**
     * Ejecuta la búsqueda de un año completo
     * Retorna true si se completó exitosamente, false si falló
     */
    private boolean ejecutarBusquedaPorAno(String nombres, String apellidoPaterno, String apellidoMaterno,
                                          int anio, List<String[]> resultadosCompartidos,
                                          boolean soloSantiago, boolean excluirSantiago) {
        Browser browser = null;
        try {
            Playwright playwright = Playwright.create();

            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setTimeout(90000)  // Aumentado a 90 segundos
            );

            Page page = browser.newPage();

            try {
                page.navigate(URL,
                        new Page.NavigateOptions()
                                .setTimeout(60000)
                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                );
            } catch (PlaywrightException e) {
                logger.error("   [%d] ❌ Error navegando a la URL: %s", anio, e.getMessage());
                return false;
            }

            page.waitForTimeout(2000);

            // Cerrar popup
            boolean cerrado = false;
            for (Frame frame : page.frames()) {
                if (frame.locator("#close-modal").count() > 0) {
                    frame.evaluate("""
            const btn = document.querySelector('#close-modal');
            if (btn) btn.click();
            document.querySelectorAll('.modal, .modal-backdrop').forEach(e => e.remove());
            document.body.classList.remove('modal-open');
        """);
                    cerrado = true;
                    break;
                }
            }

            if (!cerrado) {
                page.evaluate("""
        document.querySelectorAll('.modal, .modal-backdrop').forEach(e => e.remove());
        document.body.classList.remove('modal-open');
    """);
            }

            // Buscar iframe correcto
            Frame targetFrame = null;
            for (Frame f : page.frames()) {
                if (f.locator("button:has-text('Consulta causas')").count() > 0) {
                    targetFrame = f;
                    break;
                }
            }

            if (targetFrame == null) {
                logger.debug("   ✗ [%d] No se encontró el iframe", anio);
                return false;
            }

            targetFrame.evaluate("accesoConsultaCausas()");
            page.waitForTimeout(2000);

            targetFrame.waitForSelector("a:has-text('Nombre')");
            targetFrame.locator("a:has-text('Nombre')").click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(1500);

            targetFrame.waitForSelector("select[name='nomCompetencia']");
            Locator competenciaSelect = targetFrame.locator("select[name='nomCompetencia']");
            competenciaSelect.selectOption("3");

            page.waitForTimeout(1000);

            // Llenar campos
            targetFrame.fill("input[name='nomNombre']", nombres);
            targetFrame.fill("input[name='nomApePaterno']", apellidoPaterno);
            targetFrame.fill("input[name='nomApeMaterno']", apellidoMaterno);
            targetFrame.fill("input[id='nomEra']", String.valueOf(anio));

            logger.debug("   [%d] ✓ Formulario completado para año: %d", anio, anio);

            // Cargar TODOS los tribunales del dropdown con sus índices
            java.util.Map<String, Integer> todosTribunales = cargarTodosTribunales(targetFrame);

            if (todosTribunales.isEmpty()) {
                logger.debug("   [%d] ⚠ No se cargaron tribunales", anio);
                return false;
            }

            logger.debug("   [%d] ✓ Tribunales cargados: %d", anio, todosTribunales.size());

            // Obtener lista de tribunales a buscar según el filtro
            List<String> tribunalesABuscar = obtenerTribunalesABuscar(todosTribunales, soloSantiago, excluirSantiago);

            if (tribunalesABuscar.isEmpty()) {
                logger.debug("   [%d] ⚠ No se encontraron tribunales con el filtro especificado", anio);
                return false;
            }

            logger.debug("   [%d] ✓ Tribunales a buscar (filtrados): %d", anio, tribunalesABuscar.size());

            // Buscar en cada tribunal de la lista
            for (int i = 0; i < tribunalesABuscar.size(); i++) {
                String nombreTribunal = tribunalesABuscar.get(i);
                try {
                    // Obtener el índice del tribunal desde el mapa
                    Integer indexTribunal = todosTribunales.get(nombreTribunal);
                    if (indexTribunal == null) {
                        logger.debug("   [%d] ⚠ No se encontró índice para tribunal: %s", anio, nombreTribunal);
                        continue;
                    }

                    logger.debug("   [%d] 🔍 Buscando en: %s (índice: %d) [%d/%d]", anio, nombreTribunal, indexTribunal, i + 1, tribunalesABuscar.size());

                    // Abrir dropdown y esperar a que esté completamente cargado
                    logger.debug("   [%d]    📂 Abriendo dropdown...", anio);
                    abrirDropdownTribunales(targetFrame);

                    // Esperar a que el elemento sea visible
                    logger.debug("   [%d]    ⏳ Esperando elemento visible...", anio);
                    try {
                        String selector = String.format("li[data-original-index=\"%d\"] a", indexTribunal);
                        targetFrame.locator(selector).first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
                        logger.debug("   [%d]    ✓ Elemento visible", anio);
                    } catch (PlaywrightException e) {
                        logger.warning("   [%d]    ⚠ Elemento no visible: %s (saltando)", anio, e.getMessage());
                        continue;
                    }

                    // Esperar a que se estabilice (3 segundos)
                    Thread.sleep(3000);

                    // Seleccionar tribunal por su índice
                    logger.debug("   [%d]    🎯 Haciendo click en tribunal...", anio);
                    boolean seleccionado = seleccionarTribunalPorIndice(targetFrame, indexTribunal);

                    if (!seleccionado) {
                        logger.debug("   [%d]    ⚠ No se pudo seleccionar tribunal: %s, saltando...", anio, nombreTribunal);
                        continue;
                    }

                    logger.debug("   [%d]    ✓ Tribunal seleccionado correctamente", anio);

                    // Esperar a que el dropdown se cierre después de la selección
                    Thread.sleep(2000);

                    page.waitForTimeout(1000);

                    // Hacer click en botón de búsqueda (con reintentos si no está habilitado)
                    try {
                        boolean clickExitoso = hacerClickEnBusqueda(targetFrame, anio);
                        if (!clickExitoso) {
                            // El botón no se habilitó, es problema del formulario o tribunal
                            logger.warning("   [%d]    ⚠️  No se pudo habilitar botón para: %s (saltando tribunal)", anio, nombreTribunal);
                            continue; // Pasar al siguiente tribunal
                        }
                        logger.debug("   [%d]    ✓ Búsqueda iniciada correctamente", anio);
                    } catch (Exception e) {
                        logger.error("   [%d]    ❌ Error al ejecutar búsqueda: %s", anio, e.getMessage());
                        return false;
                    }

                    // ESTRATEGIA SIMPLE: Esperar 10 segundos fijos
                    logger.debug("   [%d]    ⏳ Esperando 10 segundos para que complete la búsqueda...", anio);
                    page.waitForTimeout(15000);

                    logger.debug("   [%d]    📋 Procesando resultados...", anio);

                    // Obtener el HTML y verificar si hay resultados
                    String html = page.content();
                    Document doc = Jsoup.parse(html);

                    // Verificar si es un mensaje de "No se han encontrado resultados"
                    Element mensajeSinResultados = doc.selectFirst("tbody#verDetalleNombre tr td[colspan='5']");

                    if (mensajeSinResultados != null && mensajeSinResultados.text().contains("No se han encontrado resultados")) {
                        logger.debug("   [%d]    ℹ️  Sin resultados para: %s", anio, nombreTribunal);
                    } else {
                        // Hay resultados, procesarlos
                        Elements rows = doc.select("table#dtaTableDetalleNombre tbody#verDetalleNombre tr");

                        for (Element row : rows) {
                            // Saltarse filas con colspan (mensajes de sin resultados)
                            if (!row.select("td[colspan]").isEmpty()) {
                                continue;
                            }

                            Elements cols = row.select("td");
                            if (cols.size() >= 5) {
                                String rolValue = cols.get(1).text().trim();
                                String fechaValue = cols.get(2).text().trim();
                                String caratuladoValue = cols.get(3).text().trim();
                                String tribunalValue = cols.get(4).text().trim();

                                String[] resultado = new String[]{
                                    nombres, apellidoPaterno, apellidoMaterno, String.valueOf(anio),
                                    rolValue, fechaValue, caratuladoValue, tribunalValue
                                };

                                // ✅ GUARDAR INMEDIATAMENTE en CSV y BD
                                String filePath = "resultados_busqueda.csv";
                                guardarResultadoEnCSVInmediato(filePath, resultado);
                                guardarResultadoEnBDInmediato(nombres, apellidoPaterno, apellidoMaterno, resultado);

                                // Agregar a lista compartida también (para compatibilidad)
                                resultadosCompartidos.add(resultado);

                                logger.info("   [%d]    ✅ Causa guardada INMEDIATAMENTE: %s en %s", anio, rolValue, tribunalValue);
                            }
                        }

                        if (!rows.isEmpty()) {
                            // Contar solo las filas que tienen datos (sin colspan)
                            long rowsConDatos = rows.stream()
                                .filter(row -> row.select("td[colspan]").isEmpty())
                                .count();

                            if (rowsConDatos > 0) {
                                logger.debug("   [%d]    ✓ %d causas encontradas en %s", anio, rowsConDatos, nombreTribunal);
                            }
                        }
                    }

                    // Si no es el último tribunal, reabre el dropdown para la siguiente iteración
                    if (i < tribunalesABuscar.size() - 1) {
                        logger.debug("   [%d]    ⏳ Preparando siguiente tribunal...", anio);
                        Thread.sleep(800);
                    }

                } catch (Exception e) {
                    logger.debug("   [%d] Error en tribunal %s: %s", anio, nombreTribunal, e.getMessage());
                    // Error en tribunal específico, pero intentamos continuar con el siguiente
                }

                page.waitForTimeout(200);
            }

            // Cerrar el dropdown SOLO al final, después de procesar todos los tribunales
            cerrarDropdownAlFinal(targetFrame);

            logger.debug("   [%d] ✓ Búsqueda completada para año %d", anio, anio);

            // Cerrar navegador correctamente y esperar a que se cierre
            cerrarNavegadorConValidacion(browser, anio);

            // Búsqueda completada exitosamente
            return true;

        } catch (PlaywrightException e) {
            logger.error("   [%d] ❌ ERROR PLAYWRIGHT (Navegador cerrado): %s", anio, e.getMessage());
            logger.error("   [%d]    Este error indica que el navegador se cerró inesperadamente", anio);
            logger.error("   [%d]    Causas posibles: timeout, crash del navegador, o fallo de conexión", anio);

            // Cerrar navegador si aún está abierto
            cerrarNavegadorConValidacion(browser, anio);

            return false;
        } catch (Exception e) {
            logger.error("   [%d] ❌ ERROR: %s", anio, e.getMessage());
            logger.error("   [%d]    Tipo: %s", anio, e.getClass().getName());

            // Cerrar navegador si aún está abierto
            cerrarNavegadorConValidacion(browser, anio);

            return false;
        }
    }

    /**
     * Cierra el navegador y espera a que se cierre completamente
     */
    private void cerrarNavegadorConValidacion(Browser browser, int anio) {
        if (browser == null) {
            return;
        }

        try {
            logger.debug("   [%d] 🔴 Iniciando cierre de navegador...", anio);
            browser.close();
            logger.debug("   [%d] ✓ Navegador cerrado correctamente", anio);

            // Esperar adicional para asegurar que se haya cerrado completamente
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.debug("   [%d] ⚠ Error al cerrar navegador (podría estar ya cerrado): %s", anio, e.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Espera a que el botón de búsqueda esté habilitado y hace click
     */
    private boolean hacerClickEnBusqueda(Frame targetFrame, int anio) {
        int maxIntentos = 3;
        int intento = 0;

        while (intento < maxIntentos) {
            intento++;

            try {
                logger.debug("   [%d]    💬 Intentando hacer click en búsqueda (intento %d/%d)...", anio, intento, maxIntentos);

                // Esperar a que el botón exista y sea visible
                Locator botonBusqueda = targetFrame.locator("#btnConConsultaNom");

                if (botonBusqueda.count() == 0) {
                    logger.warning("   [%d]    ⚠️  Botón de búsqueda no encontrado en DOM", anio);
                    return false;
                }

                // Esperar a que sea clickeable (con timeout más largo)
                logger.debug("   [%d]    ⏳ Esperando que botón sea clickeable...", anio);
                botonBusqueda.first().waitFor(new Locator.WaitForOptions().setTimeout(8000));

                // Esperar pequeño delay para que formulario esté listo
                Thread.sleep(500);

                // Hacer click
                logger.debug("   [%d]    ➜ Haciendo click en botón de búsqueda...", anio);
                botonBusqueda.first().click(new Locator.ClickOptions().setTimeout(5000));

                logger.debug("   [%d]    ✓ Click en búsqueda ejecutado correctamente", anio);
                Thread.sleep(1000);
                return true;

            } catch (PlaywrightException e) {
                logger.warning("   [%d]    ⚠️  Error en intento %d: %s", anio, intento, e.getMessage());

                if (intento < maxIntentos) {
                    try {
                        Thread.sleep(2000); // Esperar antes de reintentar
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    logger.error("   [%d]    ❌ No se pudo hacer click después de %d intentos", anio, maxIntentos);
                    return false;
                }

            } catch (Exception e) {
                logger.error("   [%d]    ❌ Error inesperado: %s", anio, e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * Selecciona un tribunal por su índice directo en el dropdown
     * El dropdown ya está abierto, solo hace click en el elemento
     */
    private boolean seleccionarTribunalPorIndice(Frame targetFrame, int index) {
        try {
            logger.debug("    🎯 Seleccionando tribunal con índice: %d", index);

            String selector = String.format("li[data-original-index=\"%d\"] a", index);
            Locator elemento = targetFrame.locator(selector);

            if (elemento.count() == 0) {
                logger.debug("    ❌ Elemento no encontrado con selector: %s", selector);
                return false;
            }

            try {
                // Esperar a que el elemento sea visible y clickeable
                logger.debug("    💬 Esperando a que elemento sea clickeable...");
                elemento.first().waitFor(new Locator.WaitForOptions().setTimeout(8000));

                // Scroll hacia el elemento si es necesario
                logger.debug("    📍 Scrolleando a elemento si es necesario...");
                elemento.first().scrollIntoViewIfNeeded();

                // Pequeña pausa antes de click
                Thread.sleep(300);

                logger.debug("    ➜ Haciendo click en el elemento...");
                elemento.first().click(new Locator.ClickOptions().setTimeout(5000));
                logger.debug("    ✓ Click ejecutado en índice %d", index);
                Thread.sleep(800);
                return true;
            } catch (PlaywrightException e) {
                logger.error("    ❌ Error en click: %s", e.getMessage());
                return false;
            }

        } catch (Exception e) {
            logger.error("    ❌ Error seleccionando tribunal: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Espera a que el dropdown sea visible y sus elementos estén renderizados
     */
    private void esperarDropdownVisible(Frame frame, int timeoutMs) throws PlaywrightException {
        try {
            logger.debug("   ⏳ Esperando dropdown visible (" + timeoutMs + "ms)...");
            frame.waitForSelector("ul.dropdown-menu.inner", new Frame.WaitForSelectorOptions()
                .setTimeout(timeoutMs));

            // Espera adicional: asegurar que elementos están clickeables
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.debug("   ✓ Dropdown y elementos listos");
        } catch (PlaywrightException e) {
            logger.warning("⚠ Timeout esperando dropdown: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Abre el dropdown con reintentos y backoff exponencial
     */
    private void abrirDropdownConReintento(Frame frame) throws PlaywrightException {
        int maxReintentos = 3;
        int delayBase = 800;

        for (int intento = 0; intento < maxReintentos; intento++) {
            try {
                logger.debug("   📂 Abriendo dropdown (intento " + (intento + 1) + "/" + maxReintentos + ")...");

                // Cerrar dropdown anterior
                try {
                    frame.evaluate("() => { document.activeElement.blur(); }");
                    Thread.sleep(300);
                } catch (Exception e) {
                    // Sin dropdown anterior
                }

                // Buscar botón del dropdown
                Locator selectButton = frame.locator("button[data-toggle='dropdown'][aria-haspopup='listbox']");
                if (selectButton.count() == 0) {
                    selectButton = frame.locator("button.dropdown-toggle");
                }
                if (selectButton.count() == 0) {
                    selectButton = frame.locator("button:has-text('Seleccione')");
                }

                if (selectButton.count() == 0) {
                    throw new PlaywrightException("No se encontró botón del dropdown");
                }

                // Esperar y hacer click
                selectButton.first().waitFor(new Locator.WaitForOptions().setTimeout(3000));
                selectButton.first().click(new Locator.ClickOptions().setTimeout(3000));
                logger.debug("   ✓ Botón presionado");

                // Esperar dropdown visible
                esperarDropdownVisible(frame, 8000);

                logger.debug("✓ Dropdown abierto correctamente");
                return; // ✓ Éxito

            } catch (PlaywrightException e) {
                if (intento < maxReintentos - 1) {
                    long delay = delayBase * (intento + 1); // Backoff exponencial
                    logger.info("🔄 Reintentando apertura dropdown... (espera " + delay + "ms)");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PlaywrightException("Interrumpido esperando reintento");
                    }
                } else {
                    logger.error("❌ No se pudo abrir dropdown después de " + maxReintentos + " intentos");
                    throw e;
                }
            }
        }
    }

    private void abrirDropdownTribunales(Frame targetFrame) {
        try {
            logger.debug("📂 Abriendo dropdown de tribunales...");
            abrirDropdownConReintento(targetFrame);
        } catch (PlaywrightException e) {
            logger.warning("⚠ Error abriendo dropdown: %s", e.getMessage());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Guarda un resultado individual inmediatamente en CSV sin esperar al final
     * Agrega una fila al archivo existente sin duplicar encabezado
     */
    private synchronized void guardarResultadoEnCSVInmediato(String filePath, String[] resultado) {
        try {
            java.io.File file = new java.io.File(filePath);

            // Si no existe el archivo, crear con encabezado
            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write("Nombres,Apellido Paterno,Apellido Materno,Año,Rol,Fecha,Caratulado,Tribunal\n");
                }
            }

            // Agregar la fila al final del archivo
            try (FileWriter writer = new FileWriter(filePath, true)) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < resultado.length; i++) {
                    String field = resultado[i].replace("\"", "\"\"");
                    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                        line.append("\"").append(field).append("\"");
                    } else {
                        line.append(field);
                    }
                    if (i < resultado.length - 1) {
                        line.append(",");
                    }
                }
                writer.write(line.toString());
                writer.write("\n");
                writer.flush();
            }
        } catch (Exception e) {
            logger.error("❌ Error guardando resultado en CSV: %s", e.getMessage());
        }
    }

    /**
     * Guarda un resultado individual inmediatamente en BD sin esperar al final
     * Versión sobrecargada que acepta parámetros de string
     */
    private synchronized void guardarResultadoEnBDInmediato(String nombres, String apellidoPaterno,
                                                            String apellidoMaterno, String[] resultado) {
        try {
            // resultado: [0] Nombres, [1] Apellido Paterno, [2] Apellido Materno, [3] Año,
            // [4] Rol, [5] Fecha, [6] Caratulado, [7] Tribunal

            String[] nombresArray = nombres.split(" ", 2);
            String primerNombre = nombresArray.length > 0 ? nombresArray[0] : "";
            String segundoNombre = nombresArray.length > 1 ? nombresArray[1] : "";

            // Buscar o crear persona
            Optional<Persona> personaExistente = personaRepository.findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                primerNombre,
                segundoNombre,
                apellidoPaterno,
                apellidoMaterno
            );

            Persona personaGuardada;
            if (personaExistente.isPresent()) {
                personaGuardada = personaExistente.get();
            } else {
                Persona personaNueva = new Persona(
                    primerNombre,
                    segundoNombre,
                    apellidoPaterno,
                    apellidoMaterno
                );
                personaGuardada = personaRepository.save(personaNueva);
            }

            // Guardar causa
            Integer anio = Integer.parseInt(resultado[3]);
            String rol = resultado[4];
            String caratula = resultado[6];
            String tribunal = resultado[7];

            Causa causa = new Causa(
                personaGuardada.getId(),
                rol,
                anio,
                caratula,
                tribunal
            );

            causaRepository.save(causa);
            logger.debug("   ✓ Causa guardada inmediatamente: %s (%s)", rol, tribunal);

        } catch (Exception e) {
            logger.error("❌ Error guardando resultado en BD: %s", e.getMessage());
        }
    }

    /**
     * Guarda un resultado individual inmediatamente en BD sin esperar al final
     * Versión original que acepta PersonaDTO
     */
    private synchronized void guardarResultadoEnBDInmediato(PersonaDTO persona, String[] resultado) {
        guardarResultadoEnBDInmediato(
            persona.getNombres(),
            persona.getApellidoPaterno(),
            persona.getApellidoMaterno(),
            resultado
        );
    }

    /**
     * Guarda los datos en un archivo CSV en la raíz del proyecto
     * Si el archivo existe, acumula los datos (sin duplicar encabezado)
     * @param filePath ruta del archivo
     * @param data lista de arrays con los datos
     */
    private void guardarEnCSV(String filePath, List<String[]> data) {
        try {
            java.io.File file = new java.io.File(filePath);
            List<String[]> datosFinales = new ArrayList<>();

            // Si el archivo existe, leer datos existentes (menos el encabezado)
            if (file.exists()) {
                try (var stream = java.nio.file.Files.lines(java.nio.file.Paths.get(filePath))) {
                    stream.skip(1) // Saltar encabezado
                        .forEach(line -> {
                            // Parsear CSV manteniendo comillas
                            List<String> fields = new ArrayList<>();
                            StringBuilder field = new StringBuilder();
                            boolean inQuotes = false;

                            for (char c : line.toCharArray()) {
                                if (c == '"') {
                                    inQuotes = !inQuotes;
                                } else if (c == ',' && !inQuotes) {
                                    fields.add(field.toString().replace("\"\"", "\""));
                                    field = new StringBuilder();
                                } else {
                                    field.append(c);
                                }
                            }
                            fields.add(field.toString().replace("\"\"", "\""));

                            if (fields.stream().anyMatch(f -> !f.isEmpty())) {
                                datosFinales.add(fields.toArray(new String[0]));
                            }
                        });
                }
            }

            // Agregar el encabezado al inicio
            datosFinales.add(0, new String[]{"Nombres", "Apellido Paterno", "Apellido Materno", "Año", "Rol", "Fecha", "Caratulado", "Tribunal"});

            // Agregar nuevos datos (saltando el encabezado del nuevo data)
            for (int i = 1; i < data.size(); i++) {
                datosFinales.add(data.get(i));
            }

            // Escribir todo al archivo
            try (FileWriter writer = new FileWriter(filePath)) {
                for (String[] row : datosFinales) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        // Escapar comillas y envolver en comillas si contiene comas
                        String field = row[i].replace("\"", "\"\"");
                        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                            line.append("\"").append(field).append("\"");
                        } else {
                            line.append(field);
                        }
                        if (i < row.length - 1) {
                            line.append(",");
                        }
                    }
                    writer.write(line.toString());
                    writer.write("\n");
                }
                logger.info("Archivo CSV actualizado correctamente: " + filePath);
            }
        } catch (Exception e) {
            logger.error("Error al guardar el archivo CSV: %s", e.getMessage());
        }
    }

    /**
     * Guarda los datos en SQL Server (tablas personas y causas)
     * @param persona datos de la persona desde Excel
     * @param data lista de arrays con los datos de causas (encabezado + filas)
     */
    private void guardarEnBD(PersonaDTO persona, List<String[]> data) {
        try {
            logger.info("📊 Guardando en SQL Server...");

            // Separar el nombre en primer nombre y segundo nombre
            String[] nombres = persona.getNombres().split(" ", 2);
            String primerNombre = nombres.length > 0 ? nombres[0] : "";
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            // Buscar o crear persona
            Optional<Persona> personaExistente = personaRepository.findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                primerNombre,
                segundoNombre,
                persona.getApellidoPaterno(),
                persona.getApellidoMaterno()
            );

            Persona personaGuardada;
            if (personaExistente.isPresent()) {
                personaGuardada = personaExistente.get();
                logger.info("   ℹ️  Persona existente encontrada, ID: " + personaGuardada.getId());
            } else {
                Persona personaNueva = new Persona(
                    primerNombre,
                    segundoNombre,
                    persona.getApellidoPaterno(),
                    persona.getApellidoMaterno()
                );
                personaGuardada = personaRepository.save(personaNueva);
                logger.info("   ✓ Persona guardada con ID: " + personaGuardada.getId());
            }

            // Guardar causas asociadas a la persona
            int causasGuardadas = 0;
            for (int i = 1; i < data.size(); i++) { // Saltar encabezado (índice 0)
                String[] row = data.get(i);
                if (row.length >= 8) {
                    try {
                        // Mapear datos desde CSV
                        // [0] Nombres, [1] Apellido Paterno, [2] Apellido Materno, [3] Año,
                        // [4] Rol, [5] Fecha, [6] Caratulado, [7] Tribunal

                        Integer anio = Integer.parseInt(row[3]);
                        String rol = row[4];
                        String caratula = row[6]; // Caratulado
                        String tribunal = row[7];

                        Causa causa = new Causa(
                            personaGuardada.getId(),
                            rol,
                            anio,
                            caratula,
                            tribunal
                        );

                        causaRepository.save(causa);
                        causasGuardadas++;

                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        logger.warning("   ⚠️  Error al procesar fila de causa: %s", e.getMessage());
                    }
                }
            }

            logger.info("   ✓ " + causasGuardadas + " causas guardadas en BD");

        } catch (Exception e) {
            logger.error("Error al guardar en SQL Server: %s", e.getMessage());
        }
    }

    /**
     * Marca una persona como procesada con la fecha y hora actual
     * Versión con ID directo
     */
    private void marcarPersonaComoProcesada(PersonaDTO persona, Integer personaProcesadaId) {
        try {
            Optional<PersonaProcesada> personaOptional = personaProcesadaRepository.findById(personaProcesadaId);

            if (personaOptional.isPresent()) {
                PersonaProcesada personaProcesada = personaOptional.get();
                personaProcesada.setProcesado(true);
                personaProcesada.setFechaProcesada(LocalDateTime.now());
                personaProcesadaRepository.save(personaProcesada);

                logger.info("   ✓ Marcada como procesada: %s %s", persona.getNombres(), persona.getApellidoPaterno());
            } else {
                logger.warning("   ⚠️  No se encontró persona con ID %d para marcar como procesada", personaProcesadaId);
            }
        } catch (Exception e) {
            logger.error("Error al marcar persona como procesada: %s", e.getMessage());
        }
    }

    /**
     * Carga todos los tribunales del dropdown y retorna un mapa con nombre -> índice
     */
    private java.util.Map<String, Integer> cargarTodosTribunales(Frame targetFrame) {
        java.util.Map<String, Integer> tribunales = new java.util.LinkedHashMap<>();
        try {
            Object resultado = targetFrame.evaluate("""
                () => {
                    const items = document.querySelectorAll('ul.dropdown-menu.inner li[data-original-index]');
                    const result = {};
                    items.forEach(item => {
                        const index = parseInt(item.getAttribute('data-original-index'));
                        const text = item.querySelector('span.text')?.textContent?.trim() || '';
                        if (text) {
                            result[text] = index;
                        }
                    });
                    return result;
                }
            """);

            if (resultado instanceof java.util.Map) {
                tribunales.putAll((java.util.Map<String, Integer>) resultado);
            }
        } catch (Exception e) {
            logger.error("Error cargando tribunales: %s", e.getMessage());
        }
        return tribunales;
    }

    /**
     * Obtiene la lista de tribunales a buscar según el filtro
     */
    private java.util.List<String> obtenerTribunalesABuscar(java.util.Map<String, Integer> todosTribunales,
                                                           boolean soloSantiago, boolean excluirSantiago) {
        java.util.List<String> resultado = new java.util.ArrayList<>();

        for (String nombre : todosTribunales.keySet()) {
            boolean esSantiago = nombre.contains("Santiago");

            if (soloSantiago && esSantiago) {
                resultado.add(nombre);
            } else if (excluirSantiago && !esSantiago) {
                resultado.add(nombre);
            }
        }

        return resultado;
    }

    /**
     * Cierra el dropdown al final del procesamiento
     */
    private void cerrarDropdownAlFinal(Frame targetFrame) {
        try {
            logger.debug("📂 Cerrando dropdown al final del procesamiento...");
            targetFrame.evaluate("""
                (function() {
                    const event = new KeyboardEvent('keydown', {
                        key: 'Escape',
                        code: 'Escape',
                        bubbles: true,
                        cancelable: true
                    });
                    document.dispatchEvent(event);
                    const dropdown = document.querySelector('.dropdown-menu.open');
                    if (dropdown) {
                        dropdown.classList.remove('open');
                    }
                })()
            """);
            Thread.sleep(500);
            logger.debug("✓ Dropdown cerrado correctamente");
        } catch (Exception e) {
            logger.debug("⚠ Error al cerrar dropdown final: %s", e.getMessage());
        }
    }

    /**
     * Marca tribunal_principal_procesado como true después de completar la búsqueda en tribunales de Santiago
     */
    private void marcarTribunalPrincipalProcesado(PersonaDTO persona) {
        try {
            String[] nombres = persona.getNombres().split(" ", 2);
            String primerNombre = nombres.length > 0 ? nombres[0] : "";
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            java.util.Optional<PersonaProcesada> personaOptional = personaProcesadaRepository
                .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                    primerNombre, segundoNombre, persona.getApellidoPaterno(), persona.getApellidoMaterno());

            if (personaOptional.isPresent()) {
                PersonaProcesada personaProcesada = personaOptional.get();
                personaProcesada.setTribunalPrincipalProcesado(true);
                personaProcesadaRepository.save(personaProcesada);

                logger.debug("✓ Marcada tribunal_principal_procesado = true para: %s %s",
                    primerNombre, persona.getApellidoPaterno());
            } else {
                logger.warning("⚠️  No se encontró persona para marcar tribunal_principal_procesado: %s %s",
                    primerNombre, persona.getApellidoPaterno());
            }
        } catch (Exception e) {
            logger.error("Error al marcar tribunal_principal_procesado: %s", e.getMessage());
        }
    }

}
