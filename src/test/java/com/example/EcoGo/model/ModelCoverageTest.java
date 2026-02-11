package com.example.EcoGo.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive POJO tests for all low-coverage model classes.
 */
class ModelCoverageTest {

    // ==================== OrderItem ====================
    @Test
    void orderItem_defaultConstructor() {
        OrderItem item = new OrderItem();
        assertNull(item.getId());
        assertNull(item.getOrderId());
        assertNull(item.getGoodsId());
        assertNull(item.getGoodsName());
        assertNull(item.getQuantity());
        assertNull(item.getPrice());
        assertNull(item.getSubtotal());
        assertNotNull(item.getCreatedAt());
    }

    @Test
    void orderItem_allArgsConstructor() {
        OrderItem item = new OrderItem("order1", "goods1", "Widget", 3, 9.99);
        assertEquals("order1", item.getOrderId());
        assertEquals("goods1", item.getGoodsId());
        assertEquals("Widget", item.getGoodsName());
        assertEquals(3, item.getQuantity());
        assertEquals(9.99, item.getPrice());
        assertEquals(9.99 * 3, item.getSubtotal(), 0.01);
        assertNotNull(item.getCreatedAt());
    }

    @Test
    void orderItem_settersAndGetters() {
        OrderItem item = new OrderItem();
        item.setId("id1");
        item.setOrderId("o1");
        item.setGoodsId("g1");
        item.setGoodsName("Gadget");
        item.setQuantity(5);
        item.setPrice(19.99);
        item.setSubtotal(99.95);
        Date now = new Date();
        item.setCreatedAt(now);

        assertEquals("id1", item.getId());
        assertEquals("o1", item.getOrderId());
        assertEquals("g1", item.getGoodsId());
        assertEquals("Gadget", item.getGoodsName());
        assertEquals(5, item.getQuantity());
        assertEquals(19.99, item.getPrice());
        assertEquals(99.95, item.getSubtotal());
        assertEquals(now, item.getCreatedAt());
    }

    // ==================== CarbonRecord ====================
    @Test
    void carbonRecord_defaultConstructor() {
        CarbonRecord rec = new CarbonRecord();
        assertNull(rec.getId());
        assertNull(rec.getUserId());
        assertNull(rec.getType());
        assertNull(rec.getCredits());
        assertNull(rec.getSource());
        assertNull(rec.getDescription());
        assertNull(rec.getCreatedAt());
    }

    @Test
    void carbonRecord_allArgsConstructor() {
        CarbonRecord rec = new CarbonRecord("user1", "EARN", 100, "ACTIVITY", "Cleaned beach");
        assertEquals("user1", rec.getUserId());
        assertEquals("EARN", rec.getType());
        assertEquals(100, rec.getCredits());
        assertEquals("ACTIVITY", rec.getSource());
        assertEquals("Cleaned beach", rec.getDescription());
        assertNotNull(rec.getCreatedAt());
    }

    @Test
    void carbonRecord_settersAndGetters() {
        CarbonRecord rec = new CarbonRecord();
        rec.setId("cr1");
        rec.setUserId("u1");
        rec.setType("SPEND");
        rec.setCredits(50);
        rec.setSource("EXCHANGE");
        rec.setDescription("Redeemed gift card");
        LocalDateTime now = LocalDateTime.now();
        rec.setCreatedAt(now);

        assertEquals("cr1", rec.getId());
        assertEquals("u1", rec.getUserId());
        assertEquals("SPEND", rec.getType());
        assertEquals(50, rec.getCredits());
        assertEquals("EXCHANGE", rec.getSource());
        assertEquals("Redeemed gift card", rec.getDescription());
        assertEquals(now, rec.getCreatedAt());
    }

    // ==================== ErrorResponse ====================
    @Test
    void errorResponse_twoArgConstructor() {
        ErrorResponse er = new ErrorResponse("ERR_001", "Something went wrong");
        assertEquals("ERR_001", er.getCode());
        assertEquals("Something went wrong", er.getMsg());
        assertNull(er.getData());
    }

    @Test
    void errorResponse_threeArgConstructor() {
        ErrorResponse er = new ErrorResponse("ERR_002", "Bad request", "details");
        assertEquals("ERR_002", er.getCode());
        assertEquals("Bad request", er.getMsg());
        assertEquals("details", er.getData());
    }

