from flask import Flask, request, jsonify
import joblib
import pandas as pd
import numpy as np
from scipy.spatial import cKDTree

app = Flask(__name__)

print("Cargando modelo...")
model = joblib.load("model/parking_model.pkl")
print("Modelo cargado")

# ============================
# Flujo vehicular
# ============================

print("Cargando dataset de flujo...")

traffic = pd.read_csv("datasets/dataset_flujo_vehicular.csv")

traffic["datetime"] = pd.to_datetime(
    traffic["HORA"],
    format="%d%b%Y:%H:%M:%S"
)

traffic["hour"] = traffic["datetime"].dt.hour

traffic = traffic.rename(columns={
    "LATITUD": "latitude",
    "LONGITUD": "longitude",
    "CANTIDAD": "vehicle_flow"
})

print(f"Dataset flujo: {len(traffic)} registros")

# ============================
# Sensores
# ============================

sensors = pd.read_csv("datasets/sensores.csv")
sensors = sensors.dropna(subset=["lat", "long"])

sensor_tree = cKDTree(
    sensors[["lat", "long"]].values
)

# ============================
# Garages
# ============================

garages = pd.read_csv(
    "datasets/estacionamientos-concesionados-de-movilidad-sustentable.csv"
)

garages = garages.dropna(subset=["lat", "long"])

garage_tree = cKDTree(
    garages[["lat", "long"]].values
)

print(f"Sensores: {len(sensors)}")
print(f"Garages: {len(garages)}")


def get_vehicle_flow(latitude, longitude, hour):

    traffic["distance"] = np.sqrt(
        (traffic["latitude"] - latitude) ** 2 +
        (traffic["longitude"] - longitude) ** 2
    )

    nearest = traffic.nsmallest(5, "distance")

    same_hour = nearest[
        nearest["hour"] == hour
    ]

    if same_hour.empty:
        return int(nearest["vehicle_flow"].mean())

    return int(same_hour["vehicle_flow"].mean())


def get_location_features(latitude, longitude):

    sensor_distance, _ = sensor_tree.query(
        [[latitude, longitude]]
    )

    garage_distance, _ = garage_tree.query(
        [[latitude, longitude]]
    )

    sensor_nearby = int(
        sensor_distance[0] < 0.002
    )

    return (
        sensor_nearby,
        float(garage_distance[0])
    )


@app.post("/predict")
def predict():

    data = request.json

    latitude = data["latitude"]
    longitude = data["longitude"]
    day = data["dayOfWeek"]
    hour = data["hour"]

    vehicle_flow = get_vehicle_flow(
        latitude,
        longitude,
        hour
    )

    sensor_nearby, garage_distance = get_location_features(
        latitude,
        longitude
    )

    features = pd.DataFrame([{
        "latitude": latitude,
        "longitude": longitude,
        "hour": hour,
        "weekday": day - 1,
        "vehicle_flow": vehicle_flow,
        "sensor_nearby": sensor_nearby,
        "garage_distance": garage_distance
    }])

    prediction = model.predict(features)[0]

    prediction = max(
        0,
        min(
            100,
            round(float(prediction))
        )
    )

    print("----------------------------")
    print(f"Lat: {latitude}")
    print(f"Lon: {longitude}")
    print(f"Hora: {hour}")
    print(f"Flow: {vehicle_flow}")
    print(f"Sensor: {sensor_nearby}")
    print(f"Garage distance: {garage_distance:.4f}")
    print(f"Predicción: {prediction}")

    return jsonify({
        "estimatedAvailabilityPercent": prediction
    })


if __name__ == "__main__":
    app.run(port=5000)