# ⚡ GUÍA RÁPIDA - PARALELISMO CON 3 THREADS

## 🎯 En 30 segundos

**ANTES:** Búsqueda secuencial
```
Año 2019 → Año 2020 → Año 2021 → Año 2022 → Año 2023 → Año 2024
[5min]    [5min]    [5min]    [5min]    [5min]    [5min]
TOTAL: 30 minutos
```

**DESPUÉS:** 3 ventanas en paralelo
```
Ventana 1:  Año 2019 ──→ Año 2022 ──→
Ventana 2:  Año 2020 ──→ Año 2023 ──→
Ventana 3:  Año 2021 ──→ Año 2024 ──→
TOTAL: ~10 minutos (3x más rápido)
```

---

## 📝 Cambios Realizados

| Cambio | Ubicación |
|--------|-----------|
| Imports paralelos | Línea 26-29 |
| MAX_THREADS = 3 | Línea 31 |
| buscarPersona() refactorizado | Línea 75-139 |
| Nuevo buscarPorNombreParalelo() | Línea 552-747 |

---

## 🔨 Configuración (Si necesitas cambiar)

**Cambiar número de threads:**
```java
// Línea 31 en PjudScraper.java
private static final int MAX_THREADS = 3; // Cambiar este número
```

| Valor | Efecto |
|-------|--------|
| 2 | Más conservador, menos riesgo bloqueo |
| 3 | Recomendado (actual) |
| 4+ | Más rápido pero mayor riesgo bloqueo PJUD |

---

## 🚀 Ejecutar

```bash
# Terminal 1: Inicia aplicación
./mvnw spring-boot:run

# Terminal 2: Busca personas (en otra terminal)
curl http://localhost:8080/api/buscar-personas

# Verás en los logs:
# ▶ [THREAD pool-1-thread-1] Procesando año: 2019
# ▶ [THREAD pool-1-thread-2] Procesando año: 2020
# ▶ [THREAD pool-1-thread-3] Procesando año: 2021
```

---

## 📊 Qué esperar

- ✅ 3 ventanas Chromium abiertas simultáneamente
- ✅ Logs de cada año en paralelo
- ✅ CSV final con todos los resultados
- ✅ 3x más rápido que antes

---

## ⚠️ Límites

- **Máximo 3 threads:** Por seguridad (evitar bloqueo PJUD)
- **Cada thread:** Propia ventana Chromium
- **Memoria:** ~200MB por thread (total ~600MB)
- **Resultado final:** Un único CSV consolidado

---

## 📚 Documentación Completa

Ver: `PARALELISMO_3_THREADS.md`

---

**¡Listo para usar!** ⚡
