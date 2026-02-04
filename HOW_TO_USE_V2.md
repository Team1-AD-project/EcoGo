# 如何启动Banner新版本（V2）

## 🎯 目标
启动带有Banner轮播功能的新版首页（HomeFragmentV2）

---

## ✅ 已完成：设置V2为默认版本

我已经将 `bottom_nav_menu.xml` 修改为默认使用V2版本：

```xml
<item
    android:id="@+id/homeFragmentV2"  <!-- 改为V2 -->
    android:icon="@drawable/ic_navigation"
    android:title="@string/nav_home" />
```

---

## 🚀 现在启动应用

### 步骤1：构建应用

在Android Studio中：
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**

或使用命令行：
```bash
cd android-app
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### 步骤2：安装到设备

在Android Studio中：
- 点击 **Run** 按钮 ▶️

或使用命令行：
```bash
.\gradlew.bat installDebug
```

### 步骤3：验证Banner功能

打开应用后，您应该在首页顶部看到：
- ✨ **轮播Banner**（3张卡片）
- 🔵 **圆点指示器**（显示当前位置）
- 🔄 **自动切换**（每4秒）
- 👆 **可手动滑动**

---

## 🎨 Banner内容

默认显示3个Banner：

### Banner 1 - 绿色
- 标题："New Challenge Available!"
- 副标题："Complete 10K steps today"
- 按钮："View" → 跳转到挑战页

### Banner 2 - 橙色
- 标题："Limited Time Offer"
- 副标题："50% off selected vouchers"
- 按钮："Shop Now" → 跳转到优惠券页

### Banner 3 - 蓝色
- 标题："Welcome to EcoGo!"
- 副标题："Start your eco-friendly journey today"
- 无按钮（仅展示）

---

## 🔄 切换回V1版本

如果您想切换回原版（无Banner）：

### 方法1：修改菜单配置
将 `bottom_nav_menu.xml` 改回：
```xml
<item
    android:id="@+id/homeFragment"  <!-- V1 -->
    ...
/>
```

### 方法2：应用内切换
1. 长按底部导航栏
2. 看到Toast提示切换成功
3. 重启应用

---

## 📊 两个版本对比

| 特性 | V1 (原版) | V2 (新版) |
|------|-----------|-----------|
| Banner轮播 | ❌ | ✅ |
| 自动切换 | ❌ | ✅ (4秒) |
| 指示器 | ❌ | ✅ |
| 其他功能 | ✅ | ✅ (完全保留) |
| 性能 | 原始 | 略有增加 |

---

## 🐛 常见问题

### Q: 看不到Banner？
**A**: 确保您：
1. 已经Clean和Rebuild项目
2. 完全卸载旧版本再安装
3. 检查 `bottom_nav_menu.xml` 是否指向 `homeFragmentV2`

### Q: Banner不自动切换？
**A**: 
- 手动滑动后会停止自动切换（这是设计行为）
- 退出并重新进入首页即可恢复自动切换

### Q: 点击Banner没反应？
**A**: 检查导航配置：
- `nav_graph.xml` 中是否有对应的destination
- 例如：`challengesFragment`、`voucherFragment` 等

### Q: 应用卡顿？
**A**: 可能的优化：
1. 减少Banner数量（从3个改为2个）
2. 增加自动切换间隔（从4秒改为6秒）
3. 禁用某些动画

---

## 🎯 快速验证清单

启动应用后检查：
- [ ] 首页显示Banner轮播
- [ ] Banner每4秒自动切换
- [ ] 可以手动左右滑动
- [ ] 圆点指示器正确显示
- [ ] 点击"View"按钮跳转到挑战页
- [ ] 点击"Shop Now"按钮跳转到优惠券页
- [ ] 原有功能（巴士、天气、积分等）正常显示

---

## 📞 需要帮助？

如果遇到问题：
1. 查看Logcat错误日志
2. 检查 `BANNER_IMPLEMENTATION_COMPLETE.md` 的测试清单
3. 确认所有文件都已正确创建

---

**状态**: ✅ V2已设为默认版本

**下一步**: Clean → Rebuild → Run → 验证功能
