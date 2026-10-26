"""Variables idénticas para entrenamiento e inferencia del modelo de reportes.

La salida del clasificador es P(report_type == FOUND), NO el porcentaje
observado de espacios físicos libres.
"""
import math
import pandas as pd

UADE_LAT = -34.61708
UADE_LON = -58.38234
MAX_UADE_RADIUS_M = 750.0
MAX_TO_TRAIN_POINT_M = 260.0

FEATURE_NAMES = [
    "latitude", "longitude", "dayOfWeek", "hour",
    "hour_sin", "hour_cos", "weekday_sin", "weekday_cos",
    "is_weekend", "is_peak", "distance_uade_m",
]


def haversine_m(lat1, lon1, lat2, lon2):
    earth = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = p2 - p1
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2 * earth * math.asin(min(1.0, math.sqrt(a)))


def nearest_distance_m(lat, lon, locations):
    return min(haversine_m(lat, lon, a, b) for a, b in locations) if locations else math.inf


def make_features(lat, lon, day_of_week, hour):
    lat, lon = float(lat), float(lon)
    day_of_week, hour = int(day_of_week), int(hour)
    if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180):
        raise ValueError("Coordenadas inválidas")
    if not 1 <= day_of_week <= 7 or not 0 <= hour <= 23:
        raise ValueError("dayOfWeek debe ser 1..7 y hour debe ser 0..23")
    wd = day_of_week - 1
    return {
        "latitude": lat, "longitude": lon,
        "dayOfWeek": day_of_week, "hour": hour,
        "hour_sin": math.sin(2 * math.pi * hour / 24),
        "hour_cos": math.cos(2 * math.pi * hour / 24),
        "weekday_sin": math.sin(2 * math.pi * wd / 7),
        "weekday_cos": math.cos(2 * math.pi * wd / 7),
        "is_weekend": int(day_of_week >= 6),
        "is_peak": int(day_of_week <= 5 and (7 <= hour <= 10 or 17 <= hour <= 20)),
        "distance_uade_m": haversine_m(lat, lon, UADE_LAT, UADE_LON),
    }


def to_frame(rows):
    return pd.DataFrame(rows, columns=FEATURE_NAMES)
