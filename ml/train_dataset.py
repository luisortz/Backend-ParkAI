import pandas as pd

print("Leyendo dataset...")



df = pd.read_csv("datasets/dataset_flujo_vehicular.csv")

print(df.head())
print()



df["datetime"] = pd.to_datetime(
    df["HORA"],
    format="%d%b%Y:%H:%M:%S"
)



df["hour"] = df["datetime"].dt.hour

# Python:
# lunes = 0
# domingo = 6

df["weekday"] = df["datetime"].dt.dayofweek



df = df.rename(columns={
    "LATITUD": "latitude",
    "LONGITUD": "longitude",
    "CANTIDAD": "vehicle_flow"
})





reference_flow = df["vehicle_flow"].quantile(0.95)

df["availability"] = (
    1 - (df["vehicle_flow"] / reference_flow)
)

df["availability"] = (
    df["availability"]
    .clip(0, 1)
    * 100
).round().astype(int)



df = df[
    [
        "latitude",
        "longitude",
        "hour",
        "weekday",
        "vehicle_flow",
        "availability"
    ]
]

print()
print("Dataset listo:")
print(df.head())

print()
print(df.describe())

print()
print("Cantidad de registros:")
print(len(df))



df.to_csv(
    "datasets/training_dataset.csv",
    index=False
)

print()
print("Dataset guardado como datasets/training_dataset.csv")