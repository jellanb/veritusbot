package com.example.veritusbot.scraper;

import com.example.veritusbot.dto.CausaDTO;
import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.ExcelService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PjudScraper {

    private static final String URL = "https://oficinajudicialvirtual.pjud.cl/indexN.php";
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_THREADS = 3; // Máximo 3 ventanas simultáneas

    @Autowired
    private ExcelService excelService;

    // ...existing code...

    /**
     * Método principal que lee personas del Excel y busca en el sitio web
     */
    public void buscarPersonasDelExcel(String nombreArchivo) {
        LocalDateTime horaInicio = LocalDateTime.now();
        String horaInicioStr = horaInicio.format(FORMATTER_HORA);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL                ║");
        System.out.println("║  Hora de inicio: " + horaInicioStr + "                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Leer personas del Excel
        List<PersonaDTO> personas = excelService.leerPersonasDelExcel(nombreArchivo);

        if (personas.isEmpty()) {
            System.err.println("❌ No se cargaron personas del Excel");
            return;
        }

        System.out.println("\n📋 Personas a buscar: " + personas.size());
        for (PersonaDTO persona : personas) {
            System.out.println("  • " + persona);
        }

        // Buscar cada persona
        int contador = 1;
        for (PersonaDTO persona : personas) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("BUSCANDO PERSONA " + contador + "/" + personas.size());
            System.out.println("=".repeat(70));

            buscarPersona(persona);
            contador++;
        }

        LocalDateTime horaFin = LocalDateTime.now();
        String horaFinStr = horaFin.format(FORMATTER_HORA);

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  BÚSQUEDA COMPLETADA                                       ║");
        System.out.println("║  Hora de inicio: " + horaInicioStr + "                               ║");
        System.out.println("║  Hora de fin:    " + horaFinStr + "                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Busca una persona específica usando paralelismo (3 ventanas máximo)
     * Cada ventana busca un año diferente simultáneamente
     */
    private void buscarPersona(PersonaDTO persona) {
        System.out.println("\n🔍 Buscando: " + persona.getNombres() + " " + persona.getApellidoPaterno()
            + " " + persona.getApellidoMaterno());
        System.out.println("   Rango de años: " + persona.getAnioInit() + " a " + persona.getAnioFin());
        System.out.println("   Modo: PARALELO (máximo " + MAX_THREADS + " ventanas simultáneas)\n");

        // Lista sincronizada para resultados compartidos entre threads
        List<String[]> todosLosResultados = new CopyOnWriteArrayList<>();
        todosLosResultados.add(new String[]{"Nombres", "Apellido Paterno", "Apellido Materno", "Año", "Rol", "Fecha", "Caratulado", "Tribunal"});

        // Crear ThreadPool con máximo MAX_THREADS
        @SuppressWarnings("resource")
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();

            // Crear una tarea por cada año en el rango
            for (int anio = persona.getAnioInit(); anio <= persona.getAnioFin(); anio++) {
                final int anioFinal = anio;

                // Enviar tarea al executor
                futures.add(executor.submit(() -> {
                    System.out.println("\n   ▶ [THREAD " + Thread.currentThread().getName() + "] Procesando año: " + anioFinal);
                    buscarPorNombreParalelo(
                        persona.getNombres(),
                        persona.getApellidoPaterno(),
                        persona.getApellidoMaterno(),
                        anioFinal,
                        todosLosResultados
                    );
                }));
            }

            // Esperar a que terminen todas las búsquedas
            System.out.println("\n⏳ Esperando a que terminen todas las búsquedas...");
            for (Future<?> future : futures) {
                try {
                    future.get(); // Esperar a que termine
                } catch (Exception e) {
                    System.err.println("Error en búsqueda paralela: " + e.getMessage());
                }
            }
        } finally {
            // Shutdown del executor
            executor.shutdown();
        }

        // Guardar resultados acumulados
        if (todosLosResultados.size() > 1) {
            String filePath = "resultados_busqueda.csv";
            guardarEnCSV(filePath, new ArrayList<>(todosLosResultados));
            System.out.println("\n✓ Total de causas guardadas: " + (todosLosResultados.size() - 1));
            System.out.println("✓ Datos guardados en: " + filePath);
        } else {
            System.out.println("\n✗ No se encontraron resultados en ningún tribunal");
        }
    }

    /**
     * Método original que busca por nombre (versión secuencial)
     */
    private void buscarPorNombreSecuencial(String nombres, String apellidoPaterno, String apellidoMaterno, int anio) {

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false) // cambiar a true después
                            .setTimeout(60000)
            );

            Page page = browser.newPage();

            page.navigate(URL,
                    new Page.NavigateOptions()
                            .setTimeout(60000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            page.waitForTimeout(5000);

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
                    System.out.println("Popup cerrado");
                    break;
                }
            }

            if (!cerrado) {
                page.evaluate("""
        document.querySelectorAll('.modal, .modal-backdrop').forEach(e => e.remove());
        document.body.classList.remove('modal-open');
    """);
            }

            // ⚠️ aquí debes inspeccionar los selectores reales con F12
            // buscar iframe correcto
            Frame targetFrame = null;
            for (Frame f : page.frames()) {
                if (f.locator("button:has-text('Consulta causas')").count() > 0) {
                    targetFrame = f;
                    break;
                }
            }

            if (targetFrame == null) {
                throw new RuntimeException("No se encontró el iframe con Consulta causas");
            }

// ejecutar JS directo
            targetFrame.evaluate("accesoConsultaCausas()");
            page.waitForTimeout(5000);

            // 6. Click en pestaña "Búsqueda por Nombre"
            targetFrame.waitForSelector("a:has-text('Nombre')");
            targetFrame.locator("a:has-text('Nombre')")
                    .click(new Locator.ClickOptions().setForce(true));

            page.waitForTimeout(3000);

            // 7. Verificación
            if (targetFrame.locator("input").count() > 0) {
                System.out.println("Formulario de búsqueda por nombre cargado correctamente");
            } else {
                System.out.println("No se detectó formulario");
            }

            // 8. Esperar a que el select de competencia esté disponible
            targetFrame.waitForSelector("select[name='nomCompetencia']");
            System.out.println("Select de competencia encontrado");

            page.waitForTimeout(1000);

            // Seleccionar "civil" en el campo "competencia"
            Locator competenciaSelect = targetFrame.locator("select[name='nomCompetencia']");
            competenciaSelect.selectOption("3");
            System.out.println("Competencia 'civil' seleccionada");

            page.waitForTimeout(2000);

            // 9. Rellenar los campos de búsqueda por nombre
            targetFrame.fill("input[name='nomNombre']", nombres);
            System.out.println("Nombre '" + nombres + "' ingresado");

            targetFrame.fill("input[name='nomApePaterno']", apellidoPaterno);
            System.out.println("Apellido paterno '" + apellidoPaterno + "' ingresado");

            targetFrame.fill("input[name='nomApeMaterno']", apellidoMaterno);
            System.out.println("Apellido materno '" + apellidoMaterno + "' ingresado");

            targetFrame.fill("input[id='nomEra']", String.valueOf(anio));
            System.out.println("Año '" + anio + "' ingresado");

            // Archivo CSV para acumular todos los resultados
            List<String[]> todosLosResultados = new ArrayList<>();
            todosLosResultados.add(new String[]{"Nombres", "Apellido Paterno", "Apellido Materno", "Año", "Rol", "Fecha", "Caratulado", "Tribunal"});

            // Abrir el dropdown UNA SOLA VEZ al inicio
            System.out.println("\n🔍 Abriendo dropdown de tribunales...");
            abrirDropdownTribunales(targetFrame);
            page.waitForTimeout(2000);

            // Obtener el máximo data-original-index disponible
            System.out.println("🔍 Buscando máximo índice...");
            Object maxIndexObj = targetFrame.evaluate("""
                (function() {
                    // Buscar TODOS los li con data-original-index en el DOM
                    const allLi = document.querySelectorAll('[data-original-index]');
                    console.log('Total elementos con data-original-index:', allLi.length);

                    let maxIndex = 0;
                    let indices = [];

                    for (let li of allLi) {
                        const indexStr = li.getAttribute('data-original-index');
                        if (indexStr) {
                            const index = parseInt(indexStr, 10);
                            indices.push(index);
                            if (index > maxIndex) {
                                maxIndex = index;
                            }
                        }
                    }

                    console.log('Índices encontrados:', indices.slice(0, 10).join(', '), '...');
                    console.log('Máximo índice:', maxIndex);

                    return maxIndex;
                })()
            """);

            int maxIndex = 0;
            if (maxIndexObj instanceof Number) {
                maxIndex = ((Number) maxIndexObj).intValue();
            }

            System.out.println("✓ Se encontraron tribunales con índices de 1 a " + maxIndex);

            if (maxIndex == 0) {
                System.err.println("❌ ERROR: No se pudo obtener el máximo índice.");
                browser.close();
                return;
            }

            // ✅ NUEVA ESTRATEGIA: Obtener TODOS los nombres de tribunales AHORA mientras el dropdown está abierto
            System.out.println("\n🔍 ════════════════════════════════════════════");
            System.out.println("🔍 INICIANDO OBTENCIÓN DE LISTA DE TRIBUNALES");
            System.out.println("🔍 ════════════════════════════════════════════");

            Map<Integer, String> tribunalesPorIndice = new HashMap<>();

            // ✅ STEP 1: Ejecutar JavaScript para obtener tribunales
            System.out.println("\n📍 STEP 1: Ejecutando JavaScript para obtener tribunales...");
            Object tribunalesObj = targetFrame.evaluate("""
                (function() {
                    console.log('=== INICIANDO BÚSQUEDA DE TRIBUNALES ===');

                    // Buscar todos los elementos con data-original-index
                    const allLi = document.querySelectorAll('[data-original-index]');
                    console.log('Total elementos encontrados en DOM:', allLi.length);

                    // Crear array en lugar de objeto (mejor conversión a Java)
                    const result = [];

                    for (let li of allLi) {
                        const indexStr = li.getAttribute('data-original-index');
                        const spanText = li.querySelector('span.text');

                        console.log('Procesando elemento:', {
                            indexStr: indexStr,
                            hasSpan: !!spanText,
                            spanText: spanText ? spanText.textContent.trim() : 'NO ENCONTRADO'
                        });

                        if (indexStr && spanText) {
                            const index = parseInt(indexStr, 10);
                            const nombre = spanText.textContent.trim();

                            result.push({
                                index: index,
                                nombre: nombre
                            });

                            console.log('✓ Agregado: Índice ' + index + ' = ' + nombre);
                        } else {
                            console.log('✗ Saltado: indexStr=' + indexStr + ', spanText=' + !!spanText);
                        }
                    }

                    console.log('=== RESULTADO FINAL ===');
                    console.log('Total tribunales en array:', result.length);
                    console.log('Primeros 5:', JSON.stringify(result.slice(0, 5)));

                    return result;
                })()
            """);

            System.out.println("✓ JavaScript ejecutado, tipo retornado: " +
                (tribunalesObj != null ? tribunalesObj.getClass().getName() : "null"));

            // ✅ STEP 2: Convertir el resultado a Map
            System.out.println("\n📍 STEP 2: Convirtiendo resultado a Map...");
            System.out.println("  Tipo de objeto recibido: " +
                (tribunalesObj != null ? tribunalesObj.getClass().getSimpleName() : "NULL"));

            if (tribunalesObj instanceof java.util.List<?> list) {
                System.out.println("  ✓ Es una List con " + list.size() + " elementos");

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    System.out.println("  Item " + i + ": tipo=" + (item != null ? item.getClass().getSimpleName() : "null"));

                    if (item instanceof java.util.Map<?, ?> itemMap) {
                        System.out.println("    Keys en map: " + itemMap.keySet());

                        Object indexObj = itemMap.get("index");
                        Object nombreObj = itemMap.get("nombre");

                        System.out.println("    index=" + indexObj + " (type: " +
                            (indexObj != null ? indexObj.getClass().getSimpleName() : "null") + ")");
                        System.out.println("    nombre=" + nombreObj + " (type: " +
                            (nombreObj != null ? nombreObj.getClass().getSimpleName() : "null") + ")");

                        if (indexObj instanceof Number && nombreObj instanceof String nombre) {
                            int idx = ((Number) indexObj).intValue();
                            tribunalesPorIndice.put(idx, nombre);

                            // Debug: mostrar los primeros y últimos
                            if (i < 3 || i == list.size() - 1) {
                                System.out.println("    ✓ Agregado al Map: " + idx + " = " + nombre);
                            }
                        } else {
                            System.out.println("    ✗ Tipos incorrectos, saltado");
                        }
                    } else {
                        System.out.println("    ✗ No es un Map, saltado");
                    }
                }
            } else {
                System.out.println("  ✗ NO es una List, es: " +
                    (tribunalesObj != null ? tribunalesObj.getClass().getName() : "null"));
                System.out.println("  Intentando conversión directa...");

                if (tribunalesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Integer, String> map = (Map<Integer, String>) tribunalesObj;
                    tribunalesPorIndice = map;
                    System.out.println("  ✓ Convertido directamente a Map");
                } else {
                    System.out.println("  ✗ No se pudo convertir");
                }
            }

            // ✅ STEP 3: Verificar resultado
            System.out.println("\n📍 STEP 3: Verificando resultado final...");
            System.out.println("  Total de tribunales en Map: " + tribunalesPorIndice.size());

            if (tribunalesPorIndice.isEmpty()) {
                System.err.println("  ✗ ¡¡¡ MAPA VACÍO !!! Este es el problema");
                System.err.println("  Valor retornado por JS: " + tribunalesObj);
                System.err.println("  Tipo: " + (tribunalesObj != null ? tribunalesObj.getClass().getName() : "null"));
            } else {
                System.out.println("  ✓ Mapa contiene datos");

                // Mostrar primeros 5
                int count = 0;
                for (Map.Entry<Integer, String> entry : tribunalesPorIndice.entrySet()) {
                    if (count < 5) {
                        System.out.println("    [" + entry.getKey() + "] = " + entry.getValue());
                        count++;
                    } else {
                        break;
                    }
                }
                System.out.println("    ... " + (tribunalesPorIndice.size() - 5) + " más");
            }

            System.out.println("\n✓ Se obtuvieron " + tribunalesPorIndice.size() + " tribunales");
            System.out.println("🔍 ════════════════════════════════════════════\n");

            // Iterar a través de cada tribunal usando data-original-index (saltando el 1 que es "Seleccione Tribunal...")
            System.out.println("\n🔄 ════════════════════════════════════════════");
            System.out.println("🔄 INICIANDO ITERACIÓN POR TRIBUNALES");
            System.out.println("🔄 ════════════════════════════════════════════");
            System.out.println("Total a procesar: " + (maxIndex - 1) + " tribunales (índices 2 a " + maxIndex + ")\n");

            for (int index = 2; index <= maxIndex; index++) {
                try {
                    System.out.println("▶ Procesando índice: " + index);

                    String nombreTribunal = tribunalesPorIndice.get(index);
                    System.out.println("  Obtenido del Map: " + nombreTribunal);

                    if (nombreTribunal == null) {
                        System.out.println("  ✗ nombreTribunal ES NULL");
                        int finalIndex = index;
                        System.out.println("  Map contiene estas keys: " + tribunalesPorIndice.keySet().stream()
                            .filter(k -> k >= finalIndex - 2 && k <= finalIndex + 2)
                            .toList());
                        System.out.println("  ⚠ No se encontró información del tribunal en índice: " + index);
                        continue;
                    }

                    if (nombreTribunal.isEmpty()) {
                        System.out.println("  ✗ nombreTribunal ESTÁ VACÍO");
                        System.out.println("  ⚠ No se encontró información del tribunal en índice: " + index);
                        continue;
                    }

                    System.out.println("\n=== Buscando en tribunal (" + (index - 1) + "/" + (maxIndex - 1) + "): " + nombreTribunal + " ===");

                    // ✅ Abrir dropdown ANTES de seleccionar
                    abrirDropdownTribunales(targetFrame);
                    page.waitForTimeout(1000);

                    // Seleccionar tribunal por su data-original-index
                    boolean seleccionado = seleccionarTribunalPorIndice(targetFrame, index);

                    if (!seleccionado) {
                        System.out.println("✗ No se pudo seleccionar el tribunal: " + nombreTribunal);
                        continue;
                    }

                    page.waitForTimeout(1500);

                    // Presionar el botón de búsqueda
                    targetFrame.click("#btnConConsultaNom");
                    System.out.println("✓ Botón de búsqueda presionado para: " + nombreTribunal);

                    page.waitForTimeout(8000); // Tiempo de espera aumentado para búsquedas sin resultados

                    // Verificar si hay resultados
                    try {
                        targetFrame.waitForSelector("table#dtaTableDetalleNombre tbody tr",
                                new Frame.WaitForSelectorOptions().setTimeout(5000));

                        // Extraer datos si los hay
                        String html = page.content();
                        Document doc = Jsoup.parse(html);
                        Elements rows = doc.select("table#dtaTableDetalleNombre tbody tr");

                        List<String[]> resultadosTribunal = new ArrayList<>();

                        // Extraer datos de cada fila
                        for (Element row : rows) {
                            // Saltar la fila de paginación (que tiene colspan)
                            if (!row.select("td[colspan]").isEmpty()) {
                                continue;
                            }

                            Elements cols = row.select("td");
                            if (cols.size() >= 5) {
                                String rolValue = cols.get(1).text().trim();
                                String fechaValue = cols.get(2).text().trim();
                                String caratuladoValue = cols.get(3).text().trim();
                                String tribunalValue = cols.get(4).text().trim();

                                // Incluir datos de búsqueda + datos del resultado
                                resultadosTribunal.add(new String[]{
                                    nombres, apellidoPaterno, apellidoMaterno, String.valueOf(anio),
                                    rolValue, fechaValue, caratuladoValue, tribunalValue
                                });
                            }
                        }

                        // Si hay resultados, agregarlos a los resultados totales
                        if (!resultadosTribunal.isEmpty()) {
                            System.out.println("✓ Se encontraron " + resultadosTribunal.size() + " causas en: " + nombreTribunal);
                            todosLosResultados.addAll(resultadosTribunal);
                        } else {
                            System.out.println("✗ Sin resultados para: " + nombreTribunal);
                        }

                    } catch (PlaywrightException e) {
                        System.out.println("✗ Sin resultados encontrados para: " + nombreTribunal);
                    }

                } catch (Exception e) {
                    System.err.println("✗ Error procesando tribunal en índice " + index + ": " + e.getMessage());
                }

                page.waitForTimeout(1500);
            }

            // Guardar todos los resultados acumulados en el archivo CSV
            if (todosLosResultados.size() > 1) { // Mayor a 1 porque la primera es el encabezado
                String filePath = "resultados_busqueda.csv";
                guardarEnCSV(filePath, todosLosResultados);
                System.out.println("\n✓ Total de causas guardadas: " + (todosLosResultados.size() - 1));
                System.out.println("✓ Datos guardados en: " + filePath);
            } else {
                System.out.println("\n✗ No se encontraron resultados en ningún tribunal");
            }

            browser.close();
        }
    }

    /**
     * Versión paralela del método buscarPorNombre
     * Múltiples threads pueden llamar esto simultáneamente, cada uno con un año diferente
     * Los resultados se agregan a una lista compartida (thread-safe)
     */
    private void buscarPorNombreParalelo(String nombres, String apellidoPaterno, String apellidoMaterno,
                                        int anio, List<String[]> resultadosCompartidos) {
        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setTimeout(60000)
            );

            Page page = browser.newPage();

            page.navigate(URL,
                    new Page.NavigateOptions()
                            .setTimeout(60000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

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
                System.err.println("   ✗ [" + anio + "] No se encontró el iframe");
                browser.close();
                return;
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

            System.out.println("   [" + anio + "] ✓ Formulario completado para año: " + anio);

            // Abrir dropdown
            abrirDropdownTribunales(targetFrame);
            page.waitForTimeout(1000);

            // Obtener máximo índice
            Object maxIndexObj = targetFrame.evaluate("""
                (function() {
                    const allLi = document.querySelectorAll('[data-original-index]');
                    let maxIndex = 0;
                    for (let li of allLi) {
                        const indexStr = li.getAttribute('data-original-index');
                        if (indexStr) {
                            const index = parseInt(indexStr, 10);
                            if (index > maxIndex) maxIndex = index;
                        }
                    }
                    return maxIndex;
                })()
            """);

            int maxIndex = 0;
            if (maxIndexObj instanceof Number) {
                maxIndex = ((Number) maxIndexObj).intValue();
            }

            if (maxIndex == 0) {
                System.err.println("   ✗ [" + anio + "] No se pudo obtener el máximo índice");
                browser.close();
                return;
            }

            // Obtener nombres de tribunales
            Map<Integer, String> tribunalesPorIndice = new HashMap<>();
            Object tribunalesObj = targetFrame.evaluate("""
                (function() {
                    const allLi = document.querySelectorAll('[data-original-index]');
                    const result = [];
                    for (let li of allLi) {
                        const indexStr = li.getAttribute('data-original-index');
                        const spanText = li.querySelector('span.text');
                        if (indexStr && spanText) {
                            result.push({
                                index: parseInt(indexStr, 10),
                                nombre: spanText.textContent.trim()
                            });
                        }
                    }
                    return result;
                })()
            """);

            if (tribunalesObj instanceof java.util.List<?> list) {
                for (Object item : list) {
                    if (item instanceof java.util.Map<?, ?> itemMap) {
                        Object indexObj = itemMap.get("index");
                        Object nombreObj = itemMap.get("nombre");
                        if (indexObj instanceof Number && nombreObj instanceof String nombre) {
                            int idx = ((Number) indexObj).intValue();
                            tribunalesPorIndice.put(idx, nombre);
                        }
                    }
                }
            }

            System.out.println("   [" + anio + "] ✓ Obtenidos " + tribunalesPorIndice.size() + " tribunales");

            // Iterar tribunales
            for (int index = 2; index <= maxIndex; index++) {
                try {
                    String nombreTribunal = tribunalesPorIndice.get(index);
                    if (nombreTribunal == null || nombreTribunal.isEmpty()) {
                        continue;
                    }

                    abrirDropdownTribunales(targetFrame);
                    page.waitForTimeout(500);

                    boolean seleccionado = seleccionarTribunalPorIndice(targetFrame, index);
                    if (!seleccionado) {
                        continue;
                    }

                    page.waitForTimeout(1000);
                    targetFrame.click("#btnConConsultaNom");

                    page.waitForTimeout(5000);

                    try {
                        targetFrame.waitForSelector("table#dtaTableDetalleNombre tbody tr",
                                new Frame.WaitForSelectorOptions().setTimeout(3000));

                        String html = page.content();
                        Document doc = Jsoup.parse(html);
                        Elements rows = doc.select("table#dtaTableDetalleNombre tbody tr");

                        for (Element row : rows) {
                            if (!row.select("td[colspan]").isEmpty()) {
                                continue;
                            }

                            Elements cols = row.select("td");
                            if (cols.size() >= 5) {
                                String rolValue = cols.get(1).text().trim();
                                String fechaValue = cols.get(2).text().trim();
                                String caratuladoValue = cols.get(3).text().trim();
                                String tribunalValue = cols.get(4).text().trim();

                                // Agregar a lista compartida de forma thread-safe
                                resultadosCompartidos.add(new String[]{
                                    nombres, apellidoPaterno, apellidoMaterno, String.valueOf(anio),
                                    rolValue, fechaValue, caratuladoValue, tribunalValue
                                });
                            }
                        }

                        if (!rows.isEmpty()) {
                            System.out.println("   [" + anio + "] ✓ " + rows.size() + " causas encontradas en " + nombreTribunal);
                        }

                    } catch (PlaywrightException e) {
                        // Sin resultados
                    }

                } catch (Exception e) {
                    // Continuar con siguiente tribunal
                }

                page.waitForTimeout(800);
            }

            System.out.println("   [" + anio + "] ✓ Búsqueda completada para año " + anio);
            browser.close();

        } catch (Exception e) {
            System.err.println("   ✗ [" + anio + "] Error: " + e.getMessage());
        }
    }
    private void abrirDropdownTribunales(Frame targetFrame) {
        try {
            // Primero, encontrar el botón que abre el dropdown
            Object foundButton = targetFrame.evaluate("""
                (function() {
                    // Buscar el button del selectpicker
                    const buttons = document.querySelectorAll('button');
                    for (let btn of buttons) {
                        if (btn.getAttribute('data-toggle') === 'dropdown' ||
                            btn.classList.contains('dropdown-toggle') ||
                            btn.classList.contains('btn-default') ||
                            btn.innerText.includes('Seleccione') ||
                            btn.title.includes('tribunal') ||
                            btn.title.toLowerCase().includes('court')) {
                            console.log('Found button:', btn.innerText, btn.className);
                            // Hacer click para abrir
                            btn.click();
                            return true;
                        }
                    }
                    // Si no encuentra por criterios específicos, buscar cualquier button cerca del combobox
                    const dropdown = document.querySelector('.bootstrap-select');
                    if (dropdown) {
                        const btn = dropdown.querySelector('button');
                        if (btn) {
                            console.log('Found button in dropdown:', btn.innerText);
                            btn.click();
                            return true;
                        }
                    }
                    return false;
                })()
            """);

            if (foundButton instanceof Boolean && (Boolean) foundButton) {
                System.out.println("✓ Botón del dropdown encontrado y presionado");
                Thread.sleep(1000);
            } else {
                System.err.println("⚠ No se encontró el botón del dropdown");
            }

            // Verificar que el dropdown está abierto
            Object isOpen = targetFrame.evaluate("""
                (function() {
                    const dropdown = document.querySelector('.dropdown-menu.open');
                    const inner = document.querySelector('ul.dropdown-menu.inner');
                    console.log('Dropdown open class exists:', !!dropdown);
                    console.log('Inner ul exists:', !!inner);
                    if (dropdown && inner) {
                        const items = document.querySelectorAll('ul.dropdown-menu.inner li');
                        console.log('Li items in dropdown:', items.length);
                        return true;
                    }
                    return false;
                })()
            """);

            if (isOpen instanceof Boolean && (Boolean) isOpen) {
                System.out.println("✓ Dropdown verificado como abierto");
            } else {
                System.out.println("⚠ Dropdown podría no estar completamente abierto");
            }

        } catch (Exception e) {
            System.err.println("Error al abrir dropdown: " + e.getMessage());
        }
    }

    /**
     * Selecciona un tribunal usando su data-original-index
     * Usa locator que es más robusto que evaluate
     */
    private boolean seleccionarTribunalPorIndice(Frame targetFrame, int index) {
        try {
            System.out.println("  🔍 Seleccionando tribunal con índice: " + index);

            // Crear selector CSS
            String selector = String.format("[data-original-index=\"%d\"] a", index);

            try {
                // Usar locator que es más robusto
                Locator elemento = targetFrame.locator(selector);

                // Verificar que el elemento existe
                if (elemento.count() > 0) {
                    System.out.println("  ✓ Elemento encontrado, haciendo click...");
                    elemento.click();
                    System.out.println("  ✓ Click ejecutado en tribunal con índice " + index);
                    Thread.sleep(800);
                    return true;
                } else {
                    System.out.println("  ❌ Elemento no encontrado con selector: " + selector);

                    // Intentar selector alternativo
                    String selectorAlt = String.format("[data-original-index=\"%d\"]", index);
                    Locator elementoAlt = targetFrame.locator(selectorAlt);
                    if (elementoAlt.count() > 0) {
                        System.out.println("  ℹ Encontrado con selector alternativo, haciendo click en li...");
                        elementoAlt.click();
                        Thread.sleep(800);
                        return true;
                    }

                    return false;
                }

            } catch (PlaywrightException e) {
                System.out.println("  ❌ PlaywrightException: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            System.err.println("  ❌ Error al seleccionar tribunal por índice " + index + ": " + e.getMessage());
            return false;
        }
    }

    public List<CausaDTO> buscarPorRut(String rut) {

        List<CausaDTO> causas = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            Page page = browser.newPage();
            page.navigate("https://consultas.pjud.cl");

            page.fill("#rut", rut);
            page.click("#btnConsultar");

            page.waitForSelector("table");

            String html = page.content();
            Document doc = Jsoup.parse(html);

            Elements rows = doc.select("table tbody tr");

            for (Element row : rows) {
                Elements cols = row.select("td");

                CausaDTO dto = new CausaDTO();
                dto.setRol(cols.get(0).text());
                dto.setTribunal(cols.get(1).text());
                dto.setCaratula(cols.get(2).text());
                dto.setEstado(cols.get(3).text());

                causas.add(dto);
            }

            browser.close();
        }

        return causas;
    }

    /**
     * Guarda los datos en un archivo CSV en la raíz del proyecto
     * @param filePath ruta del archivo
     * @param data lista de arrays con los datos
     */
    private void guardarEnCSV(String filePath, List<String[]> data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String[] row : data) {
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
            System.out.println("Archivo CSV creado/actualizado correctamente: " + filePath);
        } catch (Exception e) {
            System.err.println("Error al guardar el archivo CSV: " + e.getMessage());
        }
    }

}

