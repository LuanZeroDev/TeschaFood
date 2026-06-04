# 09. Gestión de Datos y Persistencia

*Configuración de la espina dorsal de información de la app.*

- [ ] **Configuración de Firebase Project:** Enlazar la app con Firebase (Firestore, Auth, Storage) y añadir el archivo `google-services.json`.
- [ ] **Estructura de Colecciones:**
    - [ ] `users`: Perfiles, roles (comprador/vendedor) y billetera simulada.
    - [ ] `merchants`: Datos específicos del negocio.
    - [ ] `products`: Inventario con fotos y precios.
    - [ ] `orders`: Seguimiento en tiempo real de pedidos y coordenadas del repartidor.
- [ ] **Reglas de Seguridad:** Configurar Firestore para proteger los datos de los usuarios.
- [ ] **Firebase Storage Setup:** Configurar carpetas para almacenamiento de imágenes de productos y perfiles.
- [ ] **Lógica de Repositorios (Data Layer):** Implementar las clases que interactuarán con Firebase para desacoplar la UI de la base de datos.