    @Test
    void errorResponse_settersAndGetters() {
        ErrorResponse er = new ErrorResponse("X", "Y");
        er.setCode("ERR_003");
        er.setMsg("Updated msg");
        er.setData(42);
        assertEquals("ERR_003", er.getCode());
        assertEquals("Updated msg", er.getMsg());
        assertEquals(42, er.getData());
    }

    // ==================== LeaderboardReward ====================
    @Test
    void leaderboardReward_defaultAndSetters() {
        LeaderboardReward r = new LeaderboardReward();
        assertNull(r.getId());
        assertNull(r.getType());
        assertNull(r.getPeriodKey());
        assertNull(r.getUserId());
        assertEquals(0, r.getRank());
        assertEquals(0L, r.getPointsAwarded());
        assertEquals(0.0, r.getCarbonSaved());
        assertNull(r.getDistributedAt());

        r.setId("lr1");
        r.setType("DAILY");
        r.setPeriodKey("2026-02-10");
        r.setUserId("user1");
        r.setRank(1);
        r.setPointsAwarded(100L);
        r.setCarbonSaved(5.5);
        LocalDateTime now = LocalDateTime.now();
        r.setDistributedAt(now);

        assertEquals("lr1", r.getId());
        assertEquals("DAILY", r.getType());
        assertEquals("2026-02-10", r.getPeriodKey());
        assertEquals("user1", r.getUserId());
        assertEquals(1, r.getRank());
        assertEquals(100L, r.getPointsAwarded());
        assertEquals(5.5, r.getCarbonSaved());
        assertEquals(now, r.getDistributedAt());
    }

    // ==================== ErrorCode (model) ====================
    @Test
    void errorCode_constants() {
        assertEquals("USER_CREATION_FAILED", ErrorCode.USER_CREATION_FAILED);
        assertEquals("USER_NOT_FOUND", ErrorCode.USER_NOT_FOUND);
        assertEquals("USER_UPDATE_FAILED", ErrorCode.USER_UPDATE_FAILED);
        assertEquals("USER_FETCH_FAILED", ErrorCode.USER_FETCH_FAILED);
        assertEquals("DATABASE_ERROR", ErrorCode.DATABASE_ERROR);
        assertEquals("INVALID_INPUT", ErrorCode.INVALID_INPUT);
    }

    // ==================== TransportMode ====================
    @Test
    void transportMode_defaultConstructor() {
        TransportMode tm = new TransportMode();
        assertNull(tm.getId());
        assertNull(tm.getMode());
        assertNull(tm.getModeName());
        assertEquals(0.0, tm.getCarbonFactor());
        assertNull(tm.getIcon());
        assertEquals(0, tm.getSort());
        assertFalse(tm.isGreen());
    }

    @Test
    void transportMode_allArgsConstructor() {
        TransportMode tm = new TransportMode("1001", "walk", "Walking", 0, "icon.png", 1, true);
        assertEquals("1001", tm.getId());
        assertEquals("walk", tm.getMode());
        assertEquals("Walking", tm.getModeName());
        assertEquals(0, tm.getCarbonFactor());
        assertEquals("icon.png", tm.getIcon());
        assertEquals(1, tm.getSort());
        assertTrue(tm.isGreen());
    }

    @Test
    void transportMode_settersAndGetters() {
        TransportMode tm = new TransportMode();
        tm.setId("1002");
        tm.setMode("bike");
        tm.setModeName("Bicycle");
        tm.setCarbonFactor(5.0);
        tm.setIcon("bike.png");
        tm.setSort(2);
        tm.setGreen(true);

        assertEquals("1002", tm.getId());
        assertEquals("bike", tm.getMode());
        assertEquals("Bicycle", tm.getModeName());
        assertEquals(5.0, tm.getCarbonFactor());
        assertEquals("bike.png", tm.getIcon());
        assertEquals(2, tm.getSort());
        assertTrue(tm.isGreen());
    }

    // ==================== VipGlobalSwitch ====================
    @Test
    void vipGlobalSwitch_defaultConstructor() {
        VipGlobalSwitch v = new VipGlobalSwitch();
        assertNull(v.getId());
        assertNull(v.getSwitchKey());
        assertNull(v.getDisplayName());
        assertNull(v.getDescription());
        assertTrue(v.isEnabled()); // default true
        assertNotNull(v.getUpdatedAt());
        assertNull(v.getUpdatedBy());
    }

