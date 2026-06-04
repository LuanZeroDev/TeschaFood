# 🍔 TeschaFood — Aplicación de Delivery Premium

**TeschaFood** es una aplicación Android de delivery estilo Uber Eats, desarrollada con **Jetpack Compose** y respaldada por **Supabase**. Implementa un sistema completo de autenticación, descubrimiento de productos, gestión de pedidos, seguimiento en vivo de entregas y un panel de administración para la aprobación de vendedores.

---

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Documentación & Archivos MD](#documentación--archivos-md)
- [Inicio Rápido](#inicio-rápido)
- [Flujo de la Aplicación](#flujo-de-la-aplicación)
- [Módulos Principales](#módulos-principales)

---

## 🏛️ Arquitectura

### Patrón de Diseño: **MVVM + Repository**

```
UI Layer (Compose)
    ↓
ViewModel (State Management)
    ↓
Repository (Data Access)
    ↓
Remote (Supabase) / Local (SessionManager)
```

### Componentes Clave

| Capa | Componentes | Responsabilidad |
|------|-------------|-----------------|
| **Presentación** | Screens, Composables, ViewModels | Renderizar UI, gestionar estado local |
| **Dominio** | Models (User, Product, Order) | Lógica de negocio, validaciones |
| **Datos** | Repositories, DTOs, Remote Services | Obtener/persistir datos de Supabase |
| **Infraestructura** | SupabaseClient, Google Maps, LocationService | Integraciones externas |

### Backend: Supabase (PostgreSQL + Auth + Realtime)

- **Proyecto:** `jpvppdqkaktkhvcfgfew.supabase.co`
- **Auth:** Email + Contraseña (Confirm email desactivado para demo)
- **Storage:** Bucket público `tescha-food` con subcarpetas para imágenes
  - Productos: raíz
  - Tiendas: `imagenes-tiendas/`
  - Usuarios: `imagenes-usuarios/`
- **Realtime:** Tracking de repartidores en vivo (tabla `delivery_locations`)
- **Base de Datos:**
  - Tablas: `users`, `stores`, `products`, `orders`, `order_items`, `vendor_requests`, `delivery_locations`
  - RLS (Row Level Security) activado
  - Políticas de acceso público para Storage

---

## 🛠 Tecnologías

| Librería | Versión | Uso |
|----------|---------|-----|
| **Jetpack Compose** | BOM 2025.12.00 | UI declarativa |
| **Navigation Compose** | 2.8.8 | Enrutamiento entre pantallas |
| **Material3** | BOM | Componentes de diseño |
| **Supabase Kotlin** | 3.1.4 | Auth, Postgrest, Realtime, Storage |
| **Maps Compose** | 6.4.4 | Integración de Google Maps |
| **Play Services Location** | 21.3.0 | GPS del dispositivo |
| **Coil Compose** | 2.7.0 | Carga de imágenes (Storage) |
| **Ktor Android** | 3.1.3 | Cliente HTTP para APIs |
| **kotlinx-serialization** | 1.8.1 | Serialización JSON |

---

## 📁 Estructura del Proyecto

```
com.tescha.food/
├── FoodApp.kt                    ← Application class, expone AppContainer
├── MainActivity.kt               ← Entry point, solicita permisos, inicia nav
│
├── data/
│   ├── model/                    ← User, Product, Order, OrderItem, OrderStatus
│   ├── local/
│   │   └── SessionManager.kt     ← Token local, preferencias de usuario
│   ├── remote/
│   │   ├── SupabaseClient.kt     ← Cliente singleton (Auth, Postgrest, Realtime)
│   │   ├── DistanceMatrixService.kt ← Google Maps Distance Matrix API (caché 5 min)
│   │   ├── DirectionsService.kt  ← Google Maps Directions API (polylines reales)
│   │   └── dto/                  ← DTOs de serialización
│   └── repository/
│       ├── UserRepository.kt, ProductRepository.kt, OrderRepository.kt (interfaces)
│       ├── stub/                 ← Implementaciones en memoria (desarrollo)
│       └── supabase/             ← Implementaciones reales (producción)
│
├── di/
│   └── AppContainer.kt           ← Inyección de dependencias (Supabase repos)
│
├── ui/
│   ├── components/
│   │   ├── GlassyBottomBar.kt    ← Navegación inferior con glassmorphism
│   │   └── DeliveryMap.kt        ← Mapa interactivo (Directions, Realtime)
│   ├── navigation/
│   │   ├── NavGraph.kt           ← Rutas (Screen sealed class) + AppNavHost
│   │   └── screens/
│   │       ├── AuthScreen.kt     ← Login / Registro
│   │       ├── MainScreens.kt    ← HomeScreen + WalletScreen
│   │       ├── CartScreen.kt     ← Carrito (RF-05, RF-06)
│   │       ├── CheckoutScreen.kt ← Pago simulado (RF-07)
│   │       ├── PaymentResultScreen.kt ← Confirmación animada (RF-08)
│   │       ├── ActivityScreen.kt ← Historial + tracking en vivo (RF-11)
│   │       ├── MerchantScreen.kt ← Gestión de tienda (RF-09, RF-15)
│   │       ├── AdminScreen.kt    ← Aprobación de vendedores (RF-14)
│   │       ├── ProfileScreen.kt  ← Perfil + solicitud vendedor (RF-13)
│   │       └── WelcomeScreen.kt  ← Bienvenida
│   ├── theme/
│   │   ├── Color.kt              ← Paleta: Burgundy, MetallicGold, SurfaceDark
│   │   ├── Type.kt               ← Tipografía Inter (Google Fonts)
│   │   ├── Theme.kt              ← Tema global
│   │   └── Spacing.kt            ← Sistema de espaciado (xs/sm/md/lg/xl)
│   └── viewmodel/
│       ├── MainViewModel.kt      ← Estado global (usuario, auth)
│       ├── HomeViewModel.kt      ← Productos filtrados, ordenamiento
│       ├── CartViewModel.kt      ← Carrito, cálculo de envío
│       ├── CheckoutViewModel.kt  ← Validación, simulación de pago
│       ├── ActivityViewModel.kt  ← Pedidos, tracking (Realtime + Ghost)
│       ├── ProfileViewModel.kt   ← Perfil, solicitud vendedor
│       ├── AuthViewModel.kt      ← Login, registro, errores amigables
│       ├── MerchantViewModel.kt  ← Tienda, horarios, radio
│       └── AdminViewModel.kt     ← Aprobación de solicitudes
│
├── service/
│   └── LocationService.kt        ← Captura GPS (usando Play Services Location)
│
└── tasks/                        ← Documentos de planificación por fase

app/src/main/res/
├── drawable/                     ← Logos, iconos XML
└── mipmap-*/                     ← App icons (múltiples densidades)
```

---

## 📚 Documentación & Archivos MD

### **📖 Archivos de Referencia**

| Archivo | Ubicación | Propósito |
|---------|-----------|----------|
| **CLAUDE.md** | `/CLAUDE.md` | **Guía oficial del proyecto.** Contiene comandos Gradle, arquitectura, esquema BD, usuarios de prueba, problemas conocidos. **LEER PRIMERO.** |
| **DESIGN.md** | `app/src/main/java/com/tescha/food/DESIGN.md` | **Sistema de diseño completo.** Colores, tipografía, espaciado, componentes, reglas de glassmorphism, bordes, shadows. Especificación de "Modern Heritage Dark". |
| **ROADMAP.md** | `app/src/main/java/com/tescha/food/ROADMAP.md` | **Visión de futuro.** Features planejadas, priorización, roadmap de 12 meses. |

### **🎯 Archivos de Planificación (Fases 01-09)**

Ubicados en `app/src/main/java/com/tescha/food/tasks/`:

| Fase | Archivo | Requisitos |
|------|---------|-----------|
| **01** | `01-INFRASTRUCTURE.md` | Theme Engine, navegación, Google Maps SDK, patrón Repository |
| **02** | `02-AUTH-USER.md` | Login/Registro Supabase, validación, auto-creación de perfil |
| **03** | `03-HOME-SCREEN.md` | Grid de productos, filtro tiendas, ordenamiento Haversine, RF-01/02/03/04 |
| **04** | `04-WALLET-CHECKOUT.md` | Carrito, envío dinámico, pago simulado, RF-05/06/07/08 |
| **05** | `05-ACTIVITY-TRACKING.md` | Historial pedidos, mapa en vivo, Realtime, repartidor fantasma, RF-10/11 |
| **06** | `06-MERCHANT-MODULE.md` | Tienda, coordenadas PDV, horarios, radio, RF-09/15 |
| **07** | `07-PROFILE-SETTINGS.md` | Avatar, perfil, solicitud vendedor, logout, RF-12/13 |
| **08** | `08-QA-VERIFICATION.md` | Pruebas, casos de uso, integración, RNF compliance |
| **09** | `09-DATA-MANAGEMENT.md` | Sincronización BD, usuarios de prueba, seeding |

### **📋 Otros Documentos**

| Archivo | Propósito |
|---------|----------|
| `RF && RNF.md` | Especificación completa de requisitos funcionales y no-funcionales |
| `casos de uso.md` | Diagramas y narrativas de casos de uso |
| `Especificacion para la codificacion.md` | Convenciones, naming, patrones de código |
| `Esquema de pruebas unitarias.md` | Plan de testing (unit tests) |
| `Esquema de pruebas de integracion - Modulo 1.md` | Plan de testing (integration tests) |
| `Documentacion de pruebas de integracion.md` | Resultados y metodología |

---

## ⚡ Inicio Rápido

### 1️⃣ **Clonar y Configurar**

```bash
git clone https://github.com/LuanZeroDev/TeschaFood.git
cd TeschaFood
```

### 2️⃣ **Configurar Credenciales**

Crear `local.properties` en la raíz:

```properties
MAPS_API_KEY=your_google_maps_api_key
SUPABASE_URL=https://jpvppdqkaktkhvcfgfew.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
```

**Notas sobre APIs:**
- **Google Maps API Key:** Sin restricción de aplicación (tipo "Ninguna" o "Referrers HTTP")
- Habilitar en Google Cloud Console: Maps SDK for Android, Directions API, Distance Matrix API

### 3️⃣ **Compilar & Ejecutar**

```bash
# Build debug
gradlew.bat assembleDebug

# Instalar en emulador/dispositivo
gradlew.bat installDebug

# O en IDE: Run → Run 'app'
```

### 4️⃣ **Usuarios de Prueba**

| Email | Contraseña | Rol |
|-------|-----------|-----|
| `miki@tescha.edu.mx` | `Tescha123!` | Cliente (demo en_camino) |
| `admin@tescha.edu.mx` | `Tescha123!` | Admin (ApprovalScreen) |
| `andree@tescha.edu.mx` | `Tescha123!` | Vendedor |
| `david@tescha.edu.mx` | `Tescha123!` | Repartidor |

---

## 🔄 Flujo de la Aplicación

```
MainActivity
    ↓
FoodApp (Application)
    ↓
MainViewModel.isLoggedIn?
    ├─→ NO: WelcomeScreen → AuthScreen (login/registro)
    │
    └─→ SÍ: AppScaffold
           ├─ GlassyBottomBar (navegación)
           └─ AppNavHost
              ├─ HomeScreen (discovery)
              ├─ WalletScreen (saldo)
              ├─ ActivityScreen (pedidos + tracking)
              ├─ CartScreen (carrito)
              ├─ CheckoutScreen (pago)
              ├─ PaymentResultScreen (confirmación)
              ├─ MerchantScreen (tienda)
              ├─ ProfileScreen (perfil) o AdminScreen (si admin)
```

### Autenticación

1. Usuario entra a **AuthScreen**
2. Ingresa email + contraseña
3. `AuthViewModel.login()` / `register()` usa `SupabaseUserRepository`
4. Supabase Auth emite token JWT
5. `SessionManager` guarda token localmente
6. `MainViewModel.isLoggedIn` se actualiza
7. Nav automática a **HomeScreen**

---

## 🎯 Módulos Principales

### 📍 **HomeScreen (Discovery)**

- Grid staggered de productos reales desde Supabase
- Filtro por tiendas activas (RF-03)
- Ordenamiento por distancia: Haversine → Distance Matrix API (RF-02)
- Filtro por radio máximo del vendedor (RF-01)
- Bottom sheet de detalle producto con selector de cantidad
- Badge del carrito

### 🛒 **CartScreen & CheckoutScreen**

- **CartScreen:** Lista de items, desglose (subtotal/envío/total), RF-05
- **Tarifa dinámica:** 
  - 0–2.0 km → $25.00
  - 2.01–5.0 km → $25 + (km − 2) × $10
  - \> 5.0 km → Bloqueado (RF-06)
- **CheckoutScreen:** Formulario tarjeta, validación Luhn (RF-07)
- **PaymentResultScreen:** Spinner 4s → comprobante animado (RF-08)

### 🚗 **ActivityScreen + Tracking**

- Historial de pedidos del usuario
- **Ghost Delivery:** Simulación local tienda → TESCHA (15 pasos, ~2 min)
  - Inicia al confirmar pago
  - Navega automáticamente desde `CheckoutScreen`
  - Muestra ETA regresivo
  - Polylines doradas (recorrida) + grises (pendiente)
- **Live Tracking:** Pedidos reales con `delivery_locations` (Realtime)
- Mapa expandible (toque = fullscreen)
- Banner de llegada con notificación del sistema
- **Ruta real:** `DirectionsService` → polyline decodificada (caché por par)

### 🏪 **MerchantScreen**

- Mapa interactivo para fijar coordenadas PDV (RF-09)
- Toggle activo/cerrado
- Horarios de atención (7 días)
- Slider radio máximo (RF-15)
- Muestra tarifa de envío correspondiente

### 👤 **ProfileScreen & Solicitud Vendedor**

- Avatar + datos del usuario
- Selector documento (PDF, JPEG, PNG ≤ 5MB)
- Botón "Solicitar ser vendedor" (RF-13)
- Estados: Ninguno / Pendiente / Aprobado / Rechazado
- Logout con limpieza de sesión

### 🛡️ **AdminScreen**

- Visible solo para `admin@tescha.edu.mx` (RF-14)
- Lista de solicitudes vendedor pendientes
- Datos del solicitante
- Botones Aprobar (rol → vendedor) / Rechazar

---

## 🎨 Diseño: Modern Heritage Dark

**Sistema de Diseño Documentado en `DESIGN.md`**

- **Fondo:** `#1B1112` (SurfaceDark)
- **Primary:** `#892138` (Burgundy) — acciones
- **Acentos:** `#D4AF37` (MetallicGold) — estado activo, bordes superiores
- **Glassmorphism:** `.blur(20.dp)` + contenido sin blur encima
- **Tipografía:** Inter via Google Fonts
- **Bordes:** 8dp cards, 24dp contenedores
- **Espaciado:** `LocalSpacing.current` (xs:4 / sm:8 / md:16 / lg:24 / xl:40)

---

## 🚀 Comandos Útiles

```bash
# Build
gradlew.bat assembleDebug        # Debug APK
gradlew.bat assembleRelease      # Release APK
gradlew.bat installDebug         # Instalar en dispositivo

# Tests
gradlew.bat test                 # Unit tests
gradlew.bat connectedAndroidTest # Instrumented tests
gradlew.bat lint                 # Análisis estático

# Clean
gradlew.bat clean
```

---

## ⚠️ Problemas Conocidos

| Problema | Solución |
|----------|----------|
| Emulador lento | Usar Hardware GLES 2.0 en AVD settings, preferir dispositivo físico |
| Supabase v3 Kotlin SDK | `postgresChangeFlow` sin `filter` directo (privado) — filtrar con `.filter {}` en Flow |
| UUID mismatch Auth↔users | FKs `DEFERRABLE INITIALLY DEFERRED` — usar bloque `BEGIN/COMMIT` en SQL |
| Direcciones API rechaza clave | Key no debe tener restricción "Apps de Android" — use "Ninguna" o "Referrers HTTP" |
| RF-10 en background | Requiere Foreground Service (no implementado en demo) |

---

## 📝 Cómo Contribuir

1. **Lee CLAUDE.md** — Arquitectura oficial
2. **Consulta DESIGN.md** — Especificación visual
3. **Revisa la Fase correspondiente** en `tasks/` — Requisitos
4. **Sigue convenciones** en `Especificacion para la codificacion.md`
5. **Escribe tests** (unit + integration)
6. **Commit con mensaje descriptivo** — `tipo/archivos: descripción`
7. **Abre PR** con referencia a Fase/RF

---

## 📄 Licencia

Este proyecto es parte de TESCHA (Instituto Tecnológico de Chalco). Uso educativo.

---

## 🔗 Enlaces

- **Repositorio:** https://github.com/LuanZeroDev/TeschaFood
- **Supabase:** https://jpvppdqkaktkhvcfgfew.supabase.co
- **Documentación Supabase:** https://supabase.com/docs

---

**Última actualización:** Junio 2026  
**Estado:** ✅ Fases 1-10 completadas (Infraestructura, Auth, Discovery, Compra, Tracking, Vendedor, Perfil, Admin, Pulido)
