// server.js - LogiFresh S.A. versión robusta con Circuit Breaker, métricas y resiliencia
const express = require('express');
const app = express();
app.use(express.json());

// ================= CONFIGURACIÓN =================
const PORT = process.env.PORT || 3001;
const LATENCIA_BASE_MS = 50;
const LATENCIA_MAX_MS = 200;

// Probabilidades de fallo (ahora mucho más bajas, pero el Circuit Breaker las maneja)
const PROB_ERROR_INVENTARIO = 0.005;      // 0.5% error transitorio
const PROB_ERROR_FACTURA = 0.01;          // 1% error (el CB abrirá si muchos)
const PROB_ERROR_CORREO = 0.02;           // 2% error (con reintentos se mitiga)

// Umbrales del Circuit Breaker
const CB_FAILURE_THRESHOLD = 5;           // 5 fallos en ventana
const CB_TIMEOUT_MS = 10000;              // 10 segundos abierto
const CB_HALF_OPEN_MAX_CALLS = 2;

// ========== STOCKS INICIALES (muy altos) ==========
let inventario = {
  'PROD-LACTEOA':  { nombre: 'Leche 1L',        stock: 1000000, precio: 4.50 },
  'PROD-LACTEOB':  { nombre: 'Yogurt 500g',     stock: 1000000, precio: 6.80 },
  'PROD-CARNEA':   { nombre: 'Pollo Entero 2kg',stock: 1000000, precio: 18.00 },
  'PROD-CARNEB':   { nombre: 'Lomo fino 500g',  stock: 1000000, precio: 45.00 },
  'PROD-EMBUTIDA': { nombre: 'Jamón 200g',      stock: 1000000, precio: 12.90 },
  'PROD-PESCADO':  { nombre: 'Filete 1kg',      stock: 1000000, precio: 22.50 }
};

let pedidos = new Map();          // pedidoId -> pedido
let facturas = new Map();         // pedidoId -> factura
let nextPedidoId = 1000;
let nextFacturaId = 5000;

// Promociones (siempre aplicadas correctamente)
const promociones = {
  'FRESH15': 0.15,
  'JULIO20': 0.20
};

// ================= MÉTRICAS (Prometheus) =================
const client = require('prom-client');
const register = new client.Registry();
client.collectDefaultMetrics({ register });

const httpRequestsTotal = new client.Counter({
  name: 'http_requests_total',
  help: 'Total de peticiones HTTP',
  labelNames: ['method', 'route', 'status']
});
const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duración de peticiones HTTP',
  labelNames: ['method', 'route'],
  buckets: [0.05, 0.1, 0.2, 0.5, 1, 2, 5]
});
const descuentosAplicados = new client.Counter({
  name: 'descuentos_aplicados_total',
  help: 'Total de descuentos aplicados correctamente'
});
const facturasGeneradas = new client.Counter({
  name: 'facturas_generadas_total',
  help: 'Total de facturas generadas'
});
const correosEnviados = new client.Counter({
  name: 'correos_enviados_total',
  help: 'Total de correos enviados exitosamente'
});
const circuitBreakerState = new client.Gauge({
  name: 'circuit_breaker_state',
  help: 'Estado del Circuit Breaker (0=closed,1=open,2=half-open)',
  labelNames: ['service']
});

register.registerMetric(httpRequestsTotal);
register.registerMetric(httpRequestDuration);
register.registerMetric(descuentosAplicados);
register.registerMetric(facturasGeneradas);
register.registerMetric(correosEnviados);
register.registerMetric(circuitBreakerState);

// ================= CIRCUIT BREAKER =================
class CircuitBreaker {
  constructor(name, failureThreshold, timeoutMs, halfOpenMaxCalls) {
    this.name = name;
    this.failureThreshold = failureThreshold;
    this.timeoutMs = timeoutMs;
    this.halfOpenMaxCalls = halfOpenMaxCalls;
    this.state = 'CLOSED';       // CLOSED, OPEN, HALF_OPEN
    this.failureCount = 0;
    this.lastFailureTime = 0;
    this.halfOpenCallCount = 0;
    this.updateGauge();
  }

  updateGauge() {
    const stateValue = this.state === 'CLOSED' ? 0 : (this.state === 'OPEN' ? 1 : 2);
    circuitBreakerState.labels(this.name).set(stateValue);
  }

