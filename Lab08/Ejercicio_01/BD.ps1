# Terminar conexiones existentes
$psql = "C:\Program Files\PostgreSQL\18\bin\psql"

@"
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('almacen_arequipa', 'almacen_lima', 'almacen_cusco');
"@ | & $psql -U postgres 2>$null

# Eliminar bases de datos
@"
DROP DATABASE IF EXISTS almacen_arequipa;
DROP DATABASE IF EXISTS almacen_lima;
DROP DATABASE IF EXISTS almacen_cusco;
"@ | & $psql -U postgres 2>$null

# Crear bases de datos
@"
CREATE DATABASE almacen_arequipa ENCODING 'UTF8';
CREATE DATABASE almacen_lima ENCODING 'UTF8';
CREATE DATABASE almacen_cusco ENCODING 'UTF8';
"@ | & $psql -U postgres 2>$null

# Arequipa
@"
CREATE TABLE inventario (id SERIAL PRIMARY KEY, producto VARCHAR(100) NOT NULL, stock INTEGER NOT NULL DEFAULT 0, precio_unitario DECIMAL(10,2) DEFAULT 0.00, fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT INTO inventario (producto, stock, precio_unitario) VALUES ('Paracetamol 500mg', 100, 2.50), ('Ibuprofen 400mg', 80, 3.00), ('Amoxicilina 500mg', 150, 5.00), ('Vitamina C 1000mg', 200, 1.50), ('Loratadina 10mg', 120, 4.00);
"@ | & $psql -U postgres -d almacen_arequipa 2>$null

# Lima
@"
CREATE TABLE inventario (id SERIAL PRIMARY KEY, producto VARCHAR(100) NOT NULL, stock INTEGER NOT NULL DEFAULT 0, precio_unitario DECIMAL(10,2) DEFAULT 0.00, fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT INTO inventario (producto, stock, precio_unitario) VALUES ('Paracetamol 500mg', 50, 2.50), ('Ibuprofen 400mg', 120, 3.00), ('Metformina 500mg', 300, 2.00), ('Atorvastatina 20mg', 100, 6.00), ('Omeprazol 20mg', 250, 1.80);
"@ | & $psql -U postgres -d almacen_lima 2>$null

# Cusco
@"
CREATE TABLE inventario (id SERIAL PRIMARY KEY, producto VARCHAR(100) NOT NULL, stock INTEGER NOT NULL DEFAULT 0, precio_unitario DECIMAL(10,2) DEFAULT 0.00, fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT INTO inventario (producto, stock, precio_unitario) VALUES ('Paracetamol 500mg', 75, 2.50), ('Aspirina 500mg', 90, 2.00), ('Dipirona 500mg', 110, 2.20), ('Cefalexina 500mg', 60, 4.50), ('Suero Fisiologico 500ml', 200, 0.80);
"@ | & $psql -U postgres -d almacen_cusco 2>$null

Write-Host "[OK] Setup completado - 3 bases de datos configuradas"
