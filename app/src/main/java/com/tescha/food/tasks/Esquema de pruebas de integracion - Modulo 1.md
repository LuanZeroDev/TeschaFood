# Esquema de Pruebas de Integración — Tescha Food

## Módulo 1: Gestión de Pedidos y Pagos

### Caso de uso elegido: **Realizar pago simulado y confirmación del pedido**

> Documento de pruebas de **integración** del proceso de desarrollo.
> Aplica la metodología y el formato de `Documentacion de pruebas de integracion.md`
> a un caso de uso real del **Módulo 1** de **Tescha Food** (`com.tescha.food`).

---

## 1. Introducción

### 1.1 Propósito
Verificar que las unidades del Módulo 1 funcionen correctamente **cuando se integran
entre sí** (no de forma aislada), detectando problemas en las interfaces entre los
componentes que participan en el flujo de compra: revisar carrito → calcular envío →
validar tarjeta → procesar pago → confirmar pedido.

### 1.2 Caso de uso bajo prueba
**Realizar pago simulado** (RF-07), que incluye *Capturar datos de tarjeta* y *Validar
estructura (Luhn)*, se apoya en *Mostrar desglose de costos* (RF-05) y *Calcular tarifa
de envío* (RF-06), y se extiende a la *Transición de estado asíncrono* (RF-08).

### 1.3 Unidades integradas (interfaces a probar)

| Unidad | Archivo | Rol en el flujo |
|---|---|---|
| `CartViewModel` | `ui/viewmodel/CartViewModel.kt` | Calcula subtotal, envío (RF-06) y total → `CartSummary` (RF-05) |
| `CheckoutViewModel` | `ui/viewmodel/CheckoutViewModel.kt` | Valida tarjeta (RF-07) y dispara el pago asíncrono (RF-08) |
| `OrderRepository` | `data/repository/OrderRepository.kt` (+ impl. Supabase / stub) | Persiste el pedido y su estado |
| `PaymentResultScreen` | `ui/screens/PaymentResultScreen.kt` | Refleja la transición Procesando → Pagado |

---

## 2. Estrategia de integración

Se utiliza **Integración Descendente (Top-Down)**: se prueban primero los módulos de
mayor nivel (las pantallas y *ViewModels* del flujo de pago) y se desciende hacia el
módulo de persistencia.

- **Justificación:** la app ya está construida en capas (UI → ViewModel → Repository);
  se integra el flujo de pago de arriba hacia abajo.
- **Cabos (stubs):** mientras se valida la capa superior, el `OrderRepository` real
  (Supabase) se sustituye por su **implementación stub en memoria**
  (`data/repository/stub/`), que devuelve valores fijos sin depender de la red.
- Esto permite probar la lógica de carrito, validación y transición de estado sin
  llamadas reales a la base de datos.

---

## 3. Casos de prueba de integración

| Caso | Acción | Resultado esperado | Resultado obtenido | ¿Pasó? |
| :--- | :--- | :--- | :--- | :--- |
| **C1** | Agregar 2 productos al carrito con distancia de entrega = 1.5 km y abrir el carrito. | Se muestra el desglose: Subtotal, Envío $25.00 y Total = Subtotal + Envío. Botón "Pagar" habilitado. | Desglose correcto; botón habilitado. | ✔️ |
| **C2** | Agregar un producto con distancia de entrega = 6 km. | El carrito indica "Fuera de rango" y bloquea el pago (botón deshabilitado). | Muestra "Fuera de rango" y bloquea el pago. | ✔️ |
| **C3** | En checkout, capturar tarjeta `4532 0151 1283 0367` (Luhn inválido), datos restantes válidos, y presionar "Pagar". | Mensaje "Número de tarjeta inválido". El estado del pago no cambia. | Mensaje de error mostrado; sin cambio de estado. | ✔️ |
| **C4** | Capturar datos válidos (titular, `4532 0151 1283 0366`, expiración futura, CVV 123) y presionar "Pagar". | El estado pasa a "Procesando" e, **a los 4 s**, cambia a "Pagado"; se muestra el comprobante. La UI no se congela. | Procesando → (4 s) → Pagado; UI fluida. | ✔️ |
| **C5** | Tras el pago confirmado (C4), abrir el historial de pedidos / consultar la tabla `orders`. | El pedido aparece persistido con estado "pagado". | El cambio de estado ocurre solo en memoria; **el pedido no se persiste** en `orders`. | ❌ |

---

## 4. Registro de resultados

| Caso | RF involucrados | ¿Pasó? |
|---|---|---|
| C1 | RF-05, RF-06 | ✔️ |
| C2 | RF-06 | ✔️ |
| C3 | RF-07 / RNF-07 | ✔️ |
| C4 | RF-08 / RNF-08 | ✔️ |
| C5 | RF-08 (persistencia) | ❌ |

**Resumen:** 4 de 5 casos pasaron. El flujo de cálculo, validación y transición de
estado en memoria funciona correctamente; la integración con la capa de persistencia
falla.

---

## 5. Defecto detectado (clasificación)

El fallo de **C5** corresponde a un **Defecto de Integridad / Interfaz** según la
clasificación de `Documentacion de pruebas de integracion.md`:

| Aspecto | Detalle |
|---|---|
| **Tipo de defecto** | Carencia de propagación del estado a la unidad de persistencia (interfaz entre `CheckoutViewModel` y `OrderRepository`). |
| **Descripción** | `CheckoutViewModel.pay()` cambia `PaymentState` a `CONFIRMED` en memoria (StateFlow), pero **no invoca** a `OrderRepository` para actualizar el estado del pedido a "pagado" en la base de datos. |
| **Causa posible** | No se definió la responsabilidad de persistir el resultado del pago al integrar la capa de pago con la de pedidos. |
| **Corrección sugerida** | Tras `delay(4_000)`, llamar a `orderRepository.updateOrderStatus(orderId, "pagado")` antes de marcar `CONFIRMED`, y volver a ejecutar C5 (prueba de regresión). |

---

## 6. Trazabilidad

| Caso de uso (Módulo 1) | RF | Casos de integración |
|---|---|---|
| Mostrar desglose de costos | RF-05 | C1 |
| Calcular tarifa de envío | RF-06 | C1, C2 |
| Validar estructura de tarjeta (Luhn) | RF-07 / RNF-07 | C3 |
| Transición de estado asíncrono | RF-08 / RNF-08 | C4, C5 |

---

*Fin del documento — Esquema de Pruebas de Integración (Módulo 1).*
