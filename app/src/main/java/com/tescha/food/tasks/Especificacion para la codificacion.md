# Especificación para la Codificación — Tescha Food

## Módulo 1: Gestión de Pedidos y Pagos (RF-01 a RF-08)

> Documento de la fase de **Codificación** del proceso de desarrollo.
> Define cómo se traduce el diseño y los requisitos del **Módulo 1** a código fuente
> real de la aplicación **Tescha Food** (`com.tescha.food`).

---

## 1. Introducción

### 1.1 Propósito
Establecer los lineamientos, decisiones técnicas y la organización del código que
implementa los Requisitos Funcionales del **Módulo 1 — Gestión de Pedidos y Pagos**.
Este documento sirve como guía para que cualquier integrante del equipo escriba,
ubique y mantenga el código de manera consistente.

### 1.2 Alcance
Cubre **únicamente** los requisitos del Módulo 1 y sus casos de uso asociados:

| RF | Nombre | Caso de uso principal |
|---|---|---|
| RF-01 | Localización y Filtrado | Explorar catálogo / Capturar ubicación GPS |
| RF-02 | Cálculo de Distancia por Ruta | Calcular distancia y tiempo de entrega |
| RF-03 | Filtro por Estado del Local | Excluir productos de locales inactivos/cerrados |
| RF-04 | Ordenamiento por Cercanía | Ordenar productos por cercanía |
| RF-05 | Desglose de Costos | Mostrar desglose (Subtotal/Envío/Total) |
| RF-06 | Cálculo Automatizado de Envío | Calcular tarifa de envío |
| RF-07 | Simulación de Pago con Tarjeta | Realizar pago simulado / Validar tarjeta (Luhn) |
| RF-08 | Transición de Estado Asíncrono | Procesar pago → Cambiar a "Pagado" (4 s) |

### 1.3 Documentos relacionados
- `RF && RNF.md` — Requisitos Funcionales y No Funcionales.
- `casos de uso.md` — Diagramas de casos de uso por RF.
- `DESIGN.md` — Sistema de diseño "Modern Heritage Dark".
- *Pendientes:* "Esquema de pruebas unitarias" y "Esquema de integración"
  (se elaboran en documentos aparte).

---

## 2. Pasos para realizar la codificación

Siguiendo el proceso estándar de codificación, instanciado a la aplicación real.

### 2.1 Elegir el lenguaje de programación
**Lenguaje seleccionado: Kotlin.**

Factores que determinaron la elección:
- **Plataforma de destino:** Android nativo; Kotlin es el lenguaje oficial recomendado por Google.
- **Finalidad del software:** app móvil de delivery con UI declarativa → **Jetpack Compose** (solo Kotlin).
- **Concurrencia:** *Coroutines* nativas, necesarias para RF-08 (temporizador asíncrono) y para las llamadas de red (Supabase, Google Maps) sin bloquear la UI (RNF-08).
- **Seguridad:** *null-safety* del lenguaje reduce errores en datos opcionales (p. ej. `distanceKm: Double?`, `shippingFee: Double?`).
- **Experiencia del equipo:** ecosistema unificado con las librerías Android.

### 2.2 Crear el ambiente de desarrollo
Herramientas y configuración del entorno:

| Elemento | Configuración |
|---|---|
| IDE | Android Studio |
| Sistema de build | Gradle (Kotlin DSL) — se ejecuta con `gradlew.bat` en Windows |
| UI | Compose BOM `2025.12.00` |
| Navegación | Navigation Compose `2.8.8` |
| Backend | Supabase BOM `3.1.4` (Auth, Postgrest, Realtime, Storage) |
| HTTP | Ktor Android `3.1.3` |
| Mapas | Maps Compose `6.4.4` + Play Services Maps `19.2.0` + Location `21.3.0` |
| Imágenes | Coil Compose `2.7.0` |

Claves y secretos se configuran **fuera del código** en `local.properties`
(`MAPS_API_KEY`, `SUPABASE_ANON_KEY`) y se inyectan vía `BuildConfig` y
`manifestPlaceholders`. **Nunca se hardcodean.**

Comandos del entorno:
```bash
gradlew.bat assembleDebug      # compilar
gradlew.bat installDebug       # instalar en dispositivo
gradlew.bat test               # pruebas unitarias
gradlew.bat lint               # análisis estático
```

### 2.3 Crear una estructura de proyecto
Se mantiene un orden por **responsabilidad** dentro del paquete `com.tescha.food`.
Archivos que materializan el **Módulo 1**:

