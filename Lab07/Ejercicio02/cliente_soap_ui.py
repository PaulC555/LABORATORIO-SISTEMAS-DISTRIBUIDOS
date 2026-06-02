import tkinter as tk
from tkinter import messagebox
from zeep import Client

# Inicializar cliente SOAP
wsdl_url = 'http://www.dneonline.com/calculator.asmx?WSDL'
client = Client(wsdl_url)

def calcular(operacion):
    try:
        a = int(float(entry_a.get()))
        b = int(float(entry_b.get()))
        
        if operacion == "suma":
            resultado = client.service.Add(a, b)
            op_texto = "suma"
        elif operacion == "resta":
            resultado = client.service.Subtract(a, b)
            op_texto = "resta"
        elif operacion == "multiplicacion":
            resultado = client.service.Multiply(a, b)
            op_texto = "multiplicación"
        elif operacion == "division":
            resultado = client.service.Divide(a, b)
            op_texto = "división"
        else:
            return
        
        messagebox.showinfo("Resultado", f"El resultado de la {op_texto} es: {resultado}")
    except ValueError:
        messagebox.showerror("Error", "Ingresa números válidos")
    except Exception as e:
        messagebox.showerror("Error SOAP", str(e))

# Crear ventana principal
root = tk.Tk()
root.title("Cliente SOAP - Calculadora")
root.geometry("300x250")

tk.Label(root, text="Primer número:").pack(pady=5)
entry_a = tk.Entry(root)
entry_a.pack()

tk.Label(root, text="Segundo número:").pack(pady=5)
entry_b = tk.Entry(root)
entry_b.pack()

frame_botones = tk.Frame(root)
frame_botones.pack(pady=10)

tk.Button(frame_botones, text="Sumar", command=lambda: calcular("suma")).pack(side=tk.LEFT, padx=5)
tk.Button(frame_botones, text="Restar", command=lambda: calcular("resta")).pack(side=tk.LEFT, padx=5)
tk.Button(frame_botones, text="Multiplicar", command=lambda: calcular("multiplicacion")).pack(side=tk.LEFT, padx=5)
tk.Button(frame_botones, text="Dividir", command=lambda: calcular("division")).pack(side=tk.LEFT, padx=5)

tk.Button(root, text="Salir", command=root.quit).pack(pady=10)

root.mainloop()