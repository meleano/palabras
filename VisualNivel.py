import json
from collections import Counter, defaultdict

LEVELS_FILE = "levels.json"

PREFIX_LEN = 2  # mismo valor que en el generador

def signature(word):
    return Counter(word)

def is_submultiset(a, b):
    return not (a - b)

def visualize_level(level, index):
    letters = level["letras"]
    words = level["palabras"]
    difficulty = level["dificultad"]

    n = len(letters)
    min_len = max(4, n - 3)
    max_len = n

    print("=" * 60)
    print(f" NIVEL {index}")
    print("=" * 60)
    print(f"Letras ({n}): {letters}")
    print(f"Dificultad: {difficulty}")
    print(f"Rango permitido: {min_len}–{max_len} letras")
    print(f"Palabras objetivo ({len(words)}):")
    print()

    sig_letters = signature(letters)

    # Prefijos
    prefix_buckets = defaultdict(list)

    for w in words:
        prefix_buckets[w[:PREFIX_LEN]].append(w)

    # Mostrar palabras con análisis
    for w in words:
        ok_len = min_len <= len(w) <= max_len
        ok_letters = is_submultiset(signature(w), sig_letters)

        status = []
        if not ok_len:
            status.append("❌LONGITUD")
        if not ok_letters:
            status.append("❌LETRAS")

        if len(prefix_buckets[w[:PREFIX_LEN]]) > 1:
            status.append("⚠ PREFIJO")

        status_str = " ".join(status) if status else "✔ OK"

        print(f"  - {w:<15} ({len(w)} letras)  {status_str}")

    # Resumen de prefijos
    print("\nPrefijos detectados:")
    for pref, ws in prefix_buckets.items():
        if len(ws) > 1:
            print(f"  {pref}: {ws}  ⚠ COLISIÓN")
        else:
            print(f"  {pref}: {ws}")

    print("\n")

def main():
    with open(LEVELS_FILE, "r", encoding="utf8") as f:
        levels = json.load(f)

    print(f"Total de niveles cargados: {len(levels)}\n")

    for i, lvl in enumerate(levels, start=1):
        visualize_level(lvl, i)

if __name__ == "__main__":
    main()