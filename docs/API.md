# API del servidor local y de la API central

Base: `http://<ip-computador-caja>:8080`. Todas las rutas `/api/**` (salvo `/api/auth/login`, `/api/salud` y `/api/sync/**`)
exigen el encabezado `Authorization: Bearer <token>`. Los errores devuelven `{ "mensaje": "..." }` listo para mostrar.

| Método | Ruta | Rol | HU | Descripción |
|---|---|---|---|---|
| POST | /api/auth/login | — | HU-01 | `{usuario, contrasena}` → `{token, usuarioId, nombre, rol}` |
| GET | /api/auth/yo | todos | HU-01 | Sesión actual |
| GET | /api/salud | — | HU-16 | Las tablets lo consultan para saber si hay red local |
| GET/POST | /api/usuarios | ADMINISTRADOR | HU-02 | Listar / crear usuario |
| PATCH | /api/usuarios/{id}/activo?valor= | ADMINISTRADOR | HU-02 | Activar o desactivar |
| GET | /api/menu/productos · /categorias · /estaciones | todos | HU-05 | Catálogo |
| POST/PUT/DELETE | /api/menu/productos[/{id}] | ADMINISTRADOR | HU-03 | Administrar productos |
| PATCH | /api/menu/productos/{id}/disponible?valor= | ADMINISTRADOR | HU-04 | Agotado / disponible |
| GET/POST | /api/mesas | todos / ADMIN | HU-06 | Mesas |
| PATCH | /api/mesas/{id}/estado?valor= | MESERO, ADMIN | HU-07 | Estado de la mesa |
| POST | /api/nlp/interpretar | MESERO | HU-22 | `{frase}` → propuesta del NLP (503 si el servicio falla: registro manual) |
| POST | /api/pedidos | MESERO | HU-13, HU-14 | Confirmar pedido (idempotente por `idLocal`) |
| GET | /api/pedidos/activos | todos | HU-19 | Pedidos en curso |
| PUT | /api/pedidos/{id}/lineas | MESERO | HU-15 | Modificar (imprime comanda de modificación) |
| POST | /api/pedidos/{id}/entregar | MESERO | HU-19 | Marcar entregado |
| GET | /api/kds/cola?estacion=Cocina | COCINA | HU-25, HU-26, HU-28 | Cola por estación |
| GET | /api/kds/consolidado?estacion= | COCINA | HU-29 | Consolidado de producción |
| POST | /api/kds/pedidos/{id}/avanzar?estacion=&verificado= | COCINA | HU-18, HU-30 | Tecla Enter |
| POST | /api/kds/pedidos/{id}/deshacer?estacion= | COCINA | HU-18 | Tecla 0 (10 s) |
| POST | /api/kds/pedidos/{id}/reimprimir?estacion= | COCINA | HU-27 | Tecla «.» |
| GET | /api/caja/cuentas | CAJERO | HU-31 | Cuentas abiertas |
| GET | /api/caja/mesas/{mesaId}/cuenta | CAJERO | HU-31 | Detalle de la cuenta |
| POST | /api/caja/mesas/{mesaId}/calcular | CAJERO | HU-32 | Vista previa: saldo pendiente y cambio |
| POST | /api/caja/mesas/{mesaId}/pagos | CAJERO | HU-32 | `{efectivoRecibido, valorTransferencia, referenciaTransferencia}` |
| POST | /api/sync/eventos | clave `X-Sync-Key` | HU-17 | Solo perfil `cloud` |

## WebSocket (STOMP en `/ws`)
| Tópico | Contenido |
|---|---|
| `/topic/kds` | Pedido creado o actualizado (`PedidoDto`) |
| `/topic/kds/alertas` | `{tipo: "COMANDERA_SIN_RESPUESTA", ...}` |
| `/topic/sala/avisos` | `{tipo: "PEDIDO_LISTO" \| "REQUIERE_REVISION", mesa, meseroId, mensaje}` |
| `/topic/mesas` | Mesa actualizada |
| `/topic/menu` | `{productoId, disponible}` |

## Contrato con el servicio NLP
`POST {alturia.nlp.url}/interpretar` con `{frase, catalogo:[{id, nombre, sinonimos, disponible}]}`.
La respuesta se reenvía tal cual a la tablet (ver README del repositorio `alturia-nlp`).
