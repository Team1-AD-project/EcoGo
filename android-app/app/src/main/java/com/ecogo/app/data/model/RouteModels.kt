package com.ecogo.app.data.model

/**
 * ========================================
 * 路线推荐相关模型
 * 对应 RecommendService 微服务接口
 * ========================================
 */

/**
 * 路线推荐请求
 * POST /api/mobile/route/recommend/low-carbon
 * POST /api/mobile/route/recommend/balance
 */
data class RouteRecommendRequest(
    val user_id: String,
    val start_point: GeoPoint,
    val end_point: GeoPoint
)

/**
 * 路线推荐响应
 */
data class RouteRecommendData(
    val route_id: String? = null,
    val route_type: String? = null,
    val total_distance: Double = 0.0,
    val estimated_duration: Int = 0,  // 分钟
    val total_carbon: Double = 0.0,
    val carbon_saved: Double = 0.0,
    val route_segments: List<RouteSegment>? = null,
    val route_points: List<GeoPoint>? = null,
    // 兼容旧字段
    val green_route: List<GeoPoint>? = null,
    val duration: Int? = null
)

/**
 * 路线段（多段式路线中的单段）
 */
data class RouteSegment(
    val transport_mode: String,
    val distance: Double,
    val duration: Int,
    val carbon_emission: Double,
    val instructions: String? = null,
    val polyline: List<GeoPoint>? = null
)

/**
 * 路线推荐类型
 */
enum class RouteType(val value: String) {
    LOW_CARBON("low-carbon"),      // 碳排最低
    BALANCE("balance")              // 时间-碳排平衡
}

/**
 * 路线缓存数据
 * GET /api/mobile/route/cache/{user_id}
 */
data class RouteCacheData(
    val route_info: RouteRecommendData?,
    val expire_time: String?
)

/**
 * 交通方式
 */
enum class TransportMode(val value: String, val displayName: String) {
    WALKING("walking", "步行"),
    CYCLING("cycling", "骑行"),
    BUS("bus", "公交"),
    SUBWAY("subway", "地铁"),
    DRIVING("driving", "驾车")
}
