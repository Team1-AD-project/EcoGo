package com.example.EcoGo.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive POJO tests for all low-coverage DTO classes.
 */
class DtoCoverageTest {

    // ==================== UserDto ====================
    @Test
    void userDto_defaultConstructor() {
        UserDto dto = new UserDto();
        assertNull(dto.getPhone());
        assertNull(dto.getPassword());
        assertNull(dto.getNickname());
        assertNull(dto.getAvatar());
    }

    @Test
    void userDto_allArgsConstructor() {
        UserDto dto = new UserDto("12345", "pass", "nick", "avatar.png");
        assertEquals("12345", dto.getPhone());
        assertEquals("pass", dto.getPassword());
        assertEquals("nick", dto.getNickname());
        assertEquals("avatar.png", dto.getAvatar());
    }

    @Test
    void userDto_setters() {
        UserDto dto = new UserDto();
        dto.setPhone("99999");
        dto.setPassword("secret");
        dto.setNickname("Bob");
        dto.setAvatar("bob.jpg");
        assertEquals("99999", dto.getPhone());
        assertEquals("secret", dto.getPassword());
        assertEquals("Bob", dto.getNickname());
        assertEquals("bob.jpg", dto.getAvatar());
    }

    // ==================== DashboardStatsDTO ====================
    @Test
    void dashboardStatsDto_allFields() {
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalUsers(100L);
        dto.setActiveUsers(80L);
        dto.setTotalAdvertisements(20L);
        dto.setActiveAdvertisements(15L);
        dto.setTotalActivities(50L);
        dto.setOngoingActivities(10L);
        dto.setTotalCarbonCredits(5000L);
        dto.setTotalCarbonReduction(3000L);
        dto.setRedemptionVolume(200L);

        assertEquals(100L, dto.getTotalUsers());
        assertEquals(80L, dto.getActiveUsers());
        assertEquals(20L, dto.getTotalAdvertisements());
        assertEquals(15L, dto.getActiveAdvertisements());
        assertEquals(50L, dto.getTotalActivities());
        assertEquals(10L, dto.getOngoingActivities());
        assertEquals(5000L, dto.getTotalCarbonCredits());
        assertEquals(3000L, dto.getTotalCarbonReduction());
        assertEquals(200L, dto.getRedemptionVolume());
    }

    // ==================== OrderDto ====================
    @Test
    void orderDto_allFields() {
        OrderDto dto = new OrderDto();
        dto.setId("o1");
        dto.setUserId("u1");
        dto.setProductId("p1");
        dto.setQuantity(3);
        dto.setTotalAmount(99.99);
        dto.setStatus("PAID");

        assertEquals("o1", dto.getId());
        assertEquals("u1", dto.getUserId());
        assertEquals("p1", dto.getProductId());
        assertEquals(3, dto.getQuantity());
        assertEquals(99.99, dto.getTotalAmount());
        assertEquals("PAID", dto.getStatus());
    }

