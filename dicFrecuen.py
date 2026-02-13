import os
import re
import json
import unicodedata
import requests
import pandas as pd
from collections import Counter, defaultdict
from wordfreq import word_frequency  # frecuencias por idioma

# ---------------------------------------------------------
# CONFIGURACIÓN
# ---------------------------------------------------------

RAW_FILE = "spanish_words.txt"
OUTPUT_JSON = "dictionary.json"
OUTPUT_CSV = "dictionary.csv"
OUTPUT_TXT = "dictionary.txt"
OUTPUT_STATS = "stats.json"

CORPUS_URLS = [
    "https://raw.githubusercontent.com/dwyl/spanish-words/master/spanish.txt",
    "https://raw.githubusercontent.com/javierarce/spanish-wordlist/master/spanish.txt",
    "https://raw.githubusercontent.com/words/an-array-of-spanish-words/master/index.json"
]

# ---------------------------------------------------------
# DESCARGA DE CORPUS
# ---------------------------------------------------------

def download_corpus(url):
    print(f"Descargando: {url}")
    response = requests.get(url)
    response.raise_for_status()
    return response.text


def build_raw_corpus():
    print("\n=== Construyendo spanish_words.txt ===\n")

    all_words = []

    for url in CORPUS_URLS:
        try:
            data = download_corpus(url)

            if url.endswith(".json"):
                json_data = json.loads(data)
                raw_words = json_data if isinstance(json_data, list) else json_data.get("words", [])
            else:
                raw_words = data.splitlines()

            all_words.extend(raw_words)

        except Exception as e:
            print(f"Error procesando {url}: {e}")

    print(f"Total de palabras brutas descargadas: {len(all_words)}")

    with open(RAW_FILE, "w", encoding="utf8") as f:
        for w in all_words:
            f.write(w.strip() + "\n")

    print(f"Archivo generado: {RAW_FILE}\n")


# ---------------------------------------------------------
# NORMALIZACIÓN Y LIMPIEZA
# ---------------------------------------------------------

def normalize_word(word):
    word = word.lower()

    word = ''.join(
        c for c in unicodedata.normalize('NFD', word)
        if unicodedata.category(c) != 'Mn' or c == 'ñ'
    )

    if not re.fullmatch(r"[a-zñ]+", word):
        return None

    if len(word) < 3:
        return None

    return word


# ---------------------------------------------------------
# CLASIFICACIÓN POR FRECUENCIA (usando wordfreq)
# ---------------------------------------------------------

def get_freq(word):
    # word_frequency devuelve frecuencia relativa (0–1) en escala logarítmica
    # multiplicamos para tener algo más legible
    return word_frequency(word, "es") * 1_000_000


def classify(freq):
    if freq >= 5000:
        return "fácil"
    elif freq >= 500:
        return "medio"
    else:
        return "difícil"


# ---------------------------------------------------------
# PROCESAMIENTO PRINCIPAL
# ---------------------------------------------------------

def build_dictionary():
    print("\n=== Procesando corpus ===\n")

    with open(RAW_FILE, "r", encoding="utf8") as f:
        raw_words = [w.strip() for w in f.readlines()]

    print(f"Palabras brutas cargadas: {len(raw_words)}")

    words = set()
    for w in raw_words:
        norm = normalize_word(w)
        if norm:
            words.add(norm)

    print(f"Palabras tras limpieza mínima: {len(words)}")

    # Frecuencias con wordfreq
    freq_map = {}
    for w in words:
        f = get_freq(w)
        if f > 0:  # filtra palabras totalmente desconocidas
            freq_map[w] = f

    filtered = list(freq_map.keys())
    print(f"Palabras con frecuencia conocida: {len(filtered)}")

    # Clasificación por niveles
    levels = {w: classify(freq_map[w]) for w in filtered}

    # Diccionarios por longitud
    by_length = defaultdict(list)
    for w in filtered:
        by_length[len(w)].append(w)

    # Estadísticas
    stats = {
        "total": len(filtered),
        "por_nivel": Counter(levels.values()),
        "por_longitud": {k: len(v) for k, v in by_length.items()}
    }

    # Exportación JSON
    output_json = {
        "palabras": filtered,
        "niveles": levels,
        "frecuencias": {w: freq_map[w] for w in filtered}
    }

    with open(OUTPUT_JSON, "w", encoding="utf8") as f:
        json.dump(output_json, f, ensure_ascii=False, indent=2)

    # Exportación CSV
    df = pd.DataFrame({
        "word": filtered,
        "freq": [freq_map[w] for w in filtered],
        "nivel": [levels[w] for w in filtered],
        "longitud": [len(w) for w in filtered]
    })
    df.to_csv(OUTPUT_CSV, index=False, encoding="utf8")

    # Exportación TXT
    with open(OUTPUT_TXT, "w", encoding="utf8") as f:
        for w in filtered:
            f.write(w + "\n")

    # Exportación estadísticas
    with open(OUTPUT_STATS, "w", encoding="utf8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)

    print("\n=== Exportación completada ===")
    print(f"JSON: {OUTPUT_JSON}")
    print(f"CSV: {OUTPUT_CSV}")
    print(f"TXT: {OUTPUT_TXT}")
    print(f"Estadísticas: {OUTPUT_STATS}\n")


# ---------------------------------------------------------
# EJECUCIÓN
# ---------------------------------------------------------

if __name__ == "__main__":
    build_raw_corpus()
    build_dictionary()