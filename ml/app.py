"""API de predicciones ParkAI. Usa el modelo entrenado, no consulta MySQL por request."""
from pathlib import Path

import joblib
from flask import Flask, jsonify, request

from report_features import (
    FEATURE_NAMES, UADE_LAT, UADE_LON,
    haversine_m, nearest_distance_m, make_features, to_frame,
)

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model" / "parking_report_model.pkl"
if not MODEL_PATH.is_file():
    raise RuntimeError("Falta el modelo. Ejecutá: python train_model.py")

# joblib solo debe cargar archivos propios/confiables.
bundle = joblib.load(MODEL_PATH)
model = bundle["model"]
if bundle["features"] != FEATURE_NAMES:
    raise RuntimeError("Variables incompatibles: volvé a ejecutar python train_model.py")

app = Flask(__name__)


@app.get("/health")
def health():
    return jsonify({
        "status": "ok",
        "model": "parkai-random-forest-found-classifier",
        "trainingRows": bundle["sample_count"],
        "verifiedRows": bundle["verified_count"],
        "testRows": bundle["test_count"],
        "trainingContainsTestData": bundle["training_contains_test_data"],
        "zoneRadiusMeters": bundle["uade_max_radius_m"],
    })


@app.post("/predict")
def predict():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify(error="Se esperaba un objeto JSON"), 400

    try:
        row = make_features(
            data.get("lat", data.get("latitude")),
            data.get("lon", data.get("longitude")),
            data["dayOfWeek"], data["hour"],
        )
    except (ValueError, TypeError, KeyError, OverflowError) as exc:
        return jsonify(error="Entrada inválida", detail=str(exc)), 400

    lat, lon = row["latitude"], row["longitude"]
    if haversine_m(lat, lon, UADE_LAT, UADE_LON) > bundle["uade_max_radius_m"]:
        return jsonify(error="Fuera del área de cobertura UADE"), 422

    nearest = nearest_distance_m(lat, lon, bundle["observed_locations"])
    if nearest > bundle["nearest_observed_point_max_m"]:
        return jsonify(error="Faltan observaciones cercanas", nearestMeters=round(nearest)), 422

    proba = model.predict_proba(to_frame([row])[FEATURE_NAMES])[0]
    positive_idx = list(model.classes_).index(1)
    percent = round(max(0.0, min(1.0, float(proba[positive_idx]))) * 100)
    return jsonify({
        "estimatedAvailabilityPercent": percent,
        "predictionKind": "PROBABILITY_OF_FOUND",
        "modelSource": "community-ml-reports",
        "trainingContainsTestData": bundle["training_contains_test_data"],
        "trainingRows": bundle["sample_count"],
    })


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
