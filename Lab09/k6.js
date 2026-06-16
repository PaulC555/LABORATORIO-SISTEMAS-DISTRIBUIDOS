/**
 * Script de Prueba de Rendimiento — LogiFresh S.A.
 * Herramienta: k6 
 * Escenario: 100 usuarios concurrentes durante 5 minutos
 * Comando: k6 run --summary-export=resultados.json k6.js
 * 
 * Instalación k6:
 *   Windows: winget install k6
 *   Mac:     brew install k6
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ─── Métricas personalizadas ─────────────────────────────────────────────
const erroresRegistroPedido = new Counter('errores_registro_pedido');
const erroresConsultaInventario = new Counter('errores_consulta_inventario');
const duracionRegistroPedido = new Trend('duracion_registro_pedido', true);
const tasaExitoPedidos = new Rate('tasa_exito_pedidos');
const tasaErrorFacturacion = new Rate('tasa_error_facturacion');
const descuentosNoAplicados = new Counter('descuentos_no_aplicados');
const facturasDuplicadas = new Counter('facturas_duplicadas');
const pedidosLentos = new Counter('pedidos_lentos');          // >8 segundos
const erroresEnvioCorreo = new Counter('errores_envio_correo');
const retrasosCorreo = new Counter('retrasos_envio_correo');    // envíos >5s

// ─── Configuración de escenarios ─────────────────────────────────────────
export const options = {
  scenarios: {
    carga_sostenida: {
      executor: 'constant-vus',
      vus: 100,
      duration: '5m',
    },
    rampa_inicial: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '4m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      startTime: '0s',
    },
  },

  // Umbrales de calidad (SLAs)
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    'http_req_duration{tipo:registro_pedido}': ['p(99)<5000'],
    http_req_failed: ['rate<0.05'],
    tasa_exito_pedidos: ['rate>=0.95'],
    tasa_error_facturacion: ['rate<0.02'],
    pedidos_lentos: ['rate<0.05'],        // menos del 5% de pedidos superan 8s
    descuentos_no_aplicados: ['rate<0.10'],        // hasta 10% de fallos en promos
    facturas_duplicadas: ['rate<0.05'],        // menos del 5% duplicadas
    'http_req_duration{tipo:envio_correo}': ['p(95)<5000'],       // 95% de correos <5s
    errores_envio_correo: ['rate<0.05'],        // menos del 5% de fallos en correo
  },
};

// ─── URL base del sistema ────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';

// ─── Datos de prueba ─────────────────────────────────────────────────────
const PRODUCTOS = [
  { productoId: 'PROD-LACTEOA', nombre: 'Leche 1L', precioUnitario: 4.50 },
  { productoId: 'PROD-LACTEOB', nombre: 'Yogurt 500g', precioUnitario: 6.80 },
  { productoId: 'PROD-CARNEA', nombre: 'Pollo Entero 2kg', precioUnitario: 18.00 },
  { productoId: 'PROD-CARNEB', nombre: 'Lomo fino 500g', precioUnitario: 45.00 },
  { productoId: 'PROD-EMBUTIDA', nombre: 'Jamón 200g', precioUnitario: 12.90 },
  { productoId: 'PROD-PESCADO', nombre: 'Filete 1kg', precioUnitario: 22.50 },
];

const CLIENTES = [
  { clienteId: 'CLI-METRO-001', ruc: '20100070970', nombre: 'Metro SAC' },
  { clienteId: 'CLI-WONG-001', ruc: '20100174816', nombre: 'Wong SAC' },
  { clienteId: 'CLI-TOTTUS-001', ruc: '20418993805', nombre: 'Tottus' },
  { clienteId: 'CLI-PLAZA-001', ruc: '20136919420', nombre: 'Plaza Vea' },
];

const CODIGOS_PROMO = ['FRESH15', 'JULIO20', '', '', '', '']; // 33% con promo
const DISTRITOS_AREQUIPA = [
  'Cerro Colorado', 'Cercado', 'Miraflores', 'Yanahuara',
  'Paucarpata', 'Socabaya', 'Jacobo Hunter', 'Sachaca',
];

// ─── Función para generar pedido y recordar el código promocional ────────
function generarPedidoConPromo() {
  const cliente = CLIENTES[randomIntBetween(0, CLIENTES.length - 1)];
  const numProductos = randomIntBetween(1, 4);
  const productos = [];
  const productosSeleccionados = new Set();

  for (let i = 0; i < numProductos; i++) {
    let idx;
    do { idx = randomIntBetween(0, PRODUCTOS.length - 1); }
    while (productosSeleccionados.has(idx));
    productosSeleccionados.add(idx);
    productos.push({
      productoId: PRODUCTOS[idx].productoId,
      cantidad: randomIntBetween(1, 20),
      precioUnitario: PRODUCTOS[idx].precioUnitario,
    });
  }

  const codigoPromocion = CODIGOS_PROMO[randomIntBetween(0, CODIGOS_PROMO.length - 1)];
  const distrito = DISTRITOS_AREQUIPA[randomIntBetween(0, DISTRITOS_AREQUIPA.length - 1)];

  const pedido = {
    clienteId: cliente.clienteId,
    rucCliente: cliente.ruc,
    productos,
    codigoPromocion: codigoPromocion || undefined,
    direccionEntrega: `Av. Independencia, ${distrito}, Arequipa`,
    fechaEntrega: '2026-06-16',
    notas: 'Entrega en horario de mañana, requiere cadena de frío',
  };
  return { pedido, codigoPromocion: codigoPromocion || null };
}

// ─── Función principal ejecutada por cada usuario virtual ─────────────────
export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'X-Client-Source': 'k6-load-test',
    'X-Request-ID': `k6-${__VU}-${__ITER}-${Date.now()}`,
  };

  // ── Grupo 1: Consulta de Inventario ───────────────────────────────────
  group('Consulta de Inventario', () => {
    const productoElegido = PRODUCTOS[randomIntBetween(0, PRODUCTOS.length - 1)];
    const resInventario = http.get(
      `${BASE_URL}/api/inventario/${productoElegido.productoId}/disponibilidad`,
      { headers, tags: { tipo: 'consulta_inventario' } }
    );

    const inventarioOk = check(resInventario, {
      '[Inventario] Status 200': (r) => r.status === 200,
      '[Inventario] Respuesta < 500ms': (r) => r.timings.duration < 500,
      '[Inventario] Tiene campo stock': (r) => {
        try { return JSON.parse(r.body).stockDisponible !== undefined; }
        catch { return false; }
      },
    });
    if (!inventarioOk) erroresConsultaInventario.add(1);
  });

  sleep(randomIntBetween(1, 2));

  // ── Grupo 2: Registro de Pedido ───────────────────────────────────────
  let pedidoId = null;
  let codigoPromoEnviado = null;

  group('Registro de Pedido', () => {
    const { pedido, codigoPromocion } = generarPedidoConPromo();
    codigoPromoEnviado = codigoPromocion;
    const payload = JSON.stringify(pedido);
    const startTime = Date.now();

    const resPedido = http.post(
      `${BASE_URL}/api/pedidos`,
      payload,
      { headers, tags: { tipo: 'registro_pedido' }, timeout: '10s' }
    );

    const duracion = Date.now() - startTime;
    duracionRegistroPedido.add(duracion);
    if (duracion > 8000) pedidosLentos.add(1);

    const pedidoOk = check(resPedido, {
      '[Pedido] Status 201': (r) => r.status === 201,
      '[Pedido] Respuesta < 2000ms': (r) => r.timings.duration < 2000,
      '[Pedido] Respuesta < 8000ms (SLA)': (r) => r.timings.duration < 8000,
      '[Pedido] Contiene pedidoId': (r) => {
        try { return JSON.parse(r.body).pedidoId !== undefined; }
        catch { return false; }
      },
      '[Pedido] Estado REGISTRADO': (r) => {
        try { return JSON.parse(r.body).estado === 'REGISTRADO'; }
        catch { return false; }
      },
    });

    tasaExitoPedidos.add(pedidoOk ? 1 : 0);
    if (!pedidoOk) {
      erroresRegistroPedido.add(1);
      console.warn(`[VU ${__VU}] Fallo al registrar pedido — Status: ${resPedido.status} — Duración: ${duracion}ms`);
    } else {
      try {
        pedidoId = JSON.parse(resPedido.body).pedidoId;
      } catch { }
    }
  });

  sleep(randomIntBetween(2, 4));

  // ── Si el pedido se registró, consultar estado, factura y enviar correo ──
  if (pedidoId) {
    // Grupo 3: Consultar estado del pedido
    group('Consulta Estado Pedido', () => {
      const resEstado = http.get(
        `${BASE_URL}/api/pedidos/${pedidoId}`,
        { headers, tags: { tipo: 'consulta_pedido' } }
      );
      check(resEstado, {
        '[Estado] Status 200': (r) => r.status === 200,
        '[Estado] Respuesta < 1000ms': (r) => r.timings.duration < 1000,
        '[Estado] Estado coherente': (r) => {
          try {
            const body = JSON.parse(r.body);
            const valid = ['REGISTRADO', 'CONFIRMADO', 'EN_TRANSITO', 'ENTREGADO', 'CANCELADO'];
            return valid.includes(body.estado);
          } catch { return false; }
        },
      });
    });

    sleep(randomIntBetween(1, 2));

    // Grupo 4: Verificación de Factura
    group('Verificación de Factura', () => {
      const resFactura = http.get(
        `${BASE_URL}/api/facturas/pedido/${pedidoId}`,
        { headers, tags: { tipo: 'verificacion_factura' } }
      );

      let facturaData = null;
      let esDuplicada = false;
      let descuentoReal = 0;

      const facturaOk = check(resFactura, {
        '[Factura] Status 200': (r) => r.status === 200,
        '[Factura] Formato válido': (r) => {
          try {
            const body = JSON.parse(r.body);
            const lista = Array.isArray(body) ? body : [body];
            if (lista.length === 0) return false;
            facturaData = lista[0];
            esDuplicada = lista.length > 1;
            if (esDuplicada) facturasDuplicadas.add(1);
            descuentoReal = facturaData.descuento || 0;
            return true;
          } catch { return false; }
        },
        '[Factura] Contiene numero serie': () => facturaData && facturaData.numeroSerie !== undefined,
      });

      if (facturaOk && codigoPromoEnviado && (codigoPromoEnviado === 'FRESH15' || codigoPromoEnviado === 'JULIO20')) {
        if (descuentoReal === 0) {
          descuentosNoAplicados.add(1);
          console.warn(`[VU ${__VU}] Descuento NO aplicado para código ${codigoPromoEnviado} en pedido ${pedidoId}`);
        }
      }

      tasaErrorFacturacion.add(facturaOk ? 0 : 1);
    });

    sleep(randomIntBetween(1, 2));

    // Grupo 5: Notificación por Correo
    group('Notificación por Correo', () => {
      const payloadCorreo = JSON.stringify({
        pedidoId: pedidoId,
        clienteId: 'CLI-TEST',
        email: `cliente_${pedidoId}@logifresh.com`
      });

      const resCorreo = http.post(
        `${BASE_URL}/api/notificaciones/correo`,
        payloadCorreo,
        { headers, tags: { tipo: 'envio_correo' }, timeout: '12s' }
      );

      const correoOk = check(resCorreo, {
        '[Correo] Status 200': (r) => r.status === 200,
        '[Correo] Respuesta < 5000ms (SLA)': (r) => r.timings.duration < 5000,
        '[Correo] Mensaje de éxito': (r) => {
          try { return JSON.parse(r.body).mensaje.includes('enviado'); }
          catch { return false; }
        }
      });

      if (!correoOk) {
        erroresEnvioCorreo.add(1);
        if (resCorreo.timings.duration >= 5000) {
          retrasosCorreo.add(1);
        }
        console.warn(`[VU ${__VU}] Fallo en envío de correo para pedido ${pedidoId} — Status: ${resCorreo.status} — Duración: ${resCorreo.timings.duration}ms`);
      }
    });
  }

  sleep(randomIntBetween(1, 3));
}

// ─── Setup: verificar salud del sistema ───────────────────────────────────
export function setup() {
  console.log('=== Iniciando prueba de carga LogiFresh S.A. ===');
  console.log(`URL objetivo: ${BASE_URL}`);
  console.log('Usuarios concurrentes: 100');
  console.log('Duración: 5 minutos');
  const healthCheck = http.get(`${BASE_URL}/health`);
  if (healthCheck.status !== 200) {
    console.error('ADVERTENCIA: El sistema no responde al health check.');
  }
  return { baseUrl: BASE_URL, startTime: new Date().toISOString() };
}

// ─── Teardown: reporte final ──────────────────────────────────────────────
export function teardown(data) {
  console.log('=== Prueba de carga finalizada ===');
  console.log(`Inicio: ${data.startTime}`);
  console.log(`Fin: ${new Date().toISOString()}`);
}