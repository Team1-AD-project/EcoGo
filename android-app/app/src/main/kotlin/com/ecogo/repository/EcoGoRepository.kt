package com.ecogo.repository

import android.util.Log
import com.ecogo.api.*
import com.ecogo.auth.TokenManager
import com.ecogo.data.Activity
import com.ecogo.data.BusRoute
import com.ecogo.data.CarbonFootprint
import com.ecogo.data.ChatRequest
import com.ecogo.data.ChatResponse
import com.ecogo.data.CheckIn
import com.ecogo.data.CheckInResponse
import com.ecogo.data.CheckInStatus
import com.ecogo.data.DailyGoal
import com.ecogo.data.Faculty
import com.ecogo.data.Friend
import com.ecogo.data.FriendActivity
import com.ecogo.data.HistoryItem
import com.ecogo.data.MockData
import com.ecogo.data.Notification
import com.ecogo.data.Product
import com.ecogo.data.Ranking
import com.ecogo.data.RecommendationRequest
import com.ecogo.data.RecommendationResponse
import com.ecogo.data.RedeemRequest
import com.ecogo.data.RedeemResponse
import com.ecogo.data.Voucher
import com.ecogo.data.VoucherRedeemRequest
import com.ecogo.data.WalkingRoute
import com.ecogo.data.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * EcoGo 数据仓库
 * 统一管理所有数据访问（API 调用）
 */
class EcoGoRepository {
    
    private val api = RetrofitClient.apiService
    
    // ==================== 用户认证相关 ====================
    
    /**
     * 用户注册
     */
    suspend fun register(
        username: String,
        email: String,
        nusnetId: String,
        password: String,
        faculty: String?,
        transportPreferences: List<String>?,
        interests: List<String>?,
        weeklyGoal: Int?
    ): Result<RegisterResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("DEBUG_API", "Register called with username: '$username' (length: ${username.length})")
            Log.d("DEBUG_API", "Register called with email: '$email'")
            Log.d("DEBUG_API", "Register called with nusnetId: '$nusnetId'")
            Log.d("DEBUG_API", "Register called with password length: ${password.length}")

            val request = RegisterRequest(
                username, email, nusnetId, password,
                faculty, transportPreferences, interests, weeklyGoal
            )
            Log.d("DEBUG_API", "Request created: $request")
            val response = api.register(request)
            Log.d("DEBUG_API", "API Response: success=${response.success}, message=${response.message}, data=${response.data}, code=${response.code}")

            // 检查 HTTP 状态码是否在 2xx 范围内，并且 data 不为空
            if (response.code in 200..299 && response.data != null) {
                Log.d("DEBUG_API", "Register successful, userid=${response.data.userid}")
                Result.success(response.data)
            } else {
                val errorMsg = response.message ?: "注册失败"
                Log.e("DEBUG_API", "Register failed: code=${response.code}, data=${response.data}, message=$errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("DEBUG_API", "Register failed with exception", e)
            Result.failure(e)
        }
    }
    
