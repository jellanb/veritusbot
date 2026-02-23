# 🎨 VISUALIZACIÓN DEL PARALELISMO

## Comparación Visual: Antes vs Después

### ANTES - Búsqueda Secuencial

```
TIEMPO
│
│  T1:     [2019 ..................] (5 min)
│  T2:                              [2020 ..................] (5 min)
│  T3:                                                        [2021 ..................] (5 min)
│  T4:                                                                                   [2022 ..................] (5 min)
│  T5:                                                                                                            [2023 ..................] (5 min)
│  T6:                                                                                                                                          [2024 ..................] (5 min)
│
│  TOTAL: 30 MINUTOS ❌
└──────────────────────────────────────────────────────────────────────────────────────────────────────────
```

### DESPUÉS - Búsqueda Paralela (3 Threads)

```
TIEMPO
│
│  THREAD 1:  [2019 ..........]  →  [2022 ..........]
│  THREAD 2:  [2020 ..........]  →  [2023 ..........]
│  THREAD 3:  [2021 ..........]  →  [2024 ..........]
│
│  TOTAL: ~10 MINUTOS ✅ (3x MÁS RÁPIDO)
└────────────────────────────────────────────────────────
```

---

## Estado de los Threads en Tiempo Real

### Fase 1: Primeros 3 años (0-5 min)

```
┌─────────────────────────────────────────┐
│  EXECUTOR SERVICE (MAX_THREADS = 3)    │
├─────────────────────────────────────────┤
│                                         │
│  🔴 THREAD 1: Procesando año 2019     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
│  🟢 THREAD 2: Procesando año 2020     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
│  🟡 THREAD 3: Procesando año 2021     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
│  ⚪ THREAD 4: En espera                │
│  ⚪ THREAD 5: En espera                │
│  ⚪ THREAD 6: En espera                │
│                                         │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│  COPYONWRITEARRAYLIST (Resultados)    │
│                                         │
│  [2019, CAUSA1]                         │
│  [2019, CAUSA2]                         │
│  [2020, CAUSA1]                         │
│  [2021, CAUSA1]                         │
│  [2021, CAUSA2]                         │
│  ...                                    │
└─────────────────────────────────────────┘
```

### Fase 2: Siguientes 3 años (5-10 min)

```
┌─────────────────────────────────────────┐
│  EXECUTOR SERVICE (MAX_THREADS = 3)    │
├─────────────────────────────────────────┤
│                                         │
│  🔴 THREAD 1: Procesando año 2022     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
│  🟢 THREAD 2: Procesando año 2023     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
│  🟡 THREAD 3: Procesando año 2024     │
│     ├─ Abriendo Chromium               │
│     ├─ Llenando formulario             │
│     ├─ Iterando 231 tribunales         │
│     └─ Extrayendo resultados...        │
│                                         │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│  COPYONWRITEARRAYLIST (Resultados)    │
│                                         │
│  [Todos los resultados de fases 1 y 2] │
│  [2019, CAUSA1] [2020, CAUSA1]         │
│  [2021, CAUSA1] [2022, CAUSA1]         │
│  [2023, CAUSA1] [2024, CAUSA1]         │
│  ...                                    │
│  TOTAL: 45 causas                       │
│                                         │
└─────────────────────────────────────────┘
```

---

## Flujo de Tareas

```
INICIO
  │
  ├─→ Leer persona del Excel
  │   ├─ Nombres: MIGUEL ANTONIO
  │   ├─ Apellido Paterno: SOTO
  │   ├─ Apellido Materno: FREDES
  │   └─ Rango: 2019 a 2024 (6 años)
  │
  ├─→ Crear ExecutorService (3 threads)
  │
  ├─→ Crear 6 tareas (una por año)
  │   ├─ Tarea 1: Año 2019
  │   ├─ Tarea 2: Año 2020
  │   ├─ Tarea 3: Año 2021
  │   ├─ Tarea 4: Año 2022
  │   ├─ Tarea 5: Año 2023
  │   └─ Tarea 6: Año 2024
  │
  ├─→ Enviar tareas al executor
  │   ├─ Thread 1 toma Tarea 1 (2019)
  │   ├─ Thread 2 toma Tarea 2 (2020)
  │   └─ Thread 3 toma Tarea 3 (2021)
  │
  ├─→ Threads ejecutan buscarPorNombreParalelo()
  │   └─ Cada uno procesa su año
  │
  ├─→ Agregar resultados a lista compartida
  │   └─ CopyOnWriteArrayList (thread-safe)
  │
  ├─→ Cuando termina Thread 1 (2019)
  │   └─ Toma Tarea 4 (2022)
  │
  ├─→ Cuando termina Thread 2 (2020)
  │   └─ Toma Tarea 5 (2023)
  │
  ├─→ Cuando termina Thread 3 (2021)
  │   └─ Toma Tarea 6 (2024)
  │
  ├─→ Esperar a que terminen TODAS las tareas
  │   └─ future.get() para cada uno
  │
  ├─→ Guardar CSV consolidado
  │   ├─ Nombres, Apellido Paterno, Apellido Materno, Año
  │   ├─ Rol, Fecha, Caratulado, Tribunal
  │   └─ resultados_busqueda.csv
  │
  └─→ FIN ✅
```