```
com.tescha.food/
  data/
    model/                      — Product · Order · OrderItem · OrderStatus · CartItem/CartSummary
    repository/
      ProductRepository         — interfaz
      OrderRepository           — interfaz
      supabase/
        SupabaseProductRepository — filtra tiendas activas (RF-03)
        SupabaseOrderRepository   — pedidos + Realtime de estado
    remote/
      SupabaseClient.kt         — cliente singleton
      DistanceMatrixService.kt  — distancia vial + caché 5 min (RF-02 / RNF-02)
  di/
    AppContainer.kt             — inyección de dependencias
  ui/
    screens/
      MainScreens.kt            — HomeScreen, feed de productos (RF-01/03/04)
      CartScreen.kt             — desglose de costos (RF-05)
      CheckoutScreen.kt         — formulario de pago (RF-07)
      PaymentResultScreen.kt    — transición 4 s (RF-08)
    viewmodel/
      MainViewModel.kt          — ubicación GPS (RF-01)
      HomeViewModel.kt          — filtrado + orden + Distance Matrix (RF-01/02/04)
      CartViewModel.kt          — carrito + calcShippingFee (RF-05/06)
      CheckoutViewModel.kt      — validación Luhn + simulación 4 s (RF-07/08)
```

### 2.4 Escribir el código
Reglas seguidas al escribir el código del Módulo 1:
- **Seguir la especificación previa:** cada función implementa un RF concreto y se
  marca con un comentario que lo cita (p. ej. `// RF-06: lógica de tarifa de envío`).
- **Patrón MVVM:** la lógica de negocio vive en *ViewModels*; las *Screens* (Compose)
  solo observan estado y emiten eventos.
- **Estado inmutable y observable:** se expone con `StateFlow` (`CartSummary`,
  `PaymentState`, etc.); la UI reacciona sin acoplarse a la lógica.
- **Código legible:** nombres descriptivos (`calcShippingFee`, `setDeliveryDistance`),
  funciones cortas con una responsabilidad.
- **Diseño visual:** se respeta `DESIGN.md` (paleta Modern Heritage Dark, espaciado
  `LocalSpacing`, glassmorphism) para el desglose (RF-05) y el checkout (RF-07).

### 2.5 Probar y depurar el código
- **Pruebas unitarias** de la lógica pura (sin UI): `calcShippingFee` (RF-06),
  algoritmo de Luhn y validación de expiración (RF-07). Se ejecutan con
  `gradlew.bat test`. *(El esquema detallado se entrega en documento aparte.)*
- **Depuración:** uso de **Logcat** para inspeccionar respuestas de Google Distance
  Matrix y Supabase; verificación de estados (`PROCESSING → CONFIRMED`).
- **Casos límite probados manualmente:** distancia > 5 km (bloqueo "Fuera de rango"),
  tarjeta inválida (Luhn falla), fecha de expiración pasada.

### 2.6 Documentar el código
- **Comentarios en español** que enlazan cada bloque con su requisito
  (`// RF-02`, `// RF-08`).
- Documentación externa centralizada en `CLAUDE.md` (arquitectura, flujos, BD) y en
  este documento de especificación.
- DTOs y modelos autoexplicativos en `data/model` y `data/remote/dto`.

### 2.7 Realizar la integración continua
Proceso de verificación continua del código a medida que se desarrolla
(compilación + pruebas + lint en cada cambio). **Su detalle se entrega en el
documento "Esquema de integración"** (pendiente, fuera del alcance de este documento).

---

## 3. Tipos de codificación aplicados

Mapeo de los tipos de codificación a la evidencia real del Módulo 1.

| Tipo | Aplicación en Tescha Food (Módulo 1) |
|---|---|
| **Codificación en capas** | Separación en capas: **Presentación** (`ui/screens`, `ui/viewmodel`) ↔ **Lógica/Datos** (`data/repository`) ↔ **Acceso remoto** (`data/remote`). La UI no accede directamente a la red. |
| **Codificación basada en patrones** | **Repository** (`ProductRepository`, `OrderRepository` + impl Supabase), **Inyección de dependencias** (`di/AppContainer`), **State holder / Observer** (`StateFlow`). |
| **Codificación basada en pruebas** | Lógica de negocio aislada y testeable (`calcShippingFee`, `luhn`, `validExpiry`). El esquema de pruebas se documenta aparte. |
| **Codificación basada en modelos** | El dominio se modela con *data classes*: `Product`, `Order`, `OrderItem`, `CartItem`, `CartSummary`, `OrderStatus`. La funcionalidad se especifica sobre estos modelos. |
| **Codificación colaborativa** | Trabajo de varios desarrolladores sobre el mismo código mediante **control de versiones (Git)**. |
| **Codificación de módulos** | El programa se divide en módulos por área funcional; este documento se concentra en el **Módulo 1**, desarrollado y probado de forma independiente. |

---

## 4. Especificación de codificación por requisito (RF-01 a RF-08)

Para cada requisito: caso de uso asociado, archivo/función responsable y decisiones
clave de codificación.

