# 🔍 DEBUG DETALLADO - Guía de Logs

## ✅ Cambios Implementados

Se ha reemplazado completamente la sección de obtención de tribunales con:

1. **JavaScript que devuelve un ARRAY** en lugar de un objeto
   - Mejor compatibilidad con conversión a Java
   - Devuelve: `[{index: 2, nombre: "..."}, {index: 3, nombre: "..."}, ...]`

2. **Tres STEPS claramente diferenciados**
   - STEP 1: Ejecutar JavaScript
   - STEP 2: Convertir resultado a Map
   - STEP 3: Verificar resultado final

3. **Debug detallado en cada paso**
   - Muestra tipo de objeto retornado
   - Muestra conversión de cada elemento
   - Muestra primeros y últimos tribunales en Map

4. **Debug en el loop de iteración**
   - Muestra índice procesado
   - Muestra si el valor es NULL o vacío
   - Muestra keys cercanas en el Map si hay problema

## 📊 Flujo de Logs Esperado

### Fase 1: Abrir Dropdown
```
✓ Se encontraron tribunales con índices de 1 a 231
```

### Fase 2: Obtener Lista de Tribunales (NUEVA SECCIÓN)
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
    Keys en map: [index, nombre]
    index=1 (type: Double)
    nombre=Seleccione Tribunal... (type: String)
    ✓ Agregado al Map: 1 = Seleccione Tribunal...
  Item 1: tipo=HashMap
    Keys en map: [index, nombre]
    index=2 (type: Double)
    nombre=1º Juzgado de Letras de Arica (type: String)
    ✓ Agregado al Map: 2 = 1º Juzgado de Letras de Arica
  Item 2: tipo=HashMap
    Keys en map: [index, nombre]
    index=3 (type: Double)
    nombre=1º Juzgado De Letras de Arica ex 4° (type: String)
    ✓ Agregado al Map: 3 = 1º Juzgado De Letras de Arica ex 4°
  ... (más items)
  Item 230: tipo=HashMap
    ...

📍 STEP 3: Verificando resultado final...
  Total de tribunales en Map: 231
  ✓ Mapa contiene datos
    [1] = Seleccione Tribunal...
    [2] = 1º Juzgado de Letras de Arica
    [3] = 1º Juzgado De Letras de Arica ex 4°
    [4] = 2º Juzgado de Letras de Arica
    [5] = 2º Juzgado De Letras de Arica ex 4°
    ... 226 más

✓ Se obtuvieron 231 tribunales
🔍 ════════════════════════════════════════════
```

### Fase 3: Iteración (CON MÁS DEBUG)
```
🔄 ════════════════════════════════════════════
🔄 INICIANDO ITERACIÓN POR TRIBUNALES
🔄 ════════════════════════════════════════════
Total a procesar: 230 tribunales (índices 2 a 231)

▶ Procesando índice: 2
  Obtenido del Map: 1º Juzgado de Letras de Arica
  
=== Buscando en tribunal (1/230): 1º Juzgado de Letras de Arica ===
✓ Botón del dropdown encontrado y presionado
✓ Dropdown verificado como abierto
  🔍 Seleccionando tribunal con índice: 2
  ✓ Elemento encontrado, haciendo click...
  ✓ Click ejecutado en tribunal con índice 2
✓ Botón de búsqueda presionado para: 1º Juzgado de Letras de Arica
✗ Sin resultados para: 1º Juzgado de Letras de Arica

▶ Procesando índice: 3
  Obtenido del Map: 1º Juzgado De Letras de Arica ex 4°
  
=== Buscando en tribunal (2/230): 1º Juzgado De Letras de Arica ex 4° ===
✓ Botón del dropdown encontrado y presionado
✓ Dropdown verificado como abierto
  🔍 Seleccionando tribunal con índice: 3
  ✓ Elemento encontrado, haciendo click...
  ✓ Click ejecutado en tribunal con índice 3
