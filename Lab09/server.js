const express = require('express');
const app = express();
app.use(express.json());

// ================= CONFIGURACIÓN =================
const PORT = 3001;
const LATENCIA_MIN_MS = 40;
const LATENCIA_MAX_MS = 400;

// Probabilidades de problemas realistas (ajustables)
const PROB_ERROR_INVENTARIO = 0.01;         // 1% error en consulta inventario
const PROB_ERROR_FACTURA = 0.02;            // 2% error al obtener factura
const PROB_DESCUENTO_NO_APLICADO = 0.05;    // 5% de promociones fallan
const PROB_FACTURA_DUPLICADA = 0.03;        // 3% de pedidos con factura duplicada
const PROB_INVENTARIO_INCONSISTENTE = 0.04; // 4% consulta stock desactualizado
const PROB_LENTITUD_EXTREMA = 0.02;         // 2% de pedidos tardan >8s
const PROB_ERROR_CORREO = 0.03;             // 3% de fallo al enviar correo
const PROB_RETRASO_CORREO = 0.05;           // 5% de correos con retraso > 5 segundos
const RETRASO_CORREO_MIN_MS = 5000;
const RETRASO_CORREO_MAX_MS = 10000;

// ========== STOCKS INICIALES (para soportar alta demanda) ==========
let inventario = {
  'PROD-LACTEOA': { nombre: 'Leche 1L', stock: 500000, precio: 4.50 },
  'PROD-LACTEOB': { nombre: 'Yogurt 500g', stock: 500000, precio: 6.80 },
  'PROD-CARNEA': { nombre: 'Pollo Entero 2kg', stock: 500000, precio: 18.00 },
  'PROD-CARNEB': { nombre: 'Lomo fino 500g', stock: 500000, precio: 45.00 },
  'PROD-EMBUTIDA': { nombre: 'Jamón 200g', stock: 500000, precio: 12.90 },
  'PROD-PESCADO': { nombre: 'Filete 1kg', stock: 500000, precio: 22.50 }
};

let pedidos = [];
let facturas = [];
let nextPedidoId = 1000;
let nextFacturaId = 5000;

const promociones = {
  'FRESH15': 0.15,
  'JULIO20': 0.20
};

// ================= FUNCIONES AUXILIARES =================
function sleepRandom() {
  const delay = Math.floor(Math.random() * (LATENCIA_MAX_MS - LATENCIA_MIN_MS + 1) + LATENCIA_MIN_MS);
  return new Promise(resolve => setTimeout(resolve, delay));
}

function sleepExtremo() {
  const delay = 8500 + Math.random() * 2000; // entre 8.5s y 10.5s
  return new Promise(resolve => setTimeout(resolve, delay));
}

function calcularTotales(productos, codigoPromocion) {
  let subtotal = 0;
  for (const p of productos) {
    subtotal += p.cantidad * p.precioUnitario;
  }
  let descuentoPorcentaje = 0;
  if (codigoPromocion && promociones[codigoPromocion]) {
    const falla = Math.random() < PROB_DESCUENTO_NO_APLICADO;
    if (!falla) {
      descuentoPorcentaje = promociones[codigoPromocion];
    } else {
      console.log(`[SIMULACIÓN] Descuento ${codigoPromocion} NO aplicado`);
    }
  }
  const descuento = subtotal * descuentoPorcentaje;
  const baseImponible = subtotal - descuento;
  const igv = baseImponible * 0.18;
  const total = baseImponible + igv;
  return { subtotal, descuento, igv, total, descuentoAplicado: descuentoPorcentaje > 0 };
}

// ================= ENDPOINTS =================

app.get('/health', async (req, res) => {
  await sleepRandom();
  res.status(200).json({ status: 'ok' });
});

app.get('/api/inventario/:productoId/disponibilidad', async (req, res) => {
  await sleepRandom();
  const { productoId } = req.params;
  if (Math.random() < PROB_ERROR_INVENTARIO) {
    return res.status(500).json({ error: 'Error interno al consultar inventario' });
  }
  const producto = inventario[productoId];
  if (!producto) return res.status(404).json({ error: 'Producto no encontrado' });

  let stockReportado = producto.stock;
  if (Math.random() < PROB_INVENTARIO_INCONSISTENTE) {
    stockReportado = Math.max(0, stockReportado + Math.floor(Math.random() * 100) - 50);
    console.log(`[SIMULACIÓN] Inconsistencia: ${producto.nombre} real ${producto.stock}, reportado ${stockReportado}`);
  }
  res.json({ productoId, nombre: producto.nombre, stockDisponible: stockReportado });
});

