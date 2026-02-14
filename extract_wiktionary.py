# extract_wiktionary.py
import json
from wiktextract import WiktionaryExtractor

INPUT_FILE = "data/eswiktionary-latest-pages-articles.xml.bz2"
OUTPUT_FILE = "data/eswiktionary.json"

def extract():
    extractor = WiktionaryExtractor(
        capture_language="es",
        capture_translations=False,
        capture_pronunciation=False,
        capture_examples=False,
        capture_redirects=False,
        capture_inflections=True,
        capture_etymology=False,
    )

    print("Procesando dump de Wiktionary…")
    data = extractor.extract(INPUT_FILE)

    with open(OUTPUT_FILE, "w", encoding="utf8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"JSON generado: {OUTPUT_FILE}")
    print(f"Entradas procesadas: {len(data)}")

if __name__ == "__main__":
    extract()