    @Test
    void vipGlobalSwitch_allArgsConstructor() {
        VipGlobalSwitch v = new VipGlobalSwitch("vip_discount", "VIP Discount", "Controls VIP discount feature");
        assertEquals("vip_discount", v.getSwitchKey());
        assertEquals("VIP Discount", v.getDisplayName());
        assertEquals("Controls VIP discount feature", v.getDescription());
        assertTrue(v.isEnabled());
        assertNotNull(v.getUpdatedAt());
    }

    @Test
    void vipGlobalSwitch_settersAndGetters() {
        VipGlobalSwitch v = new VipGlobalSwitch();
        v.setId("vs1");
        v.setSwitchKey("feature_x");
        v.setDisplayName("Feature X");
        v.setDescription("Enables feature X");
        v.setEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        v.setUpdatedAt(now);
        v.setUpdatedBy("admin");

        assertEquals("vs1", v.getId());
        assertEquals("feature_x", v.getSwitchKey());
        assertEquals("Feature X", v.getDisplayName());
        assertEquals("Enables feature X", v.getDescription());
        assertFalse(v.isEnabled());
        assertEquals(now, v.getUpdatedAt());
        assertEquals("admin", v.getUpdatedBy());
    }

    // ==================== Faculty ====================
    @Test
    void faculty_defaultConstructor() {
        Faculty f = new Faculty();
        assertNull(f.getId());
        assertNull(f.getName());
    }

    @Test
    void faculty_nameConstructor() {
        Faculty f = new Faculty("Engineering");
        assertEquals("Engineering", f.getName());
    }

    @Test
    void faculty_settersAndGetters() {
        Faculty f = new Faculty();
        f.setId("f1");
        f.setName("Science");
        assertEquals("f1", f.getId());
        assertEquals("Science", f.getName());
    }

    // ==================== Inventory ====================
    @Test
    void inventory_defaultConstructor() {
        Inventory inv = new Inventory();
        assertNull(inv.getId());
        assertNull(inv.getGoodsId());
        assertNull(inv.getQuantity());
        assertNull(inv.getUpdatedAt());
    }

    @Test
    void inventory_allArgsConstructor() {
        Inventory inv = new Inventory("g1", 100);
        assertEquals("g1", inv.getId());
        assertEquals("g1", inv.getGoodsId());
        assertEquals(100, inv.getQuantity());
        assertNotNull(inv.getUpdatedAt());
    }

    @Test
    void inventory_settersAndGetters() {
        Inventory inv = new Inventory();
        inv.setId("inv1");
        inv.setGoodsId("g2");
        inv.setQuantity(50);
        Date now = new Date();
        inv.setUpdatedAt(now);

        assertEquals("inv1", inv.getId());
        assertEquals("g2", inv.getGoodsId());
        assertEquals(50, inv.getQuantity());
        assertEquals(now, inv.getUpdatedAt());
    }

    // ==================== UserVoucher ====================
    @Test
    void userVoucher_defaultConstructor() {
        UserVoucher uv = new UserVoucher();
        assertNull(uv.getId());
        assertNull(uv.getUserId());
        assertNull(uv.getGoodsId());
        assertNull(uv.getVoucherName());
        assertNull(uv.getImageUrl());
        assertNull(uv.getOrderId());
        assertEquals(VoucherStatus.ACTIVE, uv.getStatus());
        assertNull(uv.getIssuedAt());
        assertNull(uv.getExpiresAt());
        assertNull(uv.getUsedAt());
        assertNull(uv.getCode());
        assertNull(uv.getCreatedAt());
        assertNull(uv.getUpdatedAt());
    }

    @Test
    void userVoucher_settersAndGetters() {
        UserVoucher uv = new UserVoucher();
        uv.setId("uv1");
        uv.setUserId("user1");
        uv.setGoodsId("goods1");
        uv.setVoucherName("10% Off");
        uv.setImageUrl("img.png");
        uv.setOrderId("ord1");
        uv.setStatus(VoucherStatus.USED);
        LocalDateTime now = LocalDateTime.now();
        uv.setIssuedAt(now);
        uv.setExpiresAt(now.plusDays(30));
        uv.setUsedAt(now.plusDays(5));
        uv.setCode("CODE123");
        uv.setCreatedAt(now);
        uv.setUpdatedAt(now);

        assertEquals("uv1", uv.getId());
        assertEquals("user1", uv.getUserId());
        assertEquals("goods1", uv.getGoodsId());
        assertEquals("10% Off", uv.getVoucherName());
        assertEquals("img.png", uv.getImageUrl());
        assertEquals("ord1", uv.getOrderId());
        assertEquals(VoucherStatus.USED, uv.getStatus());
        assertEquals(now, uv.getIssuedAt());
        assertEquals(now.plusDays(30), uv.getExpiresAt());
        assertEquals(now.plusDays(5), uv.getUsedAt());
        assertEquals("CODE123", uv.getCode());
        assertEquals(now, uv.getCreatedAt());
        assertEquals(now, uv.getUpdatedAt());
    }

