package com.ecogo.ui.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.ecogo.auth.TokenManager
import com.ecogo.data.FacultyData
import com.ecogo.data.MockData
import com.ecogo.databinding.FragmentSignupWizardBinding
import com.ecogo.repository.EcoGoRepository
import com.ecogo.ui.adapters.FacultySwipeAdapter
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * SignupWizardFragment - 注册向导
 * 
 * 六步流程:
 * Step 0: 个人信息填写（用户名、邮箱、NUSNET ID）
 * Step 1: 学院选择（滑动卡片）
 * Step 2: 交通偏好（公交/步行/骑行/拼车）
 * Step 3: 常用地点（宿舍/教学楼/学习地点）
 * Step 4: 兴趣目标（兴趣、目标、通知偏好）
 * Step 5: 小狮子换装展示
 */
class SignupWizardFragment : Fragment() {
    
    private var _binding: FragmentSignupWizardBinding? = null
    private val binding get() = _binding!!
    
    private var currentStep = 0
    private var selectedFaculty: FacultyData? = null
    
    // Step 0: Personal info
    private var username: String = ""
    private var email: String = ""
    private var nusnetId: String = ""
    private var password: String = ""
    
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
    
    // Animation references
    private var buttonAnimator: ValueAnimator? = null
    private var mascotScaleAnimator: ValueAnimator? = null
    private var mascotRotateAnimator: ValueAnimator? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            Log.d("DEBUG_SIGNUP", "SignupWizardFragment onCreateView")
            Toast.makeText(context, "📝 SignupWizard 正在加载...", Toast.LENGTH_SHORT).show()
            _binding = FragmentSignupWizardBinding.inflate(inflater, container, false)
            Log.d("DEBUG_SIGNUP", "SignupWizardFragment binding inflated")
            Toast.makeText(context, "✅ SignupWizard 加载成功！", Toast.LENGTH_SHORT).show()
            binding.root
        } catch (e: Exception) {
            Log.e("DEBUG_SIGNUP", "SignupWizardFragment onCreateView FAILED: ${e.message}", e)
            Toast.makeText(context, "❌ SignupWizard 创建失败: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            Log.d("DEBUG_SIGNUP", "SignupWizardFragment onViewCreated")

            // 恢复保存的状态
            savedInstanceState?.let {
                username = it.getString("username", "")
                email = it.getString("email", "")
                nusnetId = it.getString("nusnetId", "")
                password = it.getString("password", "")
                currentStep = it.getInt("currentStep", 0)
                Log.d("DEBUG_SIGNUP", "Restored state - username: $username, step: $currentStep")
            }

            showPersonalInfo()
            Log.d("DEBUG_SIGNUP", "SignupWizardFragment personal info shown")
        } catch (e: Exception) {
            Log.e("DEBUG_SIGNUP", "SignupWizardFragment onViewCreated FAILED: ${e.message}", e)
            Toast.makeText(requireContext(), "SignupWizard 初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showPersonalInfo() {
        currentStep = 0

        // 显示个人信息界面
        binding.layoutPersonalInfo.visibility = View.VISIBLE
        binding.layoutFacultySelection.visibility = View.GONE
        binding.layoutTransportPreference.root.visibility = View.GONE
        binding.layoutCommonLocations.root.visibility = View.GONE
        binding.layoutInterestsGoals.root.visibility = View.GONE
        binding.layoutMascotReveal.visibility = View.GONE

        // 如果有保存的数据，恢复输入框的文本
        if (username.isNotEmpty()) {
            binding.inputUsername.setText(username)
        }
        if (email.isNotEmpty()) {
            binding.inputEmail.setText(email)
        }
        if (nusnetId.isNotEmpty()) {
            binding.inputNusnet.setText(nusnetId)
        }
        if (password.isNotEmpty()) {
            binding.inputPassword.setText(password)
            binding.inputConfirmPassword.setText(password)
        }

        // 输入验证
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePersonalInfo()
            }
        }

        binding.inputUsername.addTextChangedListener(textWatcher)
        binding.inputEmail.addTextChangedListener(textWatcher)
        binding.inputNusnet.addTextChangedListener(textWatcher)
        binding.inputPassword.addTextChangedListener(textWatcher)
        binding.inputConfirmPassword.addTextChangedListener(textWatcher)

        // Next 按钮
        binding.btnNextToFaculty.isEnabled = false
        binding.btnNextToFaculty.alpha = 0.5f
        binding.btnNextToFaculty.setOnClickListener {
            username = binding.inputUsername.text.toString()
            email = binding.inputEmail.text.toString()
            nusnetId = binding.inputNusnet.text.toString()
            password = binding.inputPassword.text.toString()
            Log.d("DEBUG_SIGNUP", "Next clicked - username: '$username', email: '$email'")
            showFacultySelection()
        }
    }
    
    private fun validatePersonalInfo() {
        val usernameText = binding.inputUsername.text.toString()
        val emailText = binding.inputEmail.text.toString()
        val nusnetText = binding.inputNusnet.text.toString()
        val passwordText = binding.inputPassword.text.toString()
        val confirmPasswordText = binding.inputConfirmPassword.text.toString()
        
        // 基本验证
        val isUsernameValid = usernameText.length >= 3
        val isEmailValid = emailText.contains("@") && emailText.contains(".")
        val isNusnetValid = nusnetText.startsWith("e", ignoreCase = true) && nusnetText.length >= 7
        val isPasswordValid = passwordText.length >= 6
        val isPasswordMatch = passwordText == confirmPasswordText && passwordText.isNotEmpty()
        
        val isValid = isUsernameValid && isEmailValid && isNusnetValid && isPasswordValid && isPasswordMatch
        
        binding.btnNextToFaculty.isEnabled = isValid
        binding.btnNextToFaculty.alpha = if (isValid) 1f else 0.5f
        
        // 错误提示
        if (usernameText.isNotEmpty() && !isUsernameValid) {
            binding.inputLayoutUsername.error = "Username must be at least 3 characters"
        } else {
            binding.inputLayoutUsername.error = null
        }
        
        if (emailText.isNotEmpty() && !isEmailValid) {
            binding.inputLayoutEmail.error = "Invalid email format"
        } else {
            binding.inputLayoutEmail.error = null
        }
        
        if (nusnetText.isNotEmpty() && !isNusnetValid) {
            binding.inputLayoutNusnet.error = "Must start with 'e' and be at least 7 characters"
        } else {
            binding.inputLayoutNusnet.error = null
        }
        
        if (passwordText.isNotEmpty() && !isPasswordValid) {
            binding.inputLayoutPassword.error = "Password must be at least 6 characters"
        } else {
            binding.inputLayoutPassword.error = null
        }
        
        if (confirmPasswordText.isNotEmpty() && !isPasswordMatch) {
            binding.inputLayoutConfirmPassword.error = "Passwords do not match"
        } else {
            binding.inputLayoutConfirmPassword.error = null
        }
    }
    
    private fun showFacultySelection() {
        currentStep = 1
        
        // 切换界面
        binding.layoutPersonalInfo.visibility = View.GONE
        binding.layoutFacultySelection.visibility = View.VISIBLE
        binding.layoutTransportPreference.root.visibility = View.GONE
        binding.layoutCommonLocations.root.visibility = View.GONE
        binding.layoutInterestsGoals.root.visibility = View.GONE
        binding.layoutMascotReveal.visibility = View.GONE
        
        // 设置ViewPager2适配器（传入viewPager引用用于左右按钮）
        val adapter = FacultySwipeAdapter(
            MockData.FACULTY_DATA,
            binding.viewpagerFaculties
        ) { faculty ->
            // 选择后跳转到交通偏好
            selectedFaculty = faculty
            android.util.Log.d("DEBUG_SIGNUP", "Faculty selected: ${faculty.name}")
            
            // 短暂延迟后自动跳转
            binding.viewpagerFaculties.postDelayed({
                showTransportPreference()
            }, 300)
        }
        
        binding.viewpagerFaculties.adapter = adapter
        
        // 设置ViewPager2的页面切换监听
        binding.viewpagerFaculties.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 更新页面指示器
                binding.textPageIndicator.text = "${position + 1} / ${MockData.FACULTY_DATA.size}"
            }
        })
        
        // 初始化页面指示器
        binding.textPageIndicator.text = "1 / ${MockData.FACULTY_DATA.size}"
        
        // 配置ViewPager2的页面转换效果
        setupPageTransformer()
    }
    
    private fun setupPageTransformer() {
        binding.viewpagerFaculties.apply {
            // 设置页面间距
            offscreenPageLimit = 1
            
            // 添加页面间距
            val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.pageMargin)
            val offsetPx = resources.getDimensionPixelOffset(R.dimen.offset)
            
            setPageTransformer { page, position ->
                val absPosition = abs(position)
                
                // 缩放效果
                page.scaleY = 0.85f + (1 - absPosition) * 0.15f
                page.scaleX = 0.85f + (1 - absPosition) * 0.15f
                
                // 透明度效果
                page.alpha = 0.5f + (1 - absPosition) * 0.5f
                
                // 轻微旋转效果
                page.rotationY = position * -15f
            }
        }
    }
    
    private fun showTransportPreference() {
        currentStep = 2
        
        // 切换界面
        binding.layoutPersonalInfo.visibility = View.GONE
        binding.layoutFacultySelection.visibility = View.GONE
        binding.layoutTransportPreference.root.visibility = View.VISIBLE
        binding.layoutCommonLocations.root.visibility = View.GONE
        binding.layoutInterestsGoals.root.visibility = View.GONE
        binding.layoutMascotReveal.visibility = View.GONE
        
        Log.d("DEBUG_SIGNUP", "Showing transport preference")
        
        // 设置交通卡片点击监听
        val cards = mapOf(
            binding.layoutTransportPreference.cardBus to Pair("bus", binding.layoutTransportPreference.checkBus),
            binding.layoutTransportPreference.cardWalking to Pair("walking", binding.layoutTransportPreference.checkWalking),
            binding.layoutTransportPreference.cardCycling to Pair("cycling", binding.layoutTransportPreference.checkCycling),
            binding.layoutTransportPreference.cardCarpool to Pair("carpool", binding.layoutTransportPreference.checkCarpool)
        )
        
        cards.forEach { (card, typeAndCheck) ->
            val (type, checkView) = typeAndCheck
            card.setOnClickListener {
                if (transportPrefs.contains(type)) {
                    transportPrefs.remove(type)
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.border)
                    checkView.visibility = View.GONE
                } else {
                    transportPrefs.add(type)
                    card.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)
                    checkView.visibility = View.VISIBLE
                }
                
                // 至少选择一个才能继续
                binding.layoutTransportPreference.btnContinueTransport.isEnabled = transportPrefs.isNotEmpty()
                binding.layoutTransportPreference.btnContinueTransport.alpha = if (transportPrefs.isNotEmpty()) 1f else 0.5f
            }
        }
        
        // Continue按钮
        binding.layoutTransportPreference.btnContinueTransport.isEnabled = false
        binding.layoutTransportPreference.btnContinueTransport.alpha = 0.5f
        binding.layoutTransportPreference.btnContinueTransport.setOnClickListener {
            Log.d("DEBUG_SIGNUP", "Transport prefs selected: ${transportPrefs.joinToString(", ")}")
            showCommonLocations()
        }
    }
    
    private fun showCommonLocations() {
        currentStep = 3
        
        // 切换界面
        binding.layoutPersonalInfo.visibility = View.GONE
        binding.layoutFacultySelection.visibility = View.GONE
        binding.layoutTransportPreference.root.visibility = View.GONE
        binding.layoutCommonLocations.root.visibility = View.VISIBLE
        binding.layoutInterestsGoals.root.visibility = View.GONE
        binding.layoutMascotReveal.visibility = View.GONE
        
        Log.d("DEBUG_SIGNUP", "Showing common locations")
        
        // Chip监听
        binding.layoutCommonLocations.chipGym.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) otherLocations.add("gym") else otherLocations.remove("gym")
        }
        binding.layoutCommonLocations.chipCanteen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) otherLocations.add("canteen") else otherLocations.remove("canteen")
        }
        binding.layoutCommonLocations.chipLab.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) otherLocations.add("lab") else otherLocations.remove("lab")
        }
        binding.layoutCommonLocations.chipSports.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) otherLocations.add("sports") else otherLocations.remove("sports")
        }
        
        // Skip按钮
        binding.layoutCommonLocations.btnSkipLocations.setOnClickListener {
            Log.d("DEBUG_SIGNUP", "Locations skipped")
            showInterestsGoals()
        }
        
        // Continue按钮
        binding.layoutCommonLocations.btnContinueLocations.setOnClickListener {
            dormitory = binding.layoutCommonLocations.inputDorm.text.toString()
            teachingBuilding = binding.layoutCommonLocations.inputBuilding.text.toString()
            studySpot = binding.layoutCommonLocations.inputLibrary.text.toString()
            Log.d("DEBUG_SIGNUP", "Locations: dorm=$dormitory, building=$teachingBuilding, spot=$studySpot")
            showInterestsGoals()
        }
    }
    
    private fun showInterestsGoals() {
        currentStep = 4
        
        // 切换界面
        binding.layoutPersonalInfo.visibility = View.GONE
        binding.layoutFacultySelection.visibility = View.GONE
        binding.layoutTransportPreference.root.visibility = View.GONE
        binding.layoutCommonLocations.root.visibility = View.GONE
        binding.layoutInterestsGoals.root.visibility = View.VISIBLE
        binding.layoutMascotReveal.visibility = View.GONE
        
        Log.d("DEBUG_SIGNUP", "Showing interests and goals")
        
        // 兴趣Chips监听
        binding.layoutInterestsGoals.chipSustainability.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add("sustainability") else interests.remove("sustainability")
        }
        binding.layoutInterestsGoals.chipChallenges.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add("challenges") else interests.remove("challenges")
        }
        binding.layoutInterestsGoals.chipCommunity.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add("community") else interests.remove("community")
        }
        binding.layoutInterestsGoals.chipRewards.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add("rewards") else interests.remove("rewards")
        }
        binding.layoutInterestsGoals.chipLeaderboard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) interests.add("leaderboard") else interests.remove("leaderboard")
        }
        
        // 每周目标Slider
        binding.layoutInterestsGoals.sliderWeeklyGoal.addOnChangeListener { _, value, _ ->
            weeklyGoal = value.toInt()
            binding.layoutInterestsGoals.textGoalValue.text = weeklyGoal.toString()
        }
        
        // 通知开关
        binding.layoutInterestsGoals.switchChallenges.setOnCheckedChangeListener { _, isChecked ->
            notifyChallenges = isChecked
        }
        binding.layoutInterestsGoals.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            notifyReminders = isChecked
        }
        binding.layoutInterestsGoals.switchFriends.setOnCheckedChangeListener { _, isChecked ->
            notifyFriends = isChecked
        }
        
        // Finish按钮
        binding.layoutInterestsGoals.btnFinishSignup.setOnClickListener {
            Log.d("DEBUG_SIGNUP", "Interests: ${interests.joinToString(", ")}, Goal: $weeklyGoal")
            selectedFaculty?.let { faculty ->
                showMascotReveal(faculty)
            }
        }
    }
    
    private fun showMascotReveal(faculty: FacultyData) {
        currentStep = 5
        
        // 切换到小狮子展示界面
        binding.layoutPersonalInfo.visibility = View.GONE
        binding.layoutFacultySelection.visibility = View.GONE
        binding.layoutTransportPreference.root.visibility = View.GONE
        binding.layoutCommonLocations.root.visibility = View.GONE
        binding.layoutInterestsGoals.root.visibility = View.GONE
        binding.layoutMascotReveal.visibility = View.VISIBLE
        
        binding.textRevealTitle.text = "Welcome, $username!"
        binding.textRevealSubtitle.text = "Meet your new buddy."
        
        // 设置小狮子装备
        binding.mascotReveal.outfit = faculty.outfit
        
        // 入场动画
        mascotScaleAnimator = ValueAnimator.ofFloat(0.5f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                binding.mascotReveal.scaleX = scale
                binding.mascotReveal.scaleY = scale
            }
        }
        mascotScaleAnimator?.start()
        
        // 轻微旋转动画
        mascotRotateAnimator = ValueAnimator.ofFloat(-5f, 5f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                binding.mascotReveal.rotation = animation.animatedValue as Float
            }
        }
        mascotRotateAnimator?.start()
        
        // 学院信息
        binding.textFacultyName.text = faculty.name
        binding.textFacultySlogan.text = faculty.slogan
        binding.viewFacultyColor.setBackgroundColor(android.graphics.Color.parseColor(faculty.color))
        
        // 服装预览
        val outfitItems = mutableListOf<String>()
        if (faculty.outfit.head != "none") {
            outfitItems.add(getItemName(faculty.outfit.head))
        }
        if (faculty.outfit.face != "none") {
            outfitItems.add(getItemName(faculty.outfit.face))
        }
        if (faculty.outfit.body != "none") {
            outfitItems.add(getItemName(faculty.outfit.body))
        }
        binding.textOutfitItems.text = "Starter Outfit: ${outfitItems.joinToString(", ")}"
        
        // Let's Go! 按钮动画
        buttonAnimator = ValueAnimator.ofFloat(1f, 1.05f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                binding.btnLetsGo.scaleX = scale
                binding.btnLetsGo.scaleY = scale
            }
        }
        buttonAnimator?.start()
        
        binding.btnLetsGo.setOnClickListener {
            Log.d("DEBUG_SIGNUP", "Let's Go button clicked!")
            Toast.makeText(requireContext(), "🎯 Let's Go 按钮已点击", Toast.LENGTH_SHORT).show()
            
            // 停止所有动画
            buttonAnimator?.cancel()
            mascotScaleAnimator?.cancel()
            mascotRotateAnimator?.cancel()
            
            // 重置按钮缩放
            binding.btnLetsGo.scaleX = 1f
            binding.btnLetsGo.scaleY = 1f
            
            completeSignup(faculty)
        }
    }
    
    private fun completeSignup(faculty: FacultyData) {
        // 验证必填字段
        if (username.isBlank()) {
            Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
            Log.e("DEBUG_SIGNUP", "Username is empty")
            return
        }
        if (email.isBlank()) {
            Toast.makeText(requireContext(), "邮箱不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (nusnetId.isBlank()) {
            Toast.makeText(requireContext(), "NUSNET ID 不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DEBUG_SIGNUP", "=== Complete Registration Data ===")
        Log.d("DEBUG_SIGNUP", "Username: $username")
        Log.d("DEBUG_SIGNUP", "Email: $email")
        Log.d("DEBUG_SIGNUP", "NUSNET: $nusnetId")
        Log.d("DEBUG_SIGNUP", "Password saved: ${password.isNotEmpty()}")
        Log.d("DEBUG_SIGNUP", "Faculty: ${faculty.name}")
        Log.d("DEBUG_SIGNUP", "Transport: ${transportPrefs.joinToString(", ")}")
        Log.d("DEBUG_SIGNUP", "Dorm: $dormitory")
        Log.d("DEBUG_SIGNUP", "Building: $teachingBuilding")
        Log.d("DEBUG_SIGNUP", "Study Spot: $studySpot")
        Log.d("DEBUG_SIGNUP", "Other Locations: ${otherLocations.joinToString(", ")}")
        Log.d("DEBUG_SIGNUP", "Interests: ${interests.joinToString(", ")}")
        Log.d("DEBUG_SIGNUP", "Weekly Goal: $weeklyGoal")
        Log.d("DEBUG_SIGNUP", "Notifications: challenges=$notifyChallenges, reminders=$notifyReminders, friends=$notifyFriends")

        // 使用 API 保存注册数据（异步，导航由 API 回调处理）
        saveRegistrationData()
    }
    
    private fun saveRegistrationData() {
        // 使用 API 进行注册
        lifecycleScope.launch {
            Toast.makeText(requireContext(), "正在注册...", Toast.LENGTH_SHORT).show()

            val repository = EcoGoRepository()
            val result = repository.register(
                username = username,
                email = email,
                nusnetId = nusnetId,
                password = password,
                faculty = selectedFaculty?.id,
                transportPreferences = transportPrefs.toList(),
                interests = interests.toList(),
                weeklyGoal = weeklyGoal
            )

            result.onSuccess { registerResponse ->
                Log.d("DEBUG_SIGNUP", "Registration API successful: userid=${registerResponse.userid}")
                Log.d("DEBUG_SIGNUP", "RegisterResponse: $registerResponse")

                // 保存用户信息（暂时使用注册时输入的 email 和 username）
                TokenManager.saveToken(
                    "", // 后端不返回 token，需要通过单独的登录接口获取
                    registerResponse.userid,
                    registerResponse.nickname
                )

                // 标记首次登录（用于特定用户）
                val prefs = requireContext().getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_first_login_${registerResponse.userid}", true)
                    .apply()

                Toast.makeText(requireContext(), "注册成功！请使用注册的昵称和密码登录", Toast.LENGTH_SHORT).show()

                // 弹出返回栈返回到登录页面
                try {
                    findNavController().popBackStack()
                    Log.d("DEBUG_SIGNUP", "Navigated back to login successfully")
                } catch (e: Exception) {
                    Log.e("DEBUG_SIGNUP", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "注册成功，请返回登录页面", Toast.LENGTH_LONG).show()
                }
            }

            result.onFailure { error ->
                Log.e("DEBUG_SIGNUP", "Registration API failed: ${error.message}", error)
                Toast.makeText(
                    requireContext(),
                    "注册失败: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun getItemName(itemId: String): String {
        return when (itemId) {
            "hat_helmet" -> "Safety Helmet"
            "hat_beret" -> "Artist Beret"
            "hat_grad" -> "Grad Cap"
            "hat_cap" -> "Orange Cap"
            "face_goggles" -> "Safety Goggles"
            "glasses_sun" -> "Shades"
            "body_plaid" -> "Engin Plaid"
            "body_suit" -> "Biz Suit"
            "body_coat" -> "Lab Coat"
            "shirt_nus" -> "NUS Tee"
            "shirt_hoodie" -> "Blue Hoodie"
            else -> ""
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存当前填写的数据
        outState.putString("username", username)
        outState.putString("email", email)
        outState.putString("nusnetId", nusnetId)
        outState.putString("password", password)
        outState.putInt("currentStep", currentStep)
        Log.d("DEBUG_SIGNUP", "Saved state - username: $username")
    }

    override fun onDestroyView() {
        // 清理动画
        buttonAnimator?.cancel()
        mascotScaleAnimator?.cancel()
        mascotRotateAnimator?.cancel()

        super.onDestroyView()
        _binding = null
    }
}
