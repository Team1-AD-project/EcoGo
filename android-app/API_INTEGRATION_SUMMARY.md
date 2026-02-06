# API集成更新总结

## 🎯 完成的工作

### 1. 成功合并main分支
- ✅ 将main分支的最新后端代码合并到feat/map-engine
- ✅ 解决了logs/eco-go.log的冲突
- ✅ 确保Android前端代码与后端代码同步

### 2. 读取并分析后端实际代码
分析了以下后端文件：
- `TripController.java` - 确认API端点和请求/响应结构
- `TripDto.java` - 确认数据模型定义
- `ResponseMessage.java` - 确认响应包装格式

### 3. 更新Android API模型以匹配后端

#### 关键发现和修复：

**A. 响应包装格式**
后端所有API响应都使用`ResponseMessage<T>`包装：
```json
{
  "code": 200,
  "message": "success!",
  "data": { ... }
}
```

**B. 数据类型修正**
- ❌ 之前：`carbonSaved: Double` (误以为是kg)
- ✅ 现在：`carbonSaved: Long` (单位：克/grams)

**C. 字段名称修正**
- ❌ 之前：`greenPoints: Int`
- ✅ 现在：`pointsGained: Long`

**D. 响应类型简化**
后端的start和complete接口都直接返回`TripResponse`对象，不是单独的响应类型：
```kotlin
// 之前有单独的 TripStartResponse, TripCompleteResponse
// 现在统一使用 TripDetail (即 TripResponse)
typealias TripStartResponse = TripDetail
typealias TripCompleteResponse = TripDetail
```

---

## 📝 更新的文件

### 1. `/android-app/app/src/main/java/com/ecogo/app/data/model/TripApiModels.kt`

**主要更改：**
- `carbonSaved`类型从`Double`改为`Long`，并添加注释说明单位是克(g)
- `greenPoints`字段名改为`pointsGained`，类型改为`Long`
- 简化了`TripStartResponse`和`TripCompleteResponse`为typealias

```kotlin
// 碳减排量：单位是克(g)，不是千克(kg)
@SerializedName("carbonSaved")
val carbonSaved: Long,  // 单位：克(g)

// 获得的积分
@SerializedName("pointsGained")
val pointsGained: Long? = null,
```

### 2. `/android-app/app/src/main/java/com/ecogo/app/data/remote/TripApiService.kt`

**主要更改：**
所有API方法的返回类型都包装在`ApiResponse<T>`中：

```kotlin
// 之前
suspend fun startTrip(...): Response<TripStartResponse>

// 现在
suspend fun startTrip(...): Response<ApiResponse<TripStartResponse>>
```

这样可以正确解析后端的`ResponseMessage`包装格式。

### 3. `/android-app/app/src/main/java/com/ecogo/app/data/repository/TripRepository.kt`

**主要更改：**

**A. 所有API调用都更新为处理ApiResponse包装：**

```kotlin
val response = tripApiService.startTrip(authToken, request)

if (response.isSuccessful && response.body() != null) {
    val apiResponse = response.body()!!
    if (apiResponse.success && apiResponse.data != null) {
        // 使用 apiResponse.data 获取实际数据
        val tripId = apiResponse.data.tripId
        // ...
    } else {
        // 处理API级别的错误
        val error = "API returned error: ${apiResponse.message}"
        // ...
    }
}
```

**B. completeTrip方法的carbonSaved参数类型：**

```kotlin
// 之前
carbonSaved: Double = 0.0

// 现在
carbonSaved: Long = 0L  // 单位：克(g)
```

**C. cancelTrip返回类型：**

```kotlin
// 之前
suspend fun cancelTrip(tripId: String): Result<TripCancelResponse>

// 现在
suspend fun cancelTrip(tripId: String): Result<String>
```

---

## 🔧 如何使用更新后的API

### 1. 开始行程 (startTrip)

