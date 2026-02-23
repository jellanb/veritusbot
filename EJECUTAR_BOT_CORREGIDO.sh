#!/bin/bash

# Script para ejecutar el bot CORREGIDO

echo "=================================="
echo "🚀 BOT CORREGIDO - Dropdown Fix"
echo "=================================="
echo ""
echo "Se ha corregido el problema donde el dropdown se cerraba."
echo "Ahora el bot:"
echo "  ✓ Obtiene TODOS los nombres de tribunales primero"
echo "  ✓ Abre el dropdown ANTES de cada selección"
echo "  ✓ Usa Locator en lugar de evaluate() para mejor interacción"
echo ""

cd /Users/jellan/Documents/git/veritusbot

# Compilar
echo "Compilando..."
./mvnw clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Error de compilación"
    exit 1
fi

echo "✓ Compilación exitosa"
echo ""
echo "Iniciando bot corregido..."
echo "=================================="
echo ""

# Ejecutar
./mvnw spring-boot:run 2>&1 | tee bot_corregido.log

echo ""
echo "=================================="
echo "✅ Ejecución completada"
echo ""
echo "📊 Resultados:"
if [ -f "resultados_busqueda.csv" ]; then
    LINES=$(wc -l < resultados_busqueda.csv)
    CAUSAS=$((LINES - 1))
    echo "  Causas encontradas: $CAUSAS"
    echo "  Archivo: resultados_busqueda.csv"
else
    echo "  No se generó archivo de resultados"
fi
echo ""
echo "📝 Logs completos en: bot_corregido.log"
