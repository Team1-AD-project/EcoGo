// ======================================
// SignupWizardFragment 新步骤集成代码
// ======================================
// 将以下代码添加到 SignupWizardFragment.kt 中

// 1. 在类顶部添加新的数据字段
class SignupWizardFragment : Fragment() {
    // ... 现有字段 ...
    
    // Step 2: Transport Preferences
    private val transportPrefs = mutableSetOf<String>()
    
    // Step 3: Common Locations
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

// 2. 修改 showFacultySelection() 中的跳转逻辑
private fun showFacultySelection() {
    // ... 现有代码 ...
    
    val adapter = FacultySwipeAdapter(MockData.FACULTY_DATA) { faculty ->
        selectedFaculty = faculty
        // 修改：跳转到交通偏好页面
        binding.viewpagerFaculties.postDelayed({
            showTransportPreference()  // 改为这个
        }, 300)
    }
}

// 3. 新增 showTransportPreference() 方法
private fun showTransportPreference() {
    currentStep = 2
    
    // 隐藏其他界面
    binding.layoutPersonalInfo.visibility = View.GONE
    binding.layoutFacultySelection.visibility = View.GONE
    binding.layoutTransportPreference.visibility = View.VISIBLE
    binding.layoutCommonLocations.visibility = View.GONE
    binding.layoutInterestsGoals.visibility = View.GONE
    binding.layoutMascotReveal.visibility = View.GONE
    
    // 设置交通卡片点击监听
    val cards = listOf(
        binding.cardBus to "bus",
        binding.cardWalking to "walking",
        binding.cardCycling to "cycling",
        binding.cardCarpool to "carpool"
    )
    
    cards.forEach { (card, type) ->
        card.setOnClickListener {
            if (transportPrefs.contains(type)) {
                transportPrefs.remove(type)
                card.strokeColor = resources.getColor(R.color.border, null)
                card.findViewById<View>(getCheckViewId(type)).visibility = View.GONE
            } else {
                transportPrefs.add(type)
                card.strokeColor = resources.getColor(R.color.primary, null)
                card.findViewById<View>(getCheckViewId(type)).visibility = View.VISIBLE
            }
            
            // 至少选择一个才能继续
            binding.btnContinueTransport.isEnabled = transportPrefs.isNotEmpty()
            binding.btnContinueTransport.alpha = if (transportPrefs.isNotEmpty()) 1f else 0.5f
        }
    }
    
    // Continue按钮
    binding.btnContinueTransport.isEnabled = false
    binding.btnContinueTransport.alpha = 0.5f
    binding.btnContinueTransport.setOnClickListener {
        showCommonLocations()
    }
}

private fun getCheckViewId(type: String): Int {
    return when (type) {
        "bus" -> R.id.check_bus
        "walking" -> R.id.check_walking
        "cycling" -> R.id.check_cycling
        "carpool" -> R.id.check_carpool
        else -> 0
    }
}

// 4. 新增 showCommonLocations() 方法
private fun showCommonLocations() {
    currentStep = 3
    
    // 隐藏其他界面
    binding.layoutPersonalInfo.visibility = View.GONE
    binding.layoutFacultySelection.visibility = View.GONE
    binding.layoutTransportPreference.visibility = View.GONE
    binding.layoutCommonLocations.visibility = View.VISIBLE
    binding.layoutInterestsGoals.visibility = View.GONE
    binding.layoutMascotReveal.visibility = View.GONE
    
    // Chip监听
    binding.chipGym.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) otherLocations.add("gym") else otherLocations.remove("gym")
    }
    binding.chipCanteen.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) otherLocations.add("canteen") else otherLocations.remove("canteen")
    }
    binding.chipLab.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) otherLocations.add("lab") else otherLocations.remove("lab")
    }
    binding.chipSports.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) otherLocations.add("sports") else otherLocations.remove("sports")
    }
    
    // Skip按钮
    binding.btnSkipLocations.setOnClickListener {
        showInterestsGoals()
    }
    
    // Continue按钮
    binding.btnContinueLocations.setOnClickListener {
        dormitory = binding.inputDorm.text.toString()
        teachingBuilding = binding.inputBuilding.text.toString()
        studySpot = binding.inputLibrary.text.toString()
        showInterestsGoals()
    }
}

// 5. 新增 showInterestsGoals() 方法
private fun showInterestsGoals() {
    currentStep = 4
    
    // 隐藏其他界面
    binding.layoutPersonalInfo.visibility = View.GONE
    binding.layoutFacultySelection.visibility = View.GONE
    binding.layoutTransportPreference.visibility = View.GONE
    binding.layoutCommonLocations.visibility = View.GONE
    binding.layoutInterestsGoals.visibility = View.VISIBLE
    binding.layoutMascotReveal.visibility = View.GONE
    
    // 兴趣Chips监听
    val interestChips = mapOf(
        binding.chipSustainability to "sustainability",
        binding.chipChallenges to "challenges",
        binding.chipCommunity to "community",
        binding.chipRewards to "rewards",
        binding.chipLeaderboard to "leaderboard"
    )
    
    interestChips.forEach { (chip, interest) ->
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add(interest) else interests.remove(interest)
        }
    }
    
    // 每周目标Slider
    binding.sliderWeeklyGoal.addOnChangeListener { _, value, _ ->
        weeklyGoal = value.toInt()
        binding.textGoalValue.text = weeklyGoal.toString()
    }
    
    // 通知开关
    binding.switchChallenges.setOnCheckedChangeListener { _, isChecked ->
        notifyChallenges = isChecked
    }
    binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
        notifyReminders = isChecked
    }
    binding.switchFriends.setOnCheckedChangeListener { _, isChecked ->
        notifyFriends = isChecked
    }
    
    // Finish按钮
    binding.btnFinishSignup.setOnClickListener {
        selectedFaculty?.let { faculty ->
            showMascotReveal(faculty)
        }
    }
}

