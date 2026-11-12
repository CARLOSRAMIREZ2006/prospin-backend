#!/usr/bin/env bash
# Prueba el flujo completo contra el servidor en marcha.
# Uso: bash scripts/probar_api.sh [URL_BASE]
BASE="${1:-http://localhost:8080}"
API="$BASE/api/v1"

json() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }

echo "== 1. Login como CLIENTE"
TOKEN_CLI=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"cliente@prospin.cl","password":"Cliente123"}' | json "['token']")
echo "token cliente obtenido: ${TOKEN_CLI:0:20}..."

echo "== 2. Login como ADMIN"
TOKEN_ADM=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"admin@prospin.cl","password":"Admin12345"}' | json "['token']")

echo "== 3. Listar productos (cliente)"
curl -s "$API/productos" -H "Authorization: Bearer $TOKEN_CLI" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d),'productos')"

echo "== 4. Cliente intenta crear producto (debe ser 403)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/productos" \
  -H "Authorization: Bearer $TOKEN_CLI" -H "Content-Type: application/json" \
  -d '{"codigo":"X-1","nombre":"x","precio":1,"stock":1,"categoriaId":1}'

echo "== 5. Sin token (debe ser 401)"
curl -s -o /dev/null -w "%{http_code}\n" "$API/productos"

echo "== 6. Comprar 2 unidades de TT-001"
ID=$(curl -s "$API/productos" -H "Authorization: Bearer $TOKEN_CLI" | python3 -c "import sys,json; print([p['id'] for p in json.load(sys.stdin) if p['codigo']=='TT-001'][0])")
curl -s -X POST "$API/boletas" -H "Authorization: Bearer $TOKEN_CLI" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productoId\":$ID,\"cantidad\":2}]}" | python3 -m json.tool | head -30

echo "== 7. Cantidad mayor al stock (debe ser 409)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/boletas" -H "Authorization: Bearer $TOKEN_CLI" \
  -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":$ID,\"cantidad\":9999}]}"

echo "== 8. Admin lista todas las boletas"
curl -s "$API/boletas" -H "Authorization: Bearer $TOKEN_ADM" | python3 -c "import sys,json; print(len(json.load(sys.stdin)),'boletas')"

echo "== 9. Crear categoría y producto (admin)"
CAT=$(curl -s -X POST "$API/categorias" -H "Authorization: Bearer $TOKEN_ADM" -H "Content-Type: application/json" \
  -d '{"nombre":"Pruebas"}' | json "['id']")
curl -s -o /dev/null -w "crear producto: %{http_code}\n" -X POST "$API/productos" -H "Authorization: Bearer $TOKEN_ADM" \
  -H "Content-Type: application/json" -d "{\"codigo\":\"TEST-1\",\"nombre\":\"Test\",\"precio\":100,\"stock\":5,\"categoriaId\":$CAT}"

echo "== 10. Renovar token"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/auth/refresh" -H "Authorization: Bearer $TOKEN_ADM"

echo "Listo."