  async call(fn) {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime > this.timeoutMs) {
        this.state = 'HALF_OPEN';
        this.halfOpenCallCount = 0;
        this.updateGauge();
        console.log(`[CB:${this.name}] Transición OPEN -> HALF_OPEN`);
      } else {
        throw new Error(`CircuitBreaker ${this.name} está OPEN`);
      }
    }

    try {
      const result = await fn();
      if (this.state === 'HALF_OPEN') {
        this.halfOpenCallCount++;
        if (this.halfOpenCallCount >= this.halfOpenMaxCalls) {
          this.reset();
        }
      } else if (this.state === 'CLOSED') {
        // Éxito, no hacemos nada
      }
      return result;
    } catch (err) {
      this.recordFailure();
      throw err;
    }
  }

  recordFailure() {
    if (this.state === 'CLOSED') {
      this.failureCount++;
      if (this.failureCount >= this.failureThreshold) {
        this.open();
      }
    } else if (this.state === 'HALF_OPEN') {
      this.open();
    }
  }

  open() {
    this.state = 'OPEN';
    this.lastFailureTime = Date.now();
    this.updateGauge();
    console.log(`[CB:${this.name}] Circuito ABIERTO por ${this.timeoutMs}ms`);
  }

  reset() {
    this.state = 'CLOSED';
    this.failureCount = 0;
    this.updateGauge();
    console.log(`[CB:${this.name}] Circuito CERRADO (reset)`);
  }
}

const cbFacturacion = new CircuitBreaker('facturacion', CB_FAILURE_THRESHOLD, CB_TIMEOUT_MS, CB_HALF_OPEN_MAX_CALLS);
const cbNotificaciones = new CircuitBreaker('notificaciones', CB_FAILURE_THRESHOLD, CB_TIMEOUT_MS, CB_HALF_OPEN_MAX_CALLS);

// ================= FUNCIONES AUXILIARES =================
function sleepRandom(min = LATENCIA_BASE_MS, max = LATENCIA_MAX_MS) {
  const delay = Math.floor(Math.random() * (max - min + 1) + min);
  return new Promise(resolve => setTimeout(resolve, delay));
}

function calcularTotales(productos, codigoPromocion) {
  let subtotal = 0;
  for (const p of productos) {
    subtotal += p.cantidad * p.precioUnitario;
  }
  const descuentoPorcentaje = (codigoPromocion && promociones[codigoPromocion]) ? promociones[codigoPromocion] : 0;
  const descuento = subtotal * descuentoPorcentaje;
  if (descuentoPorcentaje > 0) descuentosAplicados.inc();
  const baseImponible = subtotal - descuento;
  const igv = baseImponible * 0.18;
  const total = baseImponible + igv;
  return { subtotal, descuento, igv, total };
}

async function generarFactura(pedidoId, totales) {
  return cbFacturacion.call(async () => {
    await sleepRandom(30, 150);
    if (Math.random() < PROB_ERROR_FACTURA) {
      throw new Error(`Error simulado en facturación para pedido ${pedidoId}`);
    }
    if (facturas.has(pedidoId)) {
      // Idempotencia: devolver factura existente
      return facturas.get(pedidoId);
    }
    const facturaId = nextFacturaId++;
    const factura = {
      facturaId,
      pedidoId,
      numeroSerie: `F001-${facturaId}`,
      montoSubtotal: totales.subtotal,
      descuento: totales.descuento,
      igv: totales.igv,
      montoTotal: totales.total,
      fechaEmision: new Date().toISOString()
    };
    facturas.set(pedidoId, factura);
    facturasGeneradas.inc();
    return factura;
  });
}

async function enviarCorreo(pedidoId, clienteId, email, retryCount = 0) {
  return cbNotificaciones.call(async () => {
    await sleepRandom(100, 300);
    if (Math.random() < PROB_ERROR_CORREO) {
      if (retryCount < 3) {
        const backoff = Math.pow(2, retryCount) * 100;
        console.log(`[Email] Reintento ${retryCount+1} para pedido ${pedidoId} tras ${backoff}ms`);
        await new Promise(resolve => setTimeout(resolve, backoff));
        return enviarCorreo(pedidoId, clienteId, email, retryCount + 1);
      } else {
        throw new Error(`Error simulado en envío de correo para pedido ${pedidoId} después de ${retryCount} reintentos`);
      }
    }
    correosEnviados.inc();
    return { mensaje: 'Correo de confirmación enviado', pedidoId, destinatario: email };
  });
}

// ================= MIDDLEWARE DE MÉTRICAS =================
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    httpRequestsTotal.labels(req.method, req.route?.path || req.path, res.statusCode).inc();
    httpRequestDuration.labels(req.method, req.route?.path || req.path).observe(duration);
  });
  next();
});

// ================= ENDPOINTS =================
app.get('/health', async (req, res) => {
  res.status(200).json({ status: 'ok', version: '2.0-robust' });
});

app.get('/ready', async (req, res) => {
  // Verificar dependencias (simulado)
  res.status(200).json({ ready: true });
});

app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