    @Test
    void userVoucher_isExpired() {
        UserVoucher uv = new UserVoucher();
        LocalDateTime now = LocalDateTime.now();

        // Not expired
        uv.setExpiresAt(now.plusDays(1));
        assertFalse(uv.isExpired(now));

        // Expired
        uv.setExpiresAt(now.minusDays(1));
        assertTrue(uv.isExpired(now));

        // Null expiry
        uv.setExpiresAt(null);
        assertFalse(uv.isExpired(now));

        // Null now
        uv.setExpiresAt(now);
        assertFalse(uv.isExpired(null));
    }

    @Test
    void userVoucher_touch() {
        UserVoucher uv = new UserVoucher();
        LocalDateTime now = LocalDateTime.now();

        // First touch: sets both createdAt and updatedAt
        uv.touch(now);
        assertEquals(now, uv.getCreatedAt());
        assertEquals(now, uv.getUpdatedAt());

        // Second touch: only updatedAt changes
        LocalDateTime later = now.plusHours(1);
        uv.touch(later);
        assertEquals(now, uv.getCreatedAt()); // unchanged
        assertEquals(later, uv.getUpdatedAt());
    }

    // ==================== VoucherStatus ====================
    @Test
    void voucherStatus_values() {
        VoucherStatus[] values = VoucherStatus.values();
        assertTrue(values.length >= 3);
        assertNotNull(VoucherStatus.valueOf("ACTIVE"));
        assertNotNull(VoucherStatus.valueOf("USED"));
        assertNotNull(VoucherStatus.valueOf("EXPIRED"));
    }

