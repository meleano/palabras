import json
from wordfreq import word_frequency

INPUT = "dictionary.json"
OUTPUT = "dictionary_reducido.json"

MIN_FREQ = 1e-7
MIN_LEN = 3
MAX_LEN = 15

def classify(freq):
    if freq >= 1e-4:
        return "fácil"
    elif freq >= 1e-6:
        return "medio"
    return "difícil"

with open(INPUT, "r", encoding="utf8") as f:
    data = json.load(f)

palabras = data["palabras"]

filtradas = {}

for w in palabras:
    if not (MIN_LEN <= len(w) <= MAX_LEN):
        continue

    freq = word_frequency(w, "es")
    if freq < MIN_FREQ:
        continue

    filtradas[w] = freq

# ordenar por frecuencia
ordenadas = dict(sorted(filtradas.items(), key=lambda x: x[1], reverse=True))

# clasificar
niveles = {w: classify(freq) for w, freq in ordenadas.items()}

resultado = {
    "palabras": list(ordenadas.keys()),
    "frecuencias": ordenadas,
    "niveles": niveles
}

with open(OUTPUT, "w", encoding="utf8") as f:
    json.dump(resultado, f, ensure_ascii=False, indent=2)

print("Diccionario reducido generado:", OUTPUT)
print("Total palabras:", len(ordenadas))