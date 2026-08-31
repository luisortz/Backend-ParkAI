from flask import Flask, request, jsonify
import joblib
import pandas as pd
import numpy as np
from scipy.spatial import cKDTree

app = Flask(__name__)

# ============================================================
# MODELO
# ============================================================

print("Cargando modelo...")

model = joblib.load(
    "model/parking_model.pkl"
)

print("Modelo cargado")


# ============================================================
# FLUJO VEHICULAR
# ============================================================

print("Cargando dataset de flujo...")

traffic = pd.read_csv(
    "datasets/dataset_flujo_vehicular.csv"
)

traffic = traffic.dropna(
    subset=["LATITUD", "LONGITUD", "CANTIDAD"]
)

traffic["datetime"] = pd.to_datetime(
    traffic["HORA"],
    format="%d%b%Y:%H:%M:%S",
    errors="coerce"
)

traffic = traffic.dropna(
    subset=["datetime"]
)

traffic["hour"] = traffic["datetime"].dt.hour

traffic = traffic.rename(columns={
    "LATITUD": "latitude",
    "LONGITUD": "longitude",
    "CANTIDAD": "vehicle_flow"
})

print(
    f"Dataset flujo: {len(traffic)} registros"
)


# ============================================================
# ESTADÍSTICAS DEL FLUJO
# ============================================================

# IMPORTANTE:
# Estos valores tienen que ser los mismos que se utilizaron
# durante el entrenamiento.

FLOW_MEAN = traffic["vehicle_flow"].mean()
FLOW_STD = traffic["vehicle_flow"].std()

print(
    f"Flow mean: {FLOW_MEAN:.2f}"
)

print(
    f"Flow std: {FLOW_STD:.2f}"
)


# ============================================================
# SENSORES
# ============================================================

print("Cargando sensores...")

sensors = pd.read_csv(
    "datasets/sensores.csv"
)

sensors = sensors.dropna(
    subset=["lat", "long"]
)

sensor_tree = cKDTree(
    sensors[["lat", "long"]].values
)

print(
    f"Sensores: {len(sensors)}"
)


# ============================================================
# GARAGES
# ============================================================

print("Cargando garages...")

garages = pd.read_csv(
    "datasets/estacionamientos-concesionados-de-movilidad-sustentable.csv"
)

garages = garages.dropna(
    subset=["lat", "long"]
)

garage_tree = cKDTree(
    garages[["lat", "long"]].values
)

print(
    f"Garages: {len(garages)}"
)


# ============================================================
# VEHICLE FLOW
# ============================================================
def get_location_flow_mean(latitude, longitude):
    distance = np.sqrt(
        (traffic["latitude"].values - latitude) ** 2 +
        (traffic["longitude"].values - longitude) ** 2
    )

    nearest_idx = np.argpartition(distance, 5)[:5]
    nearest_flows = traffic["vehicle_flow"].values[nearest_idx]

    if len(nearest_flows) == 0:
        return FLOW_MEAN

    mean_flow = nearest_flows.mean()

    if mean_flow == 0 or pd.isna(mean_flow):
        return FLOW_MEAN

    return mean_flow
def get_vehicle_flow(
    latitude,
    longitude,
    hour
):



    # Primero filtramos por hora
    same_hour = traffic[
        traffic["hour"] == hour
    ].copy()

    # Si no hay registros para esa hora,
    # usamos todos los registros.
    if same_hour.empty:
        same_hour = traffic.copy()

    # Calculamos distancia
    same_hour["distance"] = np.sqrt(
        (same_hour["latitude"] - latitude) ** 2 +
        (same_hour["longitude"] - longitude) ** 2
    )

    # Buscamos los 5 puntos más cercanos
    nearest = same_hour.nsmallest(
        5,
        "distance"
    )

    if nearest.empty:
        return int(FLOW_MEAN)

    return int(
        nearest["vehicle_flow"].mean()
    )


# ============================================================
# LOCATION FEATURES
# ============================================================

def get_location_features(
    latitude,
    longitude
):

    # -------------------------
    # SENSOR
    # -------------------------

    sensor_distance, _ = sensor_tree.query(
        [[latitude, longitude]]
    )

    sensor_distance = float(
        sensor_distance[0]
    )

    sensor_nearby = int(
        sensor_distance < 0.002
    )

    # -------------------------
    # GARAGE
    # -------------------------

    garage_distance, _ = garage_tree.query(
        [[latitude, longitude]]
    )

    garage_distance = float(
        garage_distance[0]
    )

    # -------------------------
    # GARAGES CERCANOS
    # -------------------------

    garages_nearby = garage_tree.query_ball_point(
        [latitude, longitude],
        r=0.005
    )

    garages_nearby = len(
        garages_nearby
    )

    return (
        sensor_distance,
        sensor_nearby,
        garage_distance,
        garages_nearby
    )


