import pandas as pd
import numpy as np
import joblib

from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import GroupShuffleSplit
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    r2_score
)

print("Cargando dataset...")

df = pd.read_csv(
    "datasets/training_dataset_v3.csv"
)

# ============================================================
# GROUP
# ============================================================

# Agrupamos ubicaciones cercanas.
# Esto evita que prácticamente el mismo punto aparezca
# simultáneamente en train y test.

df["location_group"] = (
    df["latitude"].round(4).astype(str)
    + "_"
    + df["longitude"].round(4).astype(str)
)

# ============================================================
# FEATURES
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

    "sensor_distance",
    "sensor_nearby",

    "garage_distance",
    "garages_nearby"
]

X = df[features]
y = df["availability"]

groups = df["location_group"]

# ============================================================
# TRAIN / TEST
# ============================================================

splitter = GroupShuffleSplit(
    n_splits=1,
    test_size=0.20,
    random_state=42
)

train_idx, test_idx = next(
    splitter.split(
        X,
        y,
        groups=groups
    )
)

X_train = X.iloc[train_idx]
X_test = X.iloc[test_idx]

y_train = y.iloc[train_idx]
y_test = y.iloc[test_idx]

print(
    "Train:",
    len(X_train)
)

print(
    "Test:",
    len(X_test)
)

# ============================================================
# RANDOM FOREST
# ============================================================

print("\nEntrenando Random Forest...")

model = RandomForestRegressor(
    n_estimators=400,
    max_depth=None,
    min_samples_split=4,
    min_samples_leaf=2,
    max_features=0.8,
    random_state=42,
    n_jobs=-1
)

model.fit(
    X_train,
    y_train
)

print("Entrenamiento finalizado")

# ============================================================
# EVALUACIÓN
# ============================================================

pred = model.predict(X_test)

mae = mean_absolute_error(
    y_test,
    pred
)

rmse = np.sqrt(
    mean_squared_error(
        y_test,
        pred
    )
)

r2 = r2_score(
    y_test,
    pred
)

print("\n============================")
print("RESULTADOS")
print("============================")

print(
    f"MAE  : {mae:.2f}"
)

print(
    f"RMSE : {rmse:.2f}"
)

print(
    f"R²   : {r2:.4f}"
)

# ============================================================
# IMPORTANCIA DE FEATURES
# ============================================================

importance = pd.DataFrame({
    "feature": features,
    "importance": model.feature_importances_
})

importance = importance.sort_values(
    "importance",
    ascending=False
)

print("\nImportancia de variables:")
print(importance)

# ============================================================
# GUARDAR MODELO
# ============================================================

joblib.dump(
    model,
    "model/parking_model.pkl"
)

print(
    "\nModelo guardado correctamente."
)