```kotlin
val repo = TripRepository.getInstance()

// 设置token（从登录系统获取）
repo.setAuthToken("your_jwt_token_here")

// 开始行程
val result = repo.startTrip(
    startLat = 22.3374,
    startLng = 114.1799,
    startPlaceName = "深圳大学",
    startAddress = "广东省深圳市南山区南海大道3688号"
)

result.onSuccess { tripId ->
    Log.d(TAG, "Trip started: $tripId")
    // 保存tripId，用于后续完成行程
}
result.onFailure { error ->
    Log.e(TAG, "Failed to start trip: ${error.message}")
}
```

### 2. 完成行程 (completeTrip)

```kotlin
val result = repo.completeTrip(
    tripId = "获取到的tripId",
    endLat = 22.3200,
    endLng = 114.1700,
    endPlaceName = "科技园",
    endAddress = "广东省深圳市南山区科技园",
    distance = 5000.0,  // 单位：米
    trackPoints = listOf(...),  // LatLng列表
    transportMode = "WALKING",
    detectedMode = "WALKING",
    mlConfidence = 0.95,
    carbonSaved = 1500L,  // 注意：单位是克(g)，不是kg！
    isGreenTrip = true
)

result.onSuccess { tripResponse ->
    Log.d(TAG, "Trip completed!")
    Log.d(TAG, "Carbon saved: ${tripResponse.carbonSaved}g")
    Log.d(TAG, "Points gained: ${tripResponse.pointsGained}")
}
```

### 3. 获取行程列表

```kotlin
// 从云端获取
val result = repo.getTripListFromCloud()

result.onSuccess { trips ->
    trips.forEach { trip ->
        Log.d(TAG, "Trip: ${trip.startPlaceName} -> ${trip.endPlaceName}")
        Log.d(TAG, "Carbon saved: ${trip.carbonSaved}g")
    }
}

// 从本地获取（更快）
val localResult = repo.getTripListFromLocal()
```

### 4. 获取当前行程

```kotlin
val result = repo.getCurrentTrip()

result.onSuccess { trip ->
    if (trip != null) {
        Log.d(TAG, "Current trip: ${trip.tripId}")
    } else {
        Log.d(TAG, "No current trip")
    }
}
```

---

## ⚠️ 重要注意事项

### 1. carbonSaved单位
- **后端存储单位：克(g)**
- **前端显示时需要转换为kg：`carbonSaved / 1000.0`**
- 例如：后端返回1500，表示1500克 = 1.5公斤

```kotlin
val carbonSavedKg = tripResponse.carbonSaved / 1000.0
println("Carbon saved: ${carbonSavedKg}kg")
```

### 2. Token管理
后端会自动从`Authorization`请求头解析userId：
```kotlin
// 在登录成功后设置token
TripRepository.getInstance().setAuthToken(jwtToken)

// 后续所有API调用都会自动使用这个token
```

**Token格式：**
```
Authorization: Bearer <your_jwt_token>
```

### 3. API错误处理
现在有两层错误处理：

**HTTP层错误（网络错误、服务器错误）：**
```kotlin
if (!response.isSuccessful) {
    // HTTP错误：404, 500等
}
```

**API层错误（业务逻辑错误）：**
```kotlin
if (!apiResponse.success) {
    // API业务错误：权限不足、数据验证失败等
    val errorMessage = apiResponse.message
}
```

### 4. 轨迹点简化
发送到后端前建议使用RouteSimplifier简化轨迹点：

```kotlin
import com.ecogo.app.util.RouteSimplifier

// 原始轨迹点
val originalPoints: List<LatLng> = ...  // 可能有1000+个点

// 简化轨迹（减少90%数据量，视觉效果几乎不变）
val simplifiedPoints = RouteSimplifier.simplify(
    points = originalPoints,
    tolerance = 20.0  // 容差：20米
)

// 或者限制最大点数
val simplifiedPoints = RouteSimplifier.simplifyToCount(
    points = originalPoints,
    targetCount = 100  // 最多100个点
)

Log.d(TAG, "Original: ${originalPoints.size} points")
Log.d(TAG, "Simplified: ${simplifiedPoints.size} points")
```

---

## 📊 API端点列表