# ============================================================
# PREDICT
# ============================================================

@app.post("/predict")
def predict():

    data = request.json

    latitude = float(
        data["latitude"]
    )

    longitude = float(
        data["longitude"]
    )

    day = int(
        data["dayOfWeek"]
    )

    hour = int(
        data["hour"]
    )

    # ========================================================
    # FLOW
    # ========================================================

    vehicle_flow = get_vehicle_flow(
        latitude,
        longitude,
        hour
    )
    location_flow_mean = get_location_flow_mean(latitude, longitude)
    flow_pressure = vehicle_flow / location_flow_mean

    # ========================================================
    # LOCATION
    # ========================================================

    (
        sensor_distance,
        sensor_nearby,
        garage_distance,
        garages_nearby
    ) = get_location_features(
        latitude,
        longitude
    )

    # ========================================================
    # WEEKDAY
    # ========================================================

    # Tu API recibe:
    #
    # 1 = lunes
    # 2 = martes
    # ...
    # 7 = domingo
    #
    # sklearn/pandas usa:
    #
    # 0 = lunes
    # 1 = martes
    # ...
    # 6 = domingo

    weekday = day - 1

    # ========================================================
    # VARIABLES TEMPORALES
    # ========================================================

    hour_sin = np.sin(
        2 * np.pi * hour / 24
    )

    hour_cos = np.cos(
        2 * np.pi * hour / 24
    )

    weekday_sin = np.sin(
        2 * np.pi * weekday / 7
    )

    weekday_cos = np.cos(
        2 * np.pi * weekday / 7
    )

    is_weekend = int(
        weekday >= 5
    )

    is_peak = int(
        hour in [7, 8, 9, 17, 18, 19, 20]
    )

    # ========================================================
    # FLOW FEATURES
    # ========================================================

    vehicle_flow_normalized = (
        vehicle_flow - FLOW_MEAN
    ) / FLOW_STD

    high_traffic = int(
        vehicle_flow >
        traffic["vehicle_flow"].quantile(0.75)
    )

    # ========================================================
    # FEATURES PARA EL MODELO
    # ========================================================

    features = pd.DataFrame([{

        "latitude": latitude,
        "longitude": longitude,

        "hour": hour,
        "weekday": weekday,

        "hour_sin": hour_sin,
        "hour_cos": hour_cos,

        "weekday_sin": weekday_sin,
        "weekday_cos": weekday_cos,

        "is_weekend": is_weekend,
        "is_peak": is_peak,

        "vehicle_flow": vehicle_flow,
        "vehicle_flow_normalized":
            vehicle_flow_normalized,

        "high_traffic": high_traffic,

        "flow_pressure": flow_pressure,

        "sensor_distance":
            sensor_distance,

        "sensor_nearby":
            sensor_nearby,

        "garage_distance":
            garage_distance,

        "garages_nearby":
            garages_nearby

    }])

    # ========================================================
    # PREDICCIÓN
    # ========================================================

    prediction = model.predict(
        features
    )[0]

    prediction = max(
        0,
        min(
            100,
            round(float(prediction))
        )
    )

    # ========================================================
    # LOG
    # ========================================================

    print("----------------------------")

    print(
        f"Lat: {latitude}"
    )

    print(
        f"Lon: {longitude}"
    )

    print(
        f"Hora: {hour}"
    )

    print(
        f"Weekday: {weekday}"
    )

    print(
        f"Flow: {vehicle_flow}"
    )

    print(
        f"Sensor distance: {sensor_distance:.6f}"
    )

    print(
        f"Sensor nearby: {sensor_nearby}"
    )

    print(
        f"Garage distance: {garage_distance:.6f}"
    )

    print(
        f"Garages nearby: {garages_nearby}"
    )

    print(
        f"Predicción: {prediction}%"
    )

    print("----------------------------")

    # ========================================================
    # RESPONSE
    # ========================================================

    return jsonify({
        "estimatedAvailabilityPercent": prediction
    })


# ============================================================
# MAIN
# ============================================================

if __name__ == "__main__":

    app.run(
        port=5000
    )