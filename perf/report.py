#!/usr/bin/env python3
"""
Consolida los resultados de perf/results/ en una tabla lista para Confluence.

  python3 perf/report.py            # todos los datasets encontrados
  python3 perf/report.py 100        # solo el escenario de 100 registros
"""
import json
import re
import sys
from pathlib import Path

RESULTS = Path(__file__).resolve().parent / "results"


def valor(metricas, nombre, clave):
    try:
        v = metricas[nombre]["values"][clave]
    except (KeyError, TypeError):
        return None
    return v


def fmt(v, dec=2):
    """Sin separador de miles: evita confundir 1.290 (mil doscientos) con 1.29."""
    return "n/d" if v is None else f"{v:.{dec}f}"


def main():
    filtro = sys.argv[1] if len(sys.argv) > 1 else None
    patron = re.compile(r"medicion-d(\d+)-vu(\d+)\.json$")

    corridas = []
    for f in sorted(RESULTS.glob("medicion-d*-vu*.json")):
        m = patron.search(f.name)
        if not m:
            continue
        dataset, vus = int(m.group(1)), int(m.group(2))
        if filtro and str(dataset) != filtro:
            continue
        datos = json.loads(f.read_text(encoding="utf-8"))
        corridas.append((dataset, vus, datos))

    if not corridas:
        sys.exit("No se encontraron resultados en perf/results/")

    corridas.sort(key=lambda c: (c[0], c[1]))

    for dataset in sorted({c[0] for c in corridas}):
        delformato = [c for c in corridas if c[0] == dataset]
        cond = delformato[0][2].get("condiciones", {})

        print(f"\n## Escenario L-{dataset} — {dataset} registros en el indice\n")
        print(f"minLevel {cond.get('min_level')} | showDetails {cond.get('show_details')} "
              f"| duracion {cond.get('duracion')} por nivel | mix: {cond.get('mix')}\n")
        print("| VUs | Latencia total avg | p95 | max | Matching avg | p95 | max | Throughput | Busquedas OK | Errores |")
        print("|----:|----:|----:|----:|----:|----:|----:|----:|----:|----:|")

        total_ok = 0
        for _, vus, datos in delformato:
            m = datos.get("metricas", {})
            ok = valor(m, "searches_ok", "count") or 0
            total_ok += ok
            print(f"| {vus} "
                  f"| {fmt(valor(m, 'http_req_duration', 'avg'))} ms "
                  f"| {fmt(valor(m, 'http_req_duration', 'p(95)'))} ms "
                  f"| {fmt(valor(m, 'http_req_duration', 'max'))} ms "
                  f"| {fmt(valor(m, 'matching_time', 'avg'))} ms "
                  f"| {fmt(valor(m, 'matching_time', 'p(95)'))} ms "
                  f"| {fmt(valor(m, 'matching_time', 'max'))} ms "
                  f"| {fmt(valor(m, 'http_reqs', 'rate'))} req/s "
                  f"| {fmt(ok, 0)} "
                  f"| {fmt(valor(m, 'search_errors', 'rate'), 4)} |")

        print(f"\nTotal de busquedas OK en el escenario: {fmt(total_ok, 0)}")
        print("(sumar el warm-up para cuadrar contra el contador del biller)")


if __name__ == "__main__":
    main()
