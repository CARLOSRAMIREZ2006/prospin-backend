#!/usr/bin/env bash
# Verifica el backend en ejecución: API, Swagger, datos semilla y permisos.
# Uso: bash scripts/verificar_backend.sh [URL_BASE]
BASE="${1:-http://localhost:8080}"
API="$BASE/api/v1"
OK=0; FALLA=0

check() {  # check "descripcion" condicion_bash
  if eval "$2"; then echo "  [OK]    $1"; OK=$((OK+1))
  else echo "  [FALLA] $1"; FALLA=$((FALLA+1)); fi
}

echo "== 1. Servicio"
codigo=$(curl -s -o /dev/null -w "%{http_code}" "$API/productos")
check "Sin token responde 401 (API protegida)" '[ "$codigo" = "401" ]'

echo "== 2. Swagger"
sw=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/swagger-ui.html")
check "Swagger UI accesible (200 o 302)" '[ "$sw" = "200" ] || [ "$sw" = "302" ]'
curl -s "$BASE/v3/api-docs" -o /tmp/openapi.json
python3 - << 'PY'
import json, sys
try:
    d = json.load(open('/tmp/openapi.json'))
except Exception:
    print("  [FALLA] No se pudo leer /v3/api-docs"); sys.exit(1)
rutas = d.get('paths', {})
esperadas = ['/api/v1/auth/registro','/api/v1/auth/login','/api/v1/auth/refresh',
  '/api/v1/productos','/api/v1/productos/{id}','/api/v1/categorias','/api/v1/categorias/{id}',
  '/api/v1/boletas','/api/v1/boletas/{id}','/api/v1/usuarios','/api/v1/usuarios/{id}']
faltan = [r for r in esperadas if r not in rutas]
print(f"  [{'OK' if not faltan else 'FALLA'}]    Rutas documentadas: {len(rutas)} (faltan: {faltan or 'ninguna'})")
# Cada endpoint con varios métodos debe mostrar códigos de error
con_errores = 0; total = 0
for ruta, metodos in rutas.items():
    for m, op in metodos.items():
        if m not in ('get','post','put','delete'): continue
        total += 1
        if any(c.startswith('4') for c in op.get('responses', {})): con_errores += 1
print(f"  [{'OK' if con_errores == total else 'FALLA'}]    Operaciones con códigos de error documentados: {con_errores}/{total}")
sys.exit(0 if not faltan and con_errores == total else 1)
PY
[ $? -eq 0 ] && OK=$((OK+1)) || FALLA=$((FALLA+1))

echo "== 3. Login y datos semilla"
token_admin=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"admin@prospin.cl","password":"Admin12345"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
check "Login de administrador entrega token" '[ -n "$token_admin" ]'
token_cli=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"cliente@prospin.cl","password":"Cliente123"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
check "Login de cliente entrega token" '[ -n "$token_cli" ]'

n_prod=$(curl -s "$API/productos" -H "Authorization: Bearer $token_admin" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
check "Catálogo con al menos 80 productos (encontrados: ${n_prod:-0})" '[ "${n_prod:-0}" -ge 80 ]'
n_cat=$(curl -s "$API/categorias" -H "Authorization: Bearer $token_admin" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
check "Categorías cargadas: ${n_cat:-0}" '[ "${n_cat:-0}" -ge 6 ]'

echo "== 4. Permisos por rol"
c=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/productos" -H "Authorization: Bearer $token_cli" \
  -H "Content-Type: application/json" -d '{}')
check "Cliente no puede crear productos (403)" '[ "$c" = "403" ]'
c=$(curl -s -o /dev/null -w "%{http_code}" "$API/usuarios" -H "Authorization: Bearer $token_cli")
check "Cliente no puede listar usuarios (403)" '[ "$c" = "403" ]'
c=$(curl -s -o /dev/null -w "%{http_code}" "$API/usuarios" -H "Authorization: Bearer $token_admin")
check "Administrador lista usuarios (200)" '[ "$c" = "200" ]'

echo
echo "RESULTADO: $OK correctas, $FALLA con falla"
[ $FALLA -eq 0 ]
