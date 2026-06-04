# Diagramas de Casos de Uso por Requisito Funcional

Este documento describe cómo debe estructurarse el diagrama de casos de uso para cada uno de los 15 Requisitos Funcionales (RF) del sistema Tescha Food. Para cada RF se especifican: **Actores**, **Casos de uso (óvalos)**, **Relaciones (include / extends / generalización)** y una explicación del flujo.

---

## Convenciones gráficas

- **Actor**: figura tipo "stick man" en los costados del sistema.
- **Caso de uso**: óvalo dentro del rectángulo del sistema.
- **Asociación**: línea simple entre actor y caso de uso.
- **`<<include>>`**: dependencia obligatoria (flecha discontinua con etiqueta) entre dos casos de uso. El caso origen *siempre* invoca al destino.
- **`<<extend>>`**: dependencia opcional (flecha discontinua con etiqueta). El caso destino puede ejecutarse desde el origen bajo cierta condición.
- **Generalización**: flecha de triángulo vacío. Se usa entre actores (Cliente, Vendedor, Repartidor, Administrador heredan de Usuario) o entre casos de uso afines.
- **Frontera del sistema**: rectángulo etiquetado "Sistema Tescha Food" que contiene todos los óvalos.

### Jerarquía de actores (aplica a todos los diagramas)

```
            Usuario (abstracto)
           /     |       |       \
        Cliente Vendedor Repartidor Admin
```

Esta generalización se reutiliza en todos los diagramas donde participe más de un rol.

---

## Módulo 1: Gestión de Pedidos y Pagos

### RF-01 — Localización y Filtrado

**Actores**
- Cliente (primario)
- Servicio de GPS del dispositivo (actor secundario / sistema externo)

**Casos de uso (óvalos)**
- Explorar catálogo
- Capturar ubicación GPS
- Filtrar negocios por radio de entrega
- Mostrar feed de productos

**Relaciones**
- `Explorar catálogo` `<<include>>` `Capturar ubicación GPS`
- `Explorar catálogo` `<<include>>` `Filtrar negocios por radio de entrega`
- `Filtrar negocios por radio de entrega` `<<include>>` `Mostrar feed de productos`

**Explicación**
El Cliente inicia el caso de uso `Explorar catálogo`, el cual obligatoriamente incluye la captura de su ubicación GPS y, a partir de ella, el filtrado por radio activo de cada negocio. El resultado final se muestra como feed. El servicio GPS actúa como actor secundario porque entrega coordenadas al sistema sin recibir respuesta del usuario.

---

### RF-02 — Cálculo de Distancia por Ruta

**Actores**
- Cliente (primario)
- Google Maps Distance Matrix API (sistema externo)

**Casos de uso**
- Calcular distancia y tiempo de entrega
- Consultar API Distance Matrix
- Consultar caché de distancias (5 min)

**Relaciones**
- `Calcular distancia y tiempo de entrega` `<<include>>` `Consultar caché de distancias`
- `Calcular distancia y tiempo de entrega` `<<extend>>` `Consultar API Distance Matrix` (extensión cuando no hay valor en caché)

**Explicación**
Cuando el Cliente abre un producto, el sistema ejecuta `Calcular distancia y tiempo`, que primero consulta la caché. Si no hay un resultado vigente, se extiende al caso de uso que llama a la API externa de Google. La relación es `extend` porque la consulta a la API es condicional (depende del estado de la caché).

---

### RF-03 — Filtro por Estado del Local

**Actores**
- Cliente (primario)
- Vendedor (secundario — origen del cambio de estado)

**Casos de uso**
- Listar productos visibles
- Excluir productos de locales inactivos/cerrados
- Marcar local como Inactivo/Cerrado

**Relaciones**
- `Listar productos visibles` `<<include>>` `Excluir productos de locales inactivos/cerrados`
- Asociación directa entre `Vendedor` y `Marcar local como Inactivo/Cerrado`

**Explicación**
El Cliente nunca interactúa con el cambio de estado del local; sólo lo padece. Por eso `Excluir productos…` se incluye obligatoriamente cuando se lista. El Vendedor participa con un caso de uso independiente que afecta la salida del filtrado.

---

### RF-04 — Ordenamiento por Cercanía

**Actores**
- Cliente (primario)

**Casos de uso**
- Mostrar feed de productos
- Ordenar productos por cercanía
- Calcular distancia Haversine

**Relaciones**
- `Mostrar feed de productos` `<<include>>` `Ordenar productos por cercanía`
- `Ordenar productos por cercanía` `<<include>>` `Calcular distancia Haversine`

**Explicación**
El ordenamiento es un paso obligatorio del flujo de visualización del feed, por lo cual se modela con `include`. La distancia Haversine es un sub-caso de uso técnico siempre invocado por el ordenador.

---

### RF-05 — Desglose de Costos

**Actores**
- Cliente (primario)

