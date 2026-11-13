#!/usr/bin/env bash
# Prueba el flujo completo. Se puede ejecutar varias veces sin conflictos.
BASE="${1:-http://localhost:8080}"
API="$BASE/api/v1"
SUFIJO=$(date +%s)

json() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }
login() { curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$1\",\"password\":\"$2\"}" | json "['token']"; }

echo "== 1. Login CLIENTE y ADMIN"
TOKEN_CLI=$(login cliente@prospin.cl Cliente123)
TOKEN_ADM=$(login admin@prospin.cl Admin12345)
echo "cliente: ${TOKEN_CLI:0:15}...  admin: ${TOKEN_ADM:0:15}..."

echo "== 2. Listar productos (cliente)"
curl -s "$API/productos" -H "Authorization: Bearer $TOKEN_CLI" | python3 -c "import sys,json; print(len(json.load(sys.stdin)),'productos')"

echo "== 3. Cliente intenta crear producto (esperado 403)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/productos" \
  -H "Authorization: Bearer $TOKEN_CLI" -H "Content-Type: application/json" \
  -d '{"codigo":"X-1","nombre":"x","precio":1,"stock":1,"categoriaId":1}'

echo "== 4. Sin token (esperado 401)"
curl -s -o /dev/null -w "%{http_code}\n" "$API/productos"

echo "== 5. Comprar 2 unidades de TT-001"
ID=$(curl -s "$API/productos" -H "Authorization: Bearer $TOKEN_CLI" | python3 -c "import sys,json; print([p['id'] for p in json.load(sys.stdin) if p['codigo']=='TT-001'][0])")
curl -s -X POST "$API/boletas" -H "Authorization: Bearer $TOKEN_CLI" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productoId\":$ID,\"cantidad\":2}]}" | python3 -c "import sys,json; b=json.load(sys.stdin); print('boleta', b['id'], 'total', b['total'])"

echo "== 6. Cantidad mayor al stock (esperado 409)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/boletas" -H "Authorization: Bearer $TOKEN_CLI" \
  -H "Content-Type: application/json" -d "{\"items\":[{\"productoId\":$ID,\"cantidad\":99}]}"

echo "== 7. Admin lista todas las boletas"
curl -s "$API/boletas" -H "Authorization: Bearer $TOKEN_ADM" | python3 -c "import sys,json; print(len(json.load(sys.stdin)),'boletas')"

echo "== 8. Admin crea categoria y producto (esperado 201)"
CAT=$(curl -s -X POST "$API/categorias" -H "Authorization: Bearer $TOKEN_ADM" -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Prueba-$SUFIJO\"}" | json "['id']")
curl -s -o /dev/null -w "crear producto: %{http_code}\n" -X POST "$API/productos" -H "Authorization: Bearer $TOKEN_ADM" \
  -H "Content-Type: application/json" \
  -d "{\"codigo\":\"TEST-$SUFIJO\",\"nombre\":\"Test\",\"precio\":100,\"stock\":5,\"categoriaId\":$CAT}"

echo "== 9. Renovar token (esperado 200)"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$API/auth/refresh" -H "Authorization: Bearer $TOKEN_ADM"

echo "Listo."
