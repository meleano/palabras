from wiktextract import Wiktextract
import json

INPUT_FILE = "data/eswiktionary.xml.bz2"
OUTPUT_FILE = "data/eswiktionary.json"

def extract_wiktionary():
    wx = Wiktextract(
        capture_language="es",
        capture_translations=False,
        capture_pronunciation=False,
        capture_examples=False,
        capture_redirects=False,
        capture_inflections=True,
        capture_etymology=False,
    )

    print("Procesando dump de Wiktionary… esto puede tardar varios minutos.")
    data = wx.extract(INPUT_FILE)

    with open(OUTPUT_FILE, "w", encoding="utf8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"Archivo generado: {OUTPUT_FILE}")
    print(f"Entradas procesadas: {len(data)}")

if __name__ == "__main__":
    extract_wiktionary()