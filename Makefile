MVNW := ./mvnw

.PHONY: build test clean run help

help:
	@echo "Targets disponibles:"
	@echo "  make build  - Compila y empaqueta la app"
	@echo "  make test   - Ejecuta los tests"
	@echo "  make clean  - Limpia artefactos generados"
	@echo "  make run    - Ejecuta la app con Spring Boot"

build:
	$(MVNW) clean package

test:
	$(MVNW) test

clean:
	$(MVNW) clean

run:
	$(MVNW) spring-boot:run