    /**
     * 用户登录
     */
    suspend fun login(nusnetId: String, password: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(nusnetId, password)
                val response = api.login(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "登录失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取用户资料
     */
    suspend fun getProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.getProfile(token)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取用户资料失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新用户资料
     */
    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.updateProfile(token, profile)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "更新用户资料失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 用户登出
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.logout(token)
            if (response.success) {
                TokenManager.clearToken()
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "登出失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 积分相关 ====================
    
    /**
     * 获取当前积分
     */
    suspend fun getCurrentPoints(): Result<CurrentPointsResponse> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.getCurrentPoints(token)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取积分失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取积分历史
     */
    suspend fun getPointsHistory(): Result<List<PointsLog>> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.getPointsHistory(token)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取积分历史失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 计算积分
     */
    suspend fun calculatePoints(mode: String, distance: Double): Result<Long> = 
        withContext(Dispatchers.IO) {
            try {
                val response = api.calculatePoints(mode, distance)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "计算积分失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取出行统计
     */
    suspend fun getTripStats(): Result<TripStats> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getAuthHeader()
            if (token == null) {
                return@withContext Result.failure(Exception("未登录"))
            }
            val response = api.getTripStats(token)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取出行统计失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 活动相关 ====================
    
    /**
     * 获取所有活动
     */
    suspend fun getAllActivities(): Result<List<Activity>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllActivities()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 根据 ID 获取活动
     */
    suspend fun getActivityById(id: String): Result<Activity> = withContext(Dispatchers.IO) {
        try {
            val response = api.getActivityById(id)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 参加活动
     */
    suspend fun joinActivity(activityId: String, userId: String): Result<Activity> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.joinActivity(activityId, userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 退出活动
     */
    suspend fun leaveActivity(activityId: String, userId: String): Result<Activity> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.leaveActivity(activityId, userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 排行榜相关 ====================

    private val mockLeaderboardPeriods = listOf("Week 4, 2026", "Week 3, 2026", "Week 2, 2026")

    private fun mockRankings(period: String): List<Ranking> = listOf(
        Ranking(period = period, rank = 1, userId = "user_001", nickname = "小明", steps = 125000, isVip = true),
        Ranking(period = period, rank = 2, userId = "user_002", nickname = "EcoRunner", steps = 98200, isVip = true),
        Ranking(period = period, rank = 3, userId = "user_003", nickname = "绿行侠", steps = 87600, isVip = false),
        Ranking(period = period, rank = 4, userId = "user_004", nickname = "步数达人", steps = 75400, isVip = true),
        Ranking(period = period, rank = 5, userId = "user_005", nickname = "晨跑王", steps = 68100, isVip = false),
        Ranking(period = period, rank = 6, userId = "user_006", nickname = "林小绿", steps = 59200, isVip = true),
        Ranking(period = period, rank = 7, userId = "user_007", nickname = "低碳生活", steps = 51800, isVip = false),
        Ranking(period = period, rank = 8, userId = "user_008", nickname = "天天走路", steps = 44500, isVip = false),
        Ranking(period = period, rank = 9, userId = "user_009", nickname = "校园行者", steps = 38100, isVip = true),
        Ranking(period = period, rank = 10, userId = "user_010", nickname = "环保先锋", steps = 32600, isVip = false),
        Ranking(period = period, rank = 11, userId = "user_011", nickname = "小步快跑", steps = 27800, isVip = false),
        Ranking(period = period, rank = 12, userId = "user_012", nickname = "绿色出行", steps = 22100, isVip = false),
    )

    /**
     * 获取排行榜数据（API 失败或空时返回 mock 数据）
     */
    suspend fun getLeaderboard(period: String = "Week 4, 2026"): Result<List<Ranking>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getRankingsByPeriod(period)
                if (response.success && response.data != null && response.data.isNotEmpty()) {
                    Result.success(response.data)
                } else {
                    Result.success(mockRankings(period))
                }
            } catch (e: Exception) {
                Result.success(mockRankings(period))
            }
        }

    /**
     * 获取可用的排行榜周期（API 失败或空时返回 mock 周期）
     */
    suspend fun getAvailablePeriods(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLeaderboardPeriods()
            if (response.success && response.data != null && response.data.isNotEmpty()) {
                Result.success(response.data)
            } else {
                Result.success(mockLeaderboardPeriods)
            }
        } catch (e: Exception) {
            Result.success(mockLeaderboardPeriods)
        }
    }
    
    // ==================== 商品相关 ====================
    
    /**
     * 获取所有商品（带分页）
     */
    suspend fun getAllGoods(
        page: Int = 1,
        size: Int = 20,
        category: String? = null,
        keyword: String? = null
    ): Result<GoodsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllGoods(page, size, category, keyword, null)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取可兑换商品
     */
    suspend fun getRedemptionGoods(vipLevel: Int? = null): Result<List<GoodsDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getRedemptionGoods(vipLevel)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取商品详情
     */
    suspend fun getGoodsById(id: String): Result<GoodsDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGoodsById(id)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 订单相关 ====================
    
    /**
     * 创建订单
     */
    suspend fun createOrder(order: OrderCreateRequest): Result<OrderDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createOrder(order)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 创建兑换订单
     */
    suspend fun createRedemptionOrder(order: OrderCreateRequest): Result<OrderDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.createRedemptionOrder(order)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取用户订单历史
     */
    suspend fun getUserOrderHistory(
        userId: String,
        status: String? = null,
        page: Int = 1,
        size: Int = 10
    ): Result<OrderHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserOrderHistory(userId, status, page, size)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 徽章相关 ====================
    
    /**
     * 购买徽章
     */
    suspend fun purchaseBadge(userId: String, badgeId: String): Result<BadgeDto> =
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf("user_id" to userId)
                val response = api.purchaseBadge(badgeId, request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 切换徽章佩戴状态
     */
    suspend fun toggleBadgeDisplay(
        userId: String,
        badgeId: String,
        isDisplay: Boolean
    ): Result<BadgeDto> = withContext(Dispatchers.IO) {
        try {
            val request = mapOf("user_id" to userId, "is_display" to isDisplay)
            val response = api.toggleBadgeDisplay(badgeId, request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取徽章商店列表
     */
    suspend fun getBadgeShopList(): Result<List<BadgeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBadgeShopList()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取我的徽章背包
     */
    suspend fun getMyBadges(userId: String): Result<List<BadgeDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMyBadges(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 统计相关 ====================
    
    /**
     * 获取仪表盘统计数据
     */
    suspend fun getDashboardStats(): Result<DashboardStatsDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getDashboardStats()
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== 路线与地图相关 ====================

    suspend fun getBusRoutes(): Result<List<BusRoute>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBusRoutes()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalkingRoutes(): Result<List<WalkingRoute>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWalkingRoutes()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFaculties(): Result<List<Faculty>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFaculties()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVouchers(): Result<List<Voucher>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVouchers()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun redeemVoucher(request: VoucherRedeemRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.redeemVoucher(request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistory(userId: String? = null): Result<List<HistoryItem>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getHistory(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRecommendation(request: RecommendationRequest): Result<RecommendationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getRecommendation(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendChat(request: ChatRequest): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.sendChat(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 新功能 API 方法 ====================
    
    /**
     * 每日签到
     */
    suspend fun checkIn(userId: String): Result<CheckInResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.performCheckIn(userId)
                if (response.success && response.data != null) {
                    val checkInResponse = CheckInResponse(
                        success = true,
                        pointsEarned = response.data["points"] as? Int ?: 10,
                        consecutiveDays = response.data["consecutiveDays"] as? Int ?: 1,
                        message = response.data["message"] as? String ?: "签到成功！"
                    )
                    Result.success(checkInResponse)
                } else {
                    Result.failure(Exception(response.message ?: "签到失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取签到状态
     */
    suspend fun getCheckInStatus(userId: String): Result<CheckInStatus> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getCheckInStatus(userId)
                if (response.success && response.data != null) {
                    val status = CheckInStatus(
                        userId = userId,
                        consecutiveDays = response.data["consecutiveDays"] as? Int ?: 0,
                        totalCheckIns = response.data["totalCheckIns"] as? Int ?: 0,
                        lastCheckInDate = response.data["lastCheckInDate"] as? String ?: "",
                        pointsEarned = response.data["pointsEarned"] as? Int ?: 0
                    )
                    Result.success(status)
                } else {
                    Result.failure(Exception(response.message ?: "获取签到状态失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取签到历史记录
     */
    suspend fun getCheckInHistory(userId: String): Result<List<CheckIn>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getCheckInHistory(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取签到历史失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取今日目标进度
     */
    suspend fun getDailyGoal(userId: String): Result<DailyGoal> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getDailyGoal(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取每日目标失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 更新每日目标
     */
    suspend fun updateDailyGoal(userId: String, updates: Map<String, Any>): Result<DailyGoal> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.updateDailyGoal(userId, updates)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "更新每日目标失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取天气和空气质量
     */
    suspend fun getWeather(location: String = "NUS"): Result<Weather> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getWeather(location)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取天气失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取通知列表
     */
    suspend fun getNotifications(userId: String): Result<List<Notification>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getNotifications(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取通知失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取未读通知
     */
    suspend fun getUnreadNotifications(userId: String): Result<List<Notification>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getUnreadNotifications(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取未读通知失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 标记通知为已读
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Notification> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.markNotificationAsRead(notificationId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "标记已读失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 全部标记为已读
     */
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.markAllNotificationsAsRead(userId)
                if (response.success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message ?: "全部标记已读失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取碳足迹数据
     */
    suspend fun getCarbonFootprint(userId: String, period: String = "monthly"): Result<CarbonFootprint> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getCarbonFootprint(userId, period)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取碳足迹失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 记录出行
     */
    suspend fun recordTrip(userId: String, tripType: String, distance: Double): Result<CarbonFootprint> =
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "userId" to userId,
                    "tripType" to tripType,
                    "distance" to distance
                )
                val response = api.recordTrip(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "记录出行失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取好友列表
     */
    suspend fun getFriends(userId: String): Result<List<Friend>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getFriends(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取好友列表失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取好友动态
     */
    suspend fun getFriendActivities(userId: String): Result<List<FriendActivity>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getFriendActivities(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取好友动态失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 添加好友
     */
    suspend fun addFriend(userId: String, friendId: String): Result<Friend> =
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf("userId" to userId, "friendId" to friendId)
                val response = api.addFriend(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "添加好友失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 删除好友
     */
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.removeFriend(userId, friendId)
                if (response.success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message ?: "删除好友失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 获取好友请求
     */
    suspend fun getFriendRequests(userId: String): Result<List<Friend>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getFriendRequests(userId)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取好友请求失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 接受好友请求
     */
    suspend fun acceptFriendRequest(userId: String, friendId: String): Result<Friend> =
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf("userId" to userId, "friendId" to friendId)
                val response = api.acceptFriendRequest(request)
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "接受好友请求失败"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 商店相关 ====================
    
    /**
     * 获取商店商品列表
     */
    suspend fun getShopProducts(
        type: String? = null,
        category: String? = null
    ): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getShopProducts(type, category)
            if (response.success && response.data != null) {
                Result.success(response.data.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取商品详情
     */
    suspend fun getProductById(productId: String): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductById(productId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 积分兑换商品
     */
    suspend fun redeemProduct(
        userId: String,
        productId: String,
        productType: String
    ): Result<RedeemResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RedeemRequest(userId, productId, productType)
            val response = api.redeemProduct(request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建支付Intent
     */
    suspend fun createPaymentIntent(
        userId: String,
        productId: String
    ): Result<PaymentIntentResponse> = withContext(Dispatchers.IO) {
        try {
            val request = PaymentIntentRequest(userId, productId)
            val response = api.createPaymentIntent(request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 确认支付
     */
    suspend fun confirmPayment(
        userId: String,
        productId: String,
        paymentIntentId: String
    ): Result<OrderDto> = withContext(Dispatchers.IO) {
        try {
            val request = ConfirmPaymentRequest(userId, productId, paymentIntentId)
            val response = api.confirmPayment(request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 社区动态相关 ====================
    
    /**
     * 获取社区动态信息流
     */
    suspend fun getFeed(userId: String): Result<List<com.ecogo.data.FeedItem>> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                // val response = api.getFeed(userId)
                // if (response.success && response.data != null) {
                //     Result.success(response.data)
                // } else {
                //     Result.failure(Exception(response.message))
                // }
                
                // 目前返回Mock数据
                val now = System.currentTimeMillis()
                val mockFeed = listOf(
                    com.ecogo.data.FeedItem(
                        id = "f1",
                        userId = "friend1",
                        username = "Alex Chen",
                        type = "TRIP",
                        content = "完成了一次绿色出行，节省了 125g CO₂！",
                        timestamp = now - 1800000, // 30分钟前
                        likes = 12
                    ),
                    com.ecogo.data.FeedItem(
                        id = "f2",
                        userId = "friend2",
                        username = "Sarah Tan",
                        type = "ACHIEVEMENT",
                        content = "解锁了 'Week Warrior' 成就！",
                        timestamp = now - 3600000, // 1小时前
                        likes = 25
                    ),
                    com.ecogo.data.FeedItem(
                        id = "f3",
                        userId = "friend3",
                        username = "Kevin Wong",
                        type = "ACTIVITY",
                        content = "参加了 Campus Clean-Up Day 活动",
                        timestamp = now - 7200000, // 2小时前
                        likes = 8
                    ),
                    com.ecogo.data.FeedItem(
                        id = "f4",
                        userId = "friend4",
                        username = "Emily Liu",
                        type = "CHALLENGE",
                        content = "在 '本周绿色出行挑战' 中取得第一名！",
                        timestamp = now - 10800000, // 3小时前
                        likes = 35
                    ),
                    com.ecogo.data.FeedItem(
                        id = "f5",
                        userId = "friend5",
                        username = "David Ng",
                        type = "TRIP",
                        content = "骑行 3.5 公里，获得了 175 积分",
                        timestamp = now - 18000000, // 5小时前
                        likes = 6
                    ),
                    com.ecogo.data.FeedItem(
                        id = "f6",
                        userId = "friend1",
                        username = "Alex Chen",
                        type = "ACHIEVEMENT",
                        content = "累计减少 5kg CO₂ 排放！",
                        timestamp = now - 86400000, // 1天前
                        likes = 42
                    )
                )
                Result.success(mockFeed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 发布动态
     */
    suspend fun postFeedItem(item: com.ecogo.data.FeedItem): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                // val response = api.postFeedItem(item)
                // if (response.success) {
                //     Result.success(Unit)
                // } else {
                //     Result.failure(Exception(response.message))
                // }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 挑战系统相关 ====================
    
    /**
     * 获取所有挑战
     */
    suspend fun getChallenges(): Result<List<com.ecogo.data.Challenge>> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                Result.success(MockData.CHALLENGES)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 根据ID获取挑战详情
     */
    suspend fun getChallengeById(id: String): Result<com.ecogo.data.Challenge> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                val challenge = MockData.CHALLENGES.find { it.id == id }
                if (challenge != null) {
                    Result.success(challenge)
                } else {
                    Result.failure(Exception("Challenge not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 接受挑战
     */
    suspend fun acceptChallenge(challengeId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 更新挑战进度
     */
    suspend fun updateChallengeProgress(challengeId: String, progress: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ==================== 绿色点位相关 ====================
    
    /**
     * 获取所有绿色点位
     */
    suspend fun getGreenSpots(): Result<List<com.ecogo.data.GreenSpot>> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                Result.success(MockData.GREEN_SPOTS)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 收集绿色点位
     */
    suspend fun collectSpot(spotId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 实际应调用真实API
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
