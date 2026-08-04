import pandas as pd
import os
import csv

DATASET_FOLDER = "datasets"

for file in os.listdir(DATASET_FOLDER):

    if not file.endswith(".csv"):
        continue

    path = os.path.join(DATASET_FOLDER, file)

    print("=" * 70)
    print(file)

    # Detectar el separador automáticamente
    with open(path, "r", encoding="utf-8") as f:
        sample = f.read(5000)
        dialect = csv.Sniffer().sniff(sample)
        separator = dialect.delimiter

    print("Separador detectado:", separator)

    df = pd.read_csv(
        path,
        sep=separator,
        low_memory=False
    )

    print(df.head())
    print()
    print(df.columns.tolist())
    print()
    print(df.shape)