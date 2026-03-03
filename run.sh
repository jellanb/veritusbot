#!/bin/bash

echo "🚀 Iniciando compilación del proyecto..."
cd /Users/jellan/Documents/git/veritusbot

# Compilar
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo ""
    echo "🚀 Iniciando aplicación..."
    java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
else
    echo "❌ Error en la compilación"
    exit 1
fi

