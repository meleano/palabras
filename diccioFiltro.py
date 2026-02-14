import json
import re
import unicodedata
import requests
from wordfreq import word_frequency

# ============================================================
# CONFIGURACIÓN
# ============================================================

OUTPUT_JSON = "dictionary.json"

SOURCES = [
    "https://raw.githubusercontent.com/dwyl/spanish-words/master/spanish.txt",
    "https://raw.githubusercontent.com/javierarce/spanish-wordlist/master/spanish.txt",
    "https://raw.githubusercontent.com/words/an-array-of-spanish-words/master/index.json"
]

MIN_LEN = 3
MAX_LEN = 15
MIN_FREQ = 1e-7  # umbral para eliminar palabras raras

# ============================================================
# UTILIDADES
# ============================================================

def normalize(word):
    """Normaliza palabra: minúsculas, sin tildes, mantiene ñ."""
    word = word.lower()
    word = ''.join(
        c for c in unicodedata.normalize('NFD', word)
        if unicodedata.category(c) != 'Mn' or c == 'ñ'
    )
    if not re.fullmatch(r"[a-zñ]+", word):
        return None
    if not (MIN_LEN <= len(word) <= MAX_LEN):
        return None
    return word

def classify(freq):
    """Clasifica según frecuencia real."""
    if freq >= 1e-4:
        return "fácil"
    elif freq >= 1e-6:
        return "medio"
    return "difícil"

# ============================================================
# DESCARGA DE FUENTES
# ============================================================

def download_source(url):
    print(f"Descargando: {url}")
    r = requests.get(url)
    r.raise_for_status()
    if url.endswith(".json"):
        data = r.json()
        if isinstance(data, list):
            return data
        return data.get("words", [])
    return r.text.splitlines()

# ============================================================
# PIPELINE PRINCIPAL
# ============================================================

def build_dictionary():
    print("\n=== Construyendo diccionario limpio ===\n")

    all_words = set()

    # 1. Descargar y normalizar
    for url in SOURCES:
        raw = download_source(url)
        for w in raw:
            norm = normalize(w)
            if norm:
                all_words.add(norm)

    print(f"Palabras tras normalización: {len(all_words)}")

    # 2. Calcular frecuencias reales
    freq_map = {}
    for w in all_words:
        f = word_frequency(w, "es")
        if f >= MIN_FREQ:
            freq_map[w] = f

    print(f"Palabras tras filtro de frecuencia: {len(freq_map)}")

    # 3. Clasificación por dificultad
    levels = {w: classify(freq_map[w]) for w in freq_map}

    # 4. Construcción del JSON final
    dictionary = {
        "palabras": sorted(freq_map.keys()),
        "frecuencias": freq_map,
        "niveles": levels
    }

    with open(OUTPUT_JSON, "w", encoding="utf8") as f:
        json.dump(dictionary, f, ensure_ascii=False, indent=2)

    print(f"\nDiccionario generado: {OUTPUT_JSON}")
    print(f"Total palabras: {len(freq_map)}")

# ============================================================
# EJECUCIÓN
# ============================================================

if __name__ == "__main__":
    build_dictionary()