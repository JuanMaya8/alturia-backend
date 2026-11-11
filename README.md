# alturia-backend

Servidor local y API central del **Sistema de Gestión de Pedidos Alturia**, prototipo del trabajo de grado
*Sistema de software con NLP (Procesamiento de Lenguaje Natural), KDS (Kitchen Display System) e impresión de comandas
para el Restaurante Alturia* — Ingeniería de Software, Universidad Cooperativa de Colombia, sede Pasto.

- **Autores:** Juan David Maya Benavides (Scrum Master · backend · NLP) y Jack Anderson Limas Solarte (Product Owner · frontend · UI/UX)
- **Asesor:** Daniel Fernando Arteaga Fajardo
- **Repositorios del sistema:** `alturia-backend` (este) · `alturia-frontend` · `alturia-nlp`

## 1. ¿Qué hace este repositorio?
Es el mismo código Java ejecutado de dos formas (perfiles de Spring):

| Perfil | Dónde corre | Base de datos | Para qué |
|---|---|---|---|
| `local` (por defecto) | Computador de la caja del restaurante | SQLite (`./data/alturia.db`) | Recibe los pedidos de las tablets por Wi-Fi, los envía al KDS en tiempo real, imprime las comandas por estación, consulta el servicio NLP en `localhost`, atiende la caja y **funciona sin internet**. |
| `cloud` | Nube (Render o Railway) | PostgreSQL | API central: recibe la sincronización del servidor local (bandeja de salida) y guarda el histórico para reportes y respaldo. |

```
 Tablets (sala) ──HTTP/WebSocket──┐
 Televisor KDS + teclado numérico ─┤      ┌──────────── Computador de la caja ────────────┐
 Navegador de la caja ─────────────┼────▶ │ alturia-backend (perfil local) ── SQLite      │
                                   │      │        │ localhost:8000                       │
                                   │      │        ▼                                      │
                                   │      │ alturia-nlp (FastAPI, sin internet)           │
                                   │      └────────┬──────────────────────────────────────┘
 Comandera DigitalPOS ◀─ESC/POS TCP 9100───────────┘
                                            │ internet (opcional): bandeja de salida
                                            ▼
                                   alturia-backend (perfil cloud) ── PostgreSQL
```

## 2. Tecnologías
Java 21 · Spring Boot 3.3 (Web, Data JPA, WebSocket/STOMP, Validation) · SQLite · PostgreSQL · ESC/POS sobre TCP · Maven · GitHub Actions.

## 3. Estructura (componentes del diagrama C4 nivel 3)
```
src/main/java/co/edu/ucc/alturia
├── auth/        Inicio de sesión, roles, sesión de pantalla del KDS (HU-01, HU-02)
├── menu/        Productos, categorías, estaciones, sinónimos y alérgenos (HU-03 a HU-05)
├── mesas/       Mesas y su estado (HU-06, HU-07)
├── pedidos/     Recepción de pedidos, enrutador por estación, registro de tiempos, KDS, consolidado (HU-13 a HU-15, HU-18, HU-19, HU-25 a HU-30)
├── impresion/   Agente de impresión ESC/POS con reintentos y alerta en el KDS (HU-27)
├── nlp/         Cliente del servicio NLP (máx. 3 s; si falla, registro manual) y ejemplos para reentrenar (HU-22 a HU-24)
├── caja/        Cuenta de la mesa, pago en efectivo/transferencia/mixto y comprobante interno (HU-31, HU-32)
├── sync/        Bandeja de salida, sincronizador con la nube y depurador del 80 % (HU-17, RNF-18)
├── reservas/    (pendiente) Reservas y pedidos programados (HU-08 a HU-12)
├── reportes/    (pendiente) Historial y tiempos (HU-20, HU-21)
├── common/      Errores, bitácora de eventos (RNF-17)
└── config/      CORS, WebSocket, salud, datos iniciales
```

