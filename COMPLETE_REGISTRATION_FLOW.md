# 完整注册流程实现方案 📋

## 概述

实现了**6步完整注册流程** + **首次使用功能引导**，全面收集用户信息用于个性化推荐。

---

## 🎯 完整注册流程（6步）

### Step 0: 个人信息填写 ✅
**文件**: `fragment_signup_wizard.xml` (layout_personal_info)
- 用户名（至少3字符）
- 邮箱地址（格式验证）
- NUSNET ID（e开头）

### Step 1: 学院选择 ✅
**文件**: `fragment_signup_wizard.xml` (layout_faculty_selection)
- ViewPager2滑动卡片
- 点击选择即跳转

### Step 2: 交通偏好 🆕
**文件**: `layout_transport_preference.xml`
- 🚌 Bus（校园巴士&公交）
- 🚶 Walking（步行）
- 🚲 Cycling（骑行/电动车）
- 🚗 Carpool（拼车）
- 多选卡片界面

### Step 3: 常用地点 🆕
**文件**: `layout_common_locations.xml`
- 🏠 Dormitory（宿舍）
- 🏫 Teaching Building（教学楼）
- 📚 Study Spot（学习地点）
- 快捷选择：Gym、Canteen、Lab、Sports Complex
- 可跳过

### Step 4: 兴趣与目标 🆕
**文件**: `layout_interests_goals.xml`
- 🌱 兴趣偏好（Sustainability、Challenges、Community、Rewards、Leaderboard）
- 🎯 每周目标（1-20次绿色出行）
- 🔔 通知偏好（挑战、活动提醒、好友动态）

### Step 5: 小狮子展示 ✅
**文件**: `fragment_signup_wizard.xml` (layout_mascot_reveal)
- 欢迎用户
- 展示小狮子和学院装备

---

## 🚀 首次使用功能引导

### 触发时机
- ✅ 只在首次注册成功后显示
- ✅ 可以跳过
- ✅ 后续登录不再显示

### 引导内容（建议4-5页）

#### 页面1: 欢迎页
```
🎉 Welcome to EcoGo!

Transform your daily commute into
an environmental impact

[图：小狮子欢迎手势]
```

#### 页面2: 绿色出行
```
🚌 Track Your Green Trips

Earn points for every eco-friendly
journey you take

[图：巴士路线规划界面]
```

#### 页面3: 挑战系统
```
🏆 Join Challenges

Compete with friends and your
faculty for rewards

[图：挑战列表]
```

#### 页面4: 小狮子换装
```
🎨 Customize Your Mascot

Unlock outfits and accessories
as you progress

[图：小狮子换装界面]
```

#### 页面5: 开始使用
```
🌱 Ready to make a difference?

Start your first green trip today!

[按钮：Let's Go!]
```

---

## 📊 收集的用户数据

### 基础信息
```kotlin
data class UserProfile(
    val username: String,
    val email: String,
    val nusnetId: String,
    val facultyId: String
)
```

### 交通偏好
```kotlin
data class TransportPreferences(
    val prefersBus: Boolean,
    val prefersWalking: Boolean,
    val prefersCycling: Boolean,
    val prefersCarpool: Boolean
)
```

### 常用地点
```kotlin
data class CommonLocations(
    val dormitory: String?,
    val teachingBuilding: String?,
    val studySpot: String?,
    val otherSpots: List<String>  // gym, canteen, lab, sports
)
```

### 兴趣与目标
```kotlin
data class InterestsAndGoals(
    val interests: List<String>,  // sustainability, challenges, community, rewards, leaderboard
    val weeklyGoal: Int,  // 1-20 trips
    val notifyChallenges: Boolean,
    val notifyReminders: Boolean,
    val notifyFriends: Boolean
)
```

### 完整注册数据
```kotlin
data class CompleteRegistration(
    val profile: UserProfile,
    val faculty: FacultyData,
    val transport: TransportPreferences,
    val locations: CommonLocations,
    val interests: InterestsAndGoals,
    val isFirstLogin: Boolean = true
)
```

