#!/usr/bin/env bash
# Muestra las tablas de la base H2 en la consola: tablas, categorías, productos (stock),
# usuarios, pedidos (boletas) y el detalle de cada pedido.
# Detiene el servicio mientras consulta (H2 en archivo no admite dos procesos a la vez)
# y lo vuelve a levantar al terminar.
DB="/home/ubuntu/prospin-backend/data/prospin"
H2JAR=$(find ~/.m2 -name "h2-*.jar" | head -1)

sudo systemctl stop prospin
trap 'sudo systemctl start prospin' EXIT

sql() {
  java -cp "$H2JAR" org.h2.tools.Shell -url "jdbc:h2:file:$DB" -user sa -password "" -sql "$1"
}

echo "=================== TABLAS (SHOW TABLES) ==================="
sql "SHOW TABLES;"

echo "=================== CATEGORIA ==================="
sql "SELECT ID, NOMBRE FROM CATEGORIA ORDER BY ID;"

echo "=================== PRODUCTO (stock) ==================="
sql "SELECT ID, CODIGO, NOMBRE, PRECIO, STOCK FROM PRODUCTO ORDER BY ID LIMIT 15;"

echo "=================== USUARIO (sin contraseñas) ==================="
sql "SELECT ID, NOMBRE, EMAIL, ROL FROM USUARIO ORDER BY ID;"

echo "=================== BOLETA (pedidos) ==================="
sql "SELECT ID, FECHA, ESTADO, TOTAL, USUARIO_ID FROM BOLETA ORDER BY ID DESC LIMIT 10;"

echo "=================== DETALLE_BOLETA (productos de cada pedido) ==================="
sql "SELECT D.ID, D.BOLETA_ID, P.CODIGO, P.NOMBRE, D.CANTIDAD, D.PRECIO_UNITARIO, D.SUBTOTAL FROM DETALLE_BOLETA D JOIN PRODUCTO P ON P.ID = D.PRODUCTO_ID ORDER BY D.BOLETA_ID DESC, D.ID DESC LIMIT 20;"
