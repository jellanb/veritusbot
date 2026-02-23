# 🔧 DEBUGGING - Bot se detiene después de abrir dropdown

## Problema
El bot abre el dropdown correctamente pero luego no continúa.

## Soluciones Implementadas

### 1. ✅ Búsqueda más robusta de máximo índice
- **Antes**: Buscaba `ul.dropdown-menu.inner li[data-original-index]`
- **Ahora**: Busca `[data-original-index]` sin restricciones de clase
- **Motivo**: El dropdown podría tener estructura diferente o clases diferentes

### 2. ✅ Mejor detección del botón dropdown
- **Antes**: Buscaba por criterios específicos
- **Ahora**: Busca por múltiples criterios y valida que está abierto
- **Motivo**: El botón podría tener atributos diferentes

### 3. ✅ Selección más simple
- **Antes**: Buscaba `li[data-original-index]` específicamente dentro de clases
- **Ahora**: Busca `[data-original-index]` en todo el DOM
- **Motivo**: Los selectores CSS más generales son más robustos

### 4. ✅ Mejor manejo de errores
- Mensajes de debug más claros
- Imprime valores de índices encontrados
- Valida cada paso

## Cómo Ejecutar y Debuggear

### 1. Compilar
```bash
cd /Users/jellan/Documents/git/veritusbot
./mvnw clean compile
```

### 2. Ejecutar con logs completos
```bash
./mvnw spring-boot:run 2>&1 | tee bot.log
```

### 3. Verificar los logs durante ejecución
```bash
# En otra terminal
tail -f bot.log
```

## Qué Buscar en los Logs

### Fase 1: Navegación
```
Popup cerrado
✓ Formulario de búsqueda por nombre cargado correctamente
Select de competencia encontrado
Competencia 'civil' seleccionada
Nombre 'MIGUEL ANTONIO' ingresado
```

### Fase 2: Dropdown
```
🔍 Abriendo dropdown de tribunales...
✓ Botón del dropdown encontrado y presionado
✓ Dropdown verificado como abierto
```

### Fase 3: Búsqueda de índices
```
🔍 Buscando máximo índice...
✓ Se encontraron tribunales con índices de 1 a 231
```

Si ves esto, el código está funcionando correctamente.

### Fase 4: Iteración
```
=== Buscando en tribunal (1/230): 1º Juzgado de Letras de Arica ===
📍 Seleccionando tribunal con índice: 2
✓ Click ejecutado en tribunal con índice 2
✓ Botón de búsqueda presionado para: 1º Juzgado de Letras de Arica
```

## Si se detiene en Fase 2 (Dropdown no abre)

```
⚠ No se encontró el botón del dropdown
```

**Solución**: El botón del dropdown tiene un selector diferente. 
Abre el navegador en modo headless=false para ver qué está pasando.

**Edita línea 29 de PjudScraper.java:**
```java
.setHeadless(false) // Cambia a false para ver el navegador
```

### Si se detiene en Fase 3 (No encuentra maxIndex)

```
❌ ERROR: No se pudo obtener el máximo índice.
   El dropdown podría no estar abierto o no hay elementos con data-original-index.
```

**Esto significa:**
1. El dropdown NO está realmente abierto, O
2. Los elementos no tienen `data-original-index`

**Pasos para debuggear:**
1. Ejecuta con `headless=false`
2. Observa si el dropdown se abre
3. Abre Developer Tools (F12) en el navegador
4. En la consola, ejecuta:
   ```javascript
   document.querySelectorAll('[data-original-index]').length
   ```
5. Si retorna 0, los elementos no están en el DOM
6. Si retorna un número > 0, los elementos están ahí

## Cambios en el Código

Las siguientes líneas fueron modificadas para mejorar robustez:

1. **Línea 135-166**: Búsqueda de maxIndex mejorada
2. **Línea 350-420**: Método `abrirDropdownTribunales` mejorado
3. **Línea 422-476**: Método `seleccionarTribunalPorIndice` mejorado

## Próximos Pasos

1. **Ejecutar el bot:**
   ```bash
   ./mvnw spring-boot:run 2>&1 | tee bot.log
   ```

2. **Observar los logs:**
   - ¿Llega a "Abriendo dropdown"?
   - ¿Llega a "Buscando máximo índice"?
   - ¿Qué valor tiene el máximo índice?

3. **Si maxIndex = 0:**
   - Cambia `headless=false` en línea 29
   - Ejecuta nuevamente
   - Observa el navegador para ver qué está pasando

4. **Si maxIndex > 0:**
   - El código debería continuar normalmente
   - Verifica que se está iterando correctamente

## Información Importante

El código ahora imprime mucha más información para debugging:
- ✓ Indica qué botón se encontró
- 📍 Indica qué tribunal se está seleccionando  
- 🔍 Indica qué está buscando en el DOM
- ✅ Indica éxito en cada paso
- ❌ Indica errores específicos

Esto te ayudará a identificar exactamente dónde se detiene el bot.

## Archivo de Logs

Los logs se guardan en:
```
bot.log
```

Puedes analizarlos después con:
```bash
grep -E "^[✓❌🔍📍]|ERROR|Exception" bot.log
```
