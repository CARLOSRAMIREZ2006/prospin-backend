#!/usr/bin/env bash
# Muestra en tabla el stock de cada producto y las últimas compras con su detalle.
# Lee los datos desde la API (la misma base de datos) y NO detiene el servicio.
# Uso: bash scripts/ver_stock.sh [URL_BASE]   (por defecto http://localhost:8080)
BASE="${1:-http://localhost:8080}"

TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"admin@prospin.cl","password":"Admin12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "No se pudo iniciar sesión. Revisa que el servicio esté activo: sudo systemctl is-active prospin"
  exit 1
fi

BASE_URL="$BASE" TOKEN="$TOKEN" python3 - << 'PY'
import os, json, urllib.request

base = os.environ["BASE_URL"]
token = os.environ["TOKEN"]

def pedir(ruta):
    req = urllib.request.Request(base + ruta, headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read().decode("utf-8"))

def tabla(titulo, columnas, filas):
    print()
    print("=" * 70)
    print(titulo)
    print("=" * 70)
    anchos = [max(len(str(c)), *(len(str(f[i])) for f in filas)) if filas else len(str(c))
              for i, c in enumerate(columnas)]
    linea = " | ".join(str(c).ljust(anchos[i]) for i, c in enumerate(columnas))
    print(linea)
    print("-" * len(linea))
    for f in filas:
        print(" | ".join(str(v).ljust(anchos[i]) for i, v in enumerate(f)))
    print(f"({len(filas)} filas)")

productos = pedir("/api/v1/productos")
tabla("PRODUCTOS Y STOCK",
      ["ID", "CODIGO", "NOMBRE", "PRECIO", "STOCK"],
      [(p["id"], p["codigo"], p["nombre"][:40], p["precio"], p["stock"]) for p in productos])

boletas = pedir("/api/v1/boletas")
boletas = sorted(boletas, key=lambda b: b["id"], reverse=True)
tabla("ÚLTIMAS COMPRAS (boletas)",
      ["BOLETA", "FECHA", "ESTADO", "TOTAL", "CLIENTE"],
      [(b["id"], str(b["fecha"])[:16], b["estado"], b["total"], b["usuario"]["email"]) for b in boletas[:10]])

detalle = []
for b in boletas[:10]:
    for d in b.get("detalles", []):
        detalle.append((b["id"], d["producto"]["codigo"], d["producto"]["nombre"][:40], d["cantidad"], d["subtotal"]))
tabla("DETALLE DE LAS ÚLTIMAS COMPRAS",
      ["BOLETA", "CODIGO", "PRODUCTO", "CANTIDAD", "SUBTOTAL"], detalle)
PY
