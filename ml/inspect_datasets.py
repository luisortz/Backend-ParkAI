import pandas as pd
import os

files = [
    "dataset_flujo_vehicular.csv",
    "estacionamiento_via_publica.csv",
    "sensores.csv",
    "conteo-vehicular.csv",
    "estacionamientos-concesionados-de-movilidad-sustentable.csv"
]

for file in files:

    print("="*80)
    print(file)

    path = os.path.join("datasets", file)

    try:

        with open(path, encoding="utf8") as f:
            first = f.readline()

        sep = ";" if first.count(";") > first.count(",") else ","

        df = pd.read_csv(path, sep=sep)

        print(df.columns.tolist())

    except Exception as e:
        print(e)