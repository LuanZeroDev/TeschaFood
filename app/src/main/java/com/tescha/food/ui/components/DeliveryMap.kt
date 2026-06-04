package com.tescha.food.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.tescha.food.R

val TESCHA_DEST = LatLng(19.234477, -98.840558)
private val GOLD_ARGB = android.graphics.Color.parseColor("#D4AF37")

@Composable
fun DeliveryMap(
    modifier: Modifier = Modifier,
    origin: LatLng? = null,
    destination: LatLng? = null,
    repartidorLocation: LatLng? = null,
    ghostVisited: List<LatLng>? = null,
    ghostRemaining: List<LatLng>? = null,
    initialPosition: LatLng = TESCHA_DEST,
    zoom: Float = 15f,
    liteMode: Boolean = false,
) {
    val context = LocalContext.current
    val focus = repartidorLocation
        ?: ghostVisited?.lastOrNull()
        ?: origin
        ?: destination
        ?: initialPosition

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(focus, zoom)
    }

    // Construye bounds incluyendo todos los puntos relevantes (origen, destino,
    // repartidor y la ruta) para que la cámara siempre encuadre toda la ruta.
    val allPoints: List<LatLng> = buildList {
        origin?.let { add(it) }
        (destination ?: TESCHA_DEST).let { add(it) }
        repartidorLocation?.let { add(it) }
        ghostVisited?.let { addAll(it) }
        ghostRemaining?.let { addAll(it) }
    }

    // Encuadrar la ruta completa la primera vez que tengamos suficientes puntos.
    var didInitialFit by remember { mutableStateOf(false) }
    LaunchedEffect(allPoints.size >= 2) {
        if (allPoints.size >= 2 && !didInitialFit) {
            val builder = LatLngBounds.builder()
            allPoints.forEach { builder.include(it) }
            runCatching {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 120)
                )
            }
            didInitialFit = true
        }
    }

    // Animar suavemente la cámara para seguir al repartidor (sin perder el encuadre).
    LaunchedEffect(repartidorLocation, ghostVisited?.lastOrNull()) {
        if (liteMode) return@LaunchedEffect
        if (allPoints.size >= 2) {
            val builder = LatLngBounds.builder()
            allPoints.forEach { builder.include(it) }
            runCatching {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 120),
                    durationMs = 800,
                )
            }
        }
    }

    val repartidorIcon: BitmapDescriptor? = remember {
        runCatching { makeCircularMarker(context, 90) }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(liteMode) },
        properties = MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = false),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            scrollGesturesEnabled = !liteMode,
            zoomGesturesEnabled = !liteMode,
            tiltGesturesEnabled = !liteMode,
            rotationGesturesEnabled = !liteMode,
        ),
    ) {
        origin?.let {
            Marker(state = MarkerState(position = it), title = "Restaurante")
        }

        val dest = destination ?: TESCHA_DEST
        Marker(
            state = MarkerState(position = dest),
            title = "TESCHA — Punto de entrega",
            snippet = "Carretera Federal México Cuautla s/n",
        )

        val visited = ghostVisited
        if (visited != null && visited.size >= 2) {
            Polyline(
                points = visited,
                color = Color(0xFFD4AF37),
                width = 12f,
                geodesic = false,
            )
        }

        val remaining = ghostRemaining
        if (remaining != null && remaining.size >= 2) {
            Polyline(
                points = remaining,
                color = Color(0x88888888),
                width = 8f,
                geodesic = false,
            )
        }

        val repartidorPos = repartidorLocation ?: ghostVisited?.lastOrNull()
        repartidorPos?.let { pos ->
            if (repartidorIcon != null) {
                Marker(
                    state = MarkerState(position = pos),
                    title = "Repartidor en camino",
                    snippet = "Tescha Food",
                    icon = repartidorIcon,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                )
            } else {
                Marker(
                    state = MarkerState(position = pos),
                    title = "Repartidor en camino",
                )
            }
        }
    }
}

private fun makeCircularMarker(context: Context, sizePx: Int): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, R.drawable.logo_repartidor)
        ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)

    // Bitmap original a tamaño natural para preservar proporción al recortar
    val srcW = drawable.intrinsicWidth.takeIf { it > 0 } ?: sizePx
    val srcH = drawable.intrinsicHeight.takeIf { it > 0 } ?: sizePx
    val src = drawable.toBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)

    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val radius = sizePx / 2f
    val border = sizePx * 0.06f

    // 1. Fondo blanco interior (para que imágenes con transparencia se vean limpias)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(radius, radius, radius - border, bg)

    // 2. Imagen ajustada FIT_CENTER dentro del círculo interior (sin deformar)
    val innerDiameter = (sizePx - 2 * border).toInt()
    val ratio = srcW.toFloat() / srcH.toFloat()
    val (drawW, drawH) = if (ratio >= 1f) {
        innerDiameter to (innerDiameter / ratio).toInt()
    } else {
        (innerDiameter * ratio).toInt() to innerDiameter
    }
    val left = ((sizePx - drawW) / 2f).toInt()
    val top = ((sizePx - drawH) / 2f).toInt()

    canvas.save()
    val clipPath = android.graphics.Path().apply {
        addCircle(radius, radius, radius - border, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawBitmap(src, null, Rect(left, top, left + drawW, top + drawH), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.restore()

    // 3. Borde dorado por encima
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GOLD_ARGB
        style = Paint.Style.STROKE
        strokeWidth = border
    }
    canvas.drawCircle(radius, radius, radius - border / 2f, borderPaint)

    return BitmapDescriptorFactory.fromBitmap(output)
}
