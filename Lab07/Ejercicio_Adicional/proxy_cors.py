from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.request
import urllib.parse

class ProxyHandler(BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        # Responder a las peticiones preflight de CORS
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, SOAPAction')
        self.end_headers()

    def do_POST(self):
        # Leer el cuerpo de la petición
        content_length = int(self.headers['Content-Length'])
        body = self.rfile.read(content_length)

        # Construir la petición al servicio SOAP real
        target_url = 'http://www.dneonline.com/calculator.asmx'
        req = urllib.request.Request(target_url, data=body, method='POST')
        req.add_header('Content-Type', self.headers.get('Content-Type', 'text/xml'))
        req.add_header('SOAPAction', self.headers.get('SOAPAction', ''))

        try:
            with urllib.request.urlopen(req) as response:
                response_body = response.read()
                self.send_response(200)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.send_header('Content-Type', response.headers.get('Content-Type', 'text/xml'))
                self.end_headers()
                self.wfile.write(response_body)
        except Exception as e:
            self.send_response(500)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(str(e).encode())

    def log_message(self, format, *args):
        pass

if __name__ == '__main__':
    port = 8080
    server = HTTPServer(('localhost', port), ProxyHandler)
    print(f"Proxy CORS corriendo en http://localhost:{port}")
    print("Presiona Ctrl+C para detener")
    server.serve_forever()