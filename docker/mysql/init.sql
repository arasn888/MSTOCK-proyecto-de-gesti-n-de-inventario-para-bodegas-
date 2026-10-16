CREATE DATABASE IF NOT EXISTS mstock_auth       CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS mstock_catalogo   CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS mstock_inventario CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS mstock_ventas     CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS mstock_compras    CHARACTER SET utf8mb4;

GRANT ALL PRIVILEGES ON mstock_auth.*       TO 'mstock_user'@'%';
GRANT ALL PRIVILEGES ON mstock_catalogo.*   TO 'mstock_user'@'%';
GRANT ALL PRIVILEGES ON mstock_inventario.* TO 'mstock_user'@'%';
GRANT ALL PRIVILEGES ON mstock_ventas.*     TO 'mstock_user'@'%';
GRANT ALL PRIVILEGES ON mstock_compras.*    TO 'mstock_user'@'%';
FLUSH PRIVILEGES;