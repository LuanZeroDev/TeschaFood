package com.tescha.food.data.remote

import com.tescha.food.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.URL

data class DistanceResult(val distanceKm: Double, val durationMinutes: Int)

// RNF-02: caché local de 5 minutos para coordenadas idénticas
private val cache = mutableMapOf<String, Pair<DistanceResult, Long>>()
private const val CACHE_TTL_MS = 5 * 60 * 1000L

object DistanceMatrixService {

    // RF-02: consulta la API de Google Maps Distance Matrix en tiempo real
    suspend fun getDistance(
        originLat: Double, originLng: Double,
        destLat: Double,   destLng: Double,
    ): DistanceResult? = withContext(Dispatchers.IO) {
        val key = "$originLat,$originLng|$destLat,$destLng"
        val now = System.currentTimeMillis()

        // RNF-02: devolver del caché si la solicitud es idéntica y tiene < 5 min
        cache[key]?.let { (result, ts) ->
            if (now - ts < CACHE_TTL_MS) return@withContext result
        }

        return@withContext try {
            val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                "?origins=$originLat,$originLng" +
                "&destinations=$destLat,$destLng" +
                "&key=${BuildConfig.MAPS_API_KEY}"

            val response = URL(url).readText()
            val json = Json.parseToJsonElement(response).jsonObject

            val status = json["status"]?.jsonPrimitive?.content
            if (status != "OK") return@withContext null

            val element = json["rows"]?.jsonArray
                ?.getOrNull(0)?.jsonObject
                ?.get("elements")?.jsonArray
                ?.getOrNull(0)?.jsonObject

            val elemStatus = element?.get("status")?.jsonPrimitive?.content
            if (elemStatus != "OK") return@withContext null

            val distanceM  = element["distance"]?.jsonObject?.get("value")?.jsonPrimitive?.long ?: return@withContext null
            val durationS  = element["duration"]?.jsonObject?.get("value")?.jsonPrimitive?.long ?: return@withContext null

            val result = DistanceResult(
                distanceKm = distanceM / 1000.0,
                durationMinutes = (durationS / 60).toInt(),
            )
            cache[key] = result to now
            result
        } catch (_: Exception) {
            null
        }
    }
}
