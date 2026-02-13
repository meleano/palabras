import os
import re
import json
import unicodedata
import requests

# ---------------------------------------------------------
# CONFIGURACIÓN
# ---------------------------------------------------------

OUTPUT_FILE = "dictionary.json"

# Fuentes abiertas del español (muy amplias)
CORPUS_URLS = [
    "https://raw.githubusercontent.com/dwyl/spanish-words/master/spanish.txt",
    "https://raw.githubusercontent.com/javierarce/spanish-wordlist/master/spanish.txt",
    "https://raw.githubusercontent.com/words/an-array-of-spanish-words/master/index.json"
]

# ---------------------------------------------------------
# FUNCIONES AUXILIARES
# ---------------------------------------------------------

def download_corpus(url):
    print(f"Descargando: {url}")
    response = requests.get(url)
    response.raise_for_status()
    return response.text


def normalize_word(word):
    # Convertir a minúsculas
    word = word.lower()

    # Eliminar acentos (pero mantener ñ)
    word = ''.join(
        c for c in unicodedata.normalize('NFD', word)
        if unicodedata.category(c) != 'Mn' or c == 'ñ'
    )

    # Mantener solo letras
    if not re.fullmatch(r"[a-zñ]+", word):
        return None

    # Longitud mínima razonable
    if len(word) < 3:
        return None

    return word


# ---------------------------------------------------------
# PROCESAMIENTO PRINCIPAL
# ---------------------------------------------------------

def build_dictionary():
    words = set()

    for url in CORPUS_URLS:
        try:
            data = download_corpus(url)

            # Si es JSON
            if url.endswith(".json"):
                json_data = json.loads(data)
                raw_words = json_data if isinstance(json_data, list) else json_data.get("words", [])
            else:
                raw_words = data.splitlines()

            for w in raw_words:
                norm = normalize_word(w.strip())
                if norm:
                    words.add(norm)

        except Exception as e:
            print(f"Error procesando {url}: {e}")

    print(f"Total palabras después de limpieza mínima: {len(words)}")

    # Convertir a lista ordenada (solo para consistencia)
    words_list = sorted(list(words))

    dictionary_json = {"palabras": words_list}

    with open(OUTPUT_FILE, "w", encoding="utf8") as f:
        json.dump(dictionary_json, f, ensure_ascii=False, indent=2)

    print(f"Diccionario generado: {OUTPUT_FILE}")
    print(f"Total palabras finales: {len(words_list)}")


# ---------------------------------------------------------
# EJECUCIÓN
# ---------------------------------------------------------

if __name__ == "__main__":
    build_dictionary()