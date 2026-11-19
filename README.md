# Anotador de gastos diarios — TP3 Estrategias de escalabilidad

App nativa para Android (Kotlin + Jetpack Compose). Continúa el MVP del TP2
(Build-Measure-Learn) y lo prepara para escalar a muchos usuarios y dispositivos.

**Alumno:** Matías Callejo — UTN FRSF, Ingeniería en Sistemas de Información.

## Qué hace la app

- **Cuenta de usuario** (Firebase Authentication, email y contraseña): ingresar, crear
  cuenta y cerrar sesión. Si ya hay sesión, la app entra directo a los gastos.
- **Carga de gastos** con descripción, monto y categoría (Comida, Transporte, Servicios,
  Otros), con mensajes de error junto al campo que falla.
- **Persistencia en la nube** (Cloud Firestore) en `users/{uid}/gastos/{gastoId}`: los
  gastos se conservan entre sesiones y dispositivos.
- **Funciona sin internet**: la caché offline de Firestore guarda los gastos en el
  teléfono y los sube sola al volver la conexión. Un indicador muestra
  *Sincronizado / Pendiente de sincronizar / Sin conexión*.
- **Historial agrupado por día** (Hoy, Ayer, fechas anteriores) con subtotal por día y el
  **total del día** siempre visible.

### Cambios respecto del TP2

| TP2 | TP3 |
|---|---|
| Gastos en memoria (`mutableStateListOf`), se pierden al cerrar | Firestore con caché offline persistente |
| Estado en el composable (`remember`) | `GastosViewModel` con `StateFlow` |
| Sin usuarios | Firebase Auth (email/contraseña) + reglas de seguridad por usuario |
| Solo "gastos de hoy" | Historial por día (pedido del usuario en la prueba del TP2) |
| El gasto inválido se ignoraba sin avisar | Mensaje de error junto al campo |

## Arquitectura

```
┌────────────────────────── App Android ──────────────────────────┐
│  UI (Jetpack Compose)                                           │
│    LoginScreen · GastosScreen                                   │
│        │ eventos                     ▲ StateFlow                │
│        ▼                             │                          │
│  GastosViewModel  (sesión, lista, errores de validación)        │
│        │                                                        │
│        ▼                                                        │
│  AuthRepository           GastosRepository                      │
│        │                       │  (snapshot listener → Flow)    │
│        │                  Caché local de Firestore (offline)    │
└────────┼───────────────────────┼────────────────────────────────┘
         ▼                       ▼
   Firebase Auth          Cloud Firestore  ← firestore.rules
                          users/{uid}/gastos/{gastoId}
```

- **UI**: solo dibuja el estado y emite eventos (flujo unidireccional, igual que en el TP2).
- **ViewModel**: guarda el estado, valida y sobrevive a rotaciones de pantalla.
- **Repositorios**: único lugar que conoce Firebase. Cambiar el origen de los datos (por
  ejemplo, a la API de Cloud Run) no obliga a tocar la UI.
- **Firestore + Auth**: servicios administrados que escalan solos (sin servidores propios)
  y entran en el plan gratuito Spark.

La evolución prevista (API propia en Cloud Run con autoescalado y balanceo) está diseñada
en [`docs/backend-cloud-run/`](docs/backend-cloud-run/README.md). **No está desplegada**
porque requiere cuenta de facturación.

## Configurar Firebase (cada persona usa su propio proyecto)

`app/google-services.json` **no se sube al repositorio** (está en `.gitignore`).

1. Crear un proyecto en <https://console.firebase.google.com> (plan Spark, gratuito).
2. Agregar una app Android con el paquete `com.example.tp2arqmoviles`, descargar
   `google-services.json` y copiarlo en la carpeta `app/`.
3. **Authentication** → Sign-in method → habilitar **Correo electrónico/contraseña**.
4. **Firestore Database** → Crear base de datos (modo producción, ubicación
   `southamerica-east1` o la más cercana).
5. Firestore → **Reglas** → pegar el contenido de [`firestore.rules`](firestore.rules) → Publicar.

## Cómo ejecutarla

1. Abrir la carpeta en Android Studio y sincronizar Gradle.
2. Correr la configuración `app` en un emulador o dispositivo con Google Play Services.
3. APK: *Build → Generate App Bundles or APKs → Generate APKs*, o `./gradlew assembleDebug`
   (queda en `app/build/outputs/apk/debug/app-debug.apk`).

## Estructura

```
app/src/main/java/com/example/tp2arqmoviles/
├── GastosApp.kt              # Application: configura la caché offline de Firestore
├── MainActivity.kt           # Elige Login o Gastos según la sesión
├── data/
│   ├── Gasto.kt              # Modelo (id, descripcion, monto, categoria, fecha)
│   ├── AuthRepository.kt     # Firebase Auth + mensajes de error en español
│   └── GastosRepository.kt   # Firestore: agregar y observar en tiempo real
└── ui/
    ├── GastosViewModel.kt    # Estado (StateFlow) y validaciones
    ├── LoginScreen.kt
    └── GastosScreen.kt
firestore.rules               # Reglas de seguridad (se pegan en la consola)
docs/backend-cloud-run/       # Diseño del backend escalable (no desplegado)
```
