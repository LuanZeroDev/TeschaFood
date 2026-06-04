# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos

```bash
# Build debug / release  (usar gradlew.bat en Windows)
gradlew.bat assembleDebug
gradlew.bat assembleRelease
gradlew.bat installDebug

# Tests
gradlew.bat test
gradlew.bat connectedAndroidTest
gradlew.bat lint
```

## Arquitectura

App de delivery premium (estilo Uber Eats), paquete `com.tescha.food`.
Backend: **Supabase** (PostgreSQL + Auth + Realtime + Storage).
Proyecto Supabase: `jpvppdqkaktkhvcfgfew.supabase.co`

### Flujo de la aplicación

`MainActivity` → `MyApplicationApp` → condición sobre `MainViewModel.isLoggedIn`:
- **No autenticado:** `WelcomeScreen` → `AuthScreen` (login / registro)
- **Autenticado:** `Scaffold` con `GlassyBottomBar` + `AppNavHost`

### Estructura de paquetes

```
com.tescha.food/
  FoodApp.kt              — Application class, expone AppContainer
  MainActivity.kt         — Entry point; pasa mainViewModel a AppNavHost

  data/
    model/                — User · Product (incluye maxDeliveryKm) · Order · OrderItem · OrderStatus
    repository/
      UserRepository      — interfaz
      ProductRepository   — interfaz
      OrderRepository     — interfaz
      stub/               — implementaciones en memoria (ya NO se usan en producción)
      supabase/           — implementaciones reales:
        SupabaseUserRepository   — Auth (login, register, auto-create perfil)
        SupabaseProductRepository — filtra tiendas activas, incluye maxDeliveryKm
        SupabaseOrderRepository  — pedidos + Realtime para cambios de estado
    remote/
      SupabaseClient.kt   — cliente singleton (Auth, Postgrest, Realtime, Storage)
      DistanceMatrixService.kt — llama a Google Maps Distance Matrix API; caché 5 min
      dto/
        UserDto · ProductDto · StoreDto · OrderDto · VendorRequestDto · VendorRequestWithUser

  di/
    AppContainer.kt       — usa implementaciones Supabase (NO Stub)

  ui/
    components/           — GlassyBottomBar · DeliveryMap (con repartidorLocation)
    navigation/
      NavGraph.kt         — Screen sealed class + AppNavHost (recibe mainViewModel)
                            Rutas: home · activity · wallet · profile · merchant
                                   cart · checkout · payment_result
    screens/
      MainScreens.kt      — HomeScreen (bottom sheet de producto, badge carrito) · WalletScreen
      ActivityScreen.kt   — historial de pedidos + mapa en vivo del repartidor
      CartScreen.kt       — carrito con desglose de costos (RF-05)
      CheckoutScreen.kt   — formulario de pago simulado + tarjeta visual (RF-07)
      PaymentResultScreen.kt — animación 4s procesando → pagado (RF-08)
      ProfileScreen.kt    — perfil, solicitud de vendedor (RF-13), logout
      MerchantScreen.kt   — mapa interactivo PDV (RF-09), horarios, radio (RF-15)
      AdminScreen.kt      — panel de aprobación de vendedores (RF-14)
      WelcomeScreen.kt    — pantalla de bienvenida
      AuthScreen.kt       — login / registro
    theme/                — Color.kt · Type.kt · Theme.kt · Spacing.kt
    viewmodel/
      MainViewModel.kt    — isLoggedIn, currentUser, logout, ubicación GPS
      AuthViewModel.kt    — login / registro
      HomeViewModel.kt    — productos filtrados (RF-01, RF-04) + Distance Matrix (RF-02)
      CartViewModel.kt    — carrito, calcShippingFee (RF-06)
      CheckoutViewModel.kt — validación Luhn (RNF-07), simulación 4s (RF-08)
      ActivityViewModel.kt — pedidos del usuario, Realtime GPS repartidor (RF-11)
      ProfileViewModel.kt — perfil, solicitud vendedor (RF-13)
      MerchantViewModel.kt — gestión de tienda (RF-09, RF-15)
      AdminViewModel.kt   — aprobación de vendedores (RF-14)

  tasks/                  — Documentos de planificación por fase (01-09)
  DESIGN.md               — Sistema de diseño completo
```

