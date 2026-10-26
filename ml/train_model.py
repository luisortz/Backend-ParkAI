"""ParkAI: entrenamiento de P(FOUND) a partir de parking_reports.

"""
import argparse
import getpass
import os
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, brier_score_loss, roc_auc_score

from report_features import (
    FEATURE_NAMES, MAX_UADE_RADIUS_M, MAX_TO_TRAIN_POINT_M,
    UADE_LAT, UADE_LON, haversine_m, make_features, to_frame,
)

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model" / "parking_report_model.pkl"


def read_mysql(verified_only: bool) -> pd.DataFrame:
    try:
        import pymysql
    except ImportError as exc:
        raise SystemExit("Instalá dependencias con: python -m pip install -r requirements.txt") from exc

    db_name = os.getenv("PARKAI_DB_NAME")
    db_user = os.getenv("PARKAI_DB_USER")
    if not db_name or not db_user:
        raise ValueError("Configurá PARKAI_DB_NAME y PARKAI_DB_USER")

    password = os.getenv("PARKAI_DB_PASSWORD")
    if password is None:
        password = getpass.getpass("Contraseña MySQL: ")

    conn = pymysql.connect(
        host=os.getenv("PARKAI_DB_HOST", "127.0.0.1"),
        port=int(os.getenv("PARKAI_DB_PORT", "3306")),
        database=db_name,
        user=db_user,
        password=password,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=8,
        read_timeout=30,
    )
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT COUNT(*) AS n
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'parking_reports'
                  AND column_name = 'synthetic_batch'
            """)
            has_batch_column = cur.fetchone()["n"] > 0
            if verified_only and not has_batch_column:
                raise ValueError(
                    "Falta synthetic_batch: no puedo distinguir reportes de prueba "
                    "de reportes reales. No se entrenó ningún modelo."
                )

            batch_field = "synthetic_batch" if has_batch_column else "NULL AS synthetic_batch"
            conditions = ["report_time >= DATE_SUB(NOW(), INTERVAL 180 DAY)"]
            if verified_only:
                conditions += ["synthetic_batch IS NULL", "report_time <= NOW()"]
            else:
                # No incluir observaciones futuras salvo las cargadas deliberadamente para pruebas.
                conditions.append("(report_time <= NOW() OR synthetic_batch IS NOT NULL)")
                if not has_batch_column:
                    conditions[-1] = "report_time <= NOW()"

            sql = f"""
                SELECT id, user_id, latitude, longitude,
                       report_time, report_type, {batch_field}
                FROM parking_reports
                WHERE {' AND '.join(conditions)}
                ORDER BY report_time, id
            """
            cur.execute(sql)
            data = pd.DataFrame(cur.fetchall())
    finally:
        conn.close()
    return data


def prepare(raw: pd.DataFrame, verified_only: bool):
    required = {"latitude", "longitude", "report_time", "report_type"}
    missing = required.difference(raw.columns)
    if missing:
        raise ValueError(f"Faltan columnas en reportes: {sorted(missing)}")

    df = raw.copy()
    if "synthetic_batch" not in df:
        if verified_only:
            raise ValueError("La fuente no identifica los registros de prueba")
        df["synthetic_batch"] = None

    df["report_time"] = pd.to_datetime(df["report_time"], errors="coerce")
    df["latitude"] = pd.to_numeric(df["latitude"], errors="coerce")
    df["longitude"] = pd.to_numeric(df["longitude"], errors="coerce")
    df["report_type"] = df["report_type"].astype(str).str.strip().str.upper()
    df = df.dropna(subset=["report_time", "latitude", "longitude"])
    df = df.loc[df["report_type"].isin(["FOUND", "NOT_FOUND"])]
    df = df.loc[df["latitude"].between(-90, 90) & df["longitude"].between(-180, 180)]
    df = df.loc[df.apply(
        lambda r: haversine_m(r["latitude"], r["longitude"], UADE_LAT, UADE_LON)
        <= MAX_UADE_RADIUS_M, axis=1,
    )].copy()

    df["is_test"] = df["synthetic_batch"].notna() & (
        df["synthetic_batch"].astype(str).str.strip() != ""
    )
    if verified_only:
        df = df.loc[~df["is_test"] & (df["report_time"] <= pd.Timestamp.now())].copy()

    # Evitar numerosos aportes repetidos de una misma cuenta en la misma cuadra/hora.
    # El lote de prueba se deja intacto para comprobar sus 300 casos.
    if "user_id" in df.columns:
        checked = df.loc[~df["is_test"]].copy()
        checked["lat_round"] = checked["latitude"].round(5)
        checked["lon_round"] = checked["longitude"].round(5)
        checked["hour_block"] = checked["report_time"].dt.floor("h")
        checked = checked.drop_duplicates(
            subset=["user_id", "lat_round", "lon_round", "hour_block"],
            keep="last",
        )
        df = pd.concat([checked, df.loc[df["is_test"]]], ignore_index=True)

    df = df.sort_values("report_time").reset_index(drop=True)
    if df.empty:
        raise ValueError("No hay reportes válidos dentro del área de entrenamiento UADE")

    tests = int(df["is_test"].sum())
    verified = len(df) - tests
    found = int((df["report_type"] == "FOUND").sum())
    not_found = int((df["report_type"] == "NOT_FOUND").sum())
    groups = df["latitude"].round(4).astype(str) + "_" + df["longitude"].round(4).astype(str)
    dates = df["report_time"].dt.date.nunique()
    print(f"Reportes útiles: {len(df)} | FOUND={found} | NOT_FOUND={not_found}")
    print(f"Ubicaciones distintas: {groups.nunique()} | Días: {dates}")
    print(f"Registros de prueba identificados: {tests} | Otros: {verified}")
    if tests:
        print("ATENCIÓN: este entrenamiento contiene registros generados para pruebas."
              " No demuestra precisión real.")

    if min(found, not_found) < 10 or len(df) < 80 or groups.nunique() < 10 or dates < 5:
        raise ValueError(
            "Datos insuficientes (mínimo 80 registros, 10 por clase, "
            "10 ubicaciones y 5 fechas). No se reemplaza el modelo existente."
        )
    if verified_only and "user_id" in df and df["user_id"].nunique() < 3:
        raise ValueError("Se requieren al menos 3 usuarios distintos para entrenamiento verificado")

    X = to_frame([
        make_features(r.latitude, r.longitude, r.report_time.isoweekday(), r.report_time.hour)
        for r in df.itertuples(index=False)
    ])
    y = (df["report_type"].to_numpy() == "FOUND").astype(int)
    return df, X, y, groups, tests, verified


def create_model():
    return RandomForestClassifier(
        n_estimators=250,
        max_depth=7,
        min_samples_leaf=5,
        min_samples_split=12,
        max_features="sqrt",
        random_state=42,
        n_jobs=-1,
    )


def main():
    parser = argparse.ArgumentParser(description="Entrenar ParkAI desde parking_reports")
    parser.add_argument("--verified-only", action="store_true",
                        help="Excluye registros generados para pruebas y fechas futuras")
    parser.add_argument("--csv", type=Path, help="Solo para probar sin conexión MySQL")
    args = parser.parse_args()
    if args.csv and args.verified_only:
        parser.error("--verified-only requiere MySQL con procedencia comprobable")

    raw = pd.read_csv(args.csv, encoding="utf-8-sig") if args.csv else read_mysql(args.verified_only)
    df, X, y, groups, tests, verified = prepare(raw, args.verified_only)

    # Evaluación por fecha: días posteriores separados, no una partición aleatoria.
    unique_dates = np.sort(df["report_time"].dt.normalize().unique())
    boundary = unique_dates[min(len(unique_dates) - 1, max(1, int(len(unique_dates) * 0.8)))]
    train_mask = (df["report_time"].dt.normalize() < boundary).to_numpy()
    test_mask = ~train_mask
    if (train_mask.sum() >= 30 and test_mask.sum() >= 10
            and len(np.unique(y[train_mask])) == 2
            and len(np.unique(y[test_mask])) == 2):
        check = create_model()
        check.fit(X.loc[train_mask, FEATURE_NAMES], y[train_mask])
        predictions = check.predict_proba(X.loc[test_mask, FEATURE_NAMES])[:, list(check.classes_).index(1)]
        print("Evaluación temporal interna (no valida la realidad cuando hay datos de prueba):")
        print(f"Accuracy: {accuracy_score(y[test_mask], predictions >= 0.5):.3f}")
        print(f"Brier: {brier_score_loss(y[test_mask], predictions):.3f}")
        print(f"ROC AUC: {roc_auc_score(y[test_mask], predictions):.3f}")
    else:
        print("No se informan métricas: faltan suficientes ejemplos de ambas clases por fecha")

    model = create_model()
    model.fit(X[FEATURE_NAMES], y)
    coords = list(dict.fromkeys(zip(df["latitude"].round(6), df["longitude"].round(6))))
    bundle = {
        "model": model,
        "features": FEATURE_NAMES,
        "label_meaning": "probability_of_FOUND_report_not_free_spaces",
        "sample_count": len(df),
        "test_count": tests,
        "verified_count": verified,
        "training_contains_test_data": bool(tests),
        "distinct_locations": int(groups.nunique()),
        "uade_max_radius_m": MAX_UADE_RADIUS_M,
        "nearest_observed_point_max_m": MAX_TO_TRAIN_POINT_M,
        "observed_locations": coords,
        "date_min": str(df["report_time"].min()),
        "date_max": str(df["report_time"].max()),
        "trained_at": datetime.now().isoformat(timespec="seconds"),
    }
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    temporary = MODEL_PATH.with_suffix(".tmp")
    joblib.dump(bundle, temporary)
    temporary.replace(MODEL_PATH)
    print(f"Modelo guardado en: {MODEL_PATH}")


if __name__ == "__main__":
    main()
