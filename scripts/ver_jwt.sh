#!/usr/bin/env bash
# Muestra el JWT de un usuario de demostración: token, encabezado, contenido,
# vencimiento y si la API lo acepta.
# Uso: bash scripts/ver_jwt.sh [correo] [clave] [URL_BASE]
EMAIL="${1:-admin@prospin.cl}"
PASS="${2:-Admin12345}"
BASE="${3:-http://localhost:8080}"

RESP=$(curl -s -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
TOKEN=$(printf '%s' "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "No se obtuvo token. Respuesta del servidor: $RESP"
  exit 1
fi

echo "== TOKEN JWT (copiable):"
echo "$TOKEN"
echo

TOKEN="$TOKEN" python3 - << 'PY'
import os, json, base64, datetime

token = os.environ["TOKEN"]
encabezado, contenido, firma = token.split(".")

def decodificar(parte):
    parte += "=" * (-len(parte) % 4)
    return json.loads(base64.urlsafe_b64decode(parte))

def fecha(epoch):
    return datetime.datetime.fromtimestamp(epoch, datetime.timezone.utc).strftime("%d/%m/%Y %H:%M:%S UTC")

print("== 1. Encabezado (cómo se firma):")
print("  ", decodificar(encabezado))
print()
print("== 2. Contenido (los datos del usuario):")
datos = decodificar(contenido)
for clave, valor in datos.items():
    if clave in ("iat", "exp"):
        print(f"   {clave}: {valor}  ({fecha(valor)})")
    else:
        print(f"   {clave}: {valor}")
print()
ahora = datetime.datetime.now(datetime.timezone.utc).timestamp()
minutos = int((datos["exp"] - ahora) // 60)
print(f"== 3. Vence en: {minutos} minutos")
print()
print("== 4. Firma (tercera parte, se muestra solo el inicio):")
print("  ", firma[:16] + "...")
PY

echo
echo "== 5. ¿La API acepta el token?"
curl -s -o /dev/null -w "   GET /api/v1/productos -> HTTP %{http_code} (debe ser 200)\n" \
  "$BASE/api/v1/productos" -H "Authorization: Bearer $TOKEN"
