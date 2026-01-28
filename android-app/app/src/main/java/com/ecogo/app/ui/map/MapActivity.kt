package com.ecogo.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ecogo.app.R
import com.ecogo.app.databinding.ActivityMapBinding
import com.ecogo.app.service.DirectionsService
import com.ecogo.app.service.LocationManager
import com.ecogo.app.service.LocationTrackingService
import com.ecogo.app.service.NavigationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

/**
 * 地图主页面
 * 实现 Google Maps 集成、行程追踪、路线推荐、地点搜索
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: MapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 地图标记
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null

    // 实时轨迹
    private var trackPolyline: Polyline? = null
    private var isFollowingUser = true  // 是否跟随用户位置

    // 导航路线（已走/未走）
    private var traveledPolyline: Polyline? = null    // 已走过的路线（灰色）
    private var remainingPolyline: Polyline? = null   // 剩余路线（蓝色）
    private var isNavigationMode = false              // 是否在导航模式

    // 起点和终点位置
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var originName: String = "我的位置"
    private var destinationName: String = ""

    // 标记当前搜索的是起点还是终点
    private var isSearchingOrigin = false

    companion object {
        private const val TAG = "MapActivity"
    }

    // 定位权限请求
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                enableMyLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 通知权限请求 (Android 13+)
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "需要通知权限来显示追踪状态", Toast.LENGTH_SHORT).show()
        }
    }

    // Places Autocomplete 启动器
    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleAutocompleteResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 Places SDK
        initPlaces()

        // 初始化 Directions API
        DirectionsService.init(this)

        // 初始化定位客户端
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 初始化地图
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
        observeViewModel()
        observeLocationManager()
        observeNavigationManager()

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()
    }

    /**
     * 请求通知权限
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 初始化 Places SDK
     */
    private fun initPlaces() {
        if (!Places.isInitialized()) {
            // 从 AndroidManifest.xml 获取 API Key
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            if (apiKey.isNotEmpty()) {
                Places.initialize(applicationContext, apiKey)
                Log.d(TAG, "Places SDK initialized")
            } else {
                Log.e(TAG, "Google Maps API Key not found")
            }
        }
    }

    /**
     * 设置 UI 事件监听
     */
    private fun setupUI() {
        // 起点输入框点击
        binding.etOrigin.setOnClickListener {
            isSearchingOrigin = true
            launchPlaceAutocomplete()
        }

        // 终点输入框点击
        binding.etDestination.setOnClickListener {
            isSearchingOrigin = false
            launchPlaceAutocomplete()
        }

        // 交换起点终点按钮
        binding.btnSwap.setOnClickListener {
            swapOriginAndDestination()
        }

        // 低碳路线按钮
        binding.btnLowCarbon.setOnClickListener {
            if (destinationLatLng != null) {
                viewModel.fetchLowCarbonRoute()
            } else {
                Toast.makeText(this, "请先选择目的地", Toast.LENGTH_SHORT).show()
            }
        }

        // 平衡路线按钮
        binding.btnBalance.setOnClickListener {
            if (destinationLatLng != null) {
                viewModel.fetchBalancedRoute()
            } else {
                Toast.makeText(this, "请先选择目的地", Toast.LENGTH_SHORT).show()
            }
        }

        // 行程追踪按钮
        binding.btnTracking.setOnClickListener {
            when (viewModel.tripState.value) {
                is TripState.Idle, is TripState.Completed -> {
                    // 检查是否有路线
                    val hasRoute = !viewModel.routePoints.value.isNullOrEmpty()
                    if (!hasRoute) {
                        // 提示用户先获取路线
                        Toast.makeText(
                            this,
                            "提示：请先点击\"低碳路线\"或\"平衡路线\"获取导航路线",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    startLocationTracking()
                    viewModel.startTracking()
                }
                is TripState.Tracking -> {
                    stopLocationTracking()
                    viewModel.stopTracking()
                }
                else -> { /* 忽略其他状态 */ }
            }
        }

        // 定位按钮
        binding.fabMyLocation.setOnClickListener {
            isFollowingUser = true
            moveToCurrentLocation()
            // 重置起点为当前位置
            resetOriginToMyLocation()
        }
    }

    /**
     * 启动位置追踪服务
     */
    private fun startLocationTracking() {
        Log.d(TAG, "Starting location tracking service")
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // 检查是否有路线，如果有则进入导航模式
        val routePoints = viewModel.routePoints.value
        if (!routePoints.isNullOrEmpty()) {
            isNavigationMode = true
            NavigationManager.setRoute(routePoints)
            NavigationManager.startNavigation()

            // 隐藏原始路线，改用导航路线显示
            routePolyline?.remove()
            routePolyline = null

            Log.d(TAG, "Navigation mode started with ${routePoints.size} points")
        } else {
            isNavigationMode = false
            Log.d(TAG, "Track recording mode started (no route)")
        }

        // 清除之前的轨迹
        trackPolyline?.remove()
        trackPolyline = null
        traveledPolyline?.remove()
        traveledPolyline = null
        remainingPolyline?.remove()
        remainingPolyline = null

        isFollowingUser = true
    }

    /**
     * 停止位置追踪服务
     */
    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking service")
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        startService(intent)

        // 停止导航
        if (isNavigationMode) {
            NavigationManager.stopNavigation()
            isNavigationMode = false

            // 清除导航路线
            traveledPolyline?.remove()
            traveledPolyline = null
            remainingPolyline?.remove()
            remainingPolyline = null
        }
    }

    /**
     * 观察 LocationManager 的位置更新
     */
    private fun observeLocationManager() {
        // 观察当前位置
        LocationManager.currentLocation.observe(this) { latLng ->
            // 更新 ViewModel
            viewModel.updateCurrentLocation(latLng)

            // 如果正在追踪且开启了跟随模式，移动相机
            if (LocationManager.isTracking.value == true && isFollowingUser) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }

        // 观察轨迹点
        LocationManager.trackPoints.observe(this) { points ->
            if (points.isNotEmpty()) {
                drawTrackPolyline(points)
            }
        }

        // 观察总距离
        LocationManager.totalDistance.observe(this) { distance ->
            if (LocationManager.isTracking.value == true && !isNavigationMode) {
                updateTrackingInfo(distance)
            }
        }
    }

    /**
     * 观察 NavigationManager 的导航状态
     */
    private fun observeNavigationManager() {
        // 观察已走过的路线
        NavigationManager.traveledPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                drawTraveledRoute(points)
            }
        }

        // 观察剩余路线
        NavigationManager.remainingPoints.observe(this) { points ->
            if (isNavigationMode && points.isNotEmpty()) {
                drawRemainingRoute(points)
            }
        }

        // 观察已行进距离
        NavigationManager.traveledDistance.observe(this) { distance ->
            if (isNavigationMode) {
                updateNavigationInfo(distance)
            }
        }

        // 观察是否到达目的地
        NavigationManager.currentRouteIndex.observe(this) { _ ->
            if (NavigationManager.hasReachedDestination()) {
                onReachedDestination()
            }
        }
    }

    /**
     * 绘制已走过的路线（灰色）
     */
    private fun drawTraveledRoute(points: List<LatLng>) {
        traveledPolyline?.remove()

        if (points.size < 2) return

        traveledPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.route_traveled))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * 绘制剩余路线（蓝色）
     */
    private fun drawRemainingRoute(points: List<LatLng>) {
        remainingPolyline?.remove()

        if (points.size < 2) return

        remainingPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.route_remaining))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * 更新导航信息显示
     */
    private fun updateNavigationInfo(traveledMeters: Float) {
        val traveledKm = traveledMeters / 1000f
        val remainingMeters = NavigationManager.remainingDistance.value ?: 0f
        val remainingKm = remainingMeters / 1000f

        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            binding.tvCarbonSaved.text = String.format("已行进: %.2f 公里", traveledKm)
            binding.tvDuration.text = String.format("剩余: %.2f 公里", remainingKm)
        }
    }

    /**
     * 到达目的地
     */
    private fun onReachedDestination() {
        Toast.makeText(this, "您已到达目的地！", Toast.LENGTH_LONG).show()
        // 可以自动停止导航
    }

    /**
     * 绘制实时轨迹
     */
    private fun drawTrackPolyline(points: List<LatLng>) {
        trackPolyline?.remove()

        if (points.size < 2) return

        trackPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(ContextCompat.getColor(this, R.color.green_primary))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
    }

    /**
     * 更新追踪信息显示
     */
    private fun updateTrackingInfo(distanceMeters: Float) {
        val distanceKm = distanceMeters / 1000f
        // 可以在路线信息卡片显示实时距离
        if (binding.cardRouteInfo.visibility == View.VISIBLE) {
            binding.tvCarbonSaved.text = String.format("已行进: %.2f 公里", distanceKm)
        }
    }

    /**
     * 启动 Places Autocomplete
     */
    private fun launchPlaceAutocomplete() {
        try {
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this)

            autocompleteLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching autocomplete: ${e.message}")
            Toast.makeText(this, "搜索服务暂不可用", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理 Autocomplete 返回结果
     */
    private fun handleAutocompleteResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    val latLng = place.latLng

                    if (latLng != null) {
                        if (isSearchingOrigin) {
                            // 设置起点
                            originLatLng = latLng
                            originName = place.name ?: place.address ?: "起点"
                            binding.etOrigin.setText(originName)
                            updateOriginMarker(latLng, originName)
                            viewModel.setOrigin(latLng)  // 使用 setOrigin 而不是 updateCurrentLocation
                        } else {
                            // 设置终点
                            destinationLatLng = latLng
                            destinationName = place.name ?: place.address ?: "目的地"
                            binding.etDestination.setText(destinationName)
                            updateDestinationMarker(latLng, destinationName)
                            viewModel.setDestination(latLng)
                        }

                        // 移动相机到选择的位置
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        // 如果起点和终点都已设置，调整相机显示两点
                        fitBoundsIfReady()
                    }
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Log.e(TAG, "Autocomplete error: ${status.statusMessage}")
                    Toast.makeText(this, "搜索出错: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Autocomplete canceled")
            }
        }
    }

    /**
     * 交换起点和终点
     */
    private fun swapOriginAndDestination() {
        // 交换位置
        val tempLatLng = originLatLng
        val tempName = originName

        originLatLng = destinationLatLng
        originName = destinationName

        destinationLatLng = tempLatLng
        destinationName = tempName

        // 更新 UI
        binding.etOrigin.setText(if (originLatLng != null) originName else "我的位置")
        binding.etDestination.setText(destinationName)

        // 更新标记
        originLatLng?.let {
            updateOriginMarker(it, originName)
            viewModel.setOrigin(it)  // 交换后更新起点
        }
        destinationLatLng?.let {
            updateDestinationMarker(it, destinationName)
            viewModel.setDestination(it)
        }

        // 清除路线
        routePolyline?.remove()
        routePolyline = null
        binding.cardRouteInfo.visibility = View.GONE
    }

    /**
     * 重置起点为当前位置
     */
    @SuppressLint("MissingPermission")
    private fun resetOriginToMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                originName = "我的位置"
                binding.etOrigin.setText(originName)
                originMarker?.remove()
                originMarker = null
                viewModel.setOrigin(latLng)  // 重置起点为当前位置
            }
        }
    }

    /**
     * 如果起点和终点都设置了，调整相机显示两点
     */
    private fun fitBoundsIfReady() {
        val origin = originLatLng ?: viewModel.currentLocation.value
        val destination = destinationLatLng

        if (origin != null && destination != null) {
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(origin)
            boundsBuilder.include(destination)
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        }
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        // 观察当前位置
        viewModel.currentLocation.observe(this) { location ->
            // 如果起点是"我的位置"，更新起点
            if (originName == "我的位置" && originLatLng == null) {
                originLatLng = location
            }
        }

        // 观察目的地
        viewModel.destination.observe(this) { destination ->
            destination?.let {
                destinationLatLng = it
            }
        }

        // 观察行程状态
        viewModel.tripState.observe(this) { state ->
            updateTrackingUI(state)
        }

        // 观察推荐路线
        viewModel.recommendedRoute.observe(this) { route ->
            route?.let { updateRouteInfo(it) }
        }

        // 观察路线点
        viewModel.routePoints.observe(this) { points ->
            drawRoute(points)
        }

        // 观察碳足迹结果
        viewModel.carbonResult.observe(this) { result ->
            result?.let {
                val carbonSavedStr = String.format("%.2f", it.carbon_saved)
                val message = if (it.is_green_trip) {
                    "绿色出行! 减碳 $carbonSavedStr kg，获得 ${it.green_points} 积分"
                } else {
                    "行程完成，减碳 $carbonSavedStr kg"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误消息
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // 观察成功消息
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    /**
     * 地图准备就绪回调
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 配置地图
        map.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = false

            // 地图点击也可以设置目的地
            setOnMapClickListener { latLng ->
                // 如果正在追踪，禁止修改目的地
                if (viewModel.tripState.value is TripState.Tracking) {
                    return@setOnMapClickListener
                }

                destinationLatLng = latLng
                destinationName = "地图上的位置"
                binding.etDestination.setText(destinationName)
                updateDestinationMarker(latLng, destinationName)
                viewModel.setDestination(latLng)
                fitBoundsIfReady()
            }

            // 长按清除目的地
            setOnMapLongClickListener {
                if (viewModel.tripState.value !is TripState.Tracking) {
                    clearDestination()
                }
            }

            // 地图移动时停止跟随
            setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isFollowingUser = false
                }
            }
        }

        // 请求定位权限
        checkLocationPermission()
    }

    /**
     * 清除目的地
     */
    private fun clearDestination() {
        destinationLatLng = null
        destinationName = ""
        binding.etDestination.setText("")
        destinationMarker?.remove()
        destinationMarker = null
        routePolyline?.remove()
        routePolyline = null
        binding.cardRouteInfo.visibility = View.GONE
        viewModel.clearDestination()
    }

    /**
     * 检查定位权限
     */
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    /**
     * 启用我的位置图层
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false

        // 获取当前位置
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                originLatLng = latLng
                viewModel.updateCurrentLocation(latLng)
                moveToCurrentLocation()
            }
        }
    }

    /**
     * 移动相机到当前位置
     */
    private fun moveToCurrentLocation() {
        val location = LocationManager.currentLocation.value
            ?: originLatLng
            ?: viewModel.currentLocation.value

        location?.let {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    /**
     * 更新起点标记
     */
    private fun updateOriginMarker(location: LatLng, title: String) {
        originMarker?.remove()
        originMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
    }

    /**
     * 更新目的地标记
     */
    private fun updateDestinationMarker(location: LatLng, title: String) {
        destinationMarker?.remove()
        destinationMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    /**
     * 绘制路线（推荐路线预览，蓝色）
     */
    private fun drawRoute(points: List<LatLng>) {
        routePolyline?.remove()

        if (points.isEmpty()) return

        // 使用蓝色显示推荐路线（与百度/谷歌地图一致）
        routePolyline = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.route_remaining))
                .geodesic(true)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        // 调整相机显示完整路线
        if (points.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    /**
     * 更新路线信息卡片
     */
    private fun updateRouteInfo(route: com.ecogo.app.data.model.RouteRecommendData) {
        binding.cardRouteInfo.visibility = View.VISIBLE

        // 路线类型
        val routeTypeText = when (route.route_type) {
            "low_carbon" -> "低碳路线"
            "balanced" -> "平衡路线"
            else -> "推荐路线"
        }
        binding.tvRouteType.text = routeTypeText

        // 碳减排
        val carbonSavedText = String.format("减碳: %.2f kg", route.carbon_saved)
        binding.tvCarbonSaved.text = carbonSavedText

        // 预计时间 (使用新字段 estimated_duration，兼容旧字段 duration)
        val durationMinutes = route.estimated_duration.takeIf { it > 0 } ?: route.duration ?: 0
        val durationText = "预计: $durationMinutes 分钟"
        binding.tvDuration.text = durationText
    }

    /**
     * 更新行程追踪 UI
     */
    private fun updateTrackingUI(state: TripState) {
        when (state) {
            is TripState.Idle -> {
                binding.btnTracking.text = getString(R.string.start_tracking)
                binding.btnTracking.isEnabled = true
                binding.layoutRouteButtons.visibility = View.VISIBLE
                binding.cardSearch.visibility = View.VISIBLE
                // 清除追踪轨迹
                trackPolyline?.remove()
                trackPolyline = null
            }
            is TripState.Starting -> {
                binding.btnTracking.text = "正在开始..."
                binding.btnTracking.isEnabled = false
            }
            is TripState.Tracking -> {
                binding.btnTracking.text = getString(R.string.stop_tracking)
                binding.btnTracking.isEnabled = true
                binding.layoutRouteButtons.visibility = View.GONE
                binding.cardSearch.visibility = View.GONE
                // 显示追踪信息卡片
                binding.cardRouteInfo.visibility = View.VISIBLE

                if (isNavigationMode) {
                    // 导航模式
                    binding.tvRouteType.text = "导航中"
                    binding.tvCarbonSaved.text = "已行进: 0.00 公里"
                    val remainingKm = (NavigationManager.remainingDistance.value ?: 0f) / 1000f
                    binding.tvDuration.text = String.format("剩余: %.2f 公里", remainingKm)
                } else {
                    // 纯轨迹记录模式
                    binding.tvRouteType.text = "行程追踪中"
                    binding.tvCarbonSaved.text = "已行进: 0.00 公里"
                    binding.tvDuration.text = "实时记录GPS轨迹"
                }
            }
            is TripState.Stopping -> {
                binding.btnTracking.text = "正在结束..."
                binding.btnTracking.isEnabled = false
            }
            is TripState.Completed -> {
                binding.btnTracking.text = getString(R.string.start_tracking)
                binding.btnTracking.isEnabled = true
                binding.layoutRouteButtons.visibility = View.VISIBLE
                binding.cardSearch.visibility = View.VISIBLE
                binding.cardRouteInfo.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 如果 Activity 销毁时还在追踪，停止服务
        if (LocationManager.isTracking.value == true) {
            stopLocationTracking()
        }
        // 清除导航状态
        if (NavigationManager.isNavigating.value == true) {
            NavigationManager.clearNavigation()
        }
    }
}