app.post('/api/pedidos', async (req, res) => {
  // Lentitud extrema ocasional
  if (Math.random() < PROB_LENTITUD_EXTREMA) {
    console.log('[SIMULACIÓN] Pedido con lentitud >8s');
    await sleepExtremo();
  } else {
    await sleepRandom();
  }

  const { clienteId, rucCliente, productos, codigoPromocion, direccionEntrega, fechaEntrega, notas } = req.body;
  if (!clienteId || !productos || productos.length === 0) {
    return res.status(400).json({ error: 'Faltan datos obligatorios' });
  }

  // Validar stock real
  for (const item of productos) {
    const prod = inventario[item.productoId];
    if (!prod) return res.status(400).json({ error: `Producto ${item.productoId} no existe` });
    if (prod.stock < item.cantidad) {
      return res.status(409).json({
        error: `Stock insuficiente para ${prod.nombre}. Disponible: ${prod.stock}, solicitado: ${item.cantidad}`
      });
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
  pedidos.push(nuevoPedido);

  // Generar factura (una o duplicada)
  const facturaId = nextFacturaId++;
  const factura = {
    facturaId, pedidoId,
    numeroSerie: `F001-${facturaId}`,
    montoSubtotal: totales.subtotal,
    descuento: totales.descuento,
    igv: totales.igv,
    montoTotal: totales.total,
    fechaEmision: new Date().toISOString()
  };
  facturas.push(factura);

  if (Math.random() < PROB_FACTURA_DUPLICADA) {
    const facturaDuplicada = { ...factura, facturaId: nextFacturaId++, numeroSerie: `F001-${nextFacturaId - 1}-DUP` };
    facturas.push(facturaDuplicada);
    console.log(`[SIMULACIÓN] Factura duplicada para pedido ${pedidoId}`);
  }

  res.status(201).json({ pedidoId, estado: nuevoPedido.estado, mensaje: 'Pedido registrado' });
});

app.get('/api/pedidos/:pedidoId', async (req, res) => {
  await sleepRandom();
  const pedido = pedidos.find(p => p.pedidoId == req.params.pedidoId);
  if (!pedido) return res.status(404).json({ error: 'Pedido no encontrado' });
  res.json({ pedidoId: pedido.pedidoId, estado: pedido.estado, clienteId: pedido.clienteId });
});

app.get('/api/facturas/pedido/:pedidoId', async (req, res) => {
  await sleepRandom();
  if (Math.random() < PROB_ERROR_FACTURA) {
    return res.status(500).json({ error: 'Error interno al obtener factura' });
  }
  const facturasPedido = facturas.filter(f => f.pedidoId == req.params.pedidoId);
  if (facturasPedido.length === 0) {
    return res.status(404).json({ error: 'Factura no encontrada' });
  }
  // Devuelve objeto si es única, array si hay duplicadas
  res.json(facturasPedido.length === 1 ? facturasPedido[0] : facturasPedido);
});

// Endpoint para simular envío de correo de confirmación
app.post('/api/notificaciones/correo', async (req, res) => {
  const { pedidoId, clienteId, email } = req.body;

  // Validación básica
  if (!pedidoId || !clienteId) {
    return res.status(400).json({ error: 'Faltan pedidoId o clienteId' });
  }

  // Simular retraso variable (a veces extremo)
  if (Math.random() < PROB_RETRASO_CORREO) {
    console.log(`[SIMULACIÓN] Correo para pedido ${pedidoId} con retraso > 5s`);
    await sleepRandom(RETRASO_CORREO_MIN_MS, RETRASO_CORREO_MAX_MS);
  } else {
    await sleepRandom(200, 800); // latencia normal
  }

  // Simular error aleatorio en el envío
  if (Math.random() < PROB_ERROR_CORREO) {
    return res.status(500).json({ error: 'Error al enviar el correo de confirmación' });
  }

  // Verificar que el pedido exista (opcional)
  const pedido = pedidos.find(p => p.pedidoId === pedidoId);
  if (!pedido) {
    return res.status(404).json({ error: 'Pedido no encontrado' });
  }

  res.status(200).json({
    mensaje: 'Correo de confirmación enviado exitosamente',
    pedidoId,
    destinatario: email || 'cliente@logifresh.com',
    timestamp: new Date().toISOString()
  });
});

app.post('/api/admin/reset', (req, res) => {
  inventario = {
    'PROD-LACTEOA': { nombre: 'Leche 1L', stock: 500000, precio: 4.50 },
    'PROD-LACTEOB': { nombre: 'Yogurt 500g', stock: 500000, precio: 6.80 },
    'PROD-CARNEA': { nombre: 'Pollo Entero 2kg', stock: 500000, precio: 18.00 },
    'PROD-CARNEB': { nombre: 'Lomo fino 500g', stock: 500000, precio: 45.00 },
    'PROD-EMBUTIDA': { nombre: 'Jamón 200g', stock: 500000, precio: 12.90 },
    'PROD-PESCADO': { nombre: 'Filete 1kg', stock: 500000, precio: 22.50 }
  };
  pedidos = [];
  facturas = [];
  nextPedidoId = 1000;
  nextFacturaId = 5000;
  res.json({ message: 'Sistema reiniciado' });
});

app.listen(PORT, () => {
  console.log(` LogiFresh S.A. corriendo en http://localhost:${PORT}`);
  console.log(`   Stocks: 500.000 unidades por producto`);
  console.log(`   Fallo descuento: ${PROB_DESCUENTO_NO_APLICADO * 100}%`);
  console.log(`   Factura duplicada: ${PROB_FACTURA_DUPLICADA * 100}%`);
  console.log(`   Lentitud extrema (>8s): ${PROB_LENTITUD_EXTREMA * 100}%`);
  console.log(`   Inconsistencia inventario: ${PROB_INVENTARIO_INCONSISTENTE * 100}%`);
  console.log(`   Error envío correo: ${PROB_ERROR_CORREO * 100}%`);
  console.log(`   Retraso correo (>5s): ${PROB_RETRASO_CORREO * 100}%`);
});