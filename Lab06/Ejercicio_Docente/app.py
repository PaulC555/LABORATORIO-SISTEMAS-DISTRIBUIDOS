from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Base de datos en memoria
productos = [
    {"id": 1, "nombre": "Laptop"},
    {"id": 2, "nombre": "Mouse"}
]
# Control de ID autoincremental
next_id = 3

@app.route('/')
def index():
    return send_from_directory('.', 'index.html')

# GET: listar productos
@app.route('/productos', methods=['GET'])
def obtener():
    return jsonify(productos)

# POST: agregar producto con ID automático (evitar duplicados)
@app.route('/productos', methods=['POST'])
def agregar():
    global next_id
    data = request.get_json()
    if not data or 'nombre' not in data:
        return jsonify({"error": "Falta el campo 'nombre'"}), 400
    
    nuevo = {
        "id": next_id,
        "nombre": data['nombre']
    }
    productos.append(nuevo)
    next_id += 1
    return jsonify({"mensaje": "Producto agregado", "producto": nuevo}), 201

# DELETE: eliminar producto por ID
@app.route('/productos/<int:id>', methods=['DELETE'])
def eliminar(id):
    global productos
    producto_existente = next((p for p in productos if p["id"] == id), None)
    if not producto_existente:
        return jsonify({"error": "Producto no encontrado"}), 404
    
    productos = [p for p in productos if p["id"] != id]
    return jsonify({"mensaje": "Producto eliminado"})

if __name__ == '__main__':
    app.run(debug=True)