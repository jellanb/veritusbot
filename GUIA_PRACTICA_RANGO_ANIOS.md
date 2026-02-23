# 📝 GUÍA PRÁCTICA - RANGO DE AÑOS

## Ejemplos de Uso

### Ejemplo 1: Búsqueda en rango 2019-2024

**CSV:**
```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JORGE ENRIQUE;AMPUERO;CABELLO;2019;2024
```

**Lo que hace el bot:**
- Año 2019: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales
- Año 2020: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales
- Año 2021: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales
- Año 2022: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales
- Año 2023: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales
- Año 2024: Busca a JORGE ENRIQUE AMPUERO CABELLO en todos los tribunales

**Resultado:**
- 1 persona × 6 años × N tribunales = Muchos registros en `resultados_busqueda.csv`

---

### Ejemplo 2: Múltiples personas con rangos diferentes

**CSV:**
```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JORGE ENRIQUE;AMPUERO;CABELLO;2019;2024
MIGUEL ANTONIO;SOTO;FREDES;2020;2023
MARIA GONZALEZ;PEREZ;MARTINEZ;2021;2024
```

**Lo que hace el bot:**

**Persona 1:** JORGE ENRIQUE AMPUERO CABELLO (2019-2024)
- Años: 2019, 2020, 2021, 2022, 2023, 2024 = 6 búsquedas

**Persona 2:** MIGUEL ANTONIO SOTO FREDES (2020-2023)
- Años: 2020, 2021, 2022, 2023 = 4 búsquedas

**Persona 3:** MARIA GONZALEZ PEREZ MARTINEZ (2021-2024)
- Años: 2021, 2022, 2023, 2024 = 4 búsquedas

**Total:** 3 personas × (6+4+4) años = 14 búsquedas

---

### Ejemplo 3: Búsqueda en un solo año

**CSV:**
```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JUAN;GARCIA;LOPEZ;2024;2024
```

**Lo que hace el bot:**
- Año 2024: Busca a JUAN GARCIA LOPEZ en todos los tribunales (solo 1 año)

**Útil para:**
- Búsquedas rápidas de casos recientes
- Verificar datos específicos de un año

---

## 📊 Logs de Ejemplo

### Cuando se inicia el bot:

```
╔════════════════════════════════════════════════════════════╗
║  INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL                ║
║  Hora de inicio: 14:30:45                                  ║
╚════════════════════════════════════════════════════════════╝

📖 Leyendo archivo: personas.csv
   Ruta: /Users/jellan/Documents/git/veritusbot/personas.csv
📊 Encabezado encontrado: NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
✓ Persona cargada: PersonaDTO{nombres='JORGE ENRIQUE', apellidoPaterno='AMPUERO', apellidoMaterno='CABELLO', anioInit=2019, anioFin=2024}
✓ Persona cargada: PersonaDTO{nombres='MIGUEL ANTONIO', apellidoPaterno='SOTO', apellidoMaterno='FREDES', anioInit=2020, anioFin=2023}

✓ Total de personas cargadas: 2

📋 Personas a buscar: 2
  • PersonaDTO{...anioInit=2019, anioFin=2024}
  • PersonaDTO{...anioInit=2020, anioFin=2023}
```

### Durante la búsqueda:

```
======================================================================
BUSCANDO PERSONA 1/2
======================================================================

🔍 Buscando: JORGE ENRIQUE AMPUERO CABELLO
   Rango de años: 2019 a 2024

   Procesando año: 2019
   [Abre navegador, navega a PJUD, ingresa datos, busca...]

   Procesando año: 2020
   [Abre navegador, navega a PJUD, ingresa datos, busca...]

   ... (continúa con 2021, 2022, 2023, 2024)

======================================================================
BUSCANDO PERSONA 2/2
======================================================================

🔍 Buscando: MIGUEL ANTONIO SOTO FREDES
   Rango de años: 2020 a 2023

   Procesando año: 2020
   [búsqueda...]
   
   Procesando año: 2021
   [búsqueda...]
   
   Procesando año: 2022
   [búsqueda...]
   
   Procesando año: 2023
   [búsqueda...]
```

