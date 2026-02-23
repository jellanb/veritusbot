# ❓ PREGUNTAS FRECUENTES - PARALELISMO CON 3 THREADS

## 🔴 Problemas Comunes

### P1: ¿Por qué no acelera más? ¿Puedo usar 10 threads?

**R:** No se recomienda. Aquí está por qué:

```
MAX_THREADS = 3:  Balance ideal (velocidad + seguridad)
MAX_THREADS = 10: Riesgo alto de bloqueo PJUD

Razonamiento:
- 10 ventanas simultáneas = El servidor PJUD puede detectar patrón de bot
- Bloqueo temporal o IP baneada = Búsqueda fallida
- 3 threads = Parecido a navegación manual
```

**Recomendación:** Mantener en 3 o máximo 4

---

### P2: ¿Qué pasa si se cae un thread?

**R:** Los otros continúan sin problemas:

```
Si Thread 1 falla en año 2019:
- Thread 1: ERROR ❌
- Thread 2: Continúa con 2020 ✅
- Thread 3: Continúa con 2021 ✅

Resultado:
- 2019: Resultado parcial o vacío
- 2020: Resultados completos
- 2021: Resultados completos
- CSV final: 2 de 3 años completados
```

**Ventaja:** No se pierden todos los datos

---

### P3: ¿Cómo sé que los 3 threads están funcionando?

**R:** Observa los logs en vivo:

```
[12:30:46] ▶ [THREAD pool-1-thread-1] Procesando año: 2019
[12:30:46] ▶ [THREAD pool-1-thread-2] Procesando año: 2020
[12:30:46] ▶ [THREAD pool-1-thread-3] Procesando año: 2021
```

**Indicadores:**
- ✅ Ves 3 líneas simultáneamente = 3 threads funcionando
- ❌ Ves 1 línea y esperas mucho = Solo 1 thread activo

---

### P4: ¿Los resultados se duplican o pierden?

**R:** No. Aquí está por qué:

```
CopyOnWriteArrayList = Thread-safe

Cada resultado se agrega así:
- Thread 1: Agrega resultado → [2019, R1]
- Thread 2: Agrega resultado → [2020, R1]
- Thread 3: Agrega resultado → [2021, R1]

Resultado final: Todos se guardan SIN duplicados
```

---

### P5: ¿Cuánta memoria consume?

**R:** Aproximadamente:

```
Per Thread:        ~150-200 MB (Chromium + datos)
3 Threads:        ~450-600 MB
Overhead Java:    ~100 MB
TOTAL:            ~550-700 MB

Comparación:
- Secuencial:     ~150-200 MB (1 thread a la vez)
- Paralelo:       ~550-700 MB (3 threads simultáneos)

Diferencia: +400-500 MB (aceptable para ganancia 3x)
```

---

### P6: ¿Funciona en máquinas de bajo rendimiento?

**R:** Depende:

```
MÁquina potente (8GB+ RAM):
- ✅ Funciona perfectamente
- ✅ Usa MAX_THREADS = 3

Máquina media (4GB RAM):
- ⚠️ Funciona pero lento
- ⚠️ Usa MAX_THREADS = 2
- ⚠️ O reduce otros procesos

Máquina débil (2GB RAM):
- ❌ Puede fallar
- ⚠️ Usa MAX_THREADS = 1 (secuencial)
```

---

## 💡 Preguntas Técnicas

### P7: ¿Cómo cambio el número de threads?

**R:** Muy simple:

1. Abre: `src/main/java/com/example/veritusbot/scraper/PjudScraper.java`
2. Busca: Línea 31
3. Cambia: `private static final int MAX_THREADS = 3;`
4. Ejemplo: `private static final int MAX_THREADS = 2;`
5. Guarda
6. Recompila: `./mvnw clean compile`
7. Ejecuta: `./mvnw spring-boot:run`

---

### P8: ¿Qué es CopyOnWriteArrayList?

**R:** Es una lista segura para múltiples threads:

```
Lista Normal (no thread-safe):
┌─────────────────────────┐
│ Thread 1: Agrega dato   │
│ Thread 2: Lee dato      │ ← Conflicto ❌
│ Thread 3: Agrega dato   │
└─────────────────────────┘

CopyOnWriteArrayList (thread-safe):
┌─────────────────────────┐
│ Thread 1: Agrega dato ✅ │
│ Thread 2: Lee dato ✅    │
│ Thread 3: Agrega dato ✅ │
│ Sin conflictos          │
└─────────────────────────┘
```

---

### P9: ¿Try-with-resources qué es?

**R:** Cierra automáticamente los recursos:

```
ANTES (Manual):
ExecutorService executor = Executors.newFixedThreadPool(3);
try {
    // usar executor
} finally {
    executor.shutdown();  // ← Manual, fácil olvidar
}

DESPUÉS (Try-with-resources):
try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
    // usar executor
}  // ← Automático, imposible olvidar
```

---

### P10: ¿Puedo usar SelectList en lugar de HashMap?

**R:** No es necesario. Aquí está por qué:

```
HashMap es mejor para este caso:
- Acceso rápido por índice (O(1))
- No necesitas orden
- Mejor para búsquedas

LinkedHashMap: Si necesitas orden
SelectList: No existe en Java

Conclusión: Mantener HashMap
```

---

## 📊 Preguntas sobre Performance