---

## Tabla de Distribución de Trabajo

```
┌─────────┬──────────────────────┬──────────────────┐
│ Thread  │ Minutos 0-5          │ Minutos 5-10     │
├─────────┼──────────────────────┼──────────────────┤
│ THREAD1 │ Año 2019 (231 trib.) │ Año 2022 (231...) │
│ THREAD2 │ Año 2020 (231 trib.) │ Año 2023 (231...) │
│ THREAD3 │ Año 2021 (231 trib.) │ Año 2024 (231...) │
└─────────┴──────────────────────┴──────────────────┘

Total de búsquedas: 6 años × 231 tribunales = 1,386 búsquedas
Tiempo total:       ~10 minutos (paralelo)
vs                  ~30 minutos (secuencial)
Mejora:             3x más rápido ⚡
```

---

## Sincronización Thread-Safe

```
┌────────────────────────────────────────────────────────┐
│              CopyOnWriteArrayList                      │
│                                                        │
│  THREAD 1                 THREAD 2         THREAD 3   │
│     │                        │                 │      │
│     └──→ add([2019, R1]) ────→ LISTA ←────────┘      │
│                         ↑         ↑                   │
│                         │         │ (thread-safe)    │
│     ┌─────────────────→ LISTA ←─────────────────┐    │
│     │                         ↑                 │     │
│     └──→ add([2019, R2]) ────→ LISTA ←──→ add([2021, R1]) │
│                                                        │
│  ✅ Sin locks manuales                                 │
│  ✅ Sin deadlocks                                      │
│  ✅ Múltiples threads escriben simultáneamente        │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

## Monitoreo de Logs en Paralelo

```
[12:30:45] 🔍 Buscando: MIGUEL ANTONIO SOTO FREDES
[12:30:45]    Rango de años: 2019 a 2024
[12:30:45]    Modo: PARALELO (máximo 3 ventanas simultáneas)

[12:30:46]    ▶ [THREAD pool-1-thread-1] Procesando año: 2019
[12:30:46]    ▶ [THREAD pool-1-thread-2] Procesando año: 2020
[12:30:46]    ▶ [THREAD pool-1-thread-3] Procesando año: 2021

[12:30:47]    [2019] ✓ Formulario completado para año: 2019
[12:30:47]    [2020] ✓ Formulario completado para año: 2020
[12:30:47]    [2021] ✓ Formulario completado para año: 2021

[12:30:48]    [2019] ✓ Obtenidos 231 tribunales
[12:30:48]    [2020] ✓ Obtenidos 231 tribunales
[12:30:48]    [2021] ✓ Obtenidos 231 tribunales

[12:30:49]    [2019] ✓ 2 causas encontradas en: 1º Juzgado...
[12:30:50]    [2020] ✓ 1 causa encontrada en: 3º Juzgado...
[12:30:51]    [2021] ✓ 3 causas encontradas en: 5º Juzgado...

[12:34:50]    [2019] ✓ Búsqueda completada para año 2019
[12:34:51]    [2020] ✓ Búsqueda completada para año 2020
[12:34:52]    [2021] ✓ Búsqueda completada para año 2021

[12:34:52]    ▶ [THREAD pool-1-thread-1] Procesando año: 2022
[12:34:52]    ▶ [THREAD pool-1-thread-2] Procesando año: 2023
[12:34:52]    ▶ [THREAD pool-1-thread-3] Procesando año: 2024

... (continúa con años 2022, 2023, 2024) ...

[12:39:55] ⏳ Esperando a que terminen todas las búsquedas...

[12:40:00] ✓ Total de causas guardadas: 45
[12:40:00] ✓ Datos guardados en: resultados_busqueda.csv
```

---

## Conclusión Visual

```
╔════════════════════════════════════════════════════════╗
║                                                        ║
║    ANTES: 1 Ventana → 6 años secuenciales            ║
║            [═════════════════════════════]  30 min    ║
║                                                        ║
║    DESPUÉS: 3 Ventanas → 6 años en paralelo          ║
║             [═══════] 10 min (3x más rápido)        ║
║                                                        ║
║    MEJORA: 3x MÁS RÁPIDO ⚡                           ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

---

**¡Paralelismo completamente visualizado!** 🎨
