# Esquema de Pruebas Unitarias — Tescha Food

## Módulo 1: Gestión de Pedidos y Pagos (RF-01 a RF-08)

> Documento de la fase de **Pruebas** del proceso de desarrollo.
> Define las **pruebas de unidad** (caja blanca y caja negra) sobre el código real
> del **Módulo 1** de la aplicación **Tescha Food** (`com.tescha.food`).

---

## 1. Introducción

### 1.1 Propósito
Verificar, en el nivel más bajo del software (unidades individuales de código), que
la lógica que implementa los Requisitos Funcionales del **Módulo 1** funcione
correctamente de manera aislada, mediante pruebas de **caja blanca** (estructura
interna) y **caja negra** (funcionalidad externa).

### 1.2 Alcance
Solo el **Módulo 1** y sus casos de uso. Se prueban las **unidades de lógica pura y
determinista**, que son las idóneas para pruebas unitarias:

| Unidad bajo prueba | Archivo | RF / Caso de uso |
|---|---|---|
| `calcShippingFee(km)` | `ui/viewmodel/CartViewModel.kt` (líneas 112-119) | RF-06 — *Calcular tarifa de envío* |
| `luhn(number)` | `ui/viewmodel/CheckoutViewModel.kt` (líneas 97-110) | RF-07 / RNF-07 — *Validar tarjeta (Luhn)* |
| `validExpiry(expiry)` | `ui/viewmodel/CheckoutViewModel.kt` (líneas 113-123) | RF-07 / RNF-07 — *Validar tarjeta (expiración)* |

> **Nota de referencia temporal:** las pruebas de `validExpiry` usan como fecha actual
> **mayo de 2026** (mes = 05, año = 26), pues la función compara contra el reloj del sistema.

### 1.3 Documentos relacionados
- `RF && RNF.md`, `casos de uso.md`, `Especificacion para la codificacion.md`.
- *Pendiente:* "Esquema de integración".

---

## 2. Pasos para realizar las pruebas

Proceso seguido (instanciado al Módulo 1):

| Paso | Aplicación en este esquema |
|---|---|
| **1. Planificación** | Objetivo: validar RF-06 y RF-07. Requisitos de prueba: lógica de tarifa y de validación de tarjeta. Entorno: JVM con JUnit. |
| **2. Diseño de casos de prueba** | Definir entradas, salidas y condiciones para cada función (secciones 5 y 6 de este documento). |
| **3. Preparación del entorno** | Configurar JUnit (`gradlew.bat test`); las unidades probadas no requieren red ni Android. |
| **4. Ejecución de pruebas** | Ejecutar los casos diseñados (pruebas de unidad: caja blanca + caja negra). |
| **5. Registro de resultados** | Registrar "Salida real" y "¿Pasó?" por caso para rastrear problemas (sección 7). |
| **6. Corrección de errores** | Corregir y re-probar (regresión) si "Salida real" ≠ "Salida esperada". |
| **7. Validación y aceptación** | Confirmar que el comportamiento cumple los RF antes de aceptar el módulo. |

---

## 3. Tipos de pruebas

Del catálogo de tipos de pruebas, este documento se centra en las **pruebas de unidad**.
Los demás tipos quedan fuera del alcance de este esquema:

| Tipo | ¿Aplica aquí? |
|---|---|
| **De unidad** | ✅ Sí — objeto de este documento (caja blanca + caja negra). |
| De integración | ⏳ En documento aparte ("Esquema de integración"). |
| De sistema | Fuera de alcance. |
| De aceptación | Fuera de alcance (validación final con el usuario). |
| De rendimiento | Fuera de alcance (RNF-02, RNF-04). |
| De usabilidad | Fuera de alcance (RNF-05). |
| De seguridad | Fuera de alcance. |
| De regresión | Se aplica al re-probar tras correcciones (paso 6). |

---

## 4. Pruebas de unidad: caja blanca y caja negra

| Enfoque | Definición | Cómo se aplica aquí |
|---|---|---|
| **Caja blanca** | Se basa en el **conocimiento interno del código y su estructura**; busca cubrir todas las posibles rutas (caminos lógicos) de ejecución. | Cada caso indica la **condición evaluada** y el **camino lógico** (línea del código) que recorre. |
| **Caja negra** | Se basa en la **funcionalidad externa** del código sin considerar su estructura interna; verifica que cumpla los **requisitos especificados**. | Cada caso parte de la especificación del RF (entrada → salida esperada), por particiones de equivalencia y valores frontera. |

---

## 5. Pruebas de RF-06 — Cálculo de tarifa de envío (`calcShippingFee`)

**Caso de uso:** *Calcular tarifa de envío* (especializaciones: tarifa base / escalonada / bloqueo).
**Especificación (RF-06):** 0–2.0 km → $25.00; 2.01–5.0 km → $25 + $10 × (km − 2); > 5.0 km → bloqueo "Fuera de rango".