---

## 🔧 SignupWizardFragment 实现要点

### 数据字段
```kotlin
class SignupWizardFragment : Fragment() {
    private var currentStep = 0
    
    // Step 0: Personal Info
    private var username: String = ""
    private var email: String = ""
    private var nusnetId: String = ""
    
    // Step 1: Faculty
    private var selectedFaculty: FacultyData? = null
    
    // Step 2: Transport
    private val transportPrefs = mutableSetOf<String>()
    
    // Step 3: Locations
    private var dormitory: String? = null
    private var teachingBuilding: String? = null
    private var studySpot: String? = null
    private val otherLocations = mutableSetOf<String>()
    
    // Step 4: Interests & Goals
    private val interests = mutableSetOf<String>()
    private var weeklyGoal: Int = 5
    private var notifyChallenges: Boolean = true
    private var notifyReminders: Boolean = true
    private var notifyFriends: Boolean = false
}
```

### 步骤流程
```kotlin
fun showPersonalInfo() { currentStep = 0 }
fun showFacultySelection() { currentStep = 1 }
fun showTransportPreference() { currentStep = 2 }  // 新增
fun showCommonLocations() { currentStep = 3 }      // 新增
fun showInterestsGoals() { currentStep = 4 }       // 新增
fun showMascotReveal() { currentStep = 5 }
```

### 新增方法框架
```kotlin
private fun showTransportPreference() {
    // 显示交通偏好界面
    binding.layoutTransportPreference.visibility = View.VISIBLE
    
    // 设置卡片点击监听
    setupTransportCards()
    
    // Continue按钮
    binding.btnContinueTransport.setOnClickListener {
        showCommonLocations()
    }
}

private fun showCommonLocations() {
    // 显示常用地点界面
    binding.layoutCommonLocations.visibility = View.VISIBLE
    
    // 收集输入
    collectLocationInputs()
    
    // Skip & Continue按钮
    binding.btnSkipLocations.setOnClickListener {
        showInterestsGoals()
    }
    binding.btnContinueLocations.setOnClickListener {
        showInterestsGoals()
    }
}

private fun showInterestsGoals() {
    // 显示兴趣目标界面
    binding.layoutInterestsGoals.visibility = View.VISIBLE
    
    // 设置ChipGroup、Slider、Switch监听
    setupInterestsAndGoals()
    
    // Finish按钮
    binding.btnFinishSignup.setOnClickListener {
        selectedFaculty?.let { showMascotReveal(it) }
    }
}
```

---

## 🎓 首次登录检测逻辑

### 使用SharedPreferences
```kotlin
// 保存首次登录标记
fun saveFirstLoginStatus(context: Context, isFirstLogin: Boolean) {
    val prefs = context.getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_first_login", isFirstLogin).apply()
}

// 检查是否首次登录
fun isFirstLogin(context: Context): Boolean {
    val prefs = context.getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_first_login", true)
}
```

### 在MainActivity中使用
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 检查是否首次登录
    if (isFirstLogin(this) && isUserLoggedIn()) {
        // 显示功能引导
        navController.navigate(R.id.onboardingFragment)
        // 标记为已显示
        saveFirstLoginStatus(this, false)
    }
}
```

---

## 📝 OnboardingFragment 增强

### 更新内容数据
```kotlin
// 在OnboardingAdapter中
data class OnboardingPage(
    val imageRes: Int,
    val emoji: String,
    val title: String,
    val description: String
)

