# 03. Pantalla de Inicio (Discovery)

*El corazón de la experiencia de compra.*

- [x] **Layout Pinterest (Staggered Grid):** `LazyVerticalStaggeredGrid` de 2 columnas con alturas variables (180–260dp) por índice, en `MainScreens.kt`.
- [x] **Algoritmo de Filtrado Geográfico:** `HomeViewModel.kt` implementa `haversineKm()` y ordena productos por distancia al usuario cuando hay coordenadas disponibles.
- [x] **Componentes de Producto (Cards):** `ProductCard` con radio 8dp, fondo `SurfaceContainer`, borde superior degradado MetallicGold de 1px para simular reflejo de luz.
- [x] **Buscador y Filtros:** Barra de búsqueda Ghost + chips horizontales scrollables por categoría (Todo, Tacos, Tortas, Pizza, Burgers, Sushi, Farmacia, Bebidas).
