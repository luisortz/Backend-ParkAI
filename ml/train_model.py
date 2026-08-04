import pandas as pd
import joblib

from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score

print("Cargando dataset...")

df = pd.read_csv("datasets/training_dataset_v2.csv")

X = df[
    [
        "latitude",
        "longitude",
        "hour",
        "weekday",
        "vehicle_flow",
        "sensor_nearby",
        "garage_distance"
    ]
]

y = df["availability"]

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42
)

print("Entrenando Random Forest...")

model = RandomForestRegressor(
    n_estimators=200,
    random_state=42,
    n_jobs=-1
)

model.fit(X_train, y_train)

print("Entrenamiento finalizado")

pred = model.predict(X_test)

print("MAE:", mean_absolute_error(y_test, pred))
print("R² :", r2_score(y_test, pred))

joblib.dump(
    model,
    "model/parking_model.pkl"
)

print("Modelo guardado")