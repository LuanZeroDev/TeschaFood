# Requisitos Funcionales (RF)

## Módulo 1: Gestión de Pedidos y Pagos
* **RF-01 (Localización y Filtrado)**: El sistema debe capturar la ubicación GPS del Cliente y filtrar el catálogo de productos para mostrar únicamente los negocios que se encuentren dentro del radio geográfico de entrega activo.
* **RF-02 (Cálculo de Distancia por Ruta)**: El sistema debe consultar en tiempo real la API de Google Maps (Distance Matrix) para obtener la distancia exacta de traslado vial y el tiempo estimado de llegada entre la ubicación del Punto de Venta (PDV) y la dirección de entrega del Cliente.
* **RF-03 (Filtro por Estado del Local)**: El sistema debe excluir del feed del Cliente los productos de aquellos vendedores cuyo estado comercial figure como "Inactivo" o "Cerrado".
* **RF-04 (Ordenamiento por Cercanía)**: El sistema debe ordenar de forma predeterminada los productos del catálogo dando prioridad de aparición a la menor distancia respecto al Cliente.
* **RF-05 (Desglose de Costos)**: El sistema debe generar y desplegar un desglose explícito del costo total (Subtotal de productos, Tarifa de envío calculada y Gran Total) antes de habilitar la opción de pago.
* **RF-06 (Cálculo Automatizado de Envío)**: El sistema debe calcular la tarifa de envío con base en los siguientes rangos de distancia con redondeo estándar a dos decimales:
    * De 0 a 2.0 km: Tarifa fija base ($25.00 MXN).
    * De 2.01 a 5.0 km: Tarifa base + $10.00 MXN por kilómetro adicional.
    * Más de 5.0 km: Bloquear el pedido y notificar "Fuera de rango".
* **RF-07 (Módulo de Simulación de Pago con Tarjeta)**: El sistema debe proporcionar una pantalla de checkout simulada donde el Cliente pueda ingresar datos de prueba ficticios (Nombre, número de tarjeta simulado, fecha de expiración y CVV) para validar el flujo completo de compra sin realizar transacciones monetarias reales.
* **RF-08 (Simulador de Transición de Estado Asíncrono)**: El sistema debe implementar un servicio automatizado en el backend (hilo secundario o temporizador) que emule la respuesta asíncrona de un banco, cambiando automáticamente el estatus del pedido de "Procesando Pago" a "Pagado" transcurridos exactamente 4 segundos después de presionar el botón de pagar.

## Módulo 2: Rastreo y Ubicación
* **RF-09 (Registro de Coordenadas del Local)**: El sistema debe permitir al actor Vendedor fijar y almacenar las coordenadas geográficas (latitud y longitud) de su Punto de Venta utilizando el GPS del dispositivo o un selector sobre un mapa interactivo.
* **RF-10 (Captura GPS del Repartidor)**: El sistema debe capturar las coordenadas de geolocalización del Repartidor de manera automática e intermitente únicamente cuando el pedido asignado se encuentre en estado "En camino".
* **RF-11 (Mapa de Seguimiento en Vivo)**: El sistema debe renderizar en la aplicación del Cliente un mapa interactivo (mediante la API de Google Maps) que muestre la posición actual del Repartidor en movimiento hacia el destino final.

## Módulo 3: Gestión de Usuarios y Roles
* **RF-12 (Registro de Clientes)**: El sistema debe permitir a los visitantes registrar una cuenta propia con el rol de Cliente ingresando nombre completo, correo electrónico institucional y una contraseña de acceso.
* **RF-13 (Solicitud de Registro de Vendedores)**: El sistema debe permitir a los usuarios enviar una solicitud de alta para el rol de Vendedor, requiriendo la carga digital de documentos probatorios desde el almacenamiento local.
* **RF-14 (Panel de Validación de Cuentas)**: El sistema debe proporcionar al Administrador un panel interactivo para revisar las solicitudes de vendedores pendientes y cambiar su estatus a "Aprobado" o "Rechazado".
* **RF-15 (Autonomía de Reglas del Local)**: El sistema debe permitir a los Vendedores aprobados modificar sus parámetros individuales: activar/desactivar el local, fijar horarios de atención y configurar su radio máximo de reparto.

---

# Requisitos No Funcionales (RNF)

