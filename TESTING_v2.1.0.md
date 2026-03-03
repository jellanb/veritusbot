# 🧪 Testing Guide - Búsqueda en Dos Fases v2.1.0

## 📋 Pre-requisitos de Testing

- [ ] SQL Server está corriendo
- [ ] BD `veritus` está actualizada (con campo `tribunal_principal_procesado`)
- [ ] Aplicación compilada: `mvn clean package -DskipTests`
- [ ] Archivo `personas.csv` en la raíz del proyecto
- [ ] Acceso a internet (para oficinajudicialvirtual.pjud.cl)

## 📝 Test 1: Validar Estructura de BD

### Objetivo
Verificar que la nueva columna se creó correctamente.

### Pasos
1. Conectar a SQL Server
2. Ejecutar:
```sql
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'personas_procesadas'
ORDER BY ORDINAL_POSITION;
```

### Validación ✅
Debe aparecer `tribunal_principal_procesado` con tipo `bit` e `IS_NULLABLE = YES`

---

## 📝 Test 2: Búsqueda Completa con Dos Fases

### Objetivo
Verificar que la búsqueda ejecuta correctamente ambas fases.

### Precondiciones
- Aplicación iniciada
- BD conectada
- `personas.csv` con al menos 1 persona

### Pasos
1. Iniciar aplicación:
   ```bash
   java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
   ```

2. Esperar a que se inicie correctamente:
   ```
   ✓ Conexión a BD: OK
   ✓ API disponible en: http://localhost:8083
   ```

3. Ejecutar búsqueda:
   ```bash
   curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"
   ```

### Validación ✅
En los logs, debe aparecer (en orden):
```
INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL
✓ Personas sincronizadas: 1
BUSCANDO PERSONA 1/1

FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)
📅 Año 2019 - Se iniciará en: 0 segundos
⊘ Tribunal omitido (filtro): 1º Juzgado de Letras de Arica
✓ Tribunal encontrado en Santiago: 1º Juzgado Civil de Santiago
✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true

FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)
📅 Año 2020 - Se iniciará en: 3 segundos
✓ Tribunal encontrado (no Santiago): 1º Juzgado de Letras de Arica
⊘ Tribunal omitido (filtro): 1º Juzgado Civil de Santiago

BÚSQUEDA COMPLETADA
```

---

## 📝 Test 3: Verificar Campo tribunal_principal_procesado

### Objetivo
Validar que el campo se actualiza correctamente después de Fase 1.

### Pasos
1. Ejecutar búsqueda (Test 2)
2. Esperar a que complete Fase 1 completamente
3. Verificar en BD:
   ```sql
   SELECT 
       primer_nombre,
       apellido_paterno,
       procesado,
       tribunal_principal_procesado,
       fecha_procesada
   FROM personas_procesadas
   WHERE primer_nombre = 'MIGUEL'; -- Ajustar según persona buscada
   ```

### Validación ✅
Resultados esperados:
```
primer_nombre  apellido_paterno  procesado  tribunal_principal_procesado  fecha_procesada
MIGUEL         SOTO              1          1                             2026-03-02 11:30:45
```

**Significado:**
- `procesado = 1` → Búsqueda completada
- `tribunal_principal_procesado = 1` → Fase 1 completada ✓
- `fecha_procesada` → Fecha y hora de completar

---

## 📝 Test 4: Filtrado de Tribunales Santiago

### Objetivo
Verificar que Fase 1 solo procesa tribunales de Santiago.

### Setup
Agregar logs al código (temporal):
```java
// En buscarPorNombreParaleloConFiltro()
if (soloSantiago) {
    logger.info("[DEBUG] Tribunal: %s | Contiene Santiago: %s | Incluir: %s",
        nombreTribunal, 
        nombreTribunal.contains("Santiago"),
        incluirTribunal);
}
```

### Pasos
1. Ejecutar búsqueda
2. Revisar logs durante Fase 1
3. Contar tribunales procesados

### Validación ✅
En Fase 1 deben procesarse SOLO tribunales que contengan "Santiago" en el nombre:
- ✓ 1º Juzgado Civil de Santiago
- ✓ 2º Juzgado Civil de Santiago
- ✓ ... hasta ...
- ✓ 30º Juzgado Civil de Santiago
- ✗ 1º Juzgado de Letras de Arica (omitido)
- ✗ 1º Juzgado Civil de Valparaíso (omitido)

---

## 📝 Test 5: Filtrado de Tribunales No-Santiago

### Objetivo
Verificar que Fase 2 procesa TODOS excepto los de Santiago.

### Pasos
1. Ejecutar búsqueda
2. Revisar logs durante Fase 2
3. Verificar que NO aparecen tribunales de Santiago

### Validación ✅
En Fase 2 deben procesarse todos EXCEPTO tribunales que contengan "Santiago":
- ✗ 1º Juzgado Civil de Santiago (omitido)
- ✗ 30º Juzgado Civil de Santiago (omitido)
- ✓ 1º Juzgado de Letras de Arica
- ✓ 1º Juzgado Civil de Valparaíso

---

## 📝 Test 6: Guardar en CSV y BD

### Objetivo
Verificar que los resultados se guardan SOLO después de completar Fase 2.

