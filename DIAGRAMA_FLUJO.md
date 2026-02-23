# Diagrama de Flujo - Bot de Búsqueda Judicial

## Flujo General del Bot

```
┌─────────────────────────────────────────────────────────────────────┐
│                    INICIO: Bot de Búsqueda Judicial                 │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 1. Inicializar Playwright                                           │
│    └─ Crear navegador Chromium                                     │
│    └─ Abrir página: oficinajudicialvirtual.pjud.cl                 │
│    └─ Esperar 5 segundos para carga                                │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 2. Cerrar Popup de Bienvenida                                       │
│    └─ Buscar button #close-modal                                   │
│    └─ Hacer click para cerrar                                      │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 3. Navegar a Consulta de Causas → Búsqueda por Nombre             │
│    └─ Hacer click en sección correspondiente                        │
│    └─ Esperar a que cargue el formulario                           │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 4. Completar Formulario                                             │
│    └─ Nombre: MIGUEL                                               │
│    └─ Segundo Nombre: ANTONIO                                      │
│    └─ Apellido Paterno: SOTO                                       │
│    └─ Apellido Materno: FREDES                                     │
│    └─ Año: 2024                                                    │
│    └─ Competencia: Civil (select nomCompetencia = "3")            │
│    └─ Corte: TODOS (select)                                        │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 5. Abrir Dropdown de Tribunales (UNA SOLA VEZ)                    │
│    └─ Detectar button con data-toggle="dropdown"                   │
│    └─ Hacer click para abrir                                       │
│    └─ Esperar 1500ms                                               │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 6. Obtener Máximo data-original-index                              │
│    └─ Evaluar JavaScript para encontrar max index                  │
│    └─ Resultado: maxIndex = 231                                    │
│    └─ Print: "Se encontraron tribunales con índices de 1 a 231"   │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 7. LOOP: for (int index = 2; index <= 231; index++)              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.1. Obtener nombre del tribunal por su índice            │   │
│  │      └─ querySelector: li[data-original-index="X"]        │   │
│  │      └─ Extraer span.text.textContent                     │   │
│  │      └─ Ej: "1º Juzgado de Letras de Arica"             │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.2. Seleccionar tribunal (sin abrir/cerrar dropdown)     │   │
│  │      └─ Scroll al elemento li[data-original-index="X"]    │   │
│  │      └─ Hacer click en el link <a>                        │   │
│  │      └─ Esperar 600ms                                     │   │
│  │      └─ Return true/false                                 │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.3. Esperar 1500ms                                       │   │
│  │      └─ Tiempo para que se renderize la selección         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.4. Hacer click en button #btnConConsultaNom             │   │
│  │      └─ Presionar botón "Buscar"                          │   │
│  │      └─ Esperar 8000ms (para resultados/timeout)         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.5. ¿Hay tabla de resultados?                            │   │
│  │      └─ waitForSelector: table#dtaTableDetalleNombre     │   │
│  │         tbody tr (timeout 5000ms)                         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                  SÍ ↓                              ↓ NO             │
│  ┌──────────────────────────┐    ┌────────────────────────────┐  │
│  │ 7.6. Extraer datos       │    │ 7.6b. Sin resultados       │  │
│  │ └─ Jsoup.parse(html)     │    │ └─ Print: "Sin resultados" │  │
│  │ └─ select tbody tr       │    │    para este tribunal       │  │
│  │ └─ Iterar rows           │    └────────────────────────────┘  │
│  │ └─ cols.get(1-4)         │                                     │
│  │    (Rol/Fecha/Car/Trib)  │                                     │
│  │ └─ Agregar a lista       │                                     │
│  └──────────────────────────┘                                     │
│                  ↓                                                  │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.7. ¿Hay resultados en esta búsqueda?                    │   │
│  │      SÍ → Agregar a todosLosResultados                    │   │
│  │          Print: "Se encontraron X causas en..."           │   │
│  │      NO → Saltar (ya impreso en 7.6b)                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ 7.8. Esperar 1500ms antes del siguiente tribunal           │   │
│  │      └─ Tiempo para que el servidor se estabilice         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                            ↓                                        │
│  SIGUIENTE ITERACIÓN (index++)                                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
           (Después de iterar todos los 230 tribunales)
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 8. Guardar Resultados en CSV                                        │
│    └─ Crear archivo: resultados_busqueda.csv                       │
│    └─ Escribir encabezado: Rol,Fecha,Caratulado,Tribunal         │
│    └─ Escribir todas las filas con datos                          │
│    └─ Cerrar archivo                                               │
│    └─ Print: "Total de causas guardadas: X"                       │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 9. Cerrar Navegador y Finalizar                                     │
│    └─ browser.close()                                              │
│    └─ Retornar lista (vacía en este caso)                         │
└─────────────────────────────────────────────────────────────────────┘
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    FIN: Bot de Búsqueda Completado                  │
│                                                                     │
│  Archivo generado: resultados_busqueda.csv                         │
│  Contenido: Todas las causas encontradas en todos los tribunales   │
└─────────────────────────────────────────────────────────────────────┘
```

## Iteraciones Detalladas

### Ejemplo: Primeras 5 iteraciones

```
Iteración 1 (index=2)
├─ Tribunal: "1º Juzgado de Letras de Arica"
├─ Búsqueda: MIGUEL ANTONIO SOTO FREDES 2024 Civil
├─ Resultado: ✗ Sin resultados
└─ Tiempo total: ~11 segundos (1.5s + 8s + 1.5s)

Iteración 2 (index=3)
├─ Tribunal: "1º Juzgado De Letras de Arica ex 4°"
├─ Búsqueda: MIGUEL ANTONIO SOTO FREDES 2024 Civil
├─ Resultado: ✗ Sin resultados
└─ Tiempo total: ~11 segundos

Iteración 3 (index=4)
├─ Tribunal: "2º Juzgado de Letras de Arica"
├─ Búsqueda: MIGUEL ANTONIO SOTO FREDES 2024 Civil
├─ Resultado: ✗ Sin resultados
└─ Tiempo total: ~11 segundos

Iteración 4 (index=5)
├─ Tribunal: "2º Juzgado De Letras de Arica ex 4°"
├─ Búsqueda: MIGUEL ANTONIO SOTO FREDES 2024 Civil
├─ Resultado: ✗ Sin resultados
└─ Tiempo total: ~11 segundos

Iteración 5 (index=6)
├─ Tribunal: "3º Juzgado de Letras de Arica"
├─ Búsqueda: MIGUEL ANTONIO SOTO FREDES 2024 Civil
├─ Resultado: ✗ Sin resultados
└─ Tiempo total: ~11 segundos

... (continúa hasta index=231)

RESUMEN FINAL
├─ Total de iteraciones: 230
├─ Tiempo estimado total: ~2530 segundos (42 minutos)
├─ Tribunales con resultados: X
└─ Archivo generado: resultados_busqueda.csv (con X causas)
```

## Puntos Clave de Optimización

1. **Dropdown abierto UNA SOLA VEZ**
   - Antes: Se abría y cerraba en cada iteración (ineficiente)
   - Ahora: Se abre al inicio y se reutiliza (eficiente)

2. **Selección sin abrir/cerrar**
   - Antes: Abrir → Seleccionar → Cerrar (3 acciones)
   - Ahora: Seleccionar directamente (1 acción)

3. **Índices utilizados correctamente**
   - Antes: Basado en cantidad de elementos (confuso)
   - Ahora: Basado en máximo data-original-index (claro)

4. **Iteración garantizada**
   - Cada tribunal se procesa exactamente UNA VEZ
   - No hay repeticiones ni saltos
