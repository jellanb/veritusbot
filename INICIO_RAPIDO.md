# 🎯 INICIO RÁPIDO - Bot de Búsqueda Judicial

## ✅ Verificación Previa

```bash
# 1. Verificar que estás en el directorio correcto
cd /Users/jellan/Documents/git/veritusbot

# 2. Verificar que Maven está instalado
./mvnw --version

# 3. Verificar que Java está instalado
java -version
```

## 📦 Compilar el Proyecto

```bash
# Compilar
./mvnw clean compile

# Esperado: BUILD SUCCESS (sin errores)
```

## 🚀 Ejecutar el Bot

### Opción 1: Desde Terminal
```bash
./mvnw spring-boot:run
```

### Opción 2: Desde el IDE
1. Abre el archivo `VeritusbotApplication.java`
2. Haz click derecho en la clase
3. Selecciona "Run" o presiona Shift+F10

## 📊 Ver los Resultados

Una vez que el bot termina:

```bash
# Ver el archivo CSV generado
cat resultados_busqueda.csv

# O abrirlo en Excel/Numbers/Google Sheets
open resultados_busqueda.csv
```

---

## ⚙️ Personalizar la Búsqueda

Edita el archivo `PjudScraper.java` en la línea 72:

```java
// ANTES
// Buscar MIGUEL ANTONIO SOTO FREDES año 2024

// DESPUÉS - Cambia estos valores:
targetFrame.fill("#nomNombre", "TU_NOMBRE");
targetFrame.fill("#nomSegNombre", "SEGUNDO_NOMBRE");
targetFrame.fill("#nomApPaterno", "APELLIDO_PATERNO");
targetFrame.fill("#nomApMaterno", "APELLIDO_MATERNO");
targetFrame.fill("#nomEra", "2024"); // Año
```

## 📈 Entender los Logs

Mientras el bot corre, verás mensajes como:

```
Se encontraron tribunales con índices de 1 a 231

=== Buscando en tribunal (1/230): 1º Juzgado de Letras de Arica ===
✓ Botón de búsqueda presionado para: 1º Juzgado de Letras de Arica
✗ Sin resultados encontrados para: 1º Juzgado de Letras de Arica

=== Buscando en tribunal (2/230): 1º Juzgado De Letras de Arica ex 4° ===
✓ Botón de búsqueda presionado para: 1º Juzgado De Letras de Arica ex 4°
✓ Se encontraron 2 causas en: 1º Juzgado De Letras de Arica ex 4°

...

✓ Total de causas guardadas: 125
✓ Datos guardados en: resultados_busqueda.csv
```

## 🔍 Estructura del CSV Generado

```csv
Rol,Fecha,Caratulado,Tribunal
C-3662-2024,27/02/2024,SANTANDER CONSUMER FINANCE LTDA./SOTO,6º Juzgado Civil de Santiago
C-1234-2024,15/03/2024,BANCO XYZ./CLIENTE,1º Juzgado Civil de Valparaíso
...
```

---

## ❓ Preguntas Frecuentes

### P: ¿Cuánto tiempo tarda?
**R**: ~42 minutos (230 tribunales × 11 segundos cada uno)

### P: ¿El CSV se sobrescribe?
**R**: Sí, en cada ejecución. Hacer backup si es necesario.

### P: ¿Qué pasa si hay timeout?
**R**: El bot continúa con el siguiente tribunal. Los datos se guardan parcialmente.

### P: ¿Puedo interrumpir la ejecución?
**R**: Sí, presiona Ctrl+C. Los datos hasta ese punto se guardan.

### P: ¿Dónde aparecen los logs?
**R**: En la consola del IDE o terminal donde ejecutaste el bot.

### P: ¿Cómo veo si funcionó?
**R**: Busca "BUILD SUCCESS" al compilar y revisa `resultados_busqueda.csv` cuando termine.

---

## 🐛 Si Algo Falla

### Error de compilación
```bash
# Limpiar caché
./mvnw clean

# Compilar nuevamente
./mvnw compile
```

### Error "Cannot find frame"
```
→ Significa que el navegador tardó en cargar
→ Aumenta el timeout en línea 31:
  .setTimeout(120000) // 120 segundos
```

### Error "Timeout waiting for selector"
```
→ La página tardó en cargar la tabla
→ Aumenta el tiempo en línea 186:
  page.waitForTimeout(15000); // 15 segundos
```

### CSV vacío
```
→ Significa que no encontró resultados
→ Prueba con otros datos personales
→ O verifica que la búsqueda sea correcta
```

---

## 📋 Checklist de Verificación

- [ ] Terminal abierta en `/Users/jellan/Documents/git/veritusbot`
- [ ] `./mvnw --version` retorna versión de Maven
- [ ] `java -version` retorna versión de Java
- [ ] `./mvnw clean compile` dice BUILD SUCCESS
- [ ] Personalizaste los datos de búsqueda (opcional)
- [ ] Listo para ejecutar: `./mvnw spring-boot:run`

---

## ✨ Bot Listo para Usar

```
┌──────────────────────────────────────┐
│  ✅ PROYECTO COMPILADO Y VALIDADO  │
│                                      │
│  Ejecuta: ./mvnw spring-boot:run    │
│  Resultado: resultados_busqueda.csv  │
│  Tiempo: ~42 minutos                 │
│  Tribunales: 230                     │
└──────────────────────────────────────┘
```

**¡Listo para iterar por todos los tribunales!** 🚀