### Pasos
1. Ejecutar búsqueda
2. Verificar `resultados_busqueda.csv` durante Fase 1
3. Verificar `resultados_busqueda.csv` después de Fase 2

### Validación ✅
- **Durante Fase 1:** El archivo NO debe existir o estar vacío
- **Después de Fase 2:** El archivo debe contener todos los resultados
- **En BD:** Tabla `causas` debe tener registros de ambas fases

---

## 📝 Test 7: Logs Detallados de Progreso

### Objetivo
Validar que los logs muestren claramente qué fase se está ejecutando.

### Pasos
1. Iniciar búsqueda
2. Capturar todos los logs
3. Verificar la secuencia

### Validación ✅
Debe haber esta secuencia clara:
```
════════════════════════════════════════════════════════════
BUSCANDO PERSONA 1/1 | Progreso: 0.0%
════════════════════════════════════════════════════════════

FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)
[búsqueda en tribunales de Santiago]
✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true

FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)
[búsqueda en otros tribunales]

✓ Total de causas para este cliente: 15
```

---

## 📝 Test 8: Reproceso de Persona (6 Meses)

### Objetivo
Verificar que puede reprocesar personas después de 6 meses.

### Precondiciones
Persona previamente procesada con `fecha_procesada` hace más de 6 meses.

### Pasos
1. Modificar BD:
   ```sql
   UPDATE personas_procesadas 
   SET procesado = 1, 
       fecha_procesada = DATEADD(MONTH, -7, GETDATE()),
       tribunal_principal_procesado = NULL
   WHERE primer_nombre = 'MIGUEL';
   ```

2. Ejecutar búsqueda nuevamente

### Validación ✅
La persona debe ser procesada de nuevo:
- `procesado = 1` (sigue siendo 1)
- `tribunal_principal_procesado = 1` (se actualiza a 1)
- `fecha_procesada = GETDATE()` (se actualiza a fecha actual)

---

## 📝 Test 9: Performance - Paralelismo Intacto

### Objetivo
Verificar que los delays de 3 segundos entre navegadores sigue funcionando.

### Pasos
1. Ejecutar búsqueda con persona de años 2019-2022 (4 años)
2. Verificar en logs los tiempos de inicio

### Validación ✅
Debe aparecer:
```
📅 Año 2019 - Se iniciará en: 0 segundos   [inicia inmediatamente]
📅 Año 2020 - Se iniciará en: 3 segundos   [3 segundos después]
📅 Año 2021 - Se iniciará en: 6 segundos   [6 segundos después]
📅 Año 2022 - Se iniciará en: 9 segundos   [9 segundos después]
```

Máximo 2 navegadores simultáneos (MAX_THREADS = 2)

---

## 📝 Test 10: Manejo de Errores en Fase 1

### Objetivo
Verificar que si falla Fase 1, la Fase 2 aún se ejecuta.

### Setup
Forzar un error (temporal):
```java
// En buscarPorNombreParaleloConFiltro(), agregar:
if (soloSantiago && anio == 2020) {
    throw new Exception("Error simulado para testing");
}
```

### Pasos
1. Ejecutar búsqueda
2. Observar que Fase 1 falla para el año 2020
3. Verificar que Fase 2 aún se ejecuta

### Validación ✅
El flujo debe ser:
```
FASE 1: [falla en año 2020 pero continúa con 2019, 2021, etc.]
FASE 2: [ejecuta normalmente]
✓ Búsqueda completada parcialmente
```

---

## 📊 Test Report Template

```markdown
# Test Report - v2.1.0

Fecha: 2026-03-02
Tester: [Nombre]
Build: veritusbot-0.0.1-SNAPSHOT.jar

## Resultados

| Test | Status | Notas |
|------|--------|-------|
| Test 1: BD Structure | ✅ | Campo creado correctamente |
| Test 2: Dos Fases | ✅ | Ambas fases se ejecutan |
| Test 3: tribunal_principal_procesado | ✅ | Se actualiza a 1 |
| Test 4: Filtro Santiago | ✅ | Solo procesa Santiago en Fase 1 |
| Test 5: Filtro No-Santiago | ✅ | Procesa otros en Fase 2 |
| Test 6: CSV y BD | ✅ | Se guardan después de Fase 2 |
| Test 7: Logs | ✅ | Logs claros y detallados |
| Test 8: Reproceso 6M | ✅ | Funciona correctamente |
| Test 9: Performance | ✅ | Delays de 3s mantienen paralelismo |
| Test 10: Error Handling | ✅ | Continúa incluso con errores |

## Conclusión

✅ Todos los tests pasaron correctamente.
Versión v2.1.0 lista para producción.
```

---

## 🚀 Checklist Final

- [ ] Todos los tests completados
- [ ] No hay errores en compilación
- [ ] BD está actualizada
- [ ] Logs muestran ambas fases
- [ ] Campo `tribunal_principal_procesado` se actualiza
- [ ] Resultados se guardan correctamente
- [ ] Performance mantiene paralelismo
- [ ] Documentación actualizada

---

**Última actualización:** 2 Marzo 2026  
**Versión:** 2.1.0  
**Estado:** ✅ READY FOR TESTING