### Navegación

`AppNavHost` recibe `mainViewModel` y `cartViewModel` compartido.
El rol del usuario determina qué ve en la pestaña **Perfil**:
- `admin@tescha.edu.mx` → `AdminScreen`
- cualquier otro → `ProfileScreen`

Para añadir una pantalla: agregar objeto a `Screen` → registrar en `AppNavHost`.

### Diseño: Modern Heritage Dark

Sistema documentado en `DESIGN.md`. Reglas críticas:

- **Fondo:** `#1B1112` (`SurfaceDark`)
- **Primary Burgundy:** `#892138` (`Burgundy`) — acciones primarias
- **Metallic Gold:** `#D4AF37` (`MetallicGold`) — acentos, estado activo
- **Glassmorphism:** fondo con `.blur(20.dp)` + contenido sin blur encima
- **Tipografía:** Inter via Google Fonts (`Type.kt`)
- **Bordes:** 8dp cards, 24dp contenedores grandes
- **Espaciado:** `LocalSpacing.current` (xs:4 / sm:8 / md:16 / lg:24 / xl:40)
- **Borde metálico superior en cards:** `Brush.horizontalGradient` con `MetallicGold`

### Google Maps

- API key en `local.properties` (`MAPS_API_KEY=...`) — nunca hardcodear
- Inyectada al manifest vía `manifestPlaceholders` y a código vía `BuildConfig.MAPS_API_KEY`
- `DeliveryMap(origin, destination, repartidorLocation, zoom)` — sigue al repartidor con animación
- `DistanceMatrixService.getDistance()` — llama a Distance Matrix API, caché 5 min (RNF-02)

### Supabase

- URL: `https://jpvppdqkaktkhvcfgfew.supabase.co`
- Anon key en `BuildConfig.SUPABASE_ANON_KEY`
- Bucket de imágenes: `tescha-food` (público)
  - Productos: raíz del bucket (ej. `pizza-todo-terreno.jpg`)
  - Tiendas: `imagenes-tiendas/`
  - Usuarios: `imagenes-usuarios/`
- Auth: Email habilitado, **Confirm email DESACTIVADO** (demo escolar)
- Al hacer login, si el usuario existe en Auth pero no en tabla `users`, se auto-crea la fila

### Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| miki@tescha.edu.mx | Tescha123! | cliente (tiene pedido en_camino para demo RF-11) |
| admin@tescha.edu.mx | (crear en dashboard) | admin → ve AdminScreen |

### Tarifa de envío (RF-06)

Implementada en `CartViewModel.calcShippingFee()` y en función SQL `calc_shipping_fee(km)`:
- 0 – 2.0 km → $25.00
- 2.01 – 5.0 km → $25 + (km − 2) × $10
- > 5.0 km → `null` (bloquea el pedido)

### Simulación de pago (RF-08)

`CheckoutViewModel.pay()` lanza una Coroutine con `delay(4_000)` → cambia estado a `CONFIRMED`.
La UI nunca se congela (RNF-08). El `PaymentResultScreen` muestra la transición animada.

## Dependencias clave

| Librería | Versión | Uso |
|---|---|---|
| Compose BOM | 2025.12.00 | UI declarativa |
| Navigation Compose | 2.8.8 | Navegación |
| Material3 + adaptive nav suite | BOM | Componentes y bottom bar |
| Material Icons Extended | BOM | Iconos |
| Compose Google Fonts | 1.7.8 | Tipografía Inter |
| Maps Compose | 6.4.4 | Mapa en Compose |
| Play Services Maps | 19.2.0 | SDK base Google Maps |
| Play Services Location | 21.3.0 | GPS del dispositivo |
| Supabase BOM | 3.1.4 | Auth + Postgrest + Realtime + Storage |
| Ktor Android | 3.1.3 | Motor HTTP para Supabase |
| kotlinx-serialization-json | 1.8.1 | Serialización DTOs |
| Coil Compose | 2.7.0 | Carga de imágenes desde Storage |

## Estado del proyecto — Fases completadas

### ✅ Fase 1 — Infraestructura
Theme Engine, navegación, spacing, repository pattern, Google Maps SDK.

