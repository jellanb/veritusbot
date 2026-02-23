# ✅ BOT CONFIGURADO PARA TRABAJAR CON DATOS DEL EXCEL

## 📊 Formato del Archivo CSV

El bot ahora está configurado para trabajar con archivos CSV que usan **punto y coma (;)** como separador.

### Estructura Actual:
```
Nombres;Apellido Paterno;Apellido Materno;Año
MIGUEL ANTONIO;SOTO;FREDES;2024
JUAN CARLOS;SMITH;JOHNSON;2023
MARIA GONZALEZ;PEREZ;MARTINEZ;2024
```

### Columnas:
| Columna | Campo | Ejemplo |
|---------|-------|---------|
| 1 | Nombres | MIGUEL ANTONIO |
| 2 | Apellido Paterno | SOTO |
| 3 | Apellido Materno | FREDES |
| 4 | Año | 2024 |

## 🔧 Cambios Realizados

### 1. ExcelService.java
- **Cambio**: Actualizado el método `parsearCSV()` para usar `;` (punto y coma) en lugar de `,` (coma)
- **Razón**: El archivo CSV usa punto y coma como separador
- **Línea cambiada**: `String[] valores = linea.split(";");`

### 2. Soporte para Múltiples Registros
El bot ahora soporta múltiples personas en el archivo CSV:
- Lee todas las filas después del encabezado
- Itera sobre cada persona automáticamente
- Busca cada una en el sitio web
- Acumula todos los resultados en `resultados_busqueda.csv`

## 🚀 Cómo Usar

### Paso 1: Editar el archivo personas.csv

Abre el archivo `personas.csv` en la raíz del proyecto y agrega personas con este formato:

```
Nombres;Apellido Paterno;Apellido Materno;Año
NOMBRE1 NOMBRE2;APELLIDO_P;APELLIDO_M;AAAA
NOMBRE3 NOMBRE4;APELLIDO_P;APELLIDO_M;AAAA
```

**Importante:**
- Usa `;` (punto y coma) como separador, NO comas
- Primera línea es el encabezado (no se procesa)
- Cada persona en una nueva línea
- Formato de año: 4 dígitos (ej: 2024)

### Paso 2: Ejecutar el bot

**Terminal 1:**
```bash
cd /Users/jellan/Documents/git/veritusbot
./mvnw spring-boot:run
```

**Terminal 2:**
```bash
curl http://localhost:8080/api/buscar-personas
```

### Paso 3: Ver resultados

```bash
cat resultados_busqueda.csv
```

## 📝 Ejemplos de Formato Correcto

✅ **CORRECTO:**
```
Nombres;Apellido Paterno;Apellido Materno;Año
MIGUEL ANTONIO;SOTO;FREDES;2024
JUAN;GONZALEZ;SMITH;2023
```

❌ **INCORRECTO:**
```
Nombres,Apellido Paterno,Apellido Materno,Año      ← Usa comas en lugar de punto y coma
MIGUEL ANTONIO;SOTO;FREDES;2024
JUAN;GONZALEZ;SMITH;23                             ← Año incompleto
```

## 🔄 Flujo de Ejecución

```
1. Bot lee personas.csv
   ↓
2. ExcelService parsea usando separador (;)
   ↓
3. Retorna List<PersonaDTO> con todas las personas
   ↓
4. PjudScraper itera CADA persona:
   ├─ MIGUEL ANTONIO, SOTO, FREDES, 2024
   ├─ JUAN CARLOS, SMITH, JOHNSON, 2023
   └─ MARIA GONZALEZ, PEREZ, MARTINEZ, 2024
   ↓
5. Para cada persona:
   • Abre navegador
   • Navega a PJUD
   • Ingresa datos
   • Busca en TODOS los tribunales
   • Guarda resultados
   ↓
6. resultados_busqueda.csv con todas las búsquedas
```

## 📊 Compilación

```
BUILD: SUCCESS ✓
ERRORS: 0
WARNINGS: Solo acentuación (no bloqueantes)
STATUS: LISTO PARA EJECUTAR ✓
```

## 💡 Notas Importantes

1. **Separador**: Debe ser `;` (punto y coma), NO `,` (coma)
2. **Encabezado**: Primera línea se salta automáticamente
3. **Múltiples registros**: El bot procesa TODOS los registros en el archivo
4. **Año**: Debe ser numérico de 4 dígitos
5. **Nombres**: Pueden tener espacios (ej: "MIGUEL ANTONIO")

¡El bot está listo para trabajar con múltiples personas! 🎉