## 4. Cómo ejecutarlo
Requisitos: JDK 21 y Maven 3.9 (o el IDE IntelliJ/VS Code con soporte Maven).

```bash
mvn spring-boot:run                      # perfil local, SQLite en ./data/alturia.db
# con el servicio NLP en otra terminal (repositorio alturia-nlp): uvicorn app.main:app --port 8000
```
Variables útiles:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `ALTURIA_NLP_URL` | `http://localhost:8000` | Servicio NLP local |
| `ALTURIA_IMPRESION_MODO` | `consola` | `consola` imprime la comanda en el log; `red` la envía a la comandera |
| `ALTURIA_COMANDERA_HOST` | `192.168.1.50` | IP fija de la DigitalPOS |
| `ALTURIA_KDS_TOKEN` | `kds-cocina-alturia` | Token de la sesión de pantalla del KDS |
| `ALTURIA_NUBE_URL` | vacío | URL de la API central; vacío = solo local |

Perfil nube: `SPRING_PROFILES_ACTIVE=cloud DATABASE_URL=jdbc:postgresql://... mvn spring-boot:run`.

**Usuarios de prueba** (los crea `DataSeeder` la primera vez): `admin/admin12345`, `mesero/mesero12345`, `caja/caja12345`.
El menú de ejemplo (cuy, trucha, jugo de lulo, gaseosa…) **debe reemplazarse por el menú real del restaurante**.

**Servir el frontend desde el servidor local:** compile `alturia-frontend` (`npm run build`) y copie el contenido de
`dist/alturia-frontend/browser/` en `src/main/resources/static/`. Así el KDS abre en `http://<ip-caja>:8080/kds?token=...`
y la caja en `http://localhost:8080/caja`.

## 5. Probar rápido con curl
```bash
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"usuario":"mesero","contrasena":"mesero12345"}' | jq -r .token)
curl -s localhost:8080/api/pedidos -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"idLocal":"demo-1","mesaId":1,"lineas":[{"productoId":1,"cantidad":2,"observacion":"sin ají"},{"productoId":8,"cantidad":1}]}'
curl -s "localhost:8080/api/kds/cola?estacion=Cocina" -H "Authorization: Bearer kds-cocina-alturia"
```
La comanda aparece en el log (modo `consola`) separada por estación: Cocina y Bar.

## 6. Pruebas
```bash
mvn verify      # pruebas unitarias (CajaServiceTest: escenarios de la HU-32) y de contexto con H2
```
GitHub Actions ejecuta lo mismo en cada push y Pull Request.

## 7. Cómo trabajamos (resumen de CONTRIBUTING.md)
- `main`: versiones entregables (etiquetas `v0.1.0-prototipo`, …). `develop`: integración.
- Ramas: `feature/juan/<hu>-<tema>` y `feature/jack/<hu>-<tema>`. Nadie hace push directo a `main` ni a `develop`.
- **Commits pequeños y frecuentes** con Conventional Commits en español: `feat(caja): calcula el cambio del pago en efectivo (HU-32)`.
- Cada Pull Request a `develop` lo revisa el otro integrante y debe pasar el CI.
- Cada carpeta tiene un dueño (ver tabla en CONTRIBUTING.md) para que los commits de Juan y Jack no se crucen.
- El tablero de Trello del proyecto refleja cada HU; el número de HU va en la rama, el commit y el PR.

## 8. Estado del prototipo
Funciona: login y roles, menú, mesas, pedidos manuales y en lenguaje natural (vía NLP), enrutamiento por estación, KDS en tiempo real,
avanzar/deshacer con teclado, verificación de alérgenos, comandas ESC/POS con reintento, consolidado, caja con pago en efectivo,
transferencia o mixto y comprobante, bandeja de salida y sincronización.
Pendiente: ver [docs/BACKLOG.md](docs/BACKLOG.md). Contrato completo de la API: [docs/API.md](docs/API.md).