### Cuando termina el bot:

```
╔════════════════════════════════════════════════════════════╗
║  BÚSQUEDA COMPLETADA                                       ║
║  Hora de inicio: 14:30:45                                  ║
║  Hora de fin:    16:15:32                                  ║
╚════════════════════════════════════════════════════════════╝
```

---

## ⏱️ Cálculo de Tiempo Estimado

**Tiempo por búsqueda (por tribunal):** ~3-5 segundos
**Tiempo por año (231 tribunales):** ~10-15 minutos
**Tiempo por persona:**
- 1 año: ~10 minutos
- 5 años: ~50 minutos
- 6 años: ~60 minutos

**Ejemplo:** 
- JORGE ENRIQUE (6 años) = ~60 minutos
- MIGUEL ANTONIO (4 años) = ~40 minutos
- MARIA GONZALEZ (4 años) = ~40 minutos
- **Total:** ~140 minutos = ~2 horas 20 minutos

---

## ✅ Validaciones del CSV

**✓ VÁLIDO:**
```
NOMBRES;APELLIDO PATERNO;APELLIDO MATERNO;ANOINIT;ANOFIN
JORGE;AMPUERO;CABELLO;2019;2024
JUAN LUIS;GARCIA;LOPEZ;2020;2023
```

**✗ INVÁLIDO:**
```
NOMBRES,APELLIDO PATERNO,APELLIDO MATERNO,ANOINIT,ANOFIN   ← Usa coma en lugar de punto y coma
JORGE;AMPUERO;CABELLO;2019;24                               ← Año incompleto
JUAN;;LOPEZ;2020;2023                                        ← Apellido paterno vacío
MARIA;PEREZ;MARTINEZ;0;2024                                  ← Año inicial = 0
CARLOS;MARTINEZ;LOPEZ;2024;2020                              ← Año inicial > año final
```

---

## 📝 Errores Comunes y Soluciones

### Error: "Fila X con formato incorrecto (esperaba 5 columnas)"

**Causa:** El CSV no tiene 5 columnas
**Solución:** Verifica que tengas:
- NOMBRES
- APELLIDO PATERNO
- APELLIDO MATERNO
- ANOINIT
- ANOFIN

### Error: "Error en AnoInit/AnoFin: NumberFormatException"

**Causa:** Los años no son números
**Solución:** Usa solo dígitos (ej: 2019, 2024)

### Error: "Fila X incompleta o con años inválidos"

**Causa:** 
- Faltan datos
- Años = 0 o negativos
- Campos vacíos

**Solución:** 
- Completa todos los campos
- Usa años positivos de 4 dígitos

---

## 🎯 Mejores Prácticas

1. **Verifica el CSV antes de ejecutar:**
   ```
   cat personas.csv
   ```

2. **Usa años realistas:**
   - Mínimo: 1990
   - Máximo: Año actual

3. **Agrupa búsquedas por rango:**
   - Búsquedas recientes: 2024
   - Búsquedas históricas: 2015-2019

4. **Crea backups:**
   ```
   cp personas.csv personas.csv.backup
   ```

5. **Monitorea los logs:**
   - Hora de inicio/fin
   - Errores durante la búsqueda
   - Cantidad de resultados

---

## 📊 Resultado Final

**Archivo:** `resultados_busqueda.csv`

```
Rol;Fecha;Caratulado;Tribunal;Año;Persona;Apellido_Paterno;Apellido_Materno
C-1234-2019;15/03/2019;DEMANDANTE vs DEMANDADO;1º Juzgado Civil;2019;JORGE ENRIQUE;AMPUERO;CABELLO
C-5678-2020;20/05/2020;DEMANDANTE vs DEMANDADO;2º Juzgado Civil;2020;JORGE ENRIQUE;AMPUERO;CABELLO
...
C-9012-2023;10/07/2023;DEMANDANTE vs DEMANDADO;3º Juzgado Civil;2023;MIGUEL ANTONIO;SOTO;FREDES
```

---

**¡El bot está listo para ejecutarse con rango de años!** 🚀
