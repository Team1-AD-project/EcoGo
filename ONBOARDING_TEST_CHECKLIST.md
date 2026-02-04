# ✅ 引导页导航测试清单

## 快速测试步骤

### 🧪 测试1: 首次注册和登录流程

**步骤**:
1. ✅ 打开应用，点击"Register"
2. ✅ 完成6步注册流程：
   - Step 0: 填写个人信息（用户名、邮箱、NUSNET ID、密码）
   - Step 1: 选择学院（滑动卡片）
   - Step 2: 选择交通方式
   - Step 3: 填写常用地点
   - Step 4: 选择兴趣和目标
   - Step 5: 查看吉祥物
3. ✅ 注册完成后，应该**自动跳转到登录页面**
4. ✅ 输入刚注册的NUSNET ID和密码
5. ✅ 点击"Sign In"
6. ✅ 应该显示**5页引导页**（功能介绍）
7. ✅ 可以左右滑动查看，或点击"Next"
8. ✅ 在最后一页点击"Get Started"
9. ✅ 应该**成功跳转到主页（Home）**
10. ✅ 底部导航栏应该显示

**预期结果**: ✅ 所有步骤顺利完成，最后到达主页

---

### 🧪 测试2: 跳过引导页

**步骤**:
1-5. 同测试1
6. ✅ 看到引导页后，点击右上角"Skip"按钮
7. ✅ 应该**直接跳转到主页**

**预期结果**: ✅ 跳过引导页，直接到达主页

---

### 🧪 测试3: 验证标志清除（重要！）

**步骤**:
1. ✅ 完成测试1或测试2（引导页已显示过）
2. ✅ 返回登录页（可以重启应用）
3. ✅ 重新登录
4. ✅ **不应该再显示引导页**，应该直接到达主页

**预期结果**: ✅ 引导页只显示一次，再次登录直接进入主页

---

### 🧪 测试4: 导航栈验证

**步骤**:
1. ✅ 完成引导页后到达主页
2. ✅ 按设备的"返回键"
3. ✅ 检查行为

**预期结果**: 
- ✅ 方案A: 退出应用（推荐）
- ✅ 方案B: 留在主页

**不应该**: ❌ 返回到引导页或登录页

---

## 🐛 如果出现问题

### 问题：引导页完成后无法跳转

**检查**:
1. 查看Logcat日志，搜索"DEBUG_ONBOARDING"
2. 应该看到：
   ```
   DEBUG_ONBOARDING: Next button clicked - completing onboarding
   DEBUG_ONBOARDING: is_first_login flag cleared
   DEBUG_ONBOARDING: Navigate to home completed successfully
   ```
3. 如果看到错误日志，记录错误信息

**可能原因**:
- ❌ Navigation graph配置错误
- ❌ HomeFragment不存在或崩溃
- ❌ 权限或资源问题

---

### 问题：每次登录都显示引导页

**检查**:
1. 查看SharedPreferences值：
   ```kotlin
   val prefs = getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
   val isFirstLogin = prefs.getBoolean("is_first_login", false)
   Log.d("TEST", "is_first_login = $isFirstLogin")
   ```
2. 引导页完成后，`is_first_login`应该是`false`

**可能原因**:
- ❌ OnboardingFragment没有正确清除标志
- ❌ SharedPreferences写入失败

---

### 问题：登录后直接进入主页（跳过引导页）

**检查**:
1. 这可能不是问题，而是因为之前已经完成过引导页
2. 检查`is_first_login`的值
3. 如果是全新注册，应该显示引导页

**解决**:
- 卸载应用重新安装（清除所有数据）
- 或手动删除SharedPreferences

---

## 📱 实际操作注意事项

1. **使用真实数据**:
   - 用户名: 至少3个字符
   - 邮箱: 有效的邮箱格式（例如: test@u.nus.edu）
   - NUSNET ID: 以E开头（例如: E1234567）
   - 密码: 至少6个字符

2. **滑动卡片**:
   - 可以左右滑动
   - 可以点击左右按钮
   - 点击卡片直接选择

3. **引导页**:
   - 共5页内容
   - 可以左右滑动
   - 最后一页按钮文字变为"Get Started"

4. **日志查看**:
   - 打开Android Studio的Logcat
   - 过滤"DEBUG_"查看所有调试日志
   - 或者搜索具体的：DEBUG_SIGNUP, DEBUG_LOGIN, DEBUG_ONBOARDING

---

## ✅ 测试通过标准

**所有测试通过的标志**:
- ✅ 能完成完整注册流程
- ✅ 注册后能跳转到登录页
- ✅ 首次登录显示引导页
- ✅ 引导页完成后跳转到主页
- ✅ 主页正常显示，底部导航栏可用
- ✅ 再次登录不显示引导页
- ✅ 返回键不会回到引导页或登录页

---

**测试愉快！** 🚀