### Mobile端点 (需要token)

| 方法 | 路径 | 功能 | 返回类型 |
|-----|------|------|---------|
| POST | `/mobile/trips/start` | 开始行程 | TripResponse |
| POST | `/mobile/trips/{tripId}/complete` | 完成行程 | TripResponse |
| POST | `/mobile/trips/{tripId}/cancel` | 取消行程 | String |
| GET | `/mobile/trips` | 获取行程列表 | List&lt;TripSummary&gt; |
| GET | `/mobile/trips/{tripId}` | 获取行程详情 | TripResponse |
| GET | `/mobile/trips/current` | 获取当前行程 | TripResponse? |

### Web/Admin端点 (需要管理员权限)

| 方法 | 路径 | 功能 | 返回类型 |
|-----|------|------|---------|
| GET | `/web/trips/all` | 获取所有行程 | List&lt;TripSummary&gt; |
| GET | `/web/trips/user/{userid}` | 获取指定用户行程 | List&lt;TripSummary&gt; |

**Base URL:** `http://47.129.124.55:8090/api/v1`

---

## 🧪 测试建议

### 1. 单元测试
测试API响应解析：
```kotlin
@Test
fun testApiResponseParsing() {
    val json = """
        {
            "code": 200,
            "message": "success!",
            "data": {
                "tripId": "test123",
                "carbonSaved": 1500
            }
        }
    """.trimIndent()

    val response = gson.fromJson<ApiResponse<TripDetail>>(json)
    assertEquals(true, response.success)
    assertEquals(1500L, response.data?.carbonSaved)
}
```

### 2. 集成测试
测试完整的开始-完成流程：
```kotlin
@Test
suspend fun testCompleteFlow() {
    val repo = TripRepository.getInstance()
    repo.setAuthToken("test_token")

    // 1. 开始行程
    val startResult = repo.startTrip(...)
    val tripId = startResult.getOrThrow()

    // 2. 完成行程
    val completeResult = repo.completeTrip(
        tripId = tripId,
        carbonSaved = 1500L  // 注意：克(g)
        // ...
    )

    assertTrue(completeResult.isSuccess)
}
```

---

## 📚 下一步工作

### 推荐实现顺序：

1. **✅ 已完成：**
   - 合并main分支
   - 更新API模型
   - 更新TripRepository

2. **🔨 待实现：**
   - 在MapActivity中集成API调用
   - 实现token管理和刷新机制
   - 添加网络错误重试逻辑
   - 实现本地缓存策略
   - 添加用户反馈（loading, success, error提示）

3. **🧪 测试：**
   - 单元测试API模型解析
   - 集成测试完整流程
   - 网络异常测试
   - Token过期处理测试

---

## 🐛 常见问题

### Q1: Token从哪里获取？
A: Token应该从登录系统获取。如果你们已经有登录系统，登录成功后会返回JWT token，保存下来并调用`TripRepository.getInstance().setAuthToken(token)`设置。

### Q2: carbonSaved怎么计算？
A: 这个应该由你们的碳排放计算模块提供。一般根据：
- 距离
- 交通方式
- 基准排放量（如果选择开车的排放量）

公式示例：`carbonSaved = distance * (baseline_emission - actual_emission)`

### Q3: 如何处理网络错误？
A: 所有API方法都返回`Result<T>`，使用`onSuccess`和`onFailure`处理：
```kotlin
result.onFailure { error ->
    when (error) {
        is IOException -> // 网络错误
        is HttpException -> // HTTP错误
        else -> // 其他错误
    }
}
```

### Q4: 本地存储和云端同步怎么协调？
A: 建议策略：
- 行程完成时同时保存到本地和上传到云端
- 获取历史时优先从本地读取（快）
- 定期后台同步云端数据
- 处理冲突时以云端为准

---

## 📞 联系和反馈

如果遇到问题：
1. 检查网络连接
2. 检查token是否有效
3. 查看Logcat日志（TAG: "TripRepository"）
4. 与后端团队确认API是否有变化

**祝开发顺利！** 🚀
