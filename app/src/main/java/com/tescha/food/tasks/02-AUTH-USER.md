# 02. Módulo de Usuario y Autenticación

*Gestión de la identidad y estados de la aplicación.*

- [x] **Lógica de Doble Perfil (Comprador/Vendedor):** Flag `isMerchant` en `User.kt`; el stub incluye usuario vendedor de prueba.
- [x] **Servicio de Ubicación:** `LocationService.kt` con `FusedLocationProviderClient`; solicitud de permisos en `MainActivity` tras login; coordenadas persistidas en `SessionManager`.
- [x] **Pantalla de Registro/Login:** `AuthScreen.kt` con campos Ghost (borde 1px, fondo transparente), animación entre modo Login/Registro, validación y feedback de errores.
- [x] **Gestión de Sesión:** `SessionManager.kt` (SharedPreferences) persiste datos del usuario y preferencias de ubicación entre sesiones.
