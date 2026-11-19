# Backend en Cloud Run — configuración objetivo

> **Esta carpeta es diseño, no está desplegada.** Cloud Run requiere un proyecto de
> Google Cloud con **cuenta de facturación** (tarjeta), aunque el uso entre dentro de la
> capa gratuita. Por esa restricción, la app **hoy usa Firebase directo** (Auth +
> Firestore, plan Spark gratuito) y este servicio queda documentado como el siguiente
> paso de la estrategia de escalabilidad.

## Qué es

Una API REST en Node.js + Express, empaquetada en un contenedor, pensada para correr en
Cloud Run (región `southamerica-east1`, San Pablo).

| Endpoint | Auth | Para qué |
|---|---|---|
| `GET /health` | No | Sonda de salud (Cloud Run la usa para saber si la instancia está viva). |
| `GET /expenses?since=<epochMillis>` | Sí | Sincronización incremental: devuelve solo los gastos nuevos desde `since`. |
| `POST /expenses/sync` | Sí | Sube en un lote los gastos cargados offline. Idempotente (usa el id del cliente). |

**Autenticación:** la app envía `Authorization: Bearer <ID token de Firebase Auth>`. El
servidor lo verifica con `firebase-admin` y toma el `uid` del token, nunca del cuerpo de
la petición. Así se reutiliza el mismo login de la app.

**Datos:** escribe en `users/{uid}/gastos` (la misma colección que usa la app), por lo que
la API y el acceso directo de Firebase pueden convivir durante una migración.

> Nota: la especificación original nombraba la colección `expenses`. Se dejó `gastos`
> como valor por defecto (variable `COLECCION_GASTOS`) para que la API lea y escriba los
> mismos datos que la app; con otro nombre serían dos bases desconectadas.

## Archivos

- `server.js` — la API.
- `package.json` — dependencias (`express`, `firebase-admin`).
- `Dockerfile` — imagen `node:20-slim`, usuario no root, puerto 8080.
- `service.yaml` — configuración declarativa del servicio de Cloud Run.

## Cómo cubre la estrategia de escalabilidad

| Aspecto (consigna TP3) | Cómo se resuelve |
|---|---|
| **Autoescalado** | `minScale: 0` (sin tráfico no hay instancias ni costo), `maxScale: 10` (tope de costo). Cloud Run agrega instancias según las peticiones concurrentes. |
| **Balanceo de carga** | Cloud Run trae un balanceador administrado delante del servicio que reparte las peticiones entre las instancias activas. `containerConcurrency: 80` define cuándo una instancia está "llena" y hace falta otra. |
| **Redundancia** | Cloud Run es un servicio regional: distribuye instancias entre varias zonas de la región. Las sondas `startupProbe`/`livenessProbe` sobre `/health` sacan de servicio y reemplazan instancias que fallan. Firestore replica los datos en varias zonas. |
| **Sin estado** | El servidor no guarda nada en memoria entre peticiones; todo vive en Firestore. Eso permite crear o destruir instancias en cualquier momento. |
| **Recursos** | 1 vCPU y 512 MiB por instancia; `timeoutSeconds: 30`; CPU solo durante las peticiones (`cpu-throttling`). |
| **Seguridad** | ID token de Firebase en cada petición, cuenta de servicio con permiso mínimo, validación de datos en el servidor. |

## Cómo se desplegaría (NO ejecutado)

Se deja como referencia; requiere facturación habilitada y la CLI `gcloud`:

```bash
# 1. Construir y subir la imagen a Artifact Registry
gcloud builds submit --tag southamerica-east1-docker.pkg.dev/PROJECT_ID/gastos/gastos-api:1.0.0

# 2. Crear/actualizar el servicio con la configuración declarativa
gcloud run services replace service.yaml --region southamerica-east1
```

## Cómo se probaría localmente (opcional)

```bash
npm install
# Requiere credenciales de una cuenta de servicio del proyecto:
# GOOGLE_APPLICATION_CREDENTIALS=ruta/al/archivo.json
npm start
curl http://localhost:8080/health
```
