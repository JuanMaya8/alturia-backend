# Trabajo pendiente del backend (después del prototipo base)

Cada ítem es una rama `feature/<persona>/<hu>-<tema>` con **commits pequeños**. No se toca código del otro sin acordarlo.

## Jack Anderson Limas Solarte (paquetes `menu`, `mesas`, `reservas`, `reportes`)
1. **HU-08 Registrar reservas** (`reservas`): entidad `Reserva` (nombreCliente, telefono, fechaHora, numeroPersonas, mesa, estado `PENDIENTE|CONFIRMADA|CLIENTE_LLEGO|ATENDIDA|CANCELADA`), `ReservaController` `GET/POST /api/reservas`. Validar choque de horario: una mesa no puede tener dos reservas que se crucen en una ventana de 2 h → 409 «La mesa ya está reservada en ese horario».
2. **HU-09 Modificar reserva**: `PUT /api/reservas/{id}`; rechazar si está ATENDIDA o CANCELADA («La reserva ya fue cerrada»).
3. **HU-10 Preparación anticipada**: entidad `PedidoProgramado` (reserva, lineas, horaEntrega, observaciones). Tarea `@Scheduled` cada minuto que, cuando `ahora >= horaEntrega - tiempoPreparacionMin`, crea el pedido con `PedidoService.crear(...)` (etiqueta RESERVA en el KDS).
4. **HU-11 Consultar reservas** `GET /api/reservas?fecha=` y **HU-12 Llegada** `POST /api/reservas/{id}/llegada` (mesa → OCUPADA).
5. **HU-20 Historial** (`reportes`): `GET /api/reportes/historial?desde&hasta&mesaId&mesero&estado&medio` y `GET /api/reportes/pagos-dia?fecha=` (total efectivo, total transferencia, número de cuentas).
6. **HU-21 Tiempos**: `GET /api/reportes/tiempos?desde&hasta` → promedio por estación y por producto desde `RegistroTiempo` (EN_PREPARACION → LISTO).
7. **HU-07** estado `POR_PAGAR`: cuando todos los pedidos de la cuenta estén entregados.
8. Pruebas unitarias de cada servicio nuevo.

## Juan David Maya Benavides (resto de paquetes)
1. Pruebas de integración de `PedidoService` con H2: idempotencia por `idLocal`, avanzar por estación, deshacer en 10 s, verificación de alérgenos, comanda de modificación.
2. **HU-17**: proyectar en la nube (`SyncController`) los eventos recibidos en tablas de reportes.
3. **RNF-18**: extender `DepuradorAlmacenamiento` a pedidos/comandas antiguos ya sincronizados.
4. **HU-24**: `GET /api/nlp/ejemplos` (ADMIN) para exportar frases corregidas al repositorio `alturia-nlp`.
5. Configuración de impresión real (modo `red`) con la DigitalPOS: probar en el restaurante y dejar evidencia.
6. `Dockerfile` y guía de despliegue en Render/Railway con perfil `cloud`.
7. Bitácora de eventos en todas las acciones relevantes (RNF-17).
