from zeep import Client

# Definir la URL del WSDL del servicio que queremos consumir
# Aclarar que en el laboratorio usa el servicio público de calculadora de dneonline.com
wsdl_url = 'http://www.dneonline.com/calculator.asmx?WSDL'

# Crear el cliente SOAP. Zeep analizará el WSDL automáticamente.
client = Client(wsdl_url)

print("--- Operaciones disponibles en el servicio ---")
for service in client.wsdl.services.values():
    for port in service.ports.values():
        operations = list(port.binding._operations.values())
        for operation in operations:
            print(f"* {operation.name}")

print("\n--- Llamando a las operaciones ---")

# Invocar el método 'Add' del servicio y almacenar el resultado
# El servicio 'calculator.asmxs' tiene operaciones como Add, Subtract, Multiply, Divide.
resultado_suma = client.service.Add(5, 8)

# Mostrar el resultado por pantalla
print(f"El resultado de 5 + 8 es: {resultado_suma}")

# Ejemplo adicional: Usar otra operación del mismo servicio
resultado_resta = client.service.Subtract(10, 4)
print(f"El resultado de 10 - 4 es: {resultado_resta}")