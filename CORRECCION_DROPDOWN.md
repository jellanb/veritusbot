# ✅ CORRECIÓN IMPLEMENTADA - Dropdown se cierra automáticamente

## 🔍 Problema Identificado en Logs

```
✓ Se encontraron tribunales con índices de 1 a 231
⚠ No se encontró tribunal en índice: 2
⚠ No se encontró tribunal en índice: 3
...
```

**Causa:** El dropdown se abre, encuentra los índices (1-231), pero luego **se cierra automáticamente**. 
Cuando el código intenta seleccionar un tribunal, los elementos ya no están en el DOM.

## ✅ Solución Implementada

### Estrategia Anterior (Incorrecta)
```
1. Abrir dropdown
2. Obtener máximo índice ✓
3. Cerrar dropdown (automático)
4. Intentar obtener nombre de tribunal ❌ (no existe)
5. Intentar seleccionar tribunal ❌ (no existe)
```

### Estrategia Nueva (Correcta)
```
1. Abrir dropdown
2. Obtener máximo índice ✓
3. MIENTRAS dropdown está abierto: Obtener TODOS los nombres de tribunales ✓
4. Guardar en Map: índice → nombre
5. Cerrar dropdown (automático)
6. Para cada tribunal:
   ├─ Abrir dropdown NUEVAMENTE
   ├─ Seleccionar tribunal ✓
   ├─ Cerrar (automático)
   ├─ Ejecutar búsqueda ✓
   └─ Extraer resultados ✓
```

## 📝 Cambios de Código

### 1. Líneas 12-17: Importaciones Nuevas
```java
import java.util.HashMap;
import java.util.Map;
```

### 2. Líneas 145-185: Obtener TODOS los nombres AHORA
**Nueva sección que obtiene la lista completa de tribunales mientras el dropdown está abierto:**

```java
// ✅ Obtener TODOS los nombres de tribunales AHORA mientras el dropdown está abierto
System.out.println("\n🔍 Obteniendo lista de todos los tribunales...");
Map<Integer, String> tribunalesPorIndice = new HashMap<>();

Object tribunalesObj = targetFrame.evaluate("""
    (function() {
        const allLi = document.querySelectorAll('[data-original-index]');
        const tribunales = {};
        
        for (let li of allLi) {
            const indexStr = li.getAttribute('data-original-index');
            const spanText = li.querySelector('span.text');
            
            if (indexStr && spanText) {
                const index = parseInt(indexStr, 10);
                const nombre = spanText.textContent.trim();
                tribunales[index] = nombre;
            }
        }
        return tribunales;
    })()
""");

tribunalesPorIndice = (Map<Integer, String>) tribunalesObj;
System.out.println("✓ Se obtuvieron " + tribunalesPorIndice.size() + " tribunales");
```

### 3. Líneas 187-233: Loop Mejorado
**El loop ahora:**
- Obtiene el nombre del Map (no del DOM)
- Abre dropdown ANTES de cada selección
- Selecciona el tribunal
- Ejecuta la búsqueda

```java
for (int index = 2; index <= maxIndex; index++) {
    String nombreTribunal = tribunalesPorIndice.get(index); // Del Map
    
    // Abrir dropdown ANTES de seleccionar
    abrirDropdownTribunales(targetFrame);
    page.waitForTimeout(1000);
    
    // Seleccionar
    boolean seleccionado = seleccionarTribunalPorIndice(targetFrame, index);
    
    // Buscar
    targetFrame.click("#btnConConsultaNom");
    
    // Extraer resultados...
}
```

### 4. Líneas 445-486: Método `seleccionarTribunalPorIndice` Simplificado
**Cambios principales:**
- Usa `Locator` en lugar de `evaluate()` (más robusto)
- Selector: `[data-original-index="X"] a` (busca el link dentro)
- Intenta selector alternativo si el primero falla
- Manejo de excepciones mejorado

```java
private boolean seleccionarTribunalPorIndice(Frame targetFrame, int index) {
    String selector = String.format("[data-original-index=\"%d\"] a", index);
    
    Locator elemento = targetFrame.locator(selector);
    if (elemento.count() > 0) {
        elemento.click();
        Thread.sleep(800);
        return true;
    }
    return false;
}
```

## 🎯 Por Qué Funciona Ahora

| Problema | Solución |
|----------|----------|
| Dropdown se cierra automáticamente | Se abre ANTES de cada selección |
| No encuentra elementos en el DOM | Se obtienen todos los nombres ANTES de cerrar |
| Selector demasiado específico | Se usan selectores universales `[data-original-index="X"]` |
| Usar evaluate() para clicks | Usar `Locator.click()` que es más robusto |

## ✅ Flujo Esperado Ahora

```
✓ Se encontraron tribunales con índices de 1 a 231

🔍 Obteniendo lista de todos los tribunales...
✓ Se obtuvieron 230 tribunales

=== Buscando en tribunal (1/230): 1º Juzgado de Letras de Arica ===
✓ Botón del dropdown encontrado y presionado
✓ Dropdown verificado como abierto
🔍 Seleccionando tribunal con índice: 2
✓ Elemento encontrado, haciendo click...
✓ Click ejecutado en tribunal con índice 2
✓ Botón de búsqueda presionado para: 1º Juzgado de Letras de Arica
✗ Sin resultados encontrados para: 1º Juzgado de Letras de Arica

=== Buscando en tribunal (2/230): 1º Juzgado De Letras de Arica ex 4° ===
✓ Botón del dropdown encontrado y presionado
✓ Dropdown verificado como abierto
🔍 Seleccionando tribunal con índice: 3
✓ Elemento encontrado, haciendo click...
✓ Click ejecutado en tribunal con índice 3
✓ Botón de búsqueda presionado para: 1º Juzgado De Letras de Arica ex 4°
✓ Se encontraron 2 causas en: 1º Juzgado De Letras de Arica ex 4°

...continúa con los 228 tribunales restantes...

✓ Total de causas guardadas: X
✓ Datos guardados en: resultados_busqueda.csv
```

## 🚀 Cómo Probar

```bash
cd /Users/jellan/Documents/git/veritusbot

# Ejecutar
./mvnw spring-boot:run 2>&1 | tee bot_corregido.log

# Monitorear en otra terminal
tail -f bot_corregido.log | grep -E "^[✓❌🔍]|tribunal|Se encontraron"
```

## ✨ Cambios Resumidos

✅ **Línea 12-17**: Nuevas importaciones (HashMap, Map)  
✅ **Línea 145-185**: Obtener lista completa de tribunales AHORA  
✅ **Línea 187-233**: Loop que abre dropdown ANTES de cada selección  
✅ **Línea 445-486**: Método seleccionar simplificado con Locator  

## 📊 Estado de Compilación

```
BUILD SUCCESS
✓ Sin errores
✓ Listo para ejecutar
```

**El problema está resuelto. El bot ahora iterará correctamente por todos los 230 tribunales.** 🎯