### RF-01 — Localización y Filtrado
- **Caso de uso:** *Explorar catálogo* `<<include>>` *Capturar ubicación GPS* + *Filtrar negocios por radio*.
- **Código:** `MainViewModel.requestLocation()` captura el GPS (FusedLocationProvider);
  `HomeViewModel` filtra productos por el radio máximo del vendedor.
- **Decisión:** filtrar con `it.distanceKm == null || it.distanceKm <= it.product.maxDeliveryKm`.

### RF-02 — Cálculo de Distancia por Ruta
- **Caso de uso:** *Calcular distancia y tiempo* `<<include>>` *Consultar caché* `<<extend>>` *Consultar API*.
- **Código:** `DistanceMatrixService.getDistance()` consulta la API web de Google
  Distance Matrix y devuelve distancia vial + ETA.
- **Decisión:** **caché en memoria con TTL de 5 min** (clave = par de coordenadas)
  para cumplir RNF-02 y evitar llamadas redundantes.

### RF-03 — Filtro por Estado del Local
- **Caso de uso:** *Listar productos visibles* `<<include>>` *Excluir locales inactivos/cerrados*.
- **Código:** `SupabaseProductRepository` consulta `stores` filtrando `status == "activo"`
  y solo devuelve productos de esas tiendas.
- **Decisión:** el filtro se resuelve en la consulta de datos, no en la UI.

### RF-04 — Ordenamiento por Cercanía
- **Caso de uso:** *Mostrar feed* `<<include>>` *Ordenar por cercanía* `<<include>>` *Calcular distancia Haversine*.
- **Código:** `HomeViewModel` aplica `sortedBy { it.distanceKm ?: Double.MAX_VALUE }`.
- **Decisión:** los productos sin distancia se envían al final para no romper el orden.

### RF-05 — Desglose de Costos
- **Caso de uso:** *Revisar carrito* `<<include>>` *Mostrar desglose* (Subtotal/Envío/Total).
- **Código:** `CartScreen` (composable `CostRow`) renderiza Subtotal → Envío (con km) → Total;
  los valores provienen de `CartViewModel.summary` (`CartSummary`).
- **Decisión:** el desglose se muestra **antes** de habilitar el pago (RNF-05, Material Design).

### RF-06 — Cálculo Automatizado de Envío
- **Caso de uso:** especializaciones de *Calcular tarifa de envío* (base / escalonada / bloqueo).
- **Código:** `CartViewModel.calcShippingFee(km)`:
  - `km <= 2.0` → `$25.00`
  - `km <= 5.0` → `25 + (km − 2) × 10`, redondeado a 2 decimales
  - `> 5.0` → `null` (bloquea el pedido, "Fuera de rango")
- **Decisión:** misma lógica replicada en la función SQL `calc_shipping_fee(km)` para consistencia.

### RF-07 — Simulación de Pago con Tarjeta
- **Caso de uso:** *Realizar pago simulado* `<<include>>` *Capturar datos* + *Validar tarjeta (Luhn)*.
- **Código:** `CheckoutScreen` captura Nombre/Número/Expiración/CVV;
  `CheckoutViewModel` valida con regex (16 dígitos), **algoritmo de Luhn** y fecha de
  expiración futura (RNF-07) antes de permitir el envío.
- **Decisión:** validación 100% **local**; no se realizan transacciones reales.

### RF-08 — Transición de Estado Asíncrono
- **Caso de uso:** *Procesar pago* `<<include>>` *Cambiar estado a "Pagado"* `<<include>>` *Notificar*.
- **Código:** `CheckoutViewModel.pay()` lanza `viewModelScope.launch { ... delay(4_000) ... }`
  cambiando `PaymentState.PROCESSING → CONFIRMED`; `PaymentResultScreen` anima la transición.
- **Decisión:** uso de **Coroutines** (suspensión, no bloqueo) → exactamente 4 s y la
  UI nunca se congela (RNF-08).

---

## 5. Convenciones y estándares de codificación

| Aspecto | Estándar adoptado |
|---|---|
| **Idioma de comentarios** | Español; cada bloque cita su RF (`// RF-06`). |
| **Nombres** | `camelCase` para funciones/variables, `PascalCase` para clases/composables; nombres descriptivos por intención. |
| **Arquitectura** | MVVM + Repository + DI (Clean Architecture por capas). |
| **Estado** | Inmutable y expuesto con `StateFlow`; la UI solo observa. |
| **Asincronía** | Siempre con Coroutines (`viewModelScope`); nunca bloquear el hilo principal. |
| **Datos opcionales** | Tipos *nullable* explícitos (`Double?`) y manejo con Elvis (`?:`). |
| **Secretos** | En `local.properties` → `BuildConfig`; nunca en el código. |
| **Errores** | Mensajes amigables en español; no exponer detalles técnicos al usuario. |
| **Formato monetario** | `String.format("%.2f", …)` para 2 decimales en el desglose. |

---

*Fin del documento — Especificación para la Codificación (Módulo 1).*