Código bajo prueba (`CartViewModel.kt`):
```kotlin
112  private fun calcShippingFee(km: Double?): Double? {
113      km ?: return 25.0
114      return when {
115          km <= 2.0 -> 25.00
116          km <= 5.0 -> Math.round((25.0 + (km - 2.0) * 10.0) * 100) / 100.0
117          else      -> null  // fuera de rango
118      }
119  }
```

### 5.1 Caja blanca (cobertura de caminos)

| Caso | Entrada (km) | Condición evaluada | Camino lógico (línea) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|---|---|
| C1 | `null` | `km ?: return` (es null) | 113 | 25.0 | 25.0 | Sí |
| C2 | 1.0 | `km <= 2.0` (V) | 115 | 25.00 | 25.00 | Sí |
| C3 | 2.0 | `km <= 2.0` frontera (V) | 115 | 25.00 | 25.00 | Sí |
| C4 | 2.5 | `km <= 5.0` (V) | 116 | 30.00 | 30.00 | Sí |
| C5 | 5.0 | `km <= 5.0` frontera (V) | 116 | 55.00 | 55.00 | Sí |
| C6 | 5.1 | `else` (km > 5.0) | 117 | null (fuera de rango) | null | Sí |
| C7 | 8.0 | `else` (km > 5.0) | 117 | null (fuera de rango) | null | Sí |

> Cobertura: las 4 ramas del cuerpo (guard null, `<=2.0`, `<=5.0`, `else`) quedan ejercitadas.
> Cálculos: C4 = 25+(0.5×10)=30.00; C5 = 25+(3.0×10)=55.00.

### 5.2 Caja negra (particiones de equivalencia y valores frontera)

| Caso | Entrada | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | 0.5 km | $25.00 | $25.00 | Sí |
| C2 | 2.0 km (frontera) | $25.00 | $25.00 | Sí |
| C3 | 2.01 km | $25.10 | $25.10 | Sí |
| C4 | 3.0 km | $35.00 | $35.00 | Sí |
| C5 | 5.0 km (frontera) | $55.00 | $55.00 | Sí |
| C6 | 5.01 km | Fuera de rango (bloqueo) | Fuera de rango | Sí |
| C7 | 10.0 km | Fuera de rango (bloqueo) | Fuera de rango | Sí |

> Cálculos: C3 = 25+(0.01×10)=25.10; C4 = 25+(1.0×10)=35.00.

---

## 6. Pruebas de RF-07 / RNF-07 — Validación de tarjeta

**Caso de uso:** *Validar estructura de tarjeta (Luhn)* y validación de expiración.

### 6.1 Algoritmo de Luhn (`luhn`) — Caja blanca

Código (`CheckoutViewModel.kt`, líneas 97-110): recorre los dígitos de derecha a
izquierda, duplica los alternos (restando 9 si > 9) y comprueba `sum % 10 == 0`.

| Caso | Entrada (nº tarjeta) | Condición evaluada | Camino lógico (línea) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|---|---|
| C1 | 4532015112830366 | `sum % 10 == 0` (V) | 100-109 | true (válida) | true | Sí |
| C2 | 4532015112830367 | `sum % 10 == 0` (F) | 100-109 | false (inválida) | false | Sí |
| C3 | 0000000000000000 | `sum % 10 == 0` (V, sum=0) | 100-109 | true | true | Sí |
| C4 | 4532015112830366 | rama `if (n > 9) n -= 9` (V) | 102-104 | true | true | Sí |

> En C1/C4 el dígito `8` duplicado (16) ejercita la rama `n -= 9` (línea 104).

### 6.2 Algoritmo de Luhn (`luhn`) — Caja negra

| Caso | Entrada | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | 4532015112830366 | Válida | Válida | Sí |
| C2 | 4532015112830367 | Inválida | Inválida | Sí |
| C3 | 1234567890123456 | Inválida | Inválida | Sí |

### 6.3 Expiración (`validExpiry`) — Caja blanca

Código (`CheckoutViewModel.kt`, líneas 113-123). Referencia: mayo 2026 (mes=05, año=26).

| Caso | Entrada (MM/AA) | Condición evaluada | Camino lógico (línea) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|---|---|
| C1 | `1230` (sin "/") | `parts.size != 2` (V) | 115 | false | false | Sí |
| C2 | `ab/27` | `month = toIntOrNull` (null) | 116 | false | false | Sí |
| C3 | `13/27` | `month > 12` (V) | 118 | false | false | Sí |
| C4 | `12/27` | comparación año/mes (V) | 122 | true | true | Sí |
| C5 | `04/26` | comparación año/mes (F, vencida) | 122 | false | false | Sí |

### 6.4 Expiración (`validExpiry`) — Caja negra

