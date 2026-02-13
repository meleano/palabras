import json
import unicodedata
import re
import pandas as pd

# Cargar corpus base
with open("spanish_words.txt", "r", encoding="utf8") as f:
    raw_words = [w.strip().lower() for w in f.readlines()]

# Normalizar (sin acentos, mantener ñ)
def normalize(word):
    word = ''.join(
        c for c in unicodedata.normalize('NFD', word)
        if unicodedata.category(c) != 'Mn' or c == 'ñ'
    )
    return word if re.fullmatch(r"[a-zñ]{3,20}", word) else None

words = set(filter(None, map(normalize, raw_words)))

# Cargar frecuencia real (SUBTLEX-ESP o EsPal)
freq_df = pd.read_csv("subtlex_esp.csv")  # debe tener columnas: 'word', 'freq'

# Filtrar palabras raras (frecuencia < umbral)
freq_df["word"] = freq_df["word"].str.lower().map(normalize)
freq_df = freq_df.dropna()
freq_df = freq_df[freq_df["freq"] >= 5]  # umbral ajustable

# Unir corpus con frecuencia
valid_words = words.intersection(set(freq_df["word"]))
merged = freq_df[freq_df["word"].isin(valid_words)].copy()

# Clasificar por nivel
def classify(freq):
    if freq >= 1000: return "fácil"
    elif freq >= 100: return "medio"
    else: return "difícil"

merged["nivel"] = merged["freq"].apply(classify)

# Estadísticas
stats = merged["nivel"].value_counts().to_dict()
print("Distribución por nivel:", stats)

# Exportar a JSON
output = {
    "palabras": merged["word"].tolist(),
    "niveles": merged.set_index("word")["nivel"].to_dict()
}

with open("dictionary.json", "w", encoding="utf8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)