val pages = listOf(
    OnboardingPage(
        emoji = "🎉",
        title = "Welcome to EcoGo!",
        description = "Transform your daily commute into environmental impact"
    ),
    OnboardingPage(
        emoji = "🚌",
        title = "Track Your Green Trips",
        description = "Earn points for every eco-friendly journey"
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Join Challenges",
        description = "Compete with friends and faculty for rewards"
    ),
    OnboardingPage(
        emoji = "🎨",
        title = "Customize Your Mascot",
        description = "Unlock outfits as you progress"
    ),
    OnboardingPage(
        emoji = "🌱",
        title = "Ready to Start?",
        description = "Begin your first green trip today!"
    )
)
```

---

## 🎯 个性化推荐应用

### 1. 路线推荐
```kotlin
// 根据交通偏好推荐路线
if (user.prefersBus) {
    // 优先推荐巴士路线
}
if (user.prefersWalking) {
    // 推荐步行路线
}
```

### 2. 活动推荐
```kotlin
// 根据兴趣推荐活动
if ("challenges" in user.interests) {
    // 推荐挑战活动
}
if ("community" in user.interests) {
    // 推荐社区活动
}
```

### 3. 地点快捷访问
```kotlin
// 首页显示常用地点快捷入口
user.commonLocations.forEach { location ->
    addQuickAccessButton(location)
}
```

### 4. 目标追踪
```kotlin
// 显示每周目标进度
val progress = user.completedTrips / user.weeklyGoal
showGoalProgress(progress)
```

### 5. 智能通知
```kotlin
// 根据通知偏好发送通知
if (user.notifyChallenges && newChallengeAvailable()) {
    sendNotification("New challenge available!")
}
```

---

## 🔐 数据持久化

### 本地存储（SharedPreferences）
```kotlin
// 保存注册数据
fun saveRegistrationData(context: Context, data: CompleteRegistration) {
    val prefs = context.getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(data)
    prefs.edit().putString("registration_data", json).apply()
}
```

### 后端API（建议）
```kotlin
// POST /api/users/register
data class RegisterRequest(
    val username: String,
    val email: String,
    val nusnetId: String,
    val facultyId: String,
    val transportPreferences: List<String>,
    val commonLocations: Map<String, String>,
    val interests: List<String>,
    val weeklyGoal: Int,
    val notificationSettings: Map<String, Boolean>
)
```

---

## ✅ 实现清单

### 已完成
- ✅ Step 0: 个人信息界面
- ✅ Step 1: 学院选择（滑动卡片）
- ✅ Step 2: 交通偏好布局
- ✅ Step 3: 常用地点布局
- ✅ Step 4: 兴趣目标布局
- ✅ Step 5: 小狮子展示

### 待集成
- ⏳ 更新SignupWizardFragment添加新步骤逻辑
- ⏳ 实现数据收集和验证
- ⏳ 更新OnboardingFragment内容为功能引导
- ⏳ 实现首次登录检测
- ⏳ 后端API集成
- ⏳ 数据持久化

---

## 🚀 快速集成指南

### 1. 添加binding引用
在SignupWizardFragment中添加新布局的binding访问：
```kotlin
// 由于使用了<include>，binding会自动包含这些布局
binding.layoutTransportPreference
binding.layoutCommonLocations
binding.layoutInterestsGoals
```

### 2. 实现新步骤方法
参考上面的"新增方法框架"实现三个新方法

### 3. 更新showFacultySelection()
```kotlin
// 在选择学院后跳转到交通偏好
selectedFaculty = faculty
postDelayed({ showTransportPreference() }, 300)
```

### 4. 更新completeSignup()
```kotlin
private fun completeSignup(faculty: FacultyData) {
    val registrationData = CompleteRegistration(
        profile = UserProfile(username, email, nusnetId, faculty.id),
        faculty = faculty,
        transport = TransportPreferences(/* ... */),
        locations = CommonLocations(/* ... */),
        interests = InterestsAndGoals(/* ... */)
    )
    
    // 保存数据
    saveRegistrationData(requireContext(), registrationData)
    
    // 标记为首次登录
    saveFirstLoginStatus(requireContext(), true)
    
    // 导航到首页（会自动触发Onboarding）
    findNavController().navigate(R.id.action_signup_to_home)
}
```

---

*文档版本: 3.0*  
*生成时间: 2026-02-03*
