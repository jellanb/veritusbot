# ✅ BOT ACTUALIZADO CON RANGO DE AÑOS Y LOGS DE TIEMPO

## 🎯 Cambios Realizados

Se ha actualizado el bot para:

1. **Agregar 2 columnas nuevas al CSV:**
   - `ANOINIT`: Año inicial de búsqueda
   - `ANOFIN`: Año final de búsqueda

2. **Iterar sobre rango de años:**
   - El bot ahora busca desde ANOINIT hasta ANOFIN (inclusive)
   - Itera año por año: 2019 → 2020 → 2021 → ... → 2024

3. **Agregar logs con hora de inicio y fin:**
   - Hora de inicio en formato HH:mm:ss
   - Hora de fin en formato HH:mm:ss

---

## 📊 Nuevo Formato del CSV

**Archivo:** `personas.csv` (raíz del proyecto)

```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JORGE ENRIQUE;AMPUERO;CABELLO;2019;2024
MIGUEL ANTONIO;SOTO;FREDES;2019;2024
```

### Estructura de Columnas:
| Columna | Campo | Ejemplo |
|---------|-------|---------|
| 1 | NOMBRES | JORGE ENRIQUE |
| 2 | APELLIDO PATERNO | AMPUERO |
| 3 | APELLIDO MATERNO | CABELLO |
| 4 | ANOINIT | 2019 |
| 5 | ANOFIN | 2024 |

---

## 🔧 Cambios Realizados en el Código

### 1. PersonaDTO.java
**Cambios:**
- Removido campo `año` (int)
- Agregado campo `anioInit` (int)
- Agregado campo `anioFin` (int)
- Actualizado constructor para 5 parámetros
- Agregados getters y setters para los nuevos campos
- Actualizado toString()

**Métodos:**
```java
public PersonaDTO(String nombres, String apellidoPaterno, 
                 String apellidoMaterno, int anioInit, int anioFin)
public int getAnioInit()
public int getAnioFin()
```

### 2. ExcelService.java
**Cambios:**
- Actualizado formato de documentación (Año → AnoInit, AnoFin)
- Modificado parseo para 5 columnas (antes 4)
- Validación para ambos años (anioInit y anioFin > 0)
- Error handling mejorado para cada año

**Validación:**
```java
if (valores.length >= 5) {
    // Parsea anioInit (columna 4)
    // Parsea anioFin (columna 5)
}
```

### 3. PjudScraper.java
**Cambios Principales:**

#### a) Imports agregados:
```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

#### b) Constante para formato de hora:
```java
private static final DateTimeFormatter FORMATTER_HORA = 
    DateTimeFormatter.ofPattern("HH:mm:ss");
```

#### c) Método `buscarPersonasDelExcel()` actualizado:
```java
public void buscarPersonasDelExcel(String nombreArchivo) {
    LocalDateTime horaInicio = LocalDateTime.now();
    String horaInicioStr = horaInicio.format(FORMATTER_HORA);
    
    // ... (búsqueda de personas)
    
    LocalDateTime horaFin = LocalDateTime.now();
    String horaFinStr = horaFin.format(FORMATTER_HORA);
    
    // Log con hora de inicio y fin
    System.out.println("║  Hora de inicio: " + horaInicioStr);
    System.out.println("║  Hora de fin:    " + horaFinStr);
}
```

#### d) Método `buscarPersona()` actualizado:
```java
private void buscarPersona(PersonaDTO persona) {
    System.out.println("   Rango de años: " + persona.getAnioInit() 
        + " a " + persona.getAnioFin());
    
    // Iterar sobre el rango de años
    for (int anio = persona.getAnioInit(); anio <= persona.getAnioFin(); anio++) {
        System.out.println("   Procesando año: " + anio);
        buscarPorNombre(personas.getNombres(), personas.getApellidoPaterno(), 
                       personas.getApellidoMaterno(), anio);
    }
}
```

---

## 📊 Compilación

```
BUILD: SUCCESS ✓
ERRORS: 0
WARNINGS: Solo de métodos no utilizados (no bloqueantes)
STATUS: LISTO PARA EJECUTAR ✓
```

---

## 🚀 Ejemplo de Uso

### Archivo personas.csv:
```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JORGE ENRIQUE;AMPUERO;CABELLO;2019;2024
MIGUEL ANTONIO;SOTO;FREDES;2020;2023
```

### Ejecución:
```bash
Terminal 1:
  ./mvnw spring-boot:run

Terminal 2:
  curl http://localhost:8080/api/buscar-personas
```

### Output esperado:
```
╔════════════════════════════════════════════════════════════╗
║  INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL                ║
║  Hora de inicio: 14:30:45                                  ║
╚════════════════════════════════════════════════════════════╝

📋 Personas a buscar: 2
  • PersonaDTO{nombres='JORGE ENRIQUE', apellidoPaterno='AMPUERO', 
               apellidoMaterno='CABELLO', anioInit=2019, anioFin=2024}

BUSCANDO PERSONA 1/2
🔍 Buscando: JORGE ENRIQUE AMPUERO CABELLO
   Rango de años: 2019 a 2024
   Procesando año: 2019
   [... busca en tribunales ...]
   Procesando año: 2020
   [... busca en tribunales ...]
   ... (continúa hasta 2024)

BUSCANDO PERSONA 2/2
🔍 Buscando: MIGUEL ANTONIO SOTO FREDES
   Rango de años: 2020 a 2023
   [... similar al anterior ...]

╔════════════════════════════════════════════════════════════╗
║  BÚSQUEDA COMPLETADA                                       ║
║  Hora de inicio: 14:30:45                                  ║
║  Hora de fin:    15:45:20                                  ║
╚════════════════════════════════════════════════════════════╝
```

---

## ⚡ Características Principales

- ✅ **Rango de años dinámico:** Cada persona puede tener su propio rango
- ✅ **Iteración automática:** De ANOINIT a ANOFIN (inclusive)
- ✅ **Logs con hora:** Formato HH:mm:ss
- ✅ **Compilación exitosa:** 0 errores
- ✅ **Compatible con múltiples personas:** Cada una con su rango de años

---

## 💡 Notas Importantes

1. **Rango de años:** ANOINIT ≤ Años buscados ≤ ANOFIN
2. **Formato de hora:** HH:mm:ss (24 horas)
3. **Iteración:** Comienza en ANOINIT y termina en ANOFIN (inclusive)
4. **Validación:** Ambos años deben ser > 0

---

## 📝 Próximos Pasos

1. Edita `personas.csv` con tus datos y rangos de años
2. Ejecuta `./mvnw spring-boot:run`
3. Llama a `curl http://localhost:8080/api/buscar-personas`
4. Revisa los logs con hora de inicio y fin
5. Verifica `resultados_busqueda.csv`

**¡El bot está listo para búsquedas por rango de años!** 🚀