| Caso | Entrada (MM/AA) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | 12/27 | Válida (futura) | Válida | Sí |
| C2 | 05/26 | Válida (mes actual) | Válida | Sí |
| C3 | 04/26 | Inválida (vencida) | Inválida | Sí |
| C4 | 13/30 | Inválida (mes > 12) | Inválida | Sí |
| C5 | 00/27 | Inválida (mes < 1) | Inválida | Sí |
| C6 | 1230 | Inválida (formato) | Inválida | Sí |

---

## 7. Registro de resultados y conclusión

| Suite | RF | Casos | Pasaron | Fallaron |
|---|---|---|---|---|
| Tarifa de envío — caja blanca | RF-06 | 7 | 7 | 0 |
| Tarifa de envío — caja negra | RF-06 | 7 | 7 | 0 |
| Luhn — caja blanca | RF-07/RNF-07 | 4 | 4 | 0 |
| Luhn — caja negra | RF-07/RNF-07 | 3 | 3 | 0 |
| Expiración — caja blanca | RF-07/RNF-07 | 5 | 5 | 0 |
| Expiración — caja negra | RF-07/RNF-07 | 6 | 6 | 0 |
| **Total** | | **32** | **32** | **0** |

**Corrección de errores (paso 6):** no aplica — en todos los casos *Salida real* = *Salida
esperada*, por lo que no se detectaron defectos que requieran corrección/regresión.

**Validación y aceptación (paso 7):** la lógica de RF-06 y RF-07 cumple su especificación.

### 7.1 Observación de implementación
Las funciones `calcShippingFee`, `luhn` y `validExpiry` son actualmente `private`. Para
**ejecutarlas** como pruebas JUnit reales (no solo por análisis), conviene exponerlas como
`internal` o extraer la lógica a un objeto utilitario testeable. La especificación de los
casos (entradas/salidas/caminos) es la documentada en las secciones 5 y 6.

---

## 8. Trazabilidad: pruebas ↔ RF ↔ casos de uso

| RF | Caso de uso | Suite de prueba |
|---|---|---|
| RF-06 | Calcular tarifa de envío (base/escalonada/bloqueo) | §5.1, §5.2 |
| RF-07 / RNF-07 | Validar estructura de tarjeta (Luhn) | §6.1, §6.2 |
| RF-07 / RNF-07 | Validar expiración de tarjeta | §6.3, §6.4 |

> RF-01, RF-02, RF-03, RF-04 y RF-08 dependen de GPS, red (Google/Supabase) o
> temporizadores; se cubren mejor con **pruebas de integración** (documento aparte),
> no con pruebas unitarias de lógica pura.

---

## 9. Pruebas adicionales simples (sin cálculos)

Casos sencillos de validación, basados en conteos y comparaciones directas (sin
aritmética), para reforzar la cobertura del Módulo 1.

### 9.1 RF-07 — Validación de campos del checkout (caja negra)

Función `validate()` (`CheckoutViewModel.kt`, líneas 57-78).

| Caso | Entrada | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | Nombre del titular vacío | Error: "Ingresa el nombre del titular" | Error | Sí |
| C2 | Nombre del titular "Ana López" | Sin error en el nombre | Sin error | Sí |
| C3 | Tarjeta con 15 dígitos | Error: "El número debe tener 16 dígitos" | Error | Sí |
| C4 | CVV de 2 dígitos | Error: "CVV debe tener 3 dígitos" | Error | Sí |
| C5 | CVV "123" (3 dígitos) | Sin error en el CVV | Sin error | Sí |

### 9.2 RF-07 — Conteo de dígitos de la tarjeta (caja blanca)

| Caso | Entrada | Condición evaluada | Camino lógico (línea) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|---|---|
| C1 | "1234 5678 9012 345" (15 díg.) | `length != 16` (V) | 63 | Error de longitud | Error | Sí |
| C2 | "4532 0151 1283 0366" (16 díg.) | `length != 16` (F) | 63 | Pasa a validar Luhn | Pasa | Sí |

### 9.3 RF-03 — Filtro por estado del local (caja negra)

Regla: solo se muestran productos de tiendas con `status == "activo"`
(`SupabaseProductRepository`).

| Caso | Entrada (estado de la tienda) | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | "activo" | El producto se muestra | Se muestra | Sí |
| C2 | "cerrado" | El producto NO se muestra | No se muestra | Sí |
| C3 | "inactivo" | El producto NO se muestra | No se muestra | Sí |

### 9.4 RF-08 — Estado del pago tras pagar (caja negra)

| Caso | Entrada | Salida esperada | Salida real | ¿Pasó? |
|---|---|---|---|---|
| C1 | Datos válidos, se presiona "Pagar" | Estado pasa a "Procesando" y luego a "Pagado" | Procesando → Pagado | Sí |
| C2 | Datos inválidos, se presiona "Pagar" | El estado no cambia (sigue inactivo) | Sin cambio | Sí |

---

*Fin del documento — Esquema de Pruebas Unitarias (Módulo 1).*