    // ==================== UserPointsLog ====================
    @Test
    void userPointsLog_defaultConstructor() {
        UserPointsLog log = new UserPointsLog();
        assertNull(log.getId());
        assertNull(log.getUserId());
        assertNull(log.getChangeType());
        assertEquals(0L, log.getPoints());
        assertNull(log.getSource());
        assertNull(log.getDescription());
        assertNull(log.getRelatedId());
        assertNull(log.getAdminAction());
        assertEquals(0L, log.getBalanceAfter());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void userPointsLog_allArgsConstructor() {
        UserPointsLog log = new UserPointsLog("u1", "gain", 100L, "trip", "trip1", 500L);
        assertEquals("u1", log.getUserId());
        assertEquals("gain", log.getChangeType());
        assertEquals(100L, log.getPoints());
        assertEquals("trip", log.getSource());
        assertEquals("trip1", log.getRelatedId());
        assertEquals(500L, log.getBalanceAfter());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void userPointsLog_settersAndGetters() {
        UserPointsLog log = new UserPointsLog();
        log.setId("pl1");
        log.setUserId("u2");
        log.setChangeType("deduct");
        log.setPoints(50L);
        log.setSource("redeem");
        log.setDescription("Redeemed voucher");
        log.setRelatedId("order1");
        log.setBalanceAfter(450L);
        LocalDateTime now = LocalDateTime.now();
        log.setCreatedAt(now);

        assertEquals("pl1", log.getId());
        assertEquals("u2", log.getUserId());
        assertEquals("deduct", log.getChangeType());
        assertEquals(50L, log.getPoints());
        assertEquals("redeem", log.getSource());
        assertEquals("Redeemed voucher", log.getDescription());
        assertEquals("order1", log.getRelatedId());
        assertEquals(450L, log.getBalanceAfter());
        assertEquals(now, log.getCreatedAt());
    }

    // ==================== UserPointsLog.AdminAction ====================
    @Test
    void adminAction_defaultConstructor() {
        UserPointsLog.AdminAction aa = new UserPointsLog.AdminAction();
        assertNull(aa.getOperatorId());
        assertNull(aa.getReason());
        assertNull(aa.getApprovalStatus());
    }

    @Test
    void adminAction_allArgsConstructor() {
        UserPointsLog.AdminAction aa = new UserPointsLog.AdminAction("admin1", "Bonus points", "approved");
        assertEquals("admin1", aa.getOperatorId());
        assertEquals("Bonus points", aa.getReason());
        assertEquals("approved", aa.getApprovalStatus());
    }

    @Test
    void adminAction_settersAndGetters() {
        UserPointsLog.AdminAction aa = new UserPointsLog.AdminAction();
        aa.setOperatorId("admin2");
        aa.setReason("Correction");
        aa.setApprovalStatus("pending");

        assertEquals("admin2", aa.getOperatorId());
        assertEquals("Correction", aa.getReason());
        assertEquals("pending", aa.getApprovalStatus());
    }

    @Test
    void userPointsLog_withAdminAction() {
        UserPointsLog log = new UserPointsLog();
        UserPointsLog.AdminAction aa = new UserPointsLog.AdminAction("admin1", "Reward", "approved");
        log.setAdminAction(aa);
        assertNotNull(log.getAdminAction());
        assertEquals("admin1", log.getAdminAction().getOperatorId());
    }

    // ==================== Goods (additional coverage) ====================
    @Test
    void goods_defaultConstructor() {
        Goods g = new Goods();
        assertNull(g.getId());
        assertNull(g.getName());
        assertNull(g.getDescription());
        assertNull(g.getPrice());
        assertNull(g.getStock());
        assertTrue(g.getIsActive());
        assertNotNull(g.getCreatedAt());
        assertNotNull(g.getUpdatedAt());
    }

    @Test
    void goods_allArgsConstructor() {
        Goods g = new Goods("Widget", "A useful widget", 9.99, 50);
        assertEquals("Widget", g.getName());
        assertEquals("A useful widget", g.getDescription());
        assertEquals(9.99, g.getPrice());
        assertEquals(50, g.getStock());
        assertTrue(g.getIsActive());
    }

    @Test
    void goods_allFieldsSettersAndGetters() {
        Goods g = new Goods();
        g.setId("g1");
        g.setName("Gadget");
        g.setDescription("Cool gadget");
        g.setPrice(29.99);
        g.setStock(200);
        g.setCategory("electronics");
        g.setType("premium");
        g.setBrand("AcmeCo");
        g.setImageUrl("gadget.jpg");
        g.setIsActive(false);
        g.setVipLevelRequired(2);
        g.setIsForRedemption(true);
        g.setRedemptionPoints(500);
        g.setRedemptionLimit(3);
        g.setTotalRedemptionCount(10);
        Date now = new Date();
        g.setCreatedAt(now);
        g.setUpdatedAt(now);

        assertEquals("g1", g.getId());
        assertEquals("Gadget", g.getName());
        assertEquals("Cool gadget", g.getDescription());
        assertEquals(29.99, g.getPrice());
        assertEquals(200, g.getStock());
        assertEquals("electronics", g.getCategory());
        assertEquals("premium", g.getType());
        assertEquals("AcmeCo", g.getBrand());
        assertEquals("gadget.jpg", g.getImageUrl());
        assertFalse(g.getIsActive());
        assertEquals(2, g.getVipLevelRequired());
        assertTrue(g.getIsForRedemption());
        assertEquals(500, g.getRedemptionPoints());
        assertEquals(3, g.getRedemptionLimit());
        assertEquals(10, g.getTotalRedemptionCount());
        assertEquals(now, g.getCreatedAt());
        assertEquals(now, g.getUpdatedAt());
    }

    // ==================== Order.OrderItem (inner class) ====================
    @Test
    void orderOrderItem_settersAndGetters() {
        Order.OrderItem oi = new Order.OrderItem();
        oi.setGoodsId("g1");
        oi.setGoodsName("Widget");
        oi.setQuantity(3);
        oi.setPrice(10.0);
        oi.setSubtotal(30.0);

        assertEquals("g1", oi.getGoodsId());
        assertEquals("Widget", oi.getGoodsName());
        assertEquals(3, oi.getQuantity());
        assertEquals(10.0, oi.getPrice());
        assertEquals(30.0, oi.getSubtotal());
    }

    // ==================== User inner classes (additional coverage) ====================
    @Test
    void userPreferences_allSettersAndGetters() {
        User.Preferences p = new User.Preferences();
        p.setLanguage("zh");
        p.setTheme("light");
        p.setPreferredTransport(new java.util.ArrayList<>(java.util.List.of("bus", "walk")));
        p.setEnablePush(true);
        p.setEnableEmail(true);
        p.setEnableBusReminder(true);
        p.setShareLocation(true);
        p.setShowOnLeaderboard(true);
        p.setShareAchievements(true);
        p.setDormitoryOrResidence("Dorm A");
        p.setMainTeachingBuilding("Building B");
        p.setFavoriteStudySpot("Library");
        p.setInterests(new java.util.ArrayList<>(java.util.List.of("coding")));
        p.setWeeklyGoals(5);
        p.setNewChallenges(true);
        p.setActivityReminders(true);
        p.setFriendActivity(true);

        assertEquals("zh", p.getLanguage());
        assertEquals("light", p.getTheme());
        assertEquals(2, p.getPreferredTransport().size());
        assertTrue(p.isEnablePush());
        assertTrue(p.isEnableEmail());
        assertTrue(p.isEnableBusReminder());
        assertTrue(p.isShareLocation());
        assertTrue(p.isShowOnLeaderboard());
        assertTrue(p.isShareAchievements());
        assertEquals("Dorm A", p.getDormitoryOrResidence());
        assertEquals("Building B", p.getMainTeachingBuilding());
        assertEquals("Library", p.getFavoriteStudySpot());
        assertEquals(1, p.getInterests().size());
        assertEquals(5, p.getWeeklyGoals());
        assertTrue(p.isNewChallenges());
        assertTrue(p.isActivityReminders());
        assertTrue(p.isFriendActivity());
    }

    @Test
    void userVip_allSettersAndGetters() {
        User.Vip v = new User.Vip();
        v.setActive(true);
        v.setPlan("MONTHLY");
        LocalDateTime now = LocalDateTime.now();
        v.setStartDate(now);
        v.setExpiryDate(now.plusMonths(1));
        v.setAutoRenew(true);
        v.setPointsMultiplier(2);

        assertTrue(v.isActive());
        assertEquals("MONTHLY", v.getPlan());
        assertEquals(now, v.getStartDate());
        assertEquals(now.plusMonths(1), v.getExpiryDate());
        assertTrue(v.isAutoRenew());
        assertEquals(2, v.getPointsMultiplier());
    }

    @Test
    void userStats_allSettersAndGetters() {
        User.Stats s = new User.Stats();
        s.setTotalTrips(10);
        s.setTotalDistance(50.5);
        s.setGreenDays(7);
        s.setWeeklyRank(3);
        s.setMonthlyRank(5);
        s.setTotalPointsFromTrips(1000L);

        assertEquals(10, s.getTotalTrips());
        assertEquals(50.5, s.getTotalDistance(), 0.01);
        assertEquals(7, s.getGreenDays());
        assertEquals(3, s.getWeeklyRank());
        assertEquals(5, s.getMonthlyRank());
        assertEquals(1000L, s.getTotalPointsFromTrips());
    }

    @Test
    void userActivityMetrics_allSettersAndGetters() {
        User.ActivityMetrics am = new User.ActivityMetrics();
        am.setActiveDays7d(5);
        am.setActiveDays30d(20);
        am.setLastTripDays(2);
        am.setLoginFrequency7d(10);
        am.setLoginDates(new java.util.ArrayList<>());

        assertEquals(5, am.getActiveDays7d());
        assertEquals(20, am.getActiveDays30d());
        assertEquals(2, am.getLastTripDays());
        assertEquals(10, am.getLoginFrequency7d());
        assertNotNull(am.getLoginDates());
        assertTrue(am.getLoginDates().isEmpty());
    }

    // ==================== Trip inner classes (GeoPoint, LocationDetail, TransportSegment) ====================
    @Test
    void tripGeoPoint_noArgConstructor() {
        Trip.GeoPoint gp = new Trip.GeoPoint();
        assertEquals(0.0, gp.getLng());
        assertEquals(0.0, gp.getLat());
    }

    @Test
    void tripGeoPoint_settersAndGetters() {
        Trip.GeoPoint gp = new Trip.GeoPoint();
        gp.setLng(116.5);
        gp.setLat(39.5);
        assertEquals(116.5, gp.getLng(), 0.01);
        assertEquals(39.5, gp.getLat(), 0.01);
    }

    @Test
    void tripLocationDetail_noArgConstructor() {
        Trip.LocationDetail ld = new Trip.LocationDetail();
        assertNull(ld.getAddress());
        assertNull(ld.getPlaceName());
        assertNull(ld.getCampusZone());
    }

    @Test
    void tripLocationDetail_settersAndGetters() {
        Trip.LocationDetail ld = new Trip.LocationDetail();
        ld.setAddress("123 Main St");
        ld.setPlaceName("Library");
        ld.setCampusZone("Zone A");
        assertEquals("123 Main St", ld.getAddress());
        assertEquals("Library", ld.getPlaceName());
        assertEquals("Zone A", ld.getCampusZone());
    }

    @Test
    void tripTransportSegment_noArgConstructor() {
        Trip.TransportSegment ts = new Trip.TransportSegment();
        assertNull(ts.getMode());
        assertEquals(0.0, ts.getSubDistance());
        assertEquals(0, ts.getSubDuration());
    }

    @Test
    void tripTransportSegment_settersAndGetters() {
        Trip.TransportSegment ts = new Trip.TransportSegment();
        ts.setMode("walk");
        ts.setSubDistance(2.5);
        ts.setSubDuration(30);
        assertEquals("walk", ts.getMode());
        assertEquals(2.5, ts.getSubDistance(), 0.01);
        assertEquals(30, ts.getSubDuration());
    }

    // ==================== ErrorCode (model) constructor coverage ====================
    @Test
    void errorCode_canInstantiate() {
        ErrorCode ec = new ErrorCode();
        assertNotNull(ec);
        // Also verify constants
        assertEquals("USER_CREATION_FAILED", ErrorCode.USER_CREATION_FAILED);
        assertEquals("DATABASE_ERROR", ErrorCode.DATABASE_ERROR);
    }

    // ==================== Advertisement ====================
    @Test
    void advertisement_fullConstructor() {
        java.time.LocalDate start = java.time.LocalDate.now();
        java.time.LocalDate end = start.plusDays(30);
        Advertisement ad = new Advertisement("Ad1", "Description", "Active", start, end, "img.png", "http://example.com", "banner");

        assertEquals("Ad1", ad.getName());
        assertEquals("Description", ad.getDescription());
        assertEquals("Active", ad.getStatus());
        assertEquals(start, ad.getStartDate());
        assertEquals(end, ad.getEndDate());
        assertEquals("img.png", ad.getImageUrl());
        assertEquals("http://example.com", ad.getLinkUrl());
        assertEquals("banner", ad.getPosition());
        assertEquals(0L, ad.getImpressions());
        assertEquals(0L, ad.getClicks());
    }

    @Test
    void advertisement_allSettersAndGetters() {
        Advertisement ad = new Advertisement();
        ad.setId("ad1");
        ad.setName("Summer Sale");
        ad.setDescription("Big discounts");
        ad.setStatus("Active");
        java.time.LocalDate start = java.time.LocalDate.of(2026, 1, 1);
        java.time.LocalDate end = java.time.LocalDate.of(2026, 12, 31);
        ad.setStartDate(start);
        ad.setEndDate(end);
        ad.setImageUrl("sale.png");
        ad.setLinkUrl("http://sale.com");
        ad.setPosition("sidebar");
        ad.setImpressions(1000L);
        ad.setClicks(50L);

        assertEquals("ad1", ad.getId());
        assertEquals("Summer Sale", ad.getName());
        assertEquals("Big discounts", ad.getDescription());
        assertEquals("Active", ad.getStatus());
        assertEquals(start, ad.getStartDate());
        assertEquals(end, ad.getEndDate());
        assertEquals("sale.png", ad.getImageUrl());
        assertEquals("http://sale.com", ad.getLinkUrl());
        assertEquals("sidebar", ad.getPosition());
        assertEquals(1000L, ad.getImpressions());
        assertEquals(50L, ad.getClicks());
    }

    @Test
    void advertisement_getClickRate_withImpressions() {
        Advertisement ad = new Advertisement();
        ad.setImpressions(200L);
        ad.setClicks(10L);
        assertEquals(5.0, ad.getClickRate(), 0.01);
    }

    @Test
    void advertisement_getClickRate_zeroImpressions() {
        Advertisement ad = new Advertisement();
        ad.setImpressions(0L);
        ad.setClicks(10L);
        assertEquals(0.0, ad.getClickRate());
    }

    @Test
    void advertisement_getClickRate_nullImpressions() {
        Advertisement ad = new Advertisement();
        ad.setImpressions(null);
        assertEquals(0.0, ad.getClickRate());
    }

    // ==================== UserChallengeProgress ====================
    @Test
    void userChallengeProgress_settersAndGetters() {
        UserChallengeProgress ucp = new UserChallengeProgress();
        ucp.setId("ucp1");
        ucp.setChallengeId("ch1");
        ucp.setUserId("u1");
        ucp.setStatus("IN_PROGRESS");
        LocalDateTime now = LocalDateTime.now();
        ucp.setJoinedAt(now);
        ucp.setCompletedAt(now.plusDays(5));

        assertEquals("ucp1", ucp.getId());
        assertEquals("ch1", ucp.getChallengeId());
        assertEquals("u1", ucp.getUserId());
        assertEquals("IN_PROGRESS", ucp.getStatus());
        assertEquals(now, ucp.getJoinedAt());
        assertEquals(now.plusDays(5), ucp.getCompletedAt());
    }

    // ==================== Order (additional coverage) ====================
    @Test
    void order_constructorDefaultValues() {
        Order o = new Order();
        assertEquals("PENDING", o.getStatus());
        assertEquals("PENDING", o.getPaymentStatus());
        assertNotNull(o.getCreatedAt());
        assertNotNull(o.getUpdatedAt());
    }

    @Test
    void order_allSettersAndGetters() {
        Order o = new Order();
        o.setId("ord1");
        o.setOrderNumber("ORD123");
        o.setUserId("u1");
        o.setTotalAmount(100.0);
        o.setShippingFee(10.0);
        o.setFinalAmount(110.0);
        o.setStatus("SHIPPED");
        o.setPaymentMethod("CREDIT");
        o.setPaymentStatus("PAID");
        o.setShippingAddress("123 Main St");
        o.setRecipientName("John");
        o.setRecipientPhone("12345");
        o.setRemark("Note");
        o.setTrackingNumber("TN001");
        o.setCarrier("DHL");
        o.setIsRedemptionOrder(true);
        o.setPointsUsed(500);
        o.setPointsEarned(50);
        Date now = new Date();
        o.setCreatedAt(now);
        o.setUpdatedAt(now);

        assertEquals("ord1", o.getId());
        assertEquals("ORD123", o.getOrderNumber());
        assertEquals("u1", o.getUserId());
        assertEquals(100.0, o.getTotalAmount(), 0.01);
        assertEquals(10.0, o.getShippingFee(), 0.01);
        assertEquals(110.0, o.getFinalAmount(), 0.01);
        assertEquals("SHIPPED", o.getStatus());
        assertEquals("CREDIT", o.getPaymentMethod());
        assertEquals("PAID", o.getPaymentStatus());
        assertEquals("123 Main St", o.getShippingAddress());
        assertEquals("John", o.getRecipientName());
        assertEquals("12345", o.getRecipientPhone());
        assertEquals("Note", o.getRemark());
        assertEquals("TN001", o.getTrackingNumber());
        assertEquals("DHL", o.getCarrier());
        assertTrue(o.getIsRedemptionOrder());
        assertEquals(500, o.getPointsUsed());
        assertEquals(50, o.getPointsEarned());
        assertEquals(now, o.getCreatedAt());
        assertEquals(now, o.getUpdatedAt());
    }

    // ==================== User (outer class) ====================
    @Test
    void user_allSettersAndGetters() {
        User u = new User();
        u.setId("uid1");
        u.setUserid("user1");
        u.setEmail("test@test.com");
        u.setPhone("12345");
        u.setPassword("pw");
        u.setNickname("Nick");
        u.setAvatar("avatar.png");
        u.setAdmin(true);
        u.setDeactivated(false);
        u.setFaculty("Engineering");
        u.setTotalCarbon(100.5);
        u.setTotalPoints(500L);
        u.setCurrentPoints(300L);
        LocalDateTime now = LocalDateTime.now();
        u.setLastLoginAt(now);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);

        assertEquals("uid1", u.getId());
        assertEquals("user1", u.getUserid());
        assertEquals("test@test.com", u.getEmail());
        assertEquals("12345", u.getPhone());
        assertEquals("pw", u.getPassword());
        assertEquals("Nick", u.getNickname());
        assertEquals("avatar.png", u.getAvatar());
        assertTrue(u.isAdmin());
        assertFalse(u.isDeactivated());
        assertEquals("Engineering", u.getFaculty());
        assertEquals(100.5, u.getTotalCarbon(), 0.01);
        assertEquals(500L, u.getTotalPoints());
        assertEquals(300L, u.getCurrentPoints());
        assertEquals(now, u.getLastLoginAt());
        assertEquals(now, u.getCreatedAt());
        assertEquals(now, u.getUpdatedAt());
    }
}
