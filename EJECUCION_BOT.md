# Instrucciones de Ejecución del Bot Judicial ✅

## Requisitos Previos

1. **Java 11+**: Instalado y en el PATH
2. **Maven**: Instalado (`./mvnw` disponible)
3. **Navegador Chromium**: Instalado automáticamente por Playwright

## Compilación

```bash
cd /Users/jellan/Documents/git/veritusbot
./mvnw clean compile
```

## Ejecución

```bash
./mvnw spring-boot:run
```

O si ejecutas desde el IDE:
- Busca el archivo `VeritusbotApplication.java`
- Ejecuta el método `main`

## Acceso a los Datos

El bot genera un archivo CSV con los resultados:

```
resultados_busqueda.csv
```

**Ubicación**: Raíz del proyecto (`/Users/jellan/Documents/git/veritusbot/`)

**Contenido**:
```csv
Rol,Fecha,Caratulado,Tribunal
C-3662-2024,27/02/2024,SANTANDER CONSUMER FINANCE LTDA./SOTO,6º Juzgado Civil de Santiago
...
```

## Flujo de Ejecución

### 1. El Bot Abre la Página
```
https://oficinajudicialvirtual.pjud.cl/home/index.php
```

### 2. Cierra Popup
Detecta y cierra el modal de bienvenida

### 3. Navega a "Consulta de Causas"
- Hace click en el botón correspondiente
- Navega a "Búsqueda por Nombre"

### 4. Completa el Formulario (Ejemplo)
- **Nombre**: MIGUEL
- **Segundo Nombre**: ANTONIO
- **Apellido Paterno**: SOTO
- **Apellido Materno**: FREDES
- **Año**: 2024
- **Competencia**: Civil
- **Corte**: TODOS

### 5. Itera por Tribunales
**230 tribunales en total** (índices 2-231):
- 1º Juzgado de Letras de Arica
- 1º Juzgado De Letras de Arica ex 4°
- ...
- 2º Juzgado de Letras de San Bernardo Ex 3°

### 6. Para cada Tribunal:
- Selecciona sin abrir/cerrar dropdown
- Ejecuta la búsqueda
- Espera 8 segundos (para búsquedas sin resultados)
- Extrae datos si hay resultados
- Guarda en CSV

### 7. Genera Reporte
```
✓ Total de causas guardadas: X
✓ Datos guardados en: resultados_busqueda.csv
```

## Configuración Personalizable

Abre `PjudScraper.java` y modifica:

```java
// Línea 24: Cambiar a true para modo headless (sin interfaz visual)
.setHeadless(false) // true = sin ventana del navegador

// Línea 31: Cambiar timeout (en milisegundos)
.setTimeout(60000) // Tiempo máximo de espera

// Línea 186: Cambiar tiempo de espera entre búsquedas
page.waitForTimeout(8000); // 8 segundos
```

## Monitoreo en Tiempo Real

Durante la ejecución, el bot imprime en consola:

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

## Solución de Problemas

### El bot se queda en un tribunal
**Causa**: Problema con la iteración  
**Solución**: Ya corregido en esta versión ✅

### El CSV se vacía
**Causa**: Es normal - se sobrescribe en cada ejecución  
**Solución**: Hacer backup antes de ejecutar

### Timeout esperando resultados
**Causa**: Servidor lento o problema de conexión  
**Solución**: Aumentar `page.waitForTimeout(8000)` a más tiempo

## Información de Dependencias

El proyecto usa:
- **Playwright** 1.40+ - Para automatización
- **Jsoup** 1.15+ - Para parsing HTML
- **Spring Boot** 3.2+ - Framework

Todas instaladas automáticamente por Maven.

## Status Actual

✅ Compilación: Exitosa  
✅ Lógica: Correcta (itera por todos los 230 tribunales)  
✅ Extracción: Funcional (obtiene datos de causas)  
✅ Guardado: Operativo (CSV actualizado)  

**El bot está listo para usar.** 🚀
