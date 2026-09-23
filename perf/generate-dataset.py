#!/usr/bin/env python3
"""
Genera datasets mock de N registros para la medicion de Linea Base 1.0.

Respeta el esquema exacto de mock-provider/MO.csv (28 columnas, delimitador ';',
comillas en todos los campos, UTF-8, LF) para que ms-ingestor lo procese sin
cambios de mapeo.

Salidas:
  mock-provider/MO-<N>.csv   dataset para servir por el mock-provider
  perf/queries-<N>.json      mix de consultas derivado del dataset

Uso:
  python3 perf/generate-dataset.py 100
  python3 perf/generate-dataset.py 1000
"""
import csv
import json
import sys
import unicodedata
from pathlib import Path

# Los primeros 20 de cada pool son los de MO.csv; los otros 20 extienden el
# espacio de nombres para que 1000 registros no repitan combinacion.
FIRST_NAMES = [
    "Juan", "María", "Carlos", "Ana", "Luis", "Sofía", "Diego", "Laura",
    "Martín", "Valentina", "Jorge", "Camila", "Pablo", "Lucía", "Federico",
    "Julieta", "Ricardo", "Paula", "Nicolás", "Carolina",
    "Esteban", "Florencia", "Gonzalo", "Mariana", "Hernán", "Agustina",
    "Rodrigo", "Victoria", "Sebastián", "Renata", "Emilio", "Bárbara",
    "Tomás", "Guadalupe", "Ignacio", "Micaela", "Alejandro", "Rocío",
    "Facundo", "Delfina",
]

LAST_NAMES = [
    "Fernández", "García", "Torres", "Ruiz", "Castro", "Silva", "Gómez",
    "López", "Sánchez", "Álvarez", "Herrera", "Molina", "Pérez", "Rodríguez",
    "Martínez", "Romero", "Benítez", "Suárez", "Rojas", "Díaz",
    "Acosta", "Medina", "Vargas", "Ibarra", "Navarro", "Cabrera", "Ortega",
    "Peralta", "Aguirre", "Cardozo", "Villalba", "Quiroga", "Bustos",
    "Figueroa", "Maldonado", "Sosa", "Ledesma", "Paredes", "Ocampo", "Zárate",
]

COUNTRIES = ["Argentina", "Uruguay", "Paraguay", "Chile", "Brazil", "Peru"]
CITIES = ["Buenos Aires", "Córdoba", "Rosario", "Mendoza", "La Plata",
          "Montevideo", "Asunción", "Santiago", "Lima", "São Paulo"]
POSITIONS = ["Public Official", "Business Executive", "Government Advisor",
             "Former Official", "Private Individual"]
TITLES = ["Mr", "Ms", "Dr", "Lic", "Ing"]
CATEGORIES = [
    ("PEP", "Politically Exposed Person", "pep"),
    ("SANCTION", "Sanctions", "sanction"),
    ("RISK", "High Risk Individual", "risk"),
    ("WATCHLIST", "Watchlist", "watchlist"),
]

HEADER = [
    "Age", "AGE DATE (AS OF DATE)", "Aliases", "ALTERNATIVE SPELLING",
    "Category", "Companies", "Countries", "Deceased", "DOB", "E/I", "Editor",
    "Entered", "Updated", "EXTERNAL SOURCES", "Further Information", "SSN",
    "Passports", "IDENTIFICATION NUMBERS", "Keywords", "Linked To", "Locations",
    "First Name", "Last Name", "PLACE OF BIRTH", "Position", "Sub-Category",
    "Title", "UID",
]

AS_OF_DATE = "2026/09/20"
MAX_RECORDS = len(FIRST_NAMES) * len(LAST_NAMES)


def strip_accents(text):
    """'Juan Fernández' -> 'Juan Fernandez' (columna ALTERNATIVE SPELLING)."""
    return "".join(c for c in unicodedata.normalize("NFD", text)
                   if unicodedata.category(c) != "Mn")