## Módulo 1: Gestión de Pedidos y Pagos
* **RNF-01 (Precisión Geográfica - para RF-01)**: El sistema debe garantizar que el margen de error en la captura de la ubicación GPS para el filtrado inicial de los locales no exceda los 15 metros de radio.
* **RNF-02 (Latencia de API Externa - para RF-02)**: El tiempo total de procesamiento y respuesta de la consulta a la API externa de Google Maps no debe superar los 1.5 segundos por petición, debiendo implementar una estrategia de caché local para coordenadas idénticas solicitadas en un lapso menor a 5 minutos (evitando llamadas redundantes y costos excesivos de API).
* **RNF-03 (Consistencia de Datos en Tiempo Real - para RF-03)**: El cambio de estado comercial de un local ("Cerrado" / "Inactivo") hecho por un vendedor debe propagarse y actualizar el catálogo de todos los clientes concurrentes en menos de 3.0 segundos.
* **RNF-04 (Rendimiento de Renderizado - para RF-04)**: El ordenamiento del feed de productos por cercanía debe completarse y mostrarse en el dispositivo móvil en menos de 500 milisegundos tras recibir los datos del servidor.
* **RNF-05 (Usabilidad y Adaptabilidad - para RF-05)**: La interfaz del desglose de costos debe ajustarse estrictamente a las guías de Material Design, garantizando que los montos económicos nunca sufran desbordamientos de texto u ocultamientos en pantallas de 5 a 7 pulgadas.
* **RNF-06 (Disponibilidad de Lógica - para RF-06)**: El módulo automatizado de tarifas de envío debe mantener una disponibilidad operativa del 99.5% durante el horario de servicio establecido por la universidad.
* **RNF-07 (Validación de Estructura Local - para RF-07)**: El módulo de checkout simulado debe validar localmente (mediante expresiones regulares y el algoritmo matemático de Luhn) que el número de tarjeta ingresado contenga una estructura válida de 16 dígitos y una fecha de expiración futura, antes de permitir enviar los datos.
* **RNF-08 (Concurrencia y No Bloqueo de UI - para RF-08)**: El servicio temporizado que simula la respuesta bancaria debe ejecutarse de forma asíncrona (mediante Coroutines en Kotlin para Android / hilos en el Backend), garantizando que la interfaz gráfica de la aplicación nunca se congele o bloquee mientras el usuario espera la confirmación.

## Módulo 2: Rastreo y Ubicación
* **RNF-09 (Usabilidad del Mapa - para RF-09)**: La interfaz del mapa interactivo para el registro de coordenadas del Punto de Venta debe mantener una tasa de refresco mínima de 30 fotogramas por segundo (FPS) al arrastrar el marcador de ubicación.
* **RNF-10 (Eficiencia Energética - para RF-10)**: La captura del GPS en segundo plano para el rol de Repartidor debe estar optimizada para no incrementar el consumo de la batería del dispositivo móvil en más de un 5% por hora de actividad continua.
* **RNF-11 (Frecuencia de Sincronización Remota - para RF-11)**: El mapa de seguimiento en vivo en el dispositivo del Cliente debe sincronizarse y actualizar las coordenadas del Repartidor de manera exacta cada 5 segundos a través de WebSockets o llamadas HTTP asíncronas optimizadas.

## Módulo 3: Gestión de Usuarios y Roles
* **RNF-12 (Complejidad de Seguridad - para RF-12)**: El sistema de registro de Clientes debe validar obligatoriamente que las contraseñas cumplan con políticas de seguridad estrictas: mínimo 8 caracteres, al menos una letra mayúscula, un número y un carácter especial.
* **RNF-13 (Restricciones de Almacenamiento - para RF-13)**: El módulo de carga digital de documentos probatorios de los Vendedores debe restringir el tamaño de los archivos a un máximo de 5 Megabytes (MB) por documento y limitar los formatos aceptados únicamente a PDF, JPEG y PNG.
* **RNF-14 (Capacidad de Concurrencia de Administración - para RF-14)**: El panel de validación de cuentas del Administrador debe ser capaz de procesar y listar hasta 50 solicitudes simultáneas sin que el tiempo de respuesta del servidor supere los 2.0 segundos de carga en pantalla.
* **RNF-15 (Mantenibilidad del Código - para RF-15)**: El módulo de configuración y autonomía de reglas del Vendedor debe estructurarse bajo los principios de Clean Architecture, garantizando que cualquier cambio futuro en las políticas de los locales pueda modificarse en el código sin alterar la lógica del backend central de pedidos.