✓ Botón de búsqueda presionado para: 1º Juzgado De Letras de Arica ex 4°
✓ Se encontraron 2 causas en: 1º Juzgado De Letras de Arica ex 4°

... (continúa con los 228 tribunales restantes)
```

## 🔴 Posibles Errores y Qué Significan

### Error 1: "MAPA VACÍO"
```
📍 STEP 3: Verificando resultado final...
  Total de tribunales en Map: 0
  ✗ ¡¡¡ MAPA VACÍO !!! Este es el problema
  Valor retornado por JS: [...]
  Tipo: java.util.ArrayList
```

**Significado:** El JavaScript devolvió una lista, pero todos los elementos son `null` o no se pudieron convertir.

**Causa probable:** El dropdown se cerró ANTES de ejecutar el JavaScript, y no encontró elementos `[data-original-index]`.

### Error 2: "nombreTribunal ES NULL"
```
▶ Procesando índice: 5
  Obtenido del Map: null
  ✗ nombreTribunal ES NULL
  Map contiene estas keys: [4, 5, 6]
  ⚠ No se encontró información del tribunal en índice: 5
```

**Significado:** El índice 5 está en el Map como KEY, pero su VALUE es NULL.

**Causa probable:** El JavaScript encontró el elemento `[data-original-index="5"]` pero no encontró `span.text` dentro de él.

### Error 3: "No es una List"
```
📍 STEP 2: Convirtiendo resultado a Map...
  Tipo de objeto recibido: HashMap
  ✗ NO es una List, es: java.util.HashMap
```

**Significado:** El JavaScript devolvió un HashMap en lugar de un Array.

**Causa probable:** El navegador o Playwright está optimizando el retorno.

## 🎯 Qué Verificar Si Hay Problemas

### Si STEP 1 falla o devuelve tipo inesperado
- El JavaScript no ejecutó correctamente
- El dropdown se cerró antes de ejecutar

### Si STEP 2 muestra "MAPA VACÍO"
- La List tiene 0 elementos o todos son null
- Los elementos `[data-original-index]` no estaban en el DOM

### Si STEP 3 muestra menos tribunales que lo esperado
- Algunos elementos no tenían `span.text`
- Algunos índices no se pudieron parsear

### Si en la iteración hay "nombreTribunal ES NULL"
- El Map fue creado pero falta ese índice específico
- Revisa qué índices tiene en los "keys cercanas"

## 📝 Cómo Ejecutar y Capturar Logs

```bash
cd /Users/jellan/Documents/git/veritusbot

# Ejecutar y guardar todos los logs
./mvnw spring-boot:run 2>&1 | tee bot_debug.log

# Ver solo la parte de obtención de tribunales
grep -A 200 "OBTENCIÓN DE LISTA" bot_debug.log

# Ver solo errores y warnings
grep -E "^✗|✓ Se obtuvieron|MAPA VACÍO|nombreTribunal" bot_debug.log
```

## ✅ Validación

Si ves esto, el problema está RESUELTO:

```
✓ Se obtuvieron 231 tribunales
🔍 ════════════════════════════════════════════

🔄 ════════════════════════════════════════════
🔄 INICIANDO ITERACIÓN POR TRIBUNALES
🔄 ════════════════════════════════════════════
```

Y después:

```
▶ Procesando índice: 2
  Obtenido del Map: 1º Juzgado de Letras de Arica

=== Buscando en tribunal (1/230): 1º Juzgado de Letras de Arica ===
```

Si ves esto, el Map tiene datos y se está iterando correctamente.

## 🚀 Ejecuta Ahora

```bash
./mvnw spring-boot:run 2>&1 | tee bot_debug.log
```

Y comparte los logs de las secciones:
- `OBTENCIÓN DE LISTA DE TRIBUNALES`
- `STEP 1`
- `STEP 2`
- `STEP 3`

Esto nos ayudará a identificar exactamente dónde falla la conversión del objeto JavaScript a Map en Java.
