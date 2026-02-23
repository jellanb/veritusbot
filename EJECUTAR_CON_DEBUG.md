# 🚀 INSTRUCCIONES FINALES - Ejecutar Con Debug

## ✅ Implementación Completada

Se han agregado **3 STEPS de debug detallado** en la sección crítica donde se obtienen los tribunales:

- **STEP 1**: Ejecutar JavaScript
- **STEP 2**: Convertir resultado a Map
- **STEP 3**: Verificar resultado final

Plus debug en la iteración que muestra:
- Índice procesado
- Valor obtenido del Map
- Si es NULL, muestra keys cercanas

## 🎯 Cómo Ejecutar

### Paso 1: Terminal
```bash
cd /Users/jellan/Documents/git/veritusbot
```

### Paso 2: Ejecutar Bot
```bash
./mvnw spring-boot:run 2>&1 | tee bot_debug.log
```

### Paso 3: Observar Logs

El bot imprimirá una sección que se ve así:

```
🔍 ════════════════════════════════════════════
🔍 INICIANDO OBTENCIÓN DE LISTA DE TRIBUNALES
🔍 ════════════════════════════════════════════

📍 STEP 1: Ejecutando JavaScript para obtener tribunales...
✓ JavaScript ejecutado, tipo retornado: java.util.ArrayList

📍 STEP 2: Convirtiendo resultado a Map...
  Tipo de objeto recibido: ArrayList
  ✓ Es una List con 231 elementos
  Item 0: tipo=HashMap
    ✓ Agregado al Map: 1 = Seleccione Tribunal...
  Item 1: tipo=HashMap
    ✓ Agregado al Map: 2 = 1º Juzgado de Letras de Arica
  ...

📍 STEP 3: Verificando resultado final...
  Total de tribunales en Map: 231
  ✓ Mapa contiene datos
    [1] = Seleccione Tribunal...
    [2] = 1º Juzgado de Letras de Arica
    ...

✓ Se obtuvieron 231 tribunales
```

## ✅ Señales de Que Todo Funciona

Busca estas líneas en los logs:

```
✓ Se obtuvieron 231 tribunales         ← El Map tiene 231 elementos
▶ Procesando índice: 2                 ← Comienza la iteración
  Obtenido del Map: 1º Juzgado...      ← El Map devuelve valores
```

Si ves esto, **el problema está resuelto** ✅

## ❌ Si Ves Algo Diferente

### Escenario 1: "Se obtuvieron 0 tribunales"
```
✓ Se obtuvieron 0 tribunales
📍 STEP 3: Verificando resultado final...
  Total de tribunales en Map: 0
  ✗ ¡¡¡ MAPA VACÍO !!! Este es el problema
```

**Significado**: El JavaScript ejecutó pero no encontró elementos.

**Causa probable**: El dropdown se cerró antes de ejecutar el JavaScript.

**Acción**: Los logs de STEP 1 y 2 te dirán exactamente qué sucedió.

### Escenario 2: "nombreTribunal ES NULL"
```
▶ Procesando índice: 5
  Obtenido del Map: null
  ✗ nombreTribunal ES NULL
  Map contiene estas keys: [4, 5, 6]
```

**Significado**: El índice está en el Map pero su valor es NULL.

**Causa probable**: El JavaScript encontró `[data-original-index="5"]` pero no encontró `span.text` dentro.

**Acción**: El JavaScript necesita mejorar su búsqueda de elementos.

## 📊 En Otra Terminal: Monitorear en Vivo

Mientras el bot está ejecutando, en otra terminal:

```bash
# Ver solo la sección de obtención de tribunales
tail -f bot_debug.log | grep -E "OBTENCIÓN|STEP|Se obtuvieron|MAPA VACÍO|nombreTribunal"

# O buscar línea por línea
tail -f bot_debug.log
```

## 📝 Archivos de Referencia

- `DEBUG_DETALLADO.md` - Explicación completa de todos los logs
- `IMPLEMENTACION_DEBUG_COMPLETADA.md` - Resumen de cambios

## 🎯 Validación Rápida

```bash
# Compilar primero
./mvnw clean compile -q

# Ejecutar y capturar primeros 50 líneas de la sección
timeout 120 ./mvnw spring-boot:run 2>&1 | grep -A 50 "OBTENCIÓN DE LISTA" | head -60
```

Esto mostrará rápidamente si el Map se está llenando o si está vacío.

## 🚀 Ahora Ejecuta

```bash
cd /Users/jellan/Documents/git/veritusbot
./mvnw spring-boot:run 2>&1 | tee bot_debug.log
```

El bot te mostrará exactamente dónde está el problema con debug detallado. ✅
