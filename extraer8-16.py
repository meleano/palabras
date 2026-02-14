import json

INPUT = "dictionary_reducido.json"
OUTPUT = "palabras8-16.json"

MIN_LEN = 8
MAX_LEN = 16

with open(INPUT, "r", encoding="utf8") as f:
    data = json.load(f)

palabras = data["palabras"]

filtradas = [w for w in palabras if MIN_LEN <= len(w) <= MAX_LEN]

resultado = {
    "palabras": filtradas,
    "total": len(filtradas)
}

with open(OUTPUT, "w", encoding="utf8") as f:
    json.dump(resultado, f, ensure_ascii=False, indent=2)

print("Archivo generado:", OUTPUT)
print("Total palabras:", len(filtradas))