### P11: ¿Realmente es 3x más rápido?

**R:** Depende de varios factores:

```
Caso Ideal (3x):
- 6 años × 5 min cada uno
- 3 threads balanceados
- Conexión estable
- Servidor PJUD responsivo

Resultado:
- Secuencial:  30 minutos
- Paralelo:    10 minutos
- Mejora:      3x ✅

Caso Real (2.5x):
- Esperas variables entre búsquedas
- Threads con diferentes tiempos
- Network intermitente

Resultado:
- Secuencial:  30 minutos
- Paralelo:    12 minutos
- Mejora:      2.5x ✅
```

---

### P12: ¿Qué pasa si dos threads buscan simultáneamente?

**R:** Totalmente normal y esperado:

```
THREAD 1: Año 2019
├─ Abre Chromium 1
├─ Rellena formulario 1
├─ Busca 231 tribunales
└─ Extrae resultados 1

THREAD 2: Año 2020 (MISMO MOMENTO)
├─ Abre Chromium 2
├─ Rellena formulario 2
├─ Busca 231 tribunales
└─ Extrae resultados 2

THREAD 3: Año 2021 (MISMO MOMENTO)
├─ Abre Chromium 3
├─ Rellena formulario 3
├─ Busca 231 tribunales
└─ Extrae resultados 3

Todos en PARALELO simultáneamente = Velocidad 3x
```

---

### P13: ¿Cómo monitoreo el progreso?

**R:** Observa estos logs:

```
[12:30:46] ▶ [THREAD pool-1-thread-1] Procesando año: 2019
[12:30:47] [2019] ✓ Formulario completado
[12:30:48] [2019] ✓ Obtenidos 231 tribunales
[12:30:50] [2019] ✓ 2 causas encontradas en: 1º Juzgado...
...
[12:34:50] [2019] ✓ Búsqueda completada para año 2019

Índices de progreso:
- "Procesando año" = Comenzó
- "Formulario completado" = Llenó datos
- "Obtenidos X tribunales" = 231 tribunales listados
- "causas encontradas" = Resultados encontrados
- "Búsqueda completada" = Terminó
```

---

## 🚨 Solución de Problemas

### P14: ¿Qué si PJUD me bloquea?

**R:** Reduce threads:

```
Síntoma: Conexiones rechazadas
Causa: Demasiadas ventanas simultáneas

Solución:
1. Parar aplicación: Ctrl+C
2. Cambiar: MAX_THREADS = 2 (en PjudScraper.java)
3. Esperar: 30-60 minutos (posible bloqueo temporal)
4. Reiniciar: Vuelve a intentar

Si persiste después de 1 hora:
- Usar VPN (si es legal)
- Esperar 24 horas
- Contactar soporte PJUD
```

---

### P15: ¿Qué si se cuelga la aplicación?

**R:** Reinicia:

```
Síntoma: Aplicación no responde

Solución:
1. Presiona Ctrl+C en terminal
2. Cierra ventanas de Chromium (si quedan abiertas)
3. Espera 5 segundos
4. Ejecuta nuevamente: ./mvnw spring-boot:run

Si persiste:
1. Limpia caché: ./mvnw clean
2. Recompila: ./mvnw compile
3. Ejecuta: ./mvnw spring-boot:run
```

---

## 📚 Preguntas sobre Aprendizaje

### P16: ¿Dónde aprender más sobre Java Concurrency?

**R:** Recursos recomendados:

```
Oficial:
- Java Concurrency in Practice (libro)
- Oracle Java Docs
- JavaDoc de java.util.concurrent

Online:
- Baeldung Concurrency
- GeeksforGeeks Java Threads
- YouTube: Code Fellows Java Concurrency

Conceptos a aprender:
- ExecutorService
- ThreadPool
- CopyOnWriteArrayList
- Future y Callable
- Locks y Semaphores
```

---

### P17: ¿Puedo adaptar esto para otros proyectos?

**R:** Claro, el patrón es:

```
1. Identificar tarea parallelizable (años, en este caso)
2. Crear ExecutorService(n threads)
3. Por cada iteración: executor.submit(tarea)
4. Usar CopyOnWriteArrayList para resultados
5. future.get() para esperar
6. Guardar resultados consolidados

Aplicaciones:
- Web scraping
- Procesamiento batch
- API calls masivos
- Descarga de archivos
- Análisis de datos
```

---

## 🎯 Resumen Rápido

| Pregunta | Respuesta Corta |
|----------|-----------------|
| ¿Usar 10 threads? | No, máximo 4 |
| ¿Se cae un thread? | Otros continúan |
| ¿Duplicados? | No, CopyOnWriteArrayList cuida eso |
| ¿Cuánta memoria? | ~600MB |
| ¿Máquina débil? | Usar 1-2 threads |
| ¿Cómo cambiar? | Editar línea 31 |
| ¿3x más rápido? | Sí, en promedio 2.5-3x |
| ¿PJUD me bloquea? | Reduce a 2 threads |

---

## 📞 Contacto

Para más preguntas:
- Lee: `PARALELISMO_3_THREADS.md`
- Ejecuta: Ver logs en tiempo real
- Experimenta: Cambia MAX_THREADS y prueba

---

**Versión:** 1.0
**Fecha:** 23 de Febrero, 2026
**Estado:** ✅ Actualizado