def names_for(uid):
    """Combinacion unica de nombre y apellido para cada uid hasta MAX_RECORDS."""
    i = uid - 1
    return FIRST_NAMES[i % len(FIRST_NAMES)], LAST_NAMES[(i // len(FIRST_NAMES)) % len(LAST_NAMES)]


def build_row(uid):
    first, last = names_for(uid)
    full = f"{first} {last}"
    category, subcategory, keyword = CATEGORIES[(uid - 1) % len(CATEGORIES)]
    country = COUNTRIES[(uid - 1) % len(COUNTRIES)]
    city = CITIES[(uid - 1) % len(CITIES)]

    return [
        str(66 - uid % 60),                      # Age
        AS_OF_DATE,                              # AGE DATE (AS OF DATE)
        f"{first[0]}. {last};{full} Test",       # Aliases
        strip_accents(full),                     # ALTERNATIVE SPELLING
        category,                                # Category
        f"Mock Company {uid % 9 + 1};Demo Holdings {uid % 9 + 1}",
        country,                                 # Countries
        "",                                      # Deceased
        f"{1960 + uid % 60:04d}-{(uid - 1) % 12 + 1:02d}-{(uid - 1) % 28 + 1:02d}",
        "I",                                     # E/I
        "MOCK_GENERATOR",                        # Editor
        f"2026/08/{(uid - 1) % 28 + 1:02d}",     # Entered
        f"2026/09/{(uid - 1) % 28 + 1:02d}",     # Updated
        "MockSourceA;MockSourceB",               # EXTERNAL SOURCES
        f"Fictitious mock record {uid} for Smart Lists testing only",
        f"{99000000000 + uid}",                  # SSN
        f"MOCKP{uid:06d}",                       # Passports
        f"MOCKID{uid:07d}",                      # IDENTIFICATION NUMBERS
        f"mock;test;{keyword}",                  # Keywords
        "",                                      # Linked To
        f"{city};{country}",                     # Locations
        first,                                   # First Name
        last,                                    # Last Name
        city,                                    # PLACE OF BIRTH
        POSITIONS[(uid - 1) % len(POSITIONS)],   # Position
        subcategory,                             # Sub-Category
        TITLES[(uid - 1) % len(TITLES)],         # Title
        str(uid),                                # UID
    ]


def typo_variant(name, uid):
    """Variante con error tipografico, para el escenario fuzzy.

    Siempre devuelve algo distinto del nombre original: si no, la consulta
    'fuzzy' seria en realidad una exacta y el mix quedaria falseado.
    """
    plain = strip_accents(name)
    first, last = plain.split(" ", 1)

    candidates = [
        plain,                                              # sin acentos
        f"{first} {last[:-2]}{last[-1]}{last[-2]}",         # ultimas dos transpuestas
        f"{first} {last[:2]}{last[3:4]}{last[2:3]}{last[4:]}",  # transposicion interna
        f"{first} {last[:-1]}",                             # falta la ultima letra
    ]
    distintos = [c for c in candidates if c != name]
    if not distintos:
        raise ValueError(f"No se pudo generar variante para {name!r}")
    return distintos[uid % len(distintos)]


def build_queries(total):
    """Mix del test: presentes, fuzzy y ausentes."""
    present = [f"{n[0]} {n[1]}" for n in (names_for(u) for u in range(1, total + 1))]
    step = max(1, total // 20)
    sample = present[::step][:20]

    return {
        "dataset_size": total,
        "exact": sample,
        "fuzzy": [typo_variant(name, i) for i, name in enumerate(sample, start=1)],
        "absent": [
            "Zoltan Kovacs", "Ingrid Lindqvist", "Hiroshi Tanaka",
            "Fatima Al Rashid", "Dmitri Volkov", "Aiko Nakamura",
            "Olumide Adebayo", "Petra Novakova", "Yusuf Demir", "Anneke Visser",
        ],
    }


def main():
    if len(sys.argv) != 2 or not sys.argv[1].isdigit():
        sys.exit("Uso: python3 perf/generate-dataset.py <cantidad-de-registros>")

    total = int(sys.argv[1])
    if not 1 <= total <= MAX_RECORDS:
        sys.exit(f"La cantidad debe estar entre 1 y {MAX_RECORDS} "
                 f"(limite del pool de nombres: {len(FIRST_NAMES)}x{len(LAST_NAMES)})")

    root = Path(__file__).resolve().parent.parent
    csv_path = root / "mock-provider" / f"MO-{total}.csv"
    json_path = root / "perf" / f"queries-{total}.json"

    with csv_path.open("w", encoding="utf-8", newline="\n") as f:
        writer = csv.writer(f, delimiter=";", quotechar='"',
                            quoting=csv.QUOTE_ALL, lineterminator="\n")
        writer.writerow(HEADER)
        for uid in range(1, total + 1):
            writer.writerow(build_row(uid))

    json_path.write_text(
        json.dumps(build_queries(total), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")

    print(f"{csv_path.relative_to(root)}: {total} registros")
    print(f"{json_path.relative_to(root)}: mix de consultas")


if __name__ == "__main__":
    main()
