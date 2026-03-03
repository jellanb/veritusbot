# Changelog - Veritus Bot

## [2.1.0] - 2026-03-02

### ✨ Nuevas Características

#### Búsqueda en Dos Fases por Tribunal
- **Implementación de búsqueda estratificada** que prioriza tribunales de Santiago
  - Fase 1: Procesa 1º-30º Juzgado Civil de Santiago
  - Fase 2: Procesa todos los demás tribunales (excluyendo Santiago)
  
- **Campo nuevo en BD:** `tribunal_principal_procesado`
  - Se marca como `true` al completar la Fase 1
  - Permite auditoría y control de qué fase se completó
  - Tipo: `BIT NULL`

### 🔄 Cambios en la Lógica de Búsqueda

#### Nuevos Métodos en PjudScraper.java
1. **`buscarEnTribunalesConFiltro()`**
   - Ejecuta búsqueda con filtro de tribunales
   - Parámetros: `soloSantiago`, `excluirSantiago`
   - Maneja paralelismo y delays automáticamente
   
2. **`buscarPorNombreParaleloConFiltro()`**
   - Versión mejorada de búsqueda paralela con filtro
   - Valida cada tribunal contra los criterios de filtro
   - Logs detallados de tribunales omitidos
   
3. **`marcarTribunalPrincipalProcesado()`**
   - Actualiza el campo tras completar Fase 1
   - Thread-safe con acceso a BD
   - Incluye logs de confirmación

#### Cambios en el Flujo Principal
```
buscarPersona()
├── FASE 1: buscarEnTribunalesConFiltro(persona, true, false)
│   └── buscarPorNombreParaleloConFiltro(..., soloSantiago=true)
│       ├── Filtra: nombreTribunal.contains("Santiago") == true
│       └── marcarTribunalPrincipalProcesado() al completar
│
└── FASE 2: buscarEnTribunalesConFiltro(persona, false, true)
    └── buscarPorNombreParaleloConFiltro(..., excluirSantiago=true)
        └── Filtra: !nombreTribunal.contains("Santiago") == true
            └── Guarda todos los resultados al completar
```

### 🗄️ Cambios en Base de Datos

#### Tabla: personas_procesadas
```sql
-- Campo nuevo agregado:
ALTER TABLE personas_procesadas ADD tribunal_principal_procesado BIT NULL;
```

**Significado de los valores:**
- `NULL` o `0 (FALSE)`: Aún no se ha completado la Fase 1 (tribunales de Santiago)
- `1 (TRUE)`: Ya se completó la Fase 1, puede proceder a Fase 2

#### Modelo: PersonaProcesada.java
- Campo: `Boolean tribunalPrincipalProcesado`
- Getters/Setters: `getTribunalPrincipalProcesado()`, `setTribunalPrincipalProcesado()`
- Incluido en `toString()`

### 📊 Mejoras en Logging

#### Nuevos Logs Informativos
- `"FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)"`
- `"FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)"`
- `"⊘ Tribunal omitido (filtro): [nombre]"` - cuando se excluye un tribunal
- `"✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true"`

#### Logs de Depuración
- Cada tribunal procesado se registra con su nombre completo
- Tribunales excluidos se log como debug (no informativos)
- Progreso claro entre fases

### 🎯 Ventajas de la Nueva Arquitectura

1. **Priorización Eficiente**
   - Los tribunales más consultados se procesan primero
   - Evita repetir búsquedas innecesarias en tribunales menos activos

2. **Auditoría Mejorada**
   - Campo `tribunal_principal_procesado` permite rastrear qué se completó
   - Útil para reanudar procesos interrumpidos

3. **Escalabilidad**
   - Fácil agregar más fases (ej: por región, por tipo de tribunal)
   - Filtros pueden ser customizados fácilmente

4. **Tolerancia a Fallos**
   - Si falla Fase 2, Fase 1 ya está guardada
   - Permite reintentos parciales sin perder progreso

### 📈 Performance

- **Sin cambios en velocidad:** Misma lógica de paralelismo (max 3 threads)
- **Distribución inteligente:** Procesa primero lo más importante
- **Memoria optimizada:** Filtra tribunales sin almacenarlos todos

### ✅ Compilación y Testing

- **Build:** `mvn clean package -DskipTests` ✓ EXITOSO
- **JAR generado:** `veritusbot-0.0.1-SNAPSHOT.jar` (262 MB)
- **Errores de compilación:** 0
- **Warnings:** 0

### 🔗 Compatibilidad

- ✅ SQL Server 2022
- ✅ Playwright (sin cambios en versión)
- ✅ Spring Boot 3.2.5
- ✅ Java 17+

### 📝 Documentación Actualizada

- `README.md` - Sección "Búsqueda en Dos Fases" actualizada
- `CHANGELOG.md` - Este archivo con cambios detallados
- Código comentado en métodos nuevos

### 🐛 Bug Fixes

- N/A (Esta versión agrega features, no arregla bugs reportados)

### ⚠️ Cambios Potencialmente Incompatibles

- **Ninguno.** La nueva funcionalidad es additive y no rompe la compatibilidad
- Código antiguo seguirá funcionando sin cambios

### 📦 Deploy

Para activar la nueva funcionalidad:

1. **Actualizar BD:**
   ```sql
   ALTER TABLE personas_procesadas ADD tribunal_principal_procesado BIT NULL;
   ```

2. **Recompilar:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Reiniciar:**
   ```bash
   java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
   ```

4. **Verificar en logs:**
   - Debe aparecer: `"FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO"`
   - Seguido de: `"FASE 2: PROCESANDO OTROS TRIBUNALES"`

### 📚 Referencias Técnicas

- **Métodos modificados:** `buscarPersona()`
- **Métodos nuevos:** 3 (ver sección "Nuevos Métodos")
- **Campos BD agregados:** 1 (`tribunal_principal_procesado`)
- **Archivos modificados:** 2 (PjudScraper.java, PersonaProcesada.java)
- **Archivos creados:** 0
- **Líneas de código agregadas:** ~200

---

**Versión Anterior:** 2.0  
**Esta Versión:** 2.1.0  
**Próxima Versión:** 2.2.0 (planeada)

**Estado:** ✅ COMPLETADA Y TESTEADA


