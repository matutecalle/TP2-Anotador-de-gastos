// API de sincronización del Anotador de gastos.
// Diseñada para Cloud Run: sin estado (stateless), así cualquier instancia puede
// atender cualquier petición y el servicio escala horizontalmente agregando instancias.
//
// IMPORTANTE: es la configuración OBJETIVO. No está desplegada (ver README.md).

const express = require('express');
const admin = require('firebase-admin');

// En Cloud Run las credenciales se obtienen solas de la cuenta de servicio del
// servicio (Application Default Credentials): no hay claves en el código.
admin.initializeApp();
const db = admin.firestore();

// Misma colección que usa la app Android (users/{uid}/gastos), para que la API y la
// app compartan los datos. Se puede cambiar por variable de entorno.
const COLECCION = process.env.COLECCION_GASTOS || 'gastos';
const MAX_LOTE = 500; // límite de operaciones de un batch de Firestore

const app = express();
app.use(express.json({ limit: '256kb' }));

// ---------- Health check (lo usan las sondas de Cloud Run) ----------
app.get('/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// ---------- Autenticación: verifica el ID token de Firebase Auth ----------
// La app envía: Authorization: Bearer <idToken>
async function verificarToken(req, res, next) {
  const header = req.get('Authorization') || '';
  const match = header.match(/^Bearer (.+)$/);
  if (!match) {
    return res.status(401).json({ error: 'Falta el token de autenticación' });
  }
  try {
    const decodificado = await admin.auth().verifyIdToken(match[1]);
    req.uid = decodificado.uid; // el uid sale del token, nunca del cliente
    next();
  } catch (e) {
    res.status(401).json({ error: 'Token inválido o vencido' });
  }
}

function coleccionDe(uid) {
  return db.collection('users').doc(uid).collection(COLECCION);
}

function gastoValido(g) {
  return g
    && typeof g.id === 'string' && g.id.length > 0 && g.id.length <= 100 && !g.id.includes('/')
    && typeof g.descripcion === 'string' && g.descripcion.trim().length > 0 && g.descripcion.length <= 200
    && typeof g.monto === 'number' && Number.isFinite(g.monto) && g.monto > 0
    && typeof g.categoria === 'string' && g.categoria.length > 0 && g.categoria.length <= 50
    && Number.isInteger(g.fecha) && g.fecha > 0;
}

// ---------- GET /expenses?since=<epochMillis> ----------
// Sincronización incremental: devuelve solo los gastos con fecha posterior a "since".
// Evita descargar todo el historial en cada apertura (menos tráfico y lecturas).
app.get('/expenses', verificarToken, async (req, res) => {
  const since = Number.parseInt(req.query.since ?? '0', 10);
  if (Number.isNaN(since) || since < 0) {
    return res.status(400).json({ error: 'El parámetro since debe ser epoch millis' });
  }
  try {
    const snap = await coleccionDe(req.uid)
      .where('fecha', '>', since)
      .orderBy('fecha', 'asc')
      .limit(MAX_LOTE)
      .get();
    const gastos = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    res.json({ gastos, hayMas: gastos.length === MAX_LOTE });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error al leer los gastos' });
  }
});

// ---------- POST /expenses/sync ----------
// Recibe los gastos cargados offline en el teléfono y los guarda en un solo batch.
// Usar el id generado en el cliente hace la operación IDEMPOTENTE: si la red se corta
// y la app reintenta, el mismo gasto se sobrescribe en lugar de duplicarse.
// Body: { "gastos": [ { id, descripcion, monto, categoria, fecha }, ... ] }
app.post('/expenses/sync', verificarToken, async (req, res) => {
  const gastos = req.body && req.body.gastos;
  if (!Array.isArray(gastos) || gastos.length === 0 || gastos.length > MAX_LOTE) {
    return res.status(400).json({ error: `Se esperan entre 1 y ${MAX_LOTE} gastos` });
  }
  const invalidos = gastos.filter((g) => !gastoValido(g)).map((g) => g && g.id);
  if (invalidos.length > 0) {
    return res.status(400).json({ error: 'Hay gastos inválidos', ids: invalidos });
  }
  try {
    const batch = db.batch();
    for (const g of gastos) {
      batch.set(coleccionDe(req.uid).doc(g.id), {
        descripcion: g.descripcion.trim(),
        monto: g.monto,
        categoria: g.categoria,
        fecha: g.fecha,
      });
    }
    await batch.commit();
    res.json({ sincronizados: gastos.length });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'Error al guardar los gastos' });
  }
});

// Cloud Run indica el puerto en la variable PORT (por defecto 8080).
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => console.log(`gastos-api escuchando en el puerto ${PORT}`));
