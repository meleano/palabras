import json
import random
from collections import Counter, defaultdict

print("=== INICIO DEL GENERADOR DE NIVELES ===")

DICT_FILE = "dictionary.json"
OUTPUT_LEVELS = "levels.json"

NUM_LEVELS = 200
MIN_WORDS = 5
MAX_WORDS = 40
PREFIX_LEN = 2
MAX_ATTEMPTS_PER_LEVEL = 200

LETTER_COUNTS = {
    "fácil": 5,
    "medio": 6,
    "difícil": 7
}

# ------------------ utilidades ------------------ #

def letter_signature(s):
    return tuple(sorted(Counter(s).items()))

def is_submultiset(sig_word, sig_letters):
    return not (Counter(dict(sig_word)) - Counter(dict(sig_letters)))

def generate_letter_set(n):
    freq = "aaaaabbcccddddeeeeeeeeeefffggghhhhiiiiijkllllmmmnnnnnnñoooooooppqrrrrrrsssssstttttuuuvvwwxxyyyzz"
    return "".join(random.choice(freq) for _ in range(n))

# ------------------ carga diccionario ------------------ #

print("Cargando dictionary.json...")

try:
    with open(DICT_FILE, "r", encoding="utf8") as f:
        data = json.load(f)
except Exception as e:
    print("ERROR: No se pudo cargar dictionary.json")
    print(e)
    exit(1)

WORDS = data.get("palabras", [])
LEVELS = data.get("niveles", {})

print(f"Palabras cargadas: {len(WORDS)}")

if len(WORDS) == 0:
    print("ERROR: El diccionario está vacío. No se pueden generar niveles.")
    exit(1)

# ------------------ índice optimizado ------------------ #

INDEX_BY_LEN = defaultdict(list)
for w in WORDS:
    INDEX_BY_LEN[len(w)].append(w)

WORD_SIG = {w: letter_signature(w) for w in WORDS}

# ------------------ lógica ------------------ #

def classify_level(words):
    score = sum(
        1 if LEVELS[w]=="fácil" else 2 if LEVELS[w]=="medio" else 3
        for w in words
    ) / len(words)

    if score < 1.7:
        return "fácil"
    elif score < 2.3:
        return "medio"
    return "difícil"

def filter_prefix_collisions(words):
    buckets = defaultdict(list)
    for w in words:
        buckets[w[:PREFIX_LEN]].append(w)

    result = []
    for pref, ws in buckets.items():
        if len(ws) == 1:
            result.append(ws[0])
        else:
            ws.sort(key=len, reverse=True)
            result.append(ws[0])
    return result

def generate_level(target):
    n_letters = LETTER_COUNTS[target]

    for attempt in range(MAX_ATTEMPTS_PER_LEVEL):
        letters = generate_letter_set(n_letters)
        sig_letters = letter_signature(letters)

        candidates = []
        for L in range(n_letters - 2, n_letters + 1):
            if L < 3:
                continue
            for w in INDEX_BY_LEN[L]:
                if is_submultiset(WORD_SIG[w], sig_letters):
                    candidates.append(w)

        if not (MIN_WORDS <= len(candidates) <= MAX_WORDS):
            continue

        candidates = filter_prefix_collisions(candidates)

        if len(candidates) < MIN_WORDS:
            continue

        difficulty = classify_level(candidates)

        print(f"Nivel generado con letras {letters} ({len(candidates)} palabras)")
        return {
            "letras": letters,
            "palabras": sorted(candidates),
            "dificultad": difficulty
        }

    print("AVISO: No se pudo generar un nivel válido tras muchos intentos.")
    return None

# ------------------ generación masiva ------------------ #

levels = []

print("Generando niveles...")

for i in range(NUM_LEVELS):
    target = random.choice(["fácil", "medio", "difícil"])
    lvl = generate_level(target)

    if lvl is None:
        lvl = {
            "letras": "AAAAA",
            "palabras": ["aaa"],
            "dificultad": "fácil"
        }

    levels.append(lvl)

print("Guardando levels.json...")

try:
    with open(OUTPUT_LEVELS, "w", encoding="utf8") as f:
        json.dump(levels, f, ensure_ascii=False, indent=2)
    print("levels.json creado correctamente.")
except Exception as e:
    print("ERROR al guardar levels.json:")
    print(e)

print("=== FIN DEL GENERADOR ===")