### ✅ Fase 2 — Autenticación (RF-12, RNF-12)
- Login / registro real con Supabase Auth
- `SupabaseUserRepository`: auto-crea fila en `users` si login desde dashboard
- Validación de contraseña: mín 8 chars, mayúscula, número, carácter especial

### ✅ Fase 3 — Pantalla de Inicio / Discovery (RF-01, RF-02, RF-03, RF-04)
- `HomeScreen` con grid staggered de productos reales desde Supabase
- Filtro de tiendas activas (RF-03)
- Ordenamiento por cercanía Haversine → enriquecido con Distance Matrix API (RF-02)
- Filtro por radio máximo del vendedor (RF-01)
- Bottom sheet de detalle del producto con selector de cantidad
- Badge del carrito en el header

### ✅ Fase 4 — Flujo de Compra (RF-05, RF-06, RF-07, RF-08)
- `CartScreen`: lista de items + desglose subtotal / envío / total (RF-05)
- `CartViewModel.calcShippingFee()`: tarifa por rangos, bloquea > 5km (RF-06)
- `CheckoutScreen`: formulario con tarjeta visual animada, validación Luhn (RF-07, RNF-07)
- `PaymentResultScreen`: spinner 4s → comprobante animado (RF-08, RNF-08)

### ✅ Fase 5 — Actividad y Seguimiento (RF-10, RF-11)
- `ActivityScreen`: historial de pedidos del usuario
- Mapa en vivo con posición del repartidor vía Supabase Realtime
- `delivery_locations` con 5 puntos GPS pre-cargados para demo
- Pedido de demo `en_camino` asignado a miki@tescha.edu.mx

### ✅ Fase 6 — Módulo Vendedor (RF-09, RF-15)
- `MerchantScreen`: mapa interactivo para fijar coordenadas PDV (RF-09)
- Toggle activo/cerrado, horarios de atención, radio máximo con slider (RF-15)
- Muestra tarifa de envío correspondiente al radio seleccionado

### ✅ Fase 7 — Perfil y Ajustes (RF-12, RF-13)
- `ProfileScreen`: avatar, nombre, email, badge de rol
- Selector de documento (PDF, JPEG, PNG ≤ 5MB) + envío de solicitud vendedor (RF-13)
- Estados: Ninguno / Pendiente / Aprobado / Rechazado
- Logout con limpieza de sesión

### ✅ Fase 8 — Panel Admin (RF-14, RNF-14)
- `AdminScreen` visible solo para `admin@tescha.edu.mx`
- Lista solicitudes pendientes con datos del solicitante
- Botones Aprobar (actualiza rol a 'vendedor') / Rechazar