// 6. 修改 showMascotReveal() 的步骤编号
private fun showMascotReveal(faculty: FacultyData) {
    currentStep = 5  // 改为5
    
    // 隐藏其他界面
    binding.layoutPersonalInfo.visibility = View.GONE
    binding.layoutFacultySelection.visibility = View.GONE
    binding.layoutTransportPreference.visibility = View.GONE
    binding.layoutCommonLocations.visibility = View.GONE
    binding.layoutInterestsGoals.visibility = View.GONE
    binding.layoutMascotReveal.visibility = View.VISIBLE
    
    // ... 其余代码保持不变 ...
}

// 7. 修改 completeSignup() 保存所有数据
private fun completeSignup(faculty: FacultyData) {
    android.util.Log.d("DEBUG_SIGNUP", "=== Complete Registration Data ===")
    android.util.Log.d("DEBUG_SIGNUP", "Username: $username")
    android.util.Log.d("DEBUG_SIGNUP", "Email: $email")
    android.util.Log.d("DEBUG_SIGNUP", "NUSNET: $nusnetId")
    android.util.Log.d("DEBUG_SIGNUP", "Faculty: ${faculty.name}")
    android.util.Log.d("DEBUG_SIGNUP", "Transport: ${transportPrefs.joinToString(", ")}")
    android.util.Log.d("DEBUG_SIGNUP", "Dorm: $dormitory")
    android.util.Log.d("DEBUG_SIGNUP", "Building: $teachingBuilding")
    android.util.Log.d("DEBUG_SIGNUP", "Study Spot: $studySpot")
    android.util.Log.d("DEBUG_SIGNUP", "Other Locations: ${otherLocations.joinToString(", ")}")
    android.util.Log.d("DEBUG_SIGNUP", "Interests: ${interests.joinToString(", ")}")
    android.util.Log.d("DEBUG_SIGNUP", "Weekly Goal: $weeklyGoal")
    android.util.Log.d("DEBUG_SIGNUP", "Notifications: challenges=$notifyChallenges, reminders=$notifyReminders, friends=$notifyFriends")
    
    // TODO: 保存到SharedPreferences
    saveRegistrationData()
    
    // TODO: 发送到后端API
    // apiService.registerUser(registrationData)
    
    // 标记为首次登录（用于触发功能引导）
    saveFirstLoginStatus(true)
    
    try {
        android.widget.Toast.makeText(
            requireContext(), 
            "Welcome to EcoGo, $username! 🎉", 
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        findNavController().navigate(R.id.action_signup_to_home)
        android.util.Log.d("DEBUG_SIGNUP", "Navigate to home completed")
    } catch (e: Exception) {
        android.util.Log.e("DEBUG_SIGNUP", "Navigation failed: ${e.message}", e)
        android.widget.Toast.makeText(requireContext(), "导航错误: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// 8. 添加辅助方法
private fun saveRegistrationData() {
    val prefs = requireContext().getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("username", username)
        putString("email", email)
        putString("nusnet_id", nusnetId)
        putString("faculty", selectedFaculty?.name)
        putStringSet("transport_prefs", transportPrefs)
        putString("dormitory", dormitory)
        putString("teaching_building", teachingBuilding)
        putString("study_spot", studySpot)
        putStringSet("other_locations", otherLocations)
        putStringSet("interests", interests)
        putInt("weekly_goal", weeklyGoal)
        putBoolean("notify_challenges", notifyChallenges)
        putBoolean("notify_reminders", notifyReminders)
        putBoolean("notify_friends", notifyFriends)
        apply()
    }
}

private fun saveFirstLoginStatus(isFirstLogin: Boolean) {
    val prefs = requireContext().getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_first_login", isFirstLogin).apply()
}

// ======================================
// MainActivity 首次登录检测
// ======================================
// 在MainActivity.kt的onCreate中添加：

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... 现有代码 ...
    
    // 检查是否首次登录
    checkAndShowOnboarding()
}

private fun checkAndShowOnboarding() {
    val prefs = getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
    val isFirstLogin = prefs.getBoolean("is_first_login", false)
    val isLoggedIn = prefs.getBoolean("is_logged_in", false)
    
    if (isFirstLogin && isLoggedIn) {
        // 显示功能引导
        navController.navigate(R.id.onboardingFragment)
        // 标记为已显示
        prefs.edit().putBoolean("is_first_login", false).apply()
    }
}

// ======================================
// OnboardingAdapter 更新内容
// ======================================
// 在OnboardingAdapter.kt中更新：

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🎉",
        title = "Welcome to EcoGo!",
        description = "Transform your daily commute\ninto environmental impact"
    ),
    OnboardingPage(
        emoji = "🚌",
        title = "Track Green Trips",
        description = "Earn points for every\neco-friendly journey"
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Join Challenges",
        description = "Compete with friends and\nyour faculty for rewards"
    ),
    OnboardingPage(
        emoji = "🎨",
        title = "Customize Mascot",
        description = "Unlock outfits and accessories\nas you progress"
    ),
    OnboardingPage(
        emoji = "🌱",
        title = "Ready to Start?",
        description = "Begin your first\ngreen trip today!"
    )
)
