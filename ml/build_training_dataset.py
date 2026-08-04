import pandas as pd
from scipy.spatial import cKDTree
import numpy as np

print("Cargando datasets...")

traffic = pd.read_csv("datasets/dataset_flujo_vehicular.csv")

traffic = traffic.dropna(
    subset=["LATITUD", "LONGITUD"]
)

traffic["datetime"] = pd.to_datetime(
    traffic["HORA"],
    format="%d%b%Y:%H:%M:%S"
)

traffic = traffic.dropna(
    subset=["datetime"]
)

traffic["hour"] = traffic["datetime"].dt.hour
traffic["weekday"] = traffic["datetime"].dt.weekday

traffic = traffic.rename(columns={
    "LATITUD": "latitude",
    "LONGITUD": "longitude",
    "CANTIDAD": "vehicle_flow"
})

# -----------------------
# Sensores
# -----------------------

sensors = pd.read_csv("datasets/sensores.csv")

sensors = sensors.dropna(
    subset=["lat", "long"]
)

# -----------------------
# Garages
# -----------------------

garages = pd.read_csv(
    "datasets/estacionamientos-concesionados-de-movilidad-sustentable.csv"
)

garages = garages.dropna(
    subset=["lat", "long"]
)

print("Datasets cargados")

sensor_tree = cKDTree(
    sensors[["lat", "long"]].values
)

garage_tree = cKDTree(
    garages[["lat", "long"]].values
)

print("Calculando sensores cercanos...")

sensor_distance, sensor_index = sensor_tree.query(
    traffic[["latitude", "longitude"]].values,
    k=1
)

print("Calculando garages cercanos...")

garage_distance, garage_index = garage_tree.query(
    traffic[["latitude", "longitude"]].values,
    k=1
)

traffic["sensor_distance"] = sensor_distance
traffic["garage_distance"] = garage_distance

traffic["sensor_nearby"] = (
    traffic["sensor_distance"] < 0.002
).astype(int)

print("Generando disponibilidad sintética...")

def calculate_availability(row):

    availability = 100

    # flujo vehicular
    availability -= row["vehicle_flow"] / 120

    # hora pico
    if 7 <= row["hour"] <= 9:
        availability -= 12

    if 17 <= row["hour"] <= 20:
        availability -= 18

    # madrugada
    if row["hour"] <= 5:
        availability += 8

    # fines de semana
    if row["weekday"] >= 5:
        availability += 10

    # cerca de un sensor
    if row["sensor_nearby"] == 1:
        availability -= 6

    # cerca de un garage
    if row["garage_distance"] < 0.003:
        availability += 10

    elif row["garage_distance"] < 0.008:
        availability += 5

    # ruido aleatorio
    availability += np.random.normal(0,5)

    return np.clip(
        round(availability),
        0,
        100
    )

traffic["availability"] = traffic.apply(
    calculate_availability,
    axis=1
)

dataset = traffic[[
    "latitude",
    "longitude",
    "hour",
    "weekday",
    "vehicle_flow",
    "sensor_nearby",
    "garage_distance",
    "availability"
]]

print(dataset.head())

dataset.to_csv(
    "datasets/training_dataset_v2.csv",
    index=False
)

print("\nCantidad registros:", len(dataset))
print("\nGuardado en datasets/training_dataset_v2.csv")