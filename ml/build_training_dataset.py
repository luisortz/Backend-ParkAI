import pandas as pd
import numpy as np
from scipy.spatial import cKDTree

print("Cargando datasets...")

# ============================================================
# FLUJO VEHICULAR
# ============================================================

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
traffic["weekday"] = traffic["datetime"].dt.weekday

traffic = traffic.rename(columns={
    "LATITUD": "latitude",
    "LONGITUD": "longitude",
    "CANTIDAD": "vehicle_flow"
})

# ============================================================
# FEATURES TEMPORALES
# ============================================================

# Hora como variable circular
traffic["hour_sin"] = np.sin(
    2 * np.pi * traffic["hour"] / 24
)

traffic["hour_cos"] = np.cos(
    2 * np.pi * traffic["hour"] / 24
)

# Día como variable circular
traffic["weekday_sin"] = np.sin(
    2 * np.pi * traffic["weekday"] / 7
)

traffic["weekday_cos"] = np.cos(
    2 * np.pi * traffic["weekday"] / 7
)

# Fin de semana
traffic["is_weekend"] = (
    traffic["weekday"] >= 5
).astype(int)

# Hora pico
traffic["is_peak"] = (
    traffic["hour"].isin([7, 8, 9, 17, 18, 19, 20])
).astype(int)

# ============================================================
# SENSOR
# ============================================================

sensors = pd.read_csv(
    "datasets/sensores.csv"
)

sensors = sensors.dropna(
    subset=["lat", "long"]
)

sensor_tree = cKDTree(
    sensors[["lat", "long"]].values
)

sensor_distance, _ = sensor_tree.query(
    traffic[["latitude", "longitude"]].values,
    k=1
)

traffic["sensor_distance"] = sensor_distance

traffic["sensor_nearby"] = (
    traffic["sensor_distance"] < 0.002
).astype(int)

# ============================================================
# GARAGES
# ============================================================

garages = pd.read_csv(
    "datasets/estacionamientos-concesionados-de-movilidad-sustentable.csv"
)

garages = garages.dropna(
    subset=["lat", "long"]
)

garage_tree = cKDTree(
    garages[["lat", "long"]].values
)

garage_distance, _ = garage_tree.query(
    traffic[["latitude", "longitude"]].values,
    k=1
)

traffic["garage_distance"] = garage_distance

# Cantidad de garages cercanos
garage_counts = garage_tree.query_ball_point(
    traffic[["latitude", "longitude"]].values,
    r=0.005
)

traffic["garages_nearby"] = [
    len(x) for x in garage_counts
]

# ============================================================
# FLUJO VEHICULAR LOCAL
# ============================================================

# Flujo promedio general
flow_mean = traffic["vehicle_flow"].mean()
flow_std = traffic["vehicle_flow"].std()

traffic["vehicle_flow_normalized"] = (
    (traffic["vehicle_flow"] - flow_mean)
    / flow_std
)


# ============================================================
# FLUJO PROMEDIO POR UBICACIÓN
# ============================================================

location_stats = (
    traffic
    .groupby(["latitude", "longitude"])["vehicle_flow"]
    .agg(["mean", "std"])
    .reset_index()
)

location_stats = location_stats.rename(columns={
    "mean": "location_flow_mean",
    "std": "location_flow_std"
})

# Unimos las estadísticas con el dataset
traffic = traffic.merge(
    location_stats,
    on=["latitude", "longitude"],
    how="left"
)

# Evitamos divisiones problemáticas
traffic["location_flow_mean"] = (
    traffic["location_flow_mean"]
    .replace(0, np.nan)
    .fillna(flow_mean)
)

# ============================================================
# PRESIÓN DE TRÁFICO LOCAL
# ============================================================

traffic["flow_pressure"] = (
    traffic["vehicle_flow"]
    / traffic["location_flow_mean"]
)

# Categoría de flujo
traffic["high_traffic"] = (
    traffic["vehicle_flow"]
    > traffic["vehicle_flow"].quantile(0.75)
).astype(int)

# ============================================================
# DISPONIBILIDAD 
# ============================================================

print("Generando target provisional...")


def calculate_availability(row):

    # ========================================================
    # BASE
    # ========================================================

    availability = 85.0

    # ========================================================
    # PRESIÓN DE TRÁFICO LOCAL
    # ========================================================

    # Si el flujo actual es mucho mayor al habitual
    # para esa ubicación, reducimos disponibilidad.

    flow_pressure = row["flow_pressure"]

    if flow_pressure > 1:
        availability -= (flow_pressure - 1) * 25

    else:
        availability += (1 - flow_pressure) * 10

    # ========================================================
    # HORA
    # ========================================================

    # Mañana
    if 7 <= row["hour"] <= 9:
        availability -= 12

    # Horario normal
    elif 10 <= row["hour"] <= 16:
        availability -= 3

    # Tarde/noche - mayor demanda
    elif 17 <= row["hour"] <= 20:
        availability -= 18

    # Noche
    elif 21 <= row["hour"] <= 23:
        availability += 3

    # Madrugada
    elif row["hour"] <= 5:
        availability += 10

    # ========================================================
    # FIN DE SEMANA
    # ========================================================

    if row["is_weekend"] == 1:
        availability += 10

    # ========================================================
    # GARAGES
    # ========================================================

    # Garage muy cercano
    if row["garage_distance"] < 0.003:

        availability += 5

    # Garage relativamente cercano
    elif row["garage_distance"] < 0.008:

        availability += 2

    # Cantidad de garages
    if row["garages_nearby"] >= 3:

        availability += 5

    elif row["garages_nearby"] >= 1:

        availability += 2

    # ========================================================
    # SENSOR
    # ========================================================

    if row["sensor_nearby"] == 1:

        availability -= 2

    # ========================================================
    # VARIABILIDAD
    # ========================================================

    # Pequeña variabilidad para evitar que todos los
    # registros con las mismas condiciones tengan
    # exactamente el mismo target.

    availability += np.random.normal(0, 2)

    # ========================================================
    # LIMITES
    # ========================================================

    return np.clip(
        round(availability),
        5,
        100
    )


traffic["availability"] = traffic.apply(
    calculate_availability,
    axis=1
)

# ============================================================
# DATASET FINAL
# ============================================================

features = [
    "latitude",
    "longitude",

    "hour",
    "weekday",


    "hour_sin",
    "hour_cos",

    "weekday_sin",
    "weekday_cos",

    "is_weekend",
    "is_peak",

    "vehicle_flow",
    "vehicle_flow_normalized",
    "high_traffic",
    "flow_pressure",

    "sensor_distance",
    "sensor_nearby",

    "garage_distance",
    "garages_nearby",
    

    "availability"
]

dataset = traffic[features].copy()

dataset.to_csv(
    "datasets/training_dataset_v3.csv",
    index=False
)

print("\nDataset generado:")
print(dataset.head())

print(
    "\nCantidad de registros:",
    len(dataset)
)

print(
    "\nGuardado en:",
    "datasets/training_dataset_v3.csv"
)
print("\n============================")
print("DISTRIBUCIÓN AVAILABILITY")
print("============================")

print(
    dataset["availability"].describe()
)

print("\nRangos de disponibilidad:")

print(
    pd.cut(
        dataset["availability"],
        bins=[0, 20, 40, 60, 80, 100]
    )
    .value_counts()
    .sort_index()
)