    // ==================== GoodsDto ====================
    @Test
    void goodsDto_defaultConstructor() {
        GoodsDto dto = new GoodsDto();
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getPrice());
        assertNull(dto.getStock());
    }

    @Test
    void goodsDto_allArgsConstructor() {
        GoodsDto dto = new GoodsDto("Widget", "Nice widget", 9.99, 100);
        assertEquals("Widget", dto.getName());
        assertEquals("Nice widget", dto.getDescription());
        assertEquals(9.99, dto.getPrice());
        assertEquals(100, dto.getStock());
    }

    @Test
    void goodsDto_setters() {
        GoodsDto dto = new GoodsDto();
        dto.setName("Gadget");
        dto.setDescription("Cool gadget");
        dto.setPrice(19.99);
        dto.setStock(50);
        assertEquals("Gadget", dto.getName());
        assertEquals("Cool gadget", dto.getDescription());
        assertEquals(19.99, dto.getPrice());
        assertEquals(50, dto.getStock());
    }

    // ==================== HeatmapDataDTO ====================
    @Test
    void heatmapDataDto_allFields() {
        HeatmapDataDTO dto = new HeatmapDataDTO();
        dto.setRegion("NUS");
        dto.setLatitude(1.3521);
        dto.setLongitude(103.8198);
        dto.setEmissionValue(500L);
        dto.setReductionValue(200L);
        dto.setIntensity("HIGH");

        assertEquals("NUS", dto.getRegion());
        assertEquals(1.3521, dto.getLatitude());
        assertEquals(103.8198, dto.getLongitude());
        assertEquals(500L, dto.getEmissionValue());
        assertEquals(200L, dto.getReductionValue());
        assertEquals("HIGH", dto.getIntensity());
    }

    @Test
    void heatmapSummary_allFields() {
        HeatmapDataDTO.HeatmapSummary summary = new HeatmapDataDTO.HeatmapSummary();
        List<HeatmapDataDTO> dataPoints = new ArrayList<>();
        dataPoints.add(new HeatmapDataDTO());
        summary.setDataPoints(dataPoints);
        summary.setRegionStats(Map.of("NUS", 100L));
        summary.setTotalEmissions(1000L);
        summary.setTotalReductions(500L);

        assertEquals(1, summary.getDataPoints().size());
        assertEquals(100L, summary.getRegionStats().get("NUS"));
        assertEquals(1000L, summary.getTotalEmissions());
        assertEquals(500L, summary.getTotalReductions());
    }

    // ==================== RegisterRequestDto ====================
    @Test
    void registerRequestDto_defaultConstructor() {
        RegisterRequestDto dto = new RegisterRequestDto();
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
        assertNull(dto.getEmail());
    }

    @Test
    void registerRequestDto_allArgsConstructor() {
        RegisterRequestDto dto = new RegisterRequestDto("user1", "pass123", "user1@example.com");
        assertEquals("user1", dto.getUsername());
        assertEquals("pass123", dto.getPassword());
        assertEquals("user1@example.com", dto.getEmail());
    }

    @Test
    void registerRequestDto_setters() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setUsername("user2");
        dto.setPassword("pass456");
        dto.setEmail("user2@example.com");
        assertEquals("user2", dto.getUsername());
        assertEquals("pass456", dto.getPassword());
        assertEquals("user2@example.com", dto.getEmail());
    }

    // ==================== LoginRequestDto ====================
    @Test
    void loginRequestDto_defaultConstructor() {
        LoginRequestDto dto = new LoginRequestDto();
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
    }

    @Test
    void loginRequestDto_allArgsConstructor() {
        LoginRequestDto dto = new LoginRequestDto("user1", "pass123");
        assertEquals("user1", dto.getUsername());
        assertEquals("pass123", dto.getPassword());
    }

    @Test
    void loginRequestDto_setters() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("user2");
        dto.setPassword("secret");
        assertEquals("user2", dto.getUsername());
        assertEquals("secret", dto.getPassword());
    }

    // ==================== OrderRequestDto ====================
    @Test
    void orderRequestDto_allFieldsAndToEntity() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId("u1");
        dto.setTotalAmount(100.0);
        dto.setShippingFee(5.0);
        dto.setFinalAmount(105.0);
        dto.setStatus("PENDING");
        dto.setPaymentMethod("credit_card");
        dto.setPaymentStatus("PENDING");
        dto.setShippingAddress("123 Main St");
        dto.setRecipientName("John");
        dto.setRecipientPhone("99999");
        dto.setRemark("No rush");
        dto.setTrackingNumber("TRK001");
        dto.setCarrier("FedEx");
        dto.setPointsUsed(50);
        dto.setPointsEarned(10);

        com.example.EcoGo.model.Order.OrderItem oi = new com.example.EcoGo.model.Order.OrderItem();
        oi.setGoodsId("g1");
        oi.setQuantity(2);
        dto.setItems(List.of(oi));

        assertEquals("u1", dto.getUserId());
        assertEquals(100.0, dto.getTotalAmount());
        assertEquals(5.0, dto.getShippingFee());
        assertEquals(105.0, dto.getFinalAmount());
        assertEquals("PENDING", dto.getStatus());
        assertEquals("credit_card", dto.getPaymentMethod());
        assertEquals("PENDING", dto.getPaymentStatus());
        assertEquals("123 Main St", dto.getShippingAddress());
        assertEquals("John", dto.getRecipientName());
        assertEquals("99999", dto.getRecipientPhone());
        assertEquals("No rush", dto.getRemark());
        assertEquals("TRK001", dto.getTrackingNumber());
        assertEquals("FedEx", dto.getCarrier());
        assertEquals(50, dto.getPointsUsed());
        assertEquals(10, dto.getPointsEarned());
        assertNotNull(dto.getItems());

        // Test toEntity
        com.example.EcoGo.model.Order order = dto.toEntity();
        assertEquals("u1", order.getUserId());
        assertEquals(100.0, order.getTotalAmount());
        assertEquals("PENDING", order.getStatus());
        assertEquals("credit_card", order.getPaymentMethod());
        assertEquals("123 Main St", order.getShippingAddress());
        assertEquals("John", order.getRecipientName());
    }

    // ==================== GoodsRequestDto ====================
    @Test
    void goodsRequestDto_allFieldsAndToEntity() {
        GoodsRequestDto dto = new GoodsRequestDto();
        dto.setName("Widget");
        dto.setDescription("Useful widget");
        dto.setPrice(9.99);
        dto.setStock(100);
        dto.setCategory("tools");
        dto.setType("normal");
        dto.setBrand("AcmeCo");
        dto.setImageUrl("widget.jpg");
        dto.setIsActive(true);
        dto.setVipLevelRequired(1);
        dto.setIsForRedemption(false);
        dto.setRedemptionPoints(0);
        dto.setRedemptionLimit(-1);

        assertEquals("Widget", dto.getName());
        assertEquals("Useful widget", dto.getDescription());
        assertEquals(9.99, dto.getPrice());
        assertEquals(100, dto.getStock());
        assertEquals("tools", dto.getCategory());
        assertEquals("normal", dto.getType());
        assertEquals("AcmeCo", dto.getBrand());
        assertEquals("widget.jpg", dto.getImageUrl());
        assertTrue(dto.getIsActive());
        assertEquals(1, dto.getVipLevelRequired());
        assertFalse(dto.getIsForRedemption());
        assertEquals(0, dto.getRedemptionPoints());
        assertEquals(-1, dto.getRedemptionLimit());

        // Test toEntity
        com.example.EcoGo.model.Goods goods = dto.toEntity();
        assertEquals("Widget", goods.getName());
        assertEquals("Useful widget", goods.getDescription());
        assertEquals(9.99, goods.getPrice());
        assertEquals(100, goods.getStock());
        assertEquals("tools", goods.getCategory());
    }

    // ==================== ActivityRequestDto ====================
    @Test
    void activityRequestDto_allFieldsAndToEntity() {
        ActivityRequestDto dto = new ActivityRequestDto();
        dto.setTitle("Beach Cleanup");
        dto.setDescription("Clean the beach");
        dto.setType("OFFLINE");
        dto.setStatus("PUBLISHED");
        dto.setRewardCredits(50);
        dto.setMaxParticipants(100);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setLatitude(1.35);
        dto.setLongitude(103.82);
        dto.setLocationName("East Coast Park");

        assertEquals("Beach Cleanup", dto.getTitle());
        assertEquals("Clean the beach", dto.getDescription());
        assertEquals("OFFLINE", dto.getType());
        assertEquals("PUBLISHED", dto.getStatus());
        assertEquals(50, dto.getRewardCredits());
        assertEquals(100, dto.getMaxParticipants());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());
        assertEquals(1.35, dto.getLatitude());
        assertEquals(103.82, dto.getLongitude());
        assertEquals("East Coast Park", dto.getLocationName());

        // Test toEntity
        com.example.EcoGo.model.Activity activity = dto.toEntity();
        assertEquals("Beach Cleanup", activity.getTitle());
        assertEquals("OFFLINE", activity.getType());
    }

    // ==================== ChallengeRequestDto ====================
    @Test
    void challengeRequestDto_allFieldsAndToEntity() {
        ChallengeRequestDto dto = new ChallengeRequestDto();
        dto.setTitle("Walk 10km");
        dto.setDescription("Walk at least 10km");
        dto.setType("DISTANCE");
        dto.setTarget(10.0);
        dto.setReward(100);
        dto.setBadge("walker");
        dto.setIcon("walk_icon.png");
        dto.setStatus("ACTIVE");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        dto.setStartTime(start);
        dto.setEndTime(end);

        assertEquals("Walk 10km", dto.getTitle());
        assertEquals("Walk at least 10km", dto.getDescription());
        assertEquals("DISTANCE", dto.getType());
        assertEquals(10.0, dto.getTarget());
        assertEquals(100, dto.getReward());
        assertEquals("walker", dto.getBadge());
        assertEquals("walk_icon.png", dto.getIcon());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(start, dto.getStartTime());
        assertEquals(end, dto.getEndTime());

        // Test toEntity
        com.example.EcoGo.model.Challenge challenge = dto.toEntity();
        assertEquals("Walk 10km", challenge.getTitle());
        assertEquals("DISTANCE", challenge.getType());
    }

    // ==================== AdvertisementRequestDto ====================
    @Test
    void advertisementRequestDto_allFieldsAndToEntity() {
        AdvertisementRequestDto dto = new AdvertisementRequestDto();
        dto.setName("Summer Sale");
        dto.setDescription("Big summer sale");
        dto.setStatus("ACTIVE");
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setImageUrl("sale.jpg");
        dto.setLinkUrl("https://example.com/sale");
        dto.setPosition("homepage");

        assertEquals("Summer Sale", dto.getName());
        assertEquals("Big summer sale", dto.getDescription());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(start, dto.getStartDate());
        assertEquals(end, dto.getEndDate());
        assertEquals("sale.jpg", dto.getImageUrl());
        assertEquals("https://example.com/sale", dto.getLinkUrl());
        assertEquals("homepage", dto.getPosition());

        // Test toEntity
        com.example.EcoGo.model.Advertisement ad = dto.toEntity();
        assertEquals("Summer Sale", ad.getName());
        assertEquals("ACTIVE", ad.getStatus());
    }

    // ==================== BadgeDto + BadgeIconDto ====================
    @Test
    void badgeDto_allFieldsAndToEntity() {
        BadgeDto dto = new BadgeDto();
        dto.setBadgeId("b1");
        dto.setName(Map.of("en", "Eco Warrior"));
        dto.setDescription(Map.of("en", "Save the planet"));
        dto.setPurchaseCost(500);
        dto.setCategory("environmental");
        dto.setSubCategory("carbon");
        dto.setAcquisitionMethod("purchase");
        dto.setCarbonThreshold(100.0);
        dto.setIsActive(true);

        BadgeDto.BadgeIconDto iconDto = new BadgeDto.BadgeIconDto();
        iconDto.setUrl("badge.png");
        iconDto.setColorScheme("green");
        dto.setIcon(iconDto);

        assertEquals("b1", dto.getBadgeId());
        assertEquals("Eco Warrior", dto.getName().get("en"));
        assertEquals("Save the planet", dto.getDescription().get("en"));
        assertEquals(500, dto.getPurchaseCost());
        assertEquals("environmental", dto.getCategory());
        assertEquals("carbon", dto.getSubCategory());
        assertEquals("purchase", dto.getAcquisitionMethod());
        assertEquals(100.0, dto.getCarbonThreshold());
        assertTrue(dto.getIsActive());
        assertEquals("badge.png", dto.getIcon().getUrl());
        assertEquals("green", dto.getIcon().getColorScheme());

        // Test toEntity
        com.example.EcoGo.model.Badge badge = dto.toEntity();
        assertEquals("b1", badge.getBadgeId());
        assertNotNull(badge.getIcon());
    }

    @Test
    void badgeDto_toEntity_nullIcon() {
        BadgeDto dto = new BadgeDto();
        dto.setBadgeId("b2");
        dto.setIcon(null);

        com.example.EcoGo.model.Badge badge = dto.toEntity();
        assertEquals("b2", badge.getBadgeId());
        assertNull(badge.getIcon());
    }

    // ==================== LeaderboardRankingDto ====================
    @Test
    void leaderboardRankingDto_defaultConstructor() {
        LeaderboardRankingDto dto = new LeaderboardRankingDto();
        assertNull(dto.getUserId());
        assertNull(dto.getNickname());
        assertEquals(0, dto.getRank());
        assertEquals(0.0, dto.getCarbonSaved());
        assertFalse(dto.isVip());
        assertEquals(0L, dto.getRewardPoints());
    }

    @Test
    void leaderboardRankingDto_allArgsConstructor_daily() {
        LeaderboardRankingDto dto = new LeaderboardRankingDto("u1", "Alice", 1, 50.5, true, "DAILY");
        assertEquals("u1", dto.getUserId());
        assertEquals("Alice", dto.getNickname());
        assertEquals(1, dto.getRank());
        assertEquals(50.5, dto.getCarbonSaved());
        assertTrue(dto.isVip());
        assertEquals(100L, dto.getRewardPoints()); // rank 1 daily: (11-1)*10 = 100
    }

    @Test
    void leaderboardRankingDto_allArgsConstructor_monthly() {
        LeaderboardRankingDto dto = new LeaderboardRankingDto("u2", "Bob", 5, 30.0, false, "MONTHLY");
        assertEquals(600L, dto.getRewardPoints()); // rank 5 monthly: (11-5)*100 = 600
    }

    @Test
    void leaderboardRankingDto_rank11_noReward() {
        LeaderboardRankingDto dto = new LeaderboardRankingDto("u3", "Charlie", 11, 10.0, false, "DAILY");
        assertEquals(0L, dto.getRewardPoints()); // rank 11 -> 0
    }

    @Test
    void leaderboardRankingDto_setters() {
        LeaderboardRankingDto dto = new LeaderboardRankingDto();
        dto.setUserId("u1");
        dto.setNickname("TestUser");
        dto.setRank(3);
        dto.setCarbonSaved(25.5);
        dto.setVip(true);
        dto.setRewardPoints(80L);

        assertEquals("u1", dto.getUserId());
        assertEquals("TestUser", dto.getNickname());
        assertEquals(3, dto.getRank());
        assertEquals(25.5, dto.getCarbonSaved());
        assertTrue(dto.isVip());
        assertEquals(80L, dto.getRewardPoints());
    }

    // ==================== LeaderboardStatsDto ====================
    @Test
    void leaderboardStatsDto_constructorAndGetters() {
        List<LeaderboardRankingDto> rankings = List.of(new LeaderboardRankingDto());
        Page<LeaderboardRankingDto> page = new PageImpl<>(rankings);
        LeaderboardStatsDto dto = new LeaderboardStatsDto(page, 1000L, 50L, 200L);

        assertNotNull(dto.getRankingsPage());
        assertEquals(1000L, dto.getTotalCarbonSaved());
        assertEquals(50L, dto.getTotalVipUsers());
        assertEquals(200L, dto.getTotalRewardsDistributed());
    }

    @Test
    void leaderboardStatsDto_setters() {
        Page<LeaderboardRankingDto> page = new PageImpl<>(List.of());
        LeaderboardStatsDto dto = new LeaderboardStatsDto(page, 0, 0, 0);
        dto.setRankingsPage(page);
        dto.setTotalCarbonSaved(500L);
        dto.setTotalVipUsers(25L);
        dto.setTotalRewardsDistributed(100L);

        assertEquals(500L, dto.getTotalCarbonSaved());
        assertEquals(25L, dto.getTotalVipUsers());
        assertEquals(100L, dto.getTotalRewardsDistributed());
    }

    // ==================== PageResponse ====================
    @Test
    void pageResponse_constructorAndGetters() {
        List<String> items = List.of("a", "b", "c");
        PageResponse<String> pr = new PageResponse<>(items, 10L, 1, 3);
        assertEquals(items, pr.getList());
        assertEquals(10L, pr.getTotal());
        assertEquals(1, pr.getPage());
        assertEquals(3, pr.getSize());
        assertEquals(4L, pr.getTotalPages()); // ceil(10/3) = 4
    }

    @Test
    void pageResponse_setters() {
        PageResponse<String> pr = new PageResponse<>(List.of(), 0, 0, 1);
        pr.setList(List.of("x"));
        pr.setTotal(100L);
        pr.setPage(5);
        pr.setSize(20);
        pr.setTotalPages(5L);

        assertEquals(1, pr.getList().size());
        assertEquals(100L, pr.getTotal());
        assertEquals(5, pr.getPage());
        assertEquals(20, pr.getSize());
        assertEquals(5L, pr.getTotalPages());
    }

    // ==================== UserResponseDto ====================
    @Test
    void userResponseDto_defaultConstructor() {
        UserResponseDto dto = new UserResponseDto();
        assertNull(dto.getEmail());
        assertNull(dto.getUserid());
        assertNull(dto.getNickname());
        assertNull(dto.getPhone());
    }

    @Test
    void userResponseDto_threeArgConstructor() {
        UserResponseDto dto = new UserResponseDto("u1", "Alice", "12345");
        assertEquals("u1", dto.getUserid());
        assertEquals("Alice", dto.getNickname());
        assertEquals("12345", dto.getPhone());
    }

    @Test
    void userResponseDto_fourArgConstructor() {
        UserResponseDto dto = new UserResponseDto("alice@example.com", "u1", "Alice", "12345");
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("u1", dto.getUserid());
        assertEquals("Alice", dto.getNickname());
        assertEquals("12345", dto.getPhone());
    }

    @Test
    void userResponseDto_setters() {
        UserResponseDto dto = new UserResponseDto();
        dto.setEmail("bob@example.com");
        dto.setUserid("u2");
        dto.setNickname("Bob");
        dto.setPhone("99999");
        assertEquals("bob@example.com", dto.getEmail());
        assertEquals("u2", dto.getUserid());
        assertEquals("Bob", dto.getNickname());
        assertEquals("99999", dto.getPhone());
    }

    // ==================== ResponseMessage ====================
    @Test
    void responseMessage_constructor() {
        ResponseMessage<String> msg = new ResponseMessage<>(200, "OK", "data");
        assertEquals(200, msg.getCode());
        assertEquals("OK", msg.getMessage());
        assertEquals("data", msg.getData());
    }

    @Test
    void responseMessage_successFactory() {
        ResponseMessage<String> msg = ResponseMessage.success("test");
        assertEquals(200, msg.getCode());
        assertEquals("success!", msg.getMessage());
        assertEquals("test", msg.getData());
    }

    @Test
    void responseMessage_setters() {
        ResponseMessage<String> msg = new ResponseMessage<>(0, "", null);
        msg.setCode(500);
        msg.setMessage("Error");
        msg.setData("details");
        assertEquals(500, msg.getCode());
        assertEquals("Error", msg.getMessage());
        assertEquals("details", msg.getData());
    }

    // ==================== ChurnRiskDTO ====================
    @Test
    void churnRiskDto_defaultConstructor() {
        ChurnRiskDTO dto = new ChurnRiskDTO();
        assertNull(dto.getUserId());
        assertNull(dto.getRiskLevel());
    }

    @Test
    void churnRiskDto_allArgsConstructor() {
        ChurnRiskDTO dto = new ChurnRiskDTO("u1", "HIGH");
        assertEquals("u1", dto.getUserId());
        assertEquals("HIGH", dto.getRiskLevel());
    }

    @Test
    void churnRiskDto_setters() {
        ChurnRiskDTO dto = new ChurnRiskDTO();
        dto.setUserId("u2");
        dto.setRiskLevel("LOW");
        assertEquals("u2", dto.getUserId());
        assertEquals("LOW", dto.getRiskLevel());
    }
}
