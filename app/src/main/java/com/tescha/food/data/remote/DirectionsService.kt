package com.tescha.food.data.remote

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.tescha.food.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL

object DirectionsService {

    private val cache = mutableMapOf<String, List<LatLng>>()

    /**
     * Devuelve la lista de puntos LatLng que componen la ruta por carretera
     * desde [origin] hasta [destination]. Si la API falla, devuelve null.
     */
    suspend fun getRoute(origin: LatLng, destination: LatLng): List<LatLng>? =
        withContext(Dispatchers.IO) {
            val key = "${origin.latitude},${origin.longitude}|${destination.latitude},${destination.longitude}"
            cache[key]?.let { return@withContext it }

            return@withContext try {
                val url = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving" +
                    "&key=${BuildConfig.MAPS_API_KEY}"

                val response = URL(url).readText()
                val json = Json.parseToJsonElement(response).jsonObject
                val status = json["status"]?.jsonPrimitive?.content
                if (status != "OK") {
                    val errMsg = json["error_message"]?.jsonPrimitive?.content
                    Log.e("DirectionsService",
                        "Directions API status=$status error_message=$errMsg. " +
                        "Asegúrate de que la API 'Directions API' esté habilitada en " +
                        "Google Cloud y que la clave NO tenga restricción de aplicación " +
                        "Android (las APIs web requieren clave sin restricción o con " +
                        "restricción 'Ninguna' / 'Referrers HTTP').")
                    return@withContext null
                }

                val encoded = json["routes"]?.jsonArray?.getOrNull(0)?.jsonObject
                    ?.get("overview_polyline")?.jsonObject
                    ?.get("points")?.jsonPrimitive?.content
                    ?: return@withContext null

                val decoded = decodePolyline(encoded)
                Log.d("DirectionsService", "Ruta obtenida con ${decoded.size} puntos")
                cache[key] = decoded
                decoded
            } catch (e: Exception) {
                Log.e("DirectionsService", "Fallo al consultar Directions API", e)
                null
            }
        }

    // Algoritmo estándar de Google para decodificar polylines codificadas.
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }
}
