# 📋 CAMBIOS IMPLEMENTADOS - FILTRADO DE PERSONAS PENDIENTES

## Resumen
Se ha implementado un sistema inteligente de filtrado que permite al bot procesar solo personas que realmente necesitan ser procesadas, optimizando tiempo y recursos.

## Cambios Realizados

### 1. **Repositorio PersonaProcesadaRepository**
**Archivo:** `src/main/java/com/example/veritusbot/repository/PersonaProcesadaRepository.java`

**Cambio:** Se simplificó el repositorio para usar filtrado en Java en lugar de queries complejas SQL.

```java
// Antes: Query JPQL compleja que causaba errores
// Ahora: Método simple que retorna todas las personas
List<PersonaProcesada> findAll();
```

### 2. **Scraper - Método buscarPersonasDelExcel()**
**Archivo:** `src/main/java/com/example/veritusbot/scraper/PjudScraper.java`

**Cambios principales:**

#### a) Sincronización de personas
- Lee personas del Excel
- Crea registros en `personas_procesadas` si no existen

#### b) Filtrado de personas pendientes
El bot ahora filtra automáticamente y solo procesa:
- **Personas con `procesado = 0`** (nunca procesadas)
- **Personas con `procesado = 1` hace más de 6 meses** (reproceso automático)

```java
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
```

#### c) Método auxiliar obtenerPersonaDelExcel()
Mapea datos de `PersonaProcesada` con información del Excel para obtener los años de búsqueda.

#### d) Procesamiento optimizado
El bot ahora:
1. Lee solo personas pendientes
2. Obtiene años de búsqueda desde el Excel
3. Procesa la búsqueda
4. Marca como procesado con timestamp

## Flujo de Ejecución

```
┌─────────────────────────────────────────────────────┐
│  1. Leer archivo Excel (personas.csv)               │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  2. Sincronizar con DB (crear nuevos registros)     │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  3. Filtrar personas pendientes:                    │
│     ✓ procesado = 0 (nunca procesadas)              │
│     ✓ procesado = 1 hace más de 6 meses             │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  4. Para cada persona pendiente:                    │
│     • Obtener años de búsqueda del Excel            │
│     • Ejecutar búsqueda en web                      │
│     • Guardar resultados en DB y CSV                │
│     • Marcar como procesado = 1                     │
│     • Registrar fecha_procesada = NOW()             │
└─────────────────────────────────────────────────────┘
```

## Ejemplo de Ejecución

```
📊 Personas pendientes de procesar: 2
   (procesado=0 O procesado=1 pero más de 6 meses)
  • Miguel Soto (primera búsqueda)
  • Juan Rodriguez (reproceso - procesado el 2025-08-27T10:30:00)

════════════════════════════════════════════════════════════
BUSCANDO PERSONA 1/2 | Progreso: 0.0%
════════════════════════════════════════════════════════════
[búsqueda...]
✅ CLIENTE 1 COMPLETADO
   Tiempo de búsqueda: 2m 45s
   Progreso total: 50.0% (1/2)

════════════════════════════════════════════════════════════
BUSCANDO PERSONA 2/2 | Progreso: 50.0%
════════════════════════════════════════════════════════════
[búsqueda...]
✅ CLIENTE 2 COMPLETADO
   Tiempo de búsqueda: 3m 12s
   Progreso total: 100.0% (2/2)

BÚSQUEDA COMPLETADA
Hora de inicio: 10:30:45
Hora de fin:    17:15:30
Tiempo total:   6h 44m 45s
Total personas procesadas: 2
✅ TODAS LAS PERSONAS PENDIENTES HAN SIDO PROCESADAS
```

## Comportamiento de la Base de Datos

### Tabla `personas_procesadas`

| Campo | Descripción |
|-------|-------------|
| `procesado` | 0 = No procesada, 1 = Procesada |
| `fecha_procesada` | Fecha y hora del último procesamiento |

### Lógica de Reproceso

- **Primera ejecución:** Se procesan personas con `procesado = 0`
- **Ejecutiones posteriores:** 
  - Se procesan nuevas personas con `procesado = 0`
  - Se reprocesa si: `procesado = 1` AND `fecha_procesada <= (HOY - 6 meses)`

## Ventajas

✅ **Optimización:** No reprocesa personas innecesariamente
✅ **Actualización automática:** Reprocesa después de 6 meses
✅ **Escalabilidad:** Funciona con múltiples ejecutiones
✅ **Trazabilidad:** Registro de cuándo se procesó cada persona
✅ **Robustez:** Filtrado en Java evita problemas de queries SQL

## Notas Técnicas

- El filtrado se hace en **Java** (no SQL) para evitar problemas de compatibilidad con diferentes BD
- Se usa `LocalDateTime` para cálculos de fechas
- El método `minusMonths(6)` calcula exactamente 6 meses atrás
- Compatible con SQL Server, MySQL, PostgreSQL, etc.