**Casos de uso**
- Revisar carrito
- Mostrar desglose de costos (Subtotal / Envío / Total)
- Habilitar opción de pago

**Relaciones**
- `Revisar carrito` `<<include>>` `Mostrar desglose de costos`
- `Habilitar opción de pago` `<<include>>` `Mostrar desglose de costos`

**Explicación**
Cualquier ruta hacia el pago debe pasar por el desglose: tanto desde `Revisar carrito` como antes de `Habilitar opción de pago`. Por eso ambos lo incluyen.

---

### RF-06 — Cálculo Automatizado de Envío

**Actores**
- Cliente (primario)

**Casos de uso**
- Calcular tarifa de envío
- Aplicar tarifa base (0–2 km)
- Aplicar tarifa escalonada (2.01–5 km)
- Bloquear pedido fuera de rango (>5 km)

**Relaciones**
- Generalización: los tres casos `Aplicar tarifa base`, `Aplicar tarifa escalonada`, `Bloquear pedido fuera de rango` son **especializaciones** de `Calcular tarifa de envío`.
- `Revisar carrito` (RF-05) `<<include>>` `Calcular tarifa de envío`

**Explicación**
Aquí se utiliza generalización entre casos de uso para representar que cada rango de distancia es una variante del cálculo. La elección de la variante depende del valor de la distancia y se resuelve internamente.

---

### RF-07 — Módulo de Simulación de Pago con Tarjeta

**Actores**
- Cliente (primario)

**Casos de uso**
- Realizar pago simulado
- Capturar datos de tarjeta (Nombre / Número / Expiración / CVV)
- Validar estructura de tarjeta (Luhn)
- Enviar orden a procesamiento

**Relaciones**
- `Realizar pago simulado` `<<include>>` `Capturar datos de tarjeta`
- `Realizar pago simulado` `<<include>>` `Validar estructura de tarjeta (Luhn)`
- `Validar estructura de tarjeta (Luhn)` `<<extend>>` `Enviar orden a procesamiento` (sólo si la validación es exitosa)

**Explicación**
La captura y la validación son obligatorias en todo intento de pago (include), mientras que la transición a "enviar orden" es una extensión condicional a que pasen las validaciones.

---

### RF-08 — Simulador de Transición de Estado Asíncrono

**Actores**
- Cliente (primario)
- Temporizador del sistema (actor secundario / sistema)

**Casos de uso**
- Procesar pago (estado "Procesando Pago")
- Cambiar estado a "Pagado" (a los 4 s)
- Notificar al cliente

**Relaciones**
- `Procesar pago` `<<include>>` `Cambiar estado a "Pagado"`
- `Cambiar estado a "Pagado"` `<<include>>` `Notificar al cliente`

**Explicación**
La transición ocurre siempre dentro del flujo de pago; por eso es `include`. El temporizador aparece como actor secundario porque dispara el evento sin intervención humana.

---

## Módulo 2: Rastreo y Ubicación

### RF-09 — Registro de Coordenadas del Local

**Actores**
- Vendedor (primario)
- Servicio de GPS del dispositivo (secundario)

**Casos de uso**
- Configurar punto de venta
- Seleccionar coordenadas con GPS
- Seleccionar coordenadas con mapa interactivo
- Guardar coordenadas del local

**Relaciones**
- Generalización: `Seleccionar coordenadas con GPS` y `Seleccionar coordenadas con mapa interactivo` son **especializaciones** de un caso abstracto `Seleccionar coordenadas`.
- `Configurar punto de venta` `<<include>>` `Seleccionar coordenadas`
- `Configurar punto de venta` `<<include>>` `Guardar coordenadas del local`

**Explicación**
La generalización modela que el Vendedor dispone de dos formas alternativas (e intercambiables) de fijar las coordenadas. Ambas terminan en el mismo guardado.

---

### RF-10 — Captura GPS del Repartidor

**Actores**
- Repartidor (primario)
- Servicio de GPS (secundario)
- Sistema (temporizador interno)

**Casos de uso**
- Atender pedido en camino
- Capturar coordenadas GPS intermitentes
- Reportar coordenadas al servidor

**Relaciones**
- `Atender pedido en camino` `<<include>>` `Capturar coordenadas GPS intermitentes`
- `Capturar coordenadas GPS intermitentes` `<<include>>` `Reportar coordenadas al servidor`
- Precondición: el pedido debe estar en estado "En camino" (anotada en el diagrama como nota UML, no como relación).

**Explicación**
La captura está condicionada al estado del pedido. Modelamos la dependencia entre captura y reporte como `include` porque cada captura se sigue de un envío al backend.

---

### RF-11 — Mapa de Seguimiento en Vivo

**Actores**
- Cliente (primario)
- Google Maps API (sistema externo)

**Casos de uso**
- Ver seguimiento del pedido
- Renderizar mapa interactivo
- Recibir coordenadas del repartidor (cada 5 s)

