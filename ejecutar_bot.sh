#!/bin/bash

# Script para ejecutar el bot con logs detallados

echo "🚀 Iniciando bot de búsqueda judicial..."
echo "=================================="
echo ""

cd /Users/jellan/Documents/git/veritusbot

# Ejecutar con salida detallada
./mvnw spring-boot:run 2>&1 | tee bot_execution.log

echo ""
echo "=================================="
echo "✅ Ejecución completada"
echo "Logs guardados en: bot_execution.log"
echo "Resultados en: resultados_busqueda.csv"