### ✅ Fase 9 — Mejoras UI / UX y Repartidor Fantasma
- **Logo en AuthScreen**: `logo-teschafood.png` mostrado sobre el formulario de login/registro
- **WalletScreen completo**: tarjeta de crédito estética con glassmorphism + saldo + acciones rápidas + movimientos recientes
- **Dirección fija de entrega**: Campus TESCHA `19.234477, -98.840558` en `CartScreen` y en toda la lógica de distancia
- **Avatar en ProfileScreen**: muestra imagen desde Supabase Storage (`imagenes-usuarios/`) si existe; fallback a inicial
- **Permisos al primer lanzamiento**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` y `POST_NOTIFICATIONS` solicitados en `LaunchedEffect(Unit)` de `MainActivity`
- **Repartidor fantasma** (`GhostDelivery`): simulación local tienda → TESCHA en 15 pasos de 8 s (~2 min); inicia al confirmar pago y navega automáticamente a ActivityScreen
- **Mapa expandible**: toque sobre el mapa abre `Dialog` pantalla completa con botón ✕; composable `TrackingMapBox` reutilizado por ghost y live tracking
- **Ruta visual en mapa**: polyline dorada (recorrida) + polyline gris (pendiente); marcador del repartidor con logo circular `logo_black.png`
- **ETA regresivo**: chip con cuenta atrás en segundos; barra de progreso tienda → TESCHA
- **Banner de llegada**: `AnimatedVisibility` con mensaje de entrega + notificación local del sistema
- **LiveTrackingCard mejorado** (pedidos reales como miki): construye ruta visual tienda → repartidor → TESCHA con mismo logo y polylines

### ✅ Fase 10 — Pulido de mapas, errores amigables y avatares
- **`GlassyBottomBar` arreglada**: el `clip(shape)` se aplica al `Box` padre → el fondo glassmorphism respeta las esquinas redondeadas (antes el blur dejaba un cuadrado gris fuera del shape)
- **Marcador del repartidor refactorizado** (`DeliveryMap.makeCircularMarker`): respeta la proporción real del PNG con FIT_CENTER dentro del círculo, fondo blanco interior, borde dorado encima, `anchor = (0.5, 0.5)` para que el chip quede centrado en la coordenada; tamaño 90 px
- **Ruta real por carretera** (`DirectionsService`): consulta Google Directions API, decodifica polyline, caché en memoria por par origen-destino; con log de errores que indica `REQUEST_DENIED` si la API key tiene restricción de aplicación Android (las APIs web exigen clave sin restricción o con restricción de referrers HTTP)
- **`ActivityViewModel.startGhostDelivery`**: ahora fetch Directions primero y re-muestrea la polyline real a 15 puntos equiespaciados por distancia (`resample` con haversine); fallback Manhattan zig-zag si Directions falla
- **Pedidos en_camino reales también son ghost**: en `loadOrders`, cuando hay un `ON_THE_WAY`, se lanza `startGhostDelivery(DEMO_STORE)` en vez de observar `delivery_locations` reales (las coords de demo dispersas por CDMX rompían la visualización). Constante `DEMO_STORE = LatLng(19.2528, -98.8572)` (~2 km del campus para que la ruta sea visible)
- **Cámara que encuadra la ruta** (`DeliveryMap`): construye `LatLngBounds` con origen + destino + repartidor + polyline y aplica `CameraUpdateFactory.newLatLngBounds(bounds, 120)` al cargar y al actualizarse la posición
- **Lite mode para miniatura del mapa**: `DeliveryMap(liteMode = true)` carga el mapa como bitmap estático en `TrackingMapBox`, eliminando intercepción de gestos y haciendo que el `clickable { fullscreen = true }` y el botón "Ampliar" sí disparen. Lite mode usa `googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) }`
- **Botón "Ampliar" como `clickable` explícito**: además del Box padre, el chip "Ampliar" tiene su propio handler
- **Avatar del perfil con texto del tamaño correcto**: `headlineLarge` cambiado a `fontSize = 36.sp` con `TextAlign.Center` para que la inicial quepa en el círculo de 80 dp
- **URL del avatar por convención**: si `users.avatar_url` está vacío o no empieza por `http`, `ProfileScreen` y `SupabaseUserRepository` construyen `https://<proj>.supabase.co/storage/v1/object/public/tescha-food/imagenes-usuarios/<email-prefix>.jpg`. Coil registra en Logcat la excepción si falla
- **Errores amigables en `AuthViewModel.friendlyError(e)`**: convierte excepciones técnicas de Supabase (`invalid_credentials`, `email already registered`, `network`, `rate limit`, etc.) a mensajes cortos en español. Ya no se muestran al usuario el URL, headers ni el JWT
- **Policy de lectura pública para Storage**: aplicada migración `tescha_food_public_read_policy` → `CREATE POLICY ... ON storage.objects FOR SELECT TO public USING (bucket_id = 'tescha-food')`. El bucket ya era `public=true` pero faltaba la policy (sin policies con RLS activado, toda lectura anónima devuelve 403). Después de esto los avatares, imágenes de tiendas y productos cargan correctamente desde Coil

### Archivos nuevos en Fase 10
- `data/remote/DirectionsService.kt` — fetch + decode polyline de Google Directions
- Constante `DEMO_STORE` en `ActivityViewModel.kt` (export pública)

### Cómo evitar errores con las APIs web de Google Maps
La clave en `local.properties` debe tener **restricción de aplicación = Ninguna** o "Referrers HTTP" — NO "Apps de Android" — porque Directions, Distance Matrix, Geocoding, Roads, etc. son APIs web y rechazan claves restringidas por SHA-1 + package. En `Restricciones de API`, habilitar al menos: Maps SDK for Android, Directions API, Distance Matrix API.

## Problemas conocidos y notas

