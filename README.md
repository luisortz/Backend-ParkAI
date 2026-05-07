# Backend-ParkAI

Backend del prototipo ParkAI desarrollado con **Java + Spring Boot**.

## Objetivo del backend
Este backend expone APIs para:
- gestionar zonas urbanas de estacionamiento,
- registrar reportes colaborativos de ocupación,
- estimar disponibilidad de estacionamiento según **zona, día y hora**.

La configuración incluye referencia al dataset público de Buenos Aires:
`https://data.buenosaires.gob.ar/dataset/estacionamiento-en-la-via-publica`.

## Requisitos
- Java 17
- Maven 3.9+
- MySQL (para ejecución local del backend)

## Configuración
Variables opcionales:
- `DB_URL` (default: `jdbc:mysql://localhost:3306/parkai`)
- `DB_USERNAME` (default: `parkai`)
- `DB_PASSWORD` (default: `parkai`)
- `PARKING_ML_SERVICE_URL` (opcional, endpoint HTTP del modelo Python/Scikit-learn)

## Endpoints principales
- `GET /api/zones`
- `POST /api/zones`
- `POST /api/reports`
- `GET /api/predictions?zoneId={id}&dayOfWeek={1-7}&hour={0-23}`

## Ejecutar
```bash
# Perfil por defecto (MySQL)
mvn spring-boot:run

# Perfil de desarrollo rápido con H2 en memoria
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

## Pruebas
```bash
mvn test
```