app.get('/api/inventario/:productoId/disponibilidad', async (req, res) => {
  await sleepRandom();
  const { productoId } = req.params;
  if (Math.random() < PROB_ERROR_INVENTARIO) {
    return res.status(500).json({ error: 'Error transitorio en inventario' });
  }
  const producto = inventario[productoId];
  if (!producto) return res.status(404).json({ error: 'Producto no encontrado' });
  res.json({ productoId, nombre: producto.nombre, stockDisponible: producto.stock });
});

app.post('/api/pedidos', async (req, res) => {
  await sleepRandom();
  const { clienteId, rucCliente, productos, codigoPromocion, direccionEntrega, fechaEntrega, notas, idempotencyKey } = req.body;
  if (!clienteId || !productos || productos.length === 0) {
    return res.status(400).json({ error: 'Faltan datos obligatorios' });
  }

  // Validar stock
  for (const item of productos) {
    const prod = inventario[item.productoId];
    if (!prod) return res.status(400).json({ error: `Producto ${item.productoId} no existe` });
    if (prod.stock < item.cantidad) {
      return res.status(409).json({ error: `Stock insuficiente para ${prod.nombre}` });
    }
  }

  // Descontar stock
  for (const item of productos) {
    inventario[item.productoId].stock -= item.cantidad;
  }

  const totales = calcularTotales(productos, codigoPromocion);
  const pedidoId = nextPedidoId++;
  const nuevoPedido = {
    pedidoId, clienteId, rucCliente, productos, codigoPromocion, direccionEntrega, fechaEntrega, notas,
    estado: 'REGISTRADO',
    fechaRegistro: new Date().toISOString(),
    totales
  };
  pedidos.set(pedidoId, nuevoPedido);

  // Generar factura (con Circuit Breaker)
  try {
    await generarFactura(pedidoId, totales);
  } catch (err) {
    console.error(`Error al generar factura para pedido ${pedidoId}:`, err.message);
    // No falla el pedido, se programa reintento asíncrono (por simplicidad, se deja pendiente)
  }

  // Envío asíncrono de correo (no bloquea respuesta)
  enviarCorreo(pedidoId, clienteId, `cliente_${pedidoId}@logifresh.com`).catch(err => {
    console.error(`Error al enviar correo para pedido ${pedidoId}:`, err.message);
  });

  res.status(201).json({ pedidoId, estado: nuevoPedido.estado, mensaje: 'Pedido registrado exitosamente' });
});

app.get('/api/pedidos/:pedidoId', async (req, res) => {
  await sleepRandom();
  const pedido = pedidos.get(parseInt(req.params.pedidoId));
  if (!pedido) return res.status(404).json({ error: 'Pedido no encontrado' });
  res.json({ pedidoId: pedido.pedidoId, estado: pedido.estado, clienteId: pedido.clienteId });
});

app.get('/api/facturas/pedido/:pedidoId', async (req, res) => {
  await sleepRandom();
  const pedidoId = parseInt(req.params.pedidoId);
  const factura = facturas.get(pedidoId);
  if (!factura) return res.status(404).json({ error: 'Factura no encontrada' });
  res.json(factura);
});

app.post('/api/notificaciones/correo', async (req, res) => {
  const { pedidoId, clienteId, email } = req.body;
  if (!pedidoId || !clienteId) {
    return res.status(400).json({ error: 'Faltan pedidoId o clienteId' });
  }
  try {
    const resultado = await enviarCorreo(pedidoId, clienteId, email || `cliente_${pedidoId}@logifresh.com`);
    res.status(200).json(resultado);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/admin/reset', (req, res) => {
  inventario = {
    'PROD-LACTEOA':  { nombre: 'Leche 1L',        stock: 1000000, precio: 4.50 },
    'PROD-LACTEOB':  { nombre: 'Yogurt 500g',     stock: 1000000, precio: 6.80 },
    'PROD-CARNEA':   { nombre: 'Pollo Entero 2kg',stock: 1000000, precio: 18.00 },
    'PROD-CARNEB':   { nombre: 'Lomo fino 500g',  stock: 1000000, precio: 45.00 },
    'PROD-EMBUTIDA': { nombre: 'Jamón 200g',      stock: 1000000, precio: 12.90 },
    'PROD-PESCADO':  { nombre: 'Filete 1kg',      stock: 1000000, precio: 22.50 }
  };
  pedidos.clear();
  facturas.clear();
  nextPedidoId = 1000;
  nextFacturaId = 5000;
  res.json({ message: 'Sistema reiniciado (estado limpio)' });
});

app.listen(PORT, () => {
  console.log(` LogiFresh S.A. (versión robusta) corriendo en http://localhost:${PORT}`);
  console.log(`   - Circuit Breaker para Facturación y Notificaciones`);
  console.log(`   - Métricas Prometheus en /metrics`);
  console.log(`   - Reintentos con backoff para correos`);
  console.log(`   - Idempotencia en facturas`);
  console.log(`   - Stock: 1.000.000 unidades por producto`);
});