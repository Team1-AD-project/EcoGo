package com.ecogo.app.service

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Google Directions API 服务
 *
 * 用于获取两点之间的真实道路路线
 */
object DirectionsService {

    private const val TAG = "DirectionsService"
    private const val DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json"

    private var apiKey: String? = null

    /**
     * 初始化 API Key
     */
    fun init(context: Context) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")
            Log.d(TAG, "Directions API initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get API key: ${e.message}")
        }
    }

    /**
     * 获取两点之间的路线
     *
     * @param origin 起点
     * @param destination 终点
     * @param mode 交通方式: driving, walking, bicycling, transit
     * @return 路线点列表，失败返回 null
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: String = "walking"
    ): DirectionsResult? = withContext(Dispatchers.IO) {
        val key = apiKey
        if (key.isNullOrEmpty()) {
            Log.e(TAG, "API key not initialized")
            return@withContext null
        }

        try {
            val url = buildUrl(origin, destination, mode, key)
            Log.d(TAG, "Requesting directions: $url")

            val response = URL(url).readText()
            parseDirectionsResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get directions: ${e.message}")
            null
        }
    }

    /**
     * 构建 API 请求 URL
     */
    private fun buildUrl(
        origin: LatLng,
        destination: LatLng,
        mode: String,
        apiKey: String
    ): String {
        return "$DIRECTIONS_API_URL?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=$mode" +
                "&key=$apiKey"
    }

    /**
     * 解析 Directions API 响应
     */
    private fun parseDirectionsResponse(response: String): DirectionsResult? {
        try {
            val json = JSONObject(response)
            val status = json.getString("status")

            if (status != "OK") {
                Log.e(TAG, "Directions API error: $status")
                return null
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.e(TAG, "No routes found")
                return null
            }

            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)

            // 获取总距离和时间
            val distance = leg.getJSONObject("distance").getInt("value") // 米
            val duration = leg.getJSONObject("duration").getInt("value") // 秒

            // 解码路线点（polyline 编码）
            val overviewPolyline = route.getJSONObject("overview_polyline")
            val encodedPoints = overviewPolyline.getString("points")
            val routePoints = decodePolyline(encodedPoints)

            Log.d(TAG, "Route found: ${routePoints.size} points, $distance meters, $duration seconds")

            return DirectionsResult(
                points = routePoints,
                distanceMeters = distance,
                durationSeconds = duration
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse directions response: ${e.message}")
            return null
        }
    }

    /**
     * 解码 Google 的 Polyline 编码
     * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     */
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

            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }
}

/**
 * Directions API 结果
 */
data class DirectionsResult(
    val points: List<LatLng>,      // 路线点列表
    val distanceMeters: Int,       // 总距离（米）
    val durationSeconds: Int       // 预计时间（秒）
)