**Relaciones**
- `Ver seguimiento del pedido` `<<include>>` `Renderizar mapa interactivo`
- `Ver seguimiento del pedido` `<<include>>` `Recibir coordenadas del repartidor`

**Explicación**
El Cliente activa el seguimiento; el mapa y la recepción de coordenadas son sub-procesos invariablemente requeridos. La API de Google Maps aparece como actor secundario para enfatizar la dependencia externa.

---

## Módulo 3: Gestión de Usuarios y Roles

### RF-12 — Registro de Clientes

**Actores**
- Visitante (primario)

**Casos de uso**
- Registrar cuenta Cliente
- Capturar datos personales (nombre, correo, contraseña)
- Validar política de contraseña
- Crear perfil en base de datos

**Relaciones**
- `Registrar cuenta Cliente` `<<include>>` `Capturar datos personales`
- `Registrar cuenta Cliente` `<<include>>` `Validar política de contraseña`
- `Validar política de contraseña` `<<extend>>` `Crear perfil en base de datos` (sólo si la validación es exitosa)

**Explicación**
El registro siempre incluye la captura y la validación de seguridad. La creación efectiva del perfil es una extensión que ocurre sólo si las validaciones pasan.

---

### RF-13 — Solicitud de Registro de Vendedores

**Actores**
- Cliente (primario, ya autenticado)
- Almacenamiento local del dispositivo (secundario)

**Casos de uso**
- Solicitar alta de Vendedor
- Adjuntar documentos probatorios
- Validar formato y tamaño del documento
- Enviar solicitud al Administrador

**Relaciones**
- `Solicitar alta de Vendedor` `<<include>>` `Adjuntar documentos probatorios`
- `Adjuntar documentos probatorios` `<<include>>` `Validar formato y tamaño del documento`
- `Solicitar alta de Vendedor` `<<extend>>` `Enviar solicitud al Administrador` (extensión condicional al éxito de la validación)

**Explicación**
La carga del documento y su validación local son obligatorias; el envío sólo ocurre si los archivos cumplen restricciones (PDF/JPEG/PNG ≤ 5 MB).

---

### RF-14 — Panel de Validación de Cuentas

**Actores**
- Administrador (primario)

**Casos de uso**
- Revisar solicitudes pendientes
- Aprobar solicitud
- Rechazar solicitud
- Notificar resultado al solicitante

**Relaciones**
- Generalización: `Aprobar solicitud` y `Rechazar solicitud` son **especializaciones** de un caso abstracto `Resolver solicitud`.
- `Resolver solicitud` `<<include>>` `Notificar resultado al solicitante`
- `Revisar solicitudes pendientes` `<<extend>>` `Resolver solicitud` (extensión cuando el Admin decide actuar)

**Explicación**
Aprobar y rechazar comparten el mismo flujo de salida (notificación), pero modifican distintos datos. Se modelan como generalización para evitar duplicar el include de la notificación.

---

### RF-15 — Autonomía de Reglas del Local

**Actores**
- Vendedor (primario, aprobado)

**Casos de uso**
- Configurar reglas del local
- Activar/desactivar local
- Fijar horarios de atención
- Configurar radio máximo de reparto

**Relaciones**
- Los tres casos (`Activar/desactivar local`, `Fijar horarios`, `Configurar radio`) son **especializaciones** (generalización) de `Configurar reglas del local`.
- Cada uno se invoca de forma independiente desde el panel del Vendedor.

**Explicación**
La generalización refleja que las tres opciones son variantes equivalentes de la configuración autónoma del local. El Vendedor puede ejecutar cualquiera de ellas sin orden obligatorio.

---

## Resumen de relaciones por tipo

| Tipo | Aparece en RF |
|---|---|
| `<<include>>` | RF-01, RF-02, RF-03, RF-04, RF-05, RF-07, RF-08, RF-09, RF-10, RF-11, RF-12, RF-13, RF-14 |
| `<<extend>>` | RF-02, RF-07, RF-12, RF-13, RF-14 |
| Generalización entre casos de uso | RF-06, RF-09, RF-14, RF-15 |
| Generalización entre actores | Aplicable de forma transversal (Usuario → Cliente / Vendedor / Repartidor / Admin) |

---

## Recomendaciones para construir cada diagrama

1. Dibujar primero la **frontera del sistema** con el rectángulo y el nombre "Sistema Tescha Food".
2. Colocar los **actores primarios** a la izquierda y los **secundarios / sistemas externos** a la derecha.
3. Introducir los **óvalos** dentro del rectángulo, agrupados por flujo lógico (entrada → procesamiento → salida).
4. Trazar primero las **asociaciones simples** y, después, las dependencias `<<include>>` y `<<extend>>` con líneas discontinuas etiquetadas.
5. Reservar la **generalización** (triángulo vacío) para casos donde existen variantes mutuamente excluyentes o jerarquías claras.
6. Añadir **notas UML** para precondiciones críticas (p. ej. "pedido en estado En camino" en RF-10).
