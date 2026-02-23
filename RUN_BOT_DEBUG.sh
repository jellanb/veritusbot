#!/bin/bash

# Script para ejecutar el bot con modo visual (headless=false)

echo "=================================="
echo "🚀 EJECUTANDO BOT - MODO DEBUG"
echo "=================================="
echo ""
echo "El navegador se abrirá automáticamente."
echo "Podrás ver exactamente qué está pasando."
echo ""
echo "Logs se guardarán en: bot_debug.log"
echo ""

cd /Users/jellan/Documents/git/veritusbot

# Compilar primero
echo "Compilando proyecto..."
./mvnw clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Error de compilación"
    exit 1
fi

echo "✓ Compilación exitosa"
echo ""
echo "Iniciando bot..."
echo "=================================="
echo ""

# Ejecutar con logs
./mvnw spring-boot:run 2>&1 | tee bot_debug.log

echo ""
echo "=================================="
echo "✅ Ejecución completada"
echo "Logs guardados en: bot_debug.log"
echo "Resultados en: resultados_busqueda.csv"