- **Emulador lento**: usar Hardware GLES 2.0 en AVD settings. Preferir dispositivo físico.
- **Supabase Kotlin SDK v3**: `postgresChangeFlow` no soporta `filter` directo (privado). Filtrar con `.filter {}` en el Flow de Kotlin.
- **Supabase select con join**: `.select("*, tabla(cols)")` no funciona en v3; hacer dos queries separadas.
- **UUID mismatch Auth↔users**: al crear usuario desde dashboard, el UUID de Auth difiere del sembrado. Las FKs de `users` se hicieron `DEFERRABLE INITIALLY DEFERRED` para permitir sincronización en transacción. Usar el bloque `BEGIN/COMMIT` documentado abajo.
- **RF-10 en producción**: la captura GPS del repartidor requiere un Foreground Service para funcionar en background. Actualmente solo captura cuando la app está abierta.
- **Coordenadas de entrega fijas**: `19.234477, -98.840558` (campus TESCHA, Chalco). Definidas en `TESCHA_DEST` (`DeliveryMap.kt`), `TESCHA_LAT/LNG` (`CartViewModel.kt`) y `TESCHA_LATLNG` (`ActivityViewModel.kt`).

## Base de datos Supabase

### Tablas principales
| Tabla | Descripción |
|---|---|
| `users` | Perfiles (id = auth.users.id, role: cliente/vendedor/repartidor/admin) |
| `stores` | Tiendas con coordenadas, estado, horarios, radio |
| `products` | Productos por tienda |
| `orders` | Pedidos con coordenadas de entrega, distancia, desglose de costos |
| `order_items` | Items de cada pedido |
| `vendor_requests` | Solicitudes de registro de vendedor |
| `delivery_locations` | Historial GPS del repartidor (solo estado en_camino) |

### Función SQL relevante
```sql
calc_shipping_fee(distance_km double) → numeric(10,2)
-- Misma lógica que CartViewModel.calcShippingFee()
```

### Sincronización UUID Auth↔users (para nuevos usuarios creados desde dashboard)
Las FKs que apuntan a `users.id` son `DEFERRABLE INITIALLY DEFERRED`. Al crear un usuario en Auth Dashboard, ejecutar:
```sql
BEGIN;
  UPDATE stores             SET owner_id      = '<auth_uuid>' WHERE owner_id      = '<old_uuid>';
  UPDATE vendor_requests    SET user_id       = '<auth_uuid>' WHERE user_id       = '<old_uuid>';
  UPDATE vendor_requests    SET reviewed_by   = '<auth_uuid>' WHERE reviewed_by   = '<old_uuid>';
  UPDATE orders             SET client_id     = '<auth_uuid>' WHERE client_id     = '<old_uuid>';
  UPDATE delivery_locations SET repartidor_id = '<auth_uuid>' WHERE repartidor_id = '<old_uuid>';
  UPDATE users              SET id            = '<auth_uuid>' WHERE id            = '<old_uuid>';
COMMIT;
```

### Usuarios de prueba (todos con contraseña `Tescha123!`)
| Email | Rol |
|---|---|
| `miki@tescha.edu.mx` | cliente |
| `emiliano@tescha.edu.mx` | cliente |
| `andree@tescha.edu.mx` | vendedor (Cafetería Coffee Club + Burger Express) |
| `celeste@tescha.edu.mx` | vendedor (El Sabor Oaxaqueño de Doña Céleste) |
| `tuno@tescha.edu.mx` | vendedor (Pizza Tuno) |
| `david@tescha.edu.mx` | repartidor |
| `admin@tescha.edu.mx` | admin → ve AdminScreen |

### Tiendas cerca del TESCHA
| Tienda | Dueño | Coordenadas | Estado |
|---|---|---|---|
| Pizza Tuno | tuno | 19.2352, -98.8415 | activo |
| Cafetería Coffee Club | andree | 19.2338, -98.8402 | activo |
| El Sabor Oaxaqueño de Doña Céleste | celeste | 19.2361, -98.8425 | activo |
| Burger Express | andree | 19.2345, -98.8410 | cerrado |

### Trigger
`trg_store_status` → `pg_notify('store_status_changes', ...)` al cambiar estado del local (RNF-03)
