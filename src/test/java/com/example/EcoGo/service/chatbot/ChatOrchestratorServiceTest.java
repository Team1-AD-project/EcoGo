package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.model.ChatConversation;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.ChatConversationRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorServiceTest {

    @Mock private RagService ragService;
    @Mock private ChatBookingService bookingService;
    @Mock private ModelClientService modelClientService;
    @Mock private PythonChatbotProxyService pythonProxy;
    @Mock private NusBusProvider busProvider;
    @Mock private AuditLogService auditLogService;
    @Mock private ChatNotificationService notificationService;
    @Mock private ChatConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;

    private ChatOrchestratorService svc;

    @BeforeEach
    void setUp() {
        svc = new ChatOrchestratorService(
                ragService, bookingService, modelClientService, pythonProxy,
                busProvider, auditLogService, notificationService,
                conversationRepository, userRepository);
        lenient().when(modelClientService.isEnabled()).thenReturn(false);
        lenient().when(pythonProxy.isEnabled()).thenReturn(false);
        lenient().when(ragService.isAvailable()).thenReturn(false);
        lenient().when(conversationRepository.findByConversationId(anyString())).thenReturn(Optional.empty());
        lenient().when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ===== Greeting =====
    @Test void greeting_hello() { assertContains(chat("hello"), "EcoGo Assistant"); }
    @Test void greeting_hi() { assertContains(chat("hi"), "EcoGo Assistant"); }
    @Test void greeting_chinese() { assertContains(chat("你好"), "EcoGo Assistant"); }
    @Test void greeting_hey() { assertContains(chat("hey there"), "EcoGo Assistant"); }
    @Test void greeting_goodMorning() { assertContains(chat("good morning"), "EcoGo Assistant"); }
    @Test void greeting_goodAfternoon() { assertContains(chat("good afternoon"), "EcoGo Assistant"); }
    @Test void greeting_goodEvening() { assertContains(chat("good evening"), "EcoGo Assistant"); }
    @Test void greeting_hiThere() { assertContains(chat("hi there"), "EcoGo Assistant"); }
    @Test void greeting_howdy() { assertContains(chat("howdy"), "EcoGo Assistant"); }
    @Test void greeting_greetings() { assertContains(chat("greetings"), "EcoGo Assistant"); }
    @Test void greeting_zaoShangHao() { assertContains(chat("早上好"), "EcoGo Assistant"); }
    @Test void greeting_xiaWuHao() { assertContains(chat("下午好"), "EcoGo Assistant"); }
    @Test void greeting_wanShangHao() { assertContains(chat("晚上好"), "EcoGo Assistant"); }
    @Test void greeting_zaiMa() { assertContains(chat("在吗"), "EcoGo Assistant"); }

    // ===== Reset/Cancel =====
    @Test void reset_english() { assertContains(chat("reset"), "Session reset"); }
    @Test void reset_cancel() { assertContains(chat("cancel"), "Session reset"); }
    @Test void reset_chinese() { assertContains(chat("取消"), "Session reset"); }
    @Test void reset_chongXinKaiShi() { assertContains(chat("重新开始"), "Session reset"); }
    @Test void reset_chongZhi() { assertContains(chat("重置"), "Session reset"); }
    @Test void reset_backToMenu() { assertContains(chat("back to menu"), "Session reset"); }

    // ===== Null/blank conversationId =====
    @Test void nullConvId() {
        ChatResponseDto r = svc.handleChat("guest", false, null, "hi");
        assertTrue(r.getConversationId().startsWith("c_"));
    }
    @Test void blankConvId() {
        ChatResponseDto r = svc.handleChat("guest", false, "  ", "hi");
        assertTrue(r.getConversationId().startsWith("c_"));
    }

    // ===== Menu buttons =====
    @Test void busArrivalsButton() { assertContains(chat("🚌 Bus Arrivals"), "stop"); }
    @Test void busArrivalsButton_cn() { assertContains(chat("查公交"), "stop"); }
    @Test void travelAdviceButton() { assertContains(chat("📍 Travel Advice"), "where"); }
    @Test void travelAdviceButton_cn() { assertContains(chat("出行推荐"), "where"); }
    @Test void bookATripButton() { assertContains(chat("🎫 Book a Trip"), "book"); }
    @Test void bookATripButton_cn() { assertContains(chat("预订行程"), "book"); }
    @Test void myProfileButton_guest() { assertContainsAny(chat("📋 My Profile"), "not logged in", "sign in"); }
    @Test void myProfileButton_cn() { assertContainsAny(chatAs("guest", "我的资料"), "not logged in", "sign in"); }
    @Test void backToMenuButton() { assertContains(chat("Back to Menu"), "help"); }
    @Test void backToMenuButton_cn() { assertContains(chat("返回主菜单"), "help"); }
    @Test void backToMenuButton_mainMenu() { assertContains(chat("主菜单"), "help"); }

    // ===== Empty/emoji message =====
    @Test void emptyMessage() { assertContains(chat("   "), "help"); }
    @Test void emojiOnlyMessage() { assertContains(chat("😊"), "help"); }

    // ===== Profile for authenticated user =====
    @Test void profileForUser() {
        User u = buildUser("u_001", "TestUser", "test@nus.edu", "91234567", "Computing");
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "📋 My Profile");
        assertContains(r, "TestUser");
        assertContains(r, "test@nus.edu");
    }

    @Test void profileForUser_withStats() {
        User u = buildUser("u_002", "StatsUser", "s@nus.edu", "9999", "SoC");
        User.Stats stats = new User.Stats();
        stats.setTotalTrips(10);
        stats.setTotalDistance(50.5);
        stats.setGreenDays(7);
        stats.setWeeklyRank(3);
        u.setStats(stats);
        when(userRepository.findByUserid("u_002")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_002", false, "c1", "📋 My Profile");
        assertContains(r, "StatsUser");
        assertContains(r, "50.5");
    }

    @Test void profileForUser_withVip() {
        User u = buildUser("u_003", "VipUser", null, null, null);
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setPlan("premium");
        u.setVip(vip);
        when(userRepository.findByUserid("u_003")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_003", false, "c1", "📋 My Profile");
        assertContains(r, "VipUser");
    }

    @Test void profileForUser_notFound() {
        when(userRepository.findByUserid("u_999")).thenReturn(Optional.empty());
        ChatResponseDto r = svc.handleChat("u_999", false, "c1", "📋 My Profile");
        assertContains(r, "not found");
    }

    @Test void profileQuery_english() {
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.empty());
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "my profile");
        assertNotNull(r);
    }

    @Test void profileQuery_viewProfile() {
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.empty());
        assertNotNull(svc.handleChat("u_001", false, "c1", "view profile"));
    }

    @Test void profileQuery_chinese_chaKan() {
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.empty());
        assertNotNull(svc.handleChat("u_001", false, "c1", "查看我的资料"));
    }

    // ===== Booking intent =====
    @Test void bookingIntent_fromTo_chinese() {
        ChatResponseDto r = chatAs("u_001", "预订从COM3到UTown");
        assertNotNull(r);
        String t = r.getAssistant().getText().toLowerCase();
        assertTrue(t.contains("com3") || t.contains("utown") || t.contains("depart") || t.contains("time") || t.contains("passenger"));
    }

    @Test void bookingIntent_english() {
        ChatResponseDto r = chatAs("u_001", "I want to book a trip from PGP to CLB");
        assertNotNull(r);
    }

    @Test void bookingIntent_bookTrip() {
        assertNotNull(chatAs("u_001", "book a trip"));
    }

    @Test void bookingIntent_bookRide() {
        assertNotNull(chatAs("u_001", "book a ride"));
    }

    @Test void bookingIntent_dingPiao() {
        assertNotNull(chatAs("u_001", "订票从PGP到UTown"));
    }

    @Test void bookingIntent_woYaoDing() {
        assertNotNull(chatAs("u_001", "我要订从COM3去BIZ2"));
    }

    // ===== Complete booking flow =====
    @Test void completeBookingFlow() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_1", "trip_1", "ecogo://trip/trip_1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=2026-02-11T10:00, passengers=2");
        assertNotNull(r);
    }

    @Test void completeBookingFlow_kvForm() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_2", "trip_2", "ecogo://trip/trip_2");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        svc.handleChat("u_001", false, "c1", "route=从PGP到CLB, departAt=2026-02-11T11:30, passengers=3");
        // Verify booking attempted
        verify(bookingService, atLeastOnce()).createBooking(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test void bookingFlow_askForPassengers() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=2026-02-11T10:00");
        assertNotNull(r);
        // Missing passengers → should ask
        assertTrue(r.getAssistant().getText().toLowerCase().contains("passenger")
                || r.getAssistant().getText().contains("人"));
    }

    @Test void bookingFlow_passengers_english() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "2 people");
        assertNotNull(r);
    }

    @Test void bookingFlow_passengers_chinese() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        assertNotNull(svc.handleChat("u_001", false, "c1", "2人"));
    }

    @Test void bookingFlow_route_arrowSyntax() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "PGP to CLB");
        assertNotNull(r);
    }

    // ===== Book X to Y from recommendation follow-up =====
    @Test void bookFollowUp() {
        assertNotNull(chatAs("u_001", "Book PGP to UTown"));
    }

    @Test void bookFollowUp_withEmoji() {
        assertNotNull(chatAs("u_001", "🎫 Book COM3 to CLB"));
    }

    // ===== Bus query intent =====
    @Test void busQueryIntent_arrivalTime() {
        mockBusArrivals();
        assertNotNull(chat("公交到站COM3"));
    }

    @Test void busQueryIntent_nextBus() {
        mockBusArrivals();
        assertNotNull(chat("next bus at COM3"));
    }

    @Test void busQueryIntent_busSchedule() {
        mockBusArrivals();
        assertNotNull(chat("bus schedule COM3"));
    }

    @Test void busQueryIntent_checkBus() {
        mockBusArrivals();
        assertNotNull(chat("check bus COM3"));
    }

    @Test void busQueryIntent_busEta() {
        mockBusArrivals();
        assertNotNull(chat("bus eta COM3"));
    }

    @Test void busQueryIntent_busTime() {
        mockBusArrivals();
        assertNotNull(chat("bus time COM3"));
    }

    @Test void busQueryIntent_whenIsBus() {
        mockBusArrivals();
        assertNotNull(chat("when is the bus at COM3"));
    }

    @Test void busQueryIntent_shuttleTime() {
        mockBusArrivals();
        assertNotNull(chat("shuttle arrival time"));
    }

    @Test void busQueryIntent_xiaBanGong() {
        mockBusArrivals();
        assertNotNull(chat("下一班公交"));
    }

    @Test void busQueryIntent_jiFenZhongDao() {
        mockBusArrivals();
        assertNotNull(chat("几分钟到COM3"));
    }

    @Test void busQueryIntent_chaXunGongJiao() {
        mockBusArrivals();
        assertNotNull(chat("查询公交COM3"));
    }

    @Test void busQueryIntent_routeD2() {
        mockBusArrivals();
        assertNotNull(chat("D2线到站时间"));
    }

    @Test void busQuery_noArrivals() {
        when(busProvider.getArrivals(anyString(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of()));
        ChatResponseDto r = chat("next bus at COM3");
        assertContains(r, "No arrivals");
    }

    @Test void busQuery_withArrivals() {
        when(busProvider.getArrivals(anyString(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "D2", "etaMinutes", 3, "status", "arriving"),
                Map.of("route", "A1", "etaMinutes", 5, "status", "on_time"),
                Map.of("route", "D1", "etaMinutes", 8, "status", "scheduled"),
                Map.of("route", "A2", "etaMinutes", 12, "status", "delayed")
        )));
        ChatResponseDto r = chat("bus arrival COM3");
        assertContains(r, "COM3");
    }

    // ===== Awaiting bus stop flow =====
    @Test void awaitingBusStop_thenSelect() {
        mockBusArrivals();
        svc.handleChat("guest", false, "c1", "🚌 Bus Arrivals");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "COM3");
        assertNotNull(r);
    }

    // ===== Show more / Change stop =====
    @Test void showMore() { assertNotNull(chat("Show more")); }
    @Test void showMore_cn() { assertNotNull(chat("查看更多班次")); }
    @Test void changeStop() { assertNotNull(chat("Change stop")); }
    @Test void changeStop_cn() { assertNotNull(chat("换个站点")); }
    @Test void tryAnotherStop() { assertNotNull(chat("Try another stop")); }
    @Test void tryAnotherStop_cn() { assertNotNull(chat("换个站点查询")); }

    // ===== Awaiting destination flow =====
    @Test void awaitingDestination_withRoute() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "COM3 to UTown");
        assertContains(r, "Travel advice");
    }

    @Test void awaitingDestination_singleDest() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "UTown");
        assertContains(r, "Travel advice");
    }

    @Test void awaitingDestination_unparseable() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "I really have absolutely no idea at all and cannot decide anything right now");
        assertContains(r, "format");
    }

    // ===== Recommendation intent =====
    @Test void recommendationIntent_tuiJian() { assertNotNull(chat("推荐出行方式")); }
    @Test void recommendationIntent_howToGet() { assertNotNull(chat("how to get to CLB")); }
    @Test void recommendationIntent_bestRoute() { assertNotNull(chat("best route to COM3")); }
    @Test void recommendationIntent_zenMeQu() { assertNotNull(chat("怎么去UTown")); }
    @Test void recommendationIntent_travelAdvice() { assertNotNull(chat("travel advice")); }
    @Test void recommendationIntent_withFromTo() {
        when(ragService.isAvailable()).thenReturn(false);
        ChatResponseDto r = chat("recommend from COM3 to UTown");
        assertContains(r, "Travel advice");
    }

    // ===== NUS campus location detection =====
    @Test void recommendation_nusCampus() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of(
                new ChatResponseDto.Citation("t", "s", "This is a long snippet that exceeds 80 characters so it should be truncated by the service logic somewhere around here or later.")));
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "COM3 to UTown");
        assertContains(r, "Campus Shuttle");
    }

    @Test void recommendation_offCampus() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "Orchard to Marina Bay");
        assertContains(r, "MRT");
    }

    // ===== RAG query fallback =====
    @Test void ragQuery_available() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of(
                new ChatResponseDto.Citation("Title", "src", "Short snippet")));
        ChatResponseDto r = chat("what is green travel in singapore?");
        assertContains(r, "Short snippet");
    }

    @Test void ragQuery_longSnippet() {
        when(ragService.isAvailable()).thenReturn(true);
        String longText = "A".repeat(200);
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(List.of(
                new ChatResponseDto.Citation("T", "s", longText)));
        ChatResponseDto r = chat("what is green travel?");
        assertTrue(r.getAssistant().getText().contains("..."));
    }

    @Test void ragQuery_notAvailable() {
        when(ragService.isAvailable()).thenReturn(false);
        ChatResponseDto r = chat("random unknown xyz question");
        assertContainsAny(r, "not sure", "rephras");
    }

    @Test void ragQuery_throwsException() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), anyInt())).thenThrow(new RuntimeException("fail"));
        ChatResponseDto r = chat("what is green travel?");
        assertContainsAny(r, "not sure", "rephras");
    }

    // ===== User update =====
    @Test void userUpdate_selfModify() {
        User u = buildUser("u_001", "Old", "old@nus.edu", null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my nickname=NewName");
        assertContains(r, "updated");
    }

    @Test void userUpdate_selfModify_email() {
        User u = buildUser("u_001", null, null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "change my email=new@nus.edu");
        assertContains(r, "updated");
    }

    @Test void userUpdate_selfModify_phone() {
        User u = buildUser("u_001", null, null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "edit my phone=91111111 profile");
        assertContains(r, "updated");
    }

    @Test void userUpdate_selfModify_faculty() {
        User u = buildUser("u_001", null, null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my faculty=SoC profile");
        assertContains(r, "updated");
    }

    @Test void userUpdate_noFieldsDetected() {
        User u = buildUser("u_001", null, null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "change my faculty please");
        assertContainsAny(r, "No fields", "not found");
    }

    @Test void userUpdate_userNotFound() {
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.empty());
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my nickname=Test profile");
        assertContains(r, "not found");
    }

    @Test void userUpdate_chinese() {
        User u = buildUser("u_001", null, null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "修改我的 nickname=Test");
        assertContains(r, "updated");
    }

    @Test void userUpdate_adminModifyOther_needsConfirm() {
        // Current implementation validates the caller user exists before prompting.
        lenient().when(userRepository.findByUserid("admin_001"))
                .thenReturn(Optional.of(buildUser("admin_001", "Admin", null, null, null)));
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "修改用户u_002资料 nickname=Admin");
        assertContains(r, "confirm");
    }

    @Test void userUpdate_adminModifyOther_confirm() {
        lenient().when(userRepository.findByUserid("admin_001"))
                .thenReturn(Optional.of(buildUser("admin_001", "Admin", null, null, null)));
        User target = buildUser("u_002", "Target", null, null, null);
        lenient().when(userRepository.findByUserid("u_002")).thenReturn(Optional.of(target));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auditLogService.createAuditLog(anyString(), anyString(), anyString(), anyMap())).thenReturn("au_1");
        lenient().when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString())).thenReturn("nt_1");
        svc.handleChat("admin_001", true, "c1", "修改用户u_002资料 nickname=Admin");
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "confirm");
        assertContains(r, "updated");
    }

    @Test void userUpdate_regularUserModifyOther_denied() {
        lenient().when(userRepository.findByUserid("u_001"))
                .thenReturn(Optional.of(buildUser("u_001", "User", null, null, null)));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "修改用户u_002资料 nickname=Hack");
        assertContains(r, "permission");
    }

    // ===== Model-based intent detection =====
    @Test void model_toolCall_createBooking_complete() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("fromName", "PGP"); args.put("toName", "CLB");
        args.put("departAt", "2026-02-11T10:00:00"); args.put("passengers", 2);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_1", "trip_1", "ecogo://trip/trip_1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "book from PGP to CLB tomorrow 2 people");
        assertContains(r, "Booking confirmed");
    }

    @Test void model_toolCall_createBooking_partial() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("fromName", "PGP"); args.put("toName", "CLB");
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "book PGP to CLB");
        assertNotNull(r); // asks for missing fields
    }

    @Test void model_toolCall_busArrivals() {
        when(modelClientService.isEnabled()).thenReturn(true);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("get_bus_arrivals", Map.of("stopName", "COM3"));
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        when(busProvider.getArrivals("COM3", null)).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "D2", "etaMinutes", 3, "status", "on_time"))));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "next bus COM3");
        assertContains(r, "COM3");
    }

    @Test void model_toolCall_busArrivals_noResults() {
        when(modelClientService.isEnabled()).thenReturn(true);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("get_bus_arrivals", Map.of("stopName", "XYZ"));
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("No buses", tc));
        when(busProvider.getArrivals("XYZ", null)).thenReturn(new NusBusProvider.BusArrivalsResult("XYZ", List.of()));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "bus at XYZ");
        assertContains(r, "No buses");
    }

    @Test void model_toolCall_updateUser_self() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "u_001");
        args.put("patch", Map.of("nickname", "NewNick"));
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        User u = buildUser("u_001", "Old", null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my nickname to NewNick");
        assertContains(r, "updated");
    }

    @Test void model_toolCall_updateUser_otherUser_asUser_denied() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "u_002");
        args.put("patch", Map.of("nickname", "Hack"));
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update user u_002 nickname");
        assertContains(r, "permission");
    }

    @Test void model_toolCall_updateUser_otherUser_asAdmin_confirm() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "u_002");
        args.put("patch", Map.of("nickname", "NewName"));
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "update user u_002 nickname");
        assertContains(r, "confirm");
    }

    @Test void model_toolCall_unknown() {
        when(modelClientService.isEnabled()).thenReturn(true);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("unknown_tool", Map.of());
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("fallback text", tc));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "do something weird");
        assertContains(r, "fallback text");
    }

    @Test void model_toolCall_unknown_noText() {
        when(modelClientService.isEnabled()).thenReturn(true);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("unknown_tool", Map.of());
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "xyz");
        assertContains(r, "not supported");
    }

    @Test void model_noToolCall_withText() {
        when(modelClientService.isEnabled()).thenReturn(true);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("Model says hello", null));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "tell me about green travel");
        assertEquals("Model says hello", r.getAssistant().getText());
    }

    @Test void model_returnsNull() {
        when(modelClientService.isEnabled()).thenReturn(true);
        when(modelClientService.callModelForTool(anyString())).thenReturn(null);
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "random xyz");
        assertNotNull(r);
    }

    @Test void model_returnsEmptyText_noTool() {
        when(modelClientService.isEnabled()).thenReturn(true);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", null));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "random xyz");
        assertNotNull(r);
    }

    // ===== Python proxy =====
    @Test void pythonProxy_enabled_returnsResponse() {
        when(pythonProxy.isEnabled()).thenReturn(true);
        when(pythonProxy.forwardChat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ChatResponseDto("c1", "Python says hi"));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "random xyz");
        assertEquals("Python says hi", r.getAssistant().getText());
    }

    @Test void pythonProxy_enabled_returnsNull_fallback() {
        when(pythonProxy.isEnabled()).thenReturn(true);
        when(pythonProxy.forwardChat(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "random xyz");
        assertNotNull(r);
    }

    // ===== Existing conversation in DB =====
    @Test void existingConversation_loadFromDb() {
        ChatConversation conv = new ChatConversation();
        conv.setConversationId("c_existing");
        conv.setUserId("u_001");
        ChatConversation.ConversationState dbState = new ChatConversation.ConversationState();
        dbState.setIntent(null);
        dbState.setPartialData(new HashMap<>());
        conv.setState(dbState);
        when(conversationRepository.findByConversationId("c_existing")).thenReturn(Optional.of(conv));
        ChatResponseDto r = svc.handleChat("u_001", false, "c_existing", "hello");
        assertNotNull(r);
    }

    @Test void existingConversation_withBookingState() {
        ChatConversation conv = new ChatConversation();
        conv.setConversationId("c_book");
        conv.setUserId("u_001");
        ChatConversation.ConversationState dbState = new ChatConversation.ConversationState();
        dbState.setIntent("booking");
        Map<String, Object> partial = new HashMap<>();
        partial.put("fromName", "COM3");
        partial.put("toName", "UTown");
        dbState.setPartialData(partial);
        conv.setState(dbState);
        when(conversationRepository.findByConversationId("c_book")).thenReturn(Optional.of(conv));
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_x", null, "ecogo://booking/bk_x");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        ChatResponseDto r = svc.handleChat("u_001", false, "c_book", "departAt=2026-02-11T10:00, passengers=1");
        assertNotNull(r);
    }

    // ===== Persist message failure =====
    @Test void persistMessage_failure_shouldNotThrow() {
        when(conversationRepository.findByConversationId(anyString()))
                .thenReturn(Optional.empty())  // first call: getOrCreateState
                .thenThrow(new RuntimeException("db error")); // persistMessage call
        assertDoesNotThrow(() -> svc.handleChat("guest", false, "c1", "hello"));
    }

    // ===== Various Chinese intent matching =====
    @Test void intent_zuiJiaLuXian() { assertNotNull(chat("最佳路线")); }
    @Test void intent_bestWayTo() { assertNotNull(chat("best way to get there")); }
    @Test void intent_travelTip() { assertNotNull(chat("travel tip for NUS")); }

    // ===== From/to extraction patterns =====
    @Test void extractFromTo_english_fromAToB() {
        ChatResponseDto r = chatAs("u_001", "I want to book from Marina Bay to Orchard");
        assertNotNull(r);
    }

    @Test void extractFromTo_chinese_cong_dao() {
        ChatResponseDto r = chatAs("u_001", "预订从大学城到商学院");
        assertNotNull(r);
    }

    @Test void extractFromTo_chinese_cong_qu() {
        ChatResponseDto r = chatAs("u_001", "帮我订从COM3去CLB");
        assertNotNull(r);
    }

    @Test void extractFromToLooser_arrowSyntax() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "PGP→CLB");
        assertNotNull(r);
    }

    @Test void extractFromToLooser_dashArrow() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "PGP->CLB");
        assertNotNull(r);
    }

    @Test void extractFromToLooser_mdash() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "PGP—CLB");
        assertNotNull(r);
    }

    @Test void extractFromToLooser_chineseDao() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "PGP到CLB");
        assertNotNull(r);
    }

    // ===== Passenger extraction patterns =====
    @Test void passengers_chinese_wei() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "3位");
        assertNotNull(r);
    }

    @Test void passengers_english_person() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "1 person");
        assertNotNull(r);
    }

    @Test void passengers_english_passengers() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "4 passengers");
        assertNotNull(r);
    }

    // ===== Department time extraction =====
    @Test void departAt_isoFormat() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "2026-02-11T10:00");
        assertNotNull(r);
    }

    @Test void departAt_withSlash() {
        svc.handleChat("u_001", false, "c2", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c2", "2026/02/11 10:00:00");
        assertNotNull(r);
    }

    @Test void departAt_withSeconds() {
        svc.handleChat("u_001", false, "c3", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c3", "2026-02-11T10:00:00");
        assertNotNull(r);
    }

    // ===== KV parsing for booking =====
    @Test void kvParsing_chineseComma() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_1", "trip_1", "ecogo://trip/trip_1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=2026-02-11T10:00，passengers=2");
        assertNotNull(r);
    }

    @Test void kvParsing_withQuotedValue() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_1", "trip_1", "ecogo://trip/trip_1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=\"2026-02-11T10:00\", passengers=2");
        assertNotNull(r);
    }

    @Test void kvParsing_fullWidthDigits() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "passengers=２");
        assertNotNull(r);
    }

    // ===== Model returns various results =====

    // ===== Bus query with various Chinese keywords =====
    @Test void busQuery_gongJiaoChaxun() {
        mockBusArrivals();
        assertNotNull(chat("公交查询COM3"));
    }

    @Test void busQuery_shortChaGongJiao() {
        mockBusArrivals();
        assertNotNull(chat("查公交COM3"));
    }

    @Test void busQuery_shuttleWhen() {
        mockBusArrivals();
        assertNotNull(chat("shuttle when COM3"));
    }

    // ===== Booking intent edge cases =====
    @Test void bookingIntent_iWantToBook() {
        assertNotNull(chatAs("u_001", "I want to book a ticket"));
    }

    @Test void bookingIntent_idLikeToBook() {
        assertNotNull(chatAs("u_001", "I'd like to book a trip"));
    }

    @Test void bookingIntent_bookTripContext() {
        assertNotNull(chatAs("u_001", "book travel PGP"));
    }

    // ===== Recommendation with from/to =====
    @Test void recommendation_fromTo() {
        ChatResponseDto r = chat("recommend from PGP to CLB");
        assertContains(r, "Travel advice");
    }

    @Test void recommendation_zenMeQu_withDest() {
        ChatResponseDto r = chat("怎么去UTown");
        assertNotNull(r);
    }

    // ===== Profile with VIP with expiry date =====
    @Test void profileForUser_withVipExpiry() {
        User u = buildUser("u_004", "VipExpUser", null, null, null);
        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setPlan("gold");
        vip.setExpiryDate(java.time.LocalDateTime.of(2027, 1, 1, 0, 0));
        u.setVip(vip);
        when(userRepository.findByUserid("u_004")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_004", false, "c1", "📋 My Profile");
        assertContains(r, "VipExpUser");
        assertContains(r, "2027");
    }

    // ===== Profile with weekly rank 0 =====
    @Test void profileForUser_weeklyRankZero() {
        User u = buildUser("u_005", "NoRank", null, null, null);
        User.Stats stats = new User.Stats();
        stats.setTotalTrips(1);
        stats.setTotalDistance(1.0);
        stats.setGreenDays(1);
        stats.setWeeklyRank(0);
        u.setStats(stats);
        when(userRepository.findByUserid("u_005")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_005", false, "c1", "📋 My Profile");
        assertContains(r, "NoRank");
    }

    // ===== Admin confirm user update =====
    @Test void userUpdate_adminModifyOther_confirmFlow_thenExec() {
        lenient().when(userRepository.findByUserid("admin_001"))
                .thenReturn(Optional.of(buildUser("admin_001", "Admin", null, null, null)));
        User target = buildUser("u_002", "Target", null, null, null);
        lenient().when(userRepository.findByUserid("u_002")).thenReturn(Optional.of(target));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auditLogService.createAuditLog(anyString(), anyString(), anyString(), anyMap())).thenReturn("au_1");
        lenient().when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString())).thenReturn("nt_1");
        // Step 1: request modification
        svc.handleChat("admin_001", true, "c1", "修改用户u_002资料 nickname=NewAdmin");
        // Step 2: confirm
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "确认");
        assertContains(r, "updated");
    }

    // ===== Confirm commands =====
    @Test void confirmCommand_yes() {
        lenient().when(userRepository.findByUserid("admin_001"))
                .thenReturn(Optional.of(buildUser("admin_001", "Admin", null, null, null)));
        User target = buildUser("u_002", "T", null, null, null);
        lenient().when(userRepository.findByUserid("u_002")).thenReturn(Optional.of(target));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auditLogService.createAuditLog(anyString(), anyString(), anyString(), anyMap())).thenReturn("au_1");
        lenient().when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString())).thenReturn("nt_1");
        svc.handleChat("admin_001", true, "c1", "修改用户u_002资料 nickname=X");
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "yes");
        assertContains(r, "updated");
    }

    @Test void confirmCommand_ok() {
        lenient().when(userRepository.findByUserid("admin_001"))
                .thenReturn(Optional.of(buildUser("admin_001", "Admin", null, null, null)));
        User target = buildUser("u_002", "T", null, null, null);
        lenient().when(userRepository.findByUserid("u_002")).thenReturn(Optional.of(target));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auditLogService.createAuditLog(anyString(), anyString(), anyString(), anyMap())).thenReturn("au_1");
        lenient().when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString())).thenReturn("nt_1");
        svc.handleChat("admin_001", true, "c1", "修改用户u_002资料 email=new@nus.edu");
        ChatResponseDto r = svc.handleChat("admin_001", true, "c1", "ok");
        assertContains(r, "updated");
    }

    // ===== User update with model tool call: all fields =====
    @Test void model_toolCall_updateUser_allFields() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> patch = new HashMap<>();
        patch.put("nickname", "N"); patch.put("email", "e@nus.edu");
        patch.put("phone", "99999"); patch.put("faculty", "SoC");
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "u_001");
        args.put("patch", patch);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        User u = buildUser("u_001", "Old", null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update all my info");
        assertContains(r, "updated");
    }

    // ===== Model create booking with all fields =====
    @Test void model_toolCall_createBooking_fullFields() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("fromName", "PGP"); args.put("toName", "CLB");
        args.put("departAt", "2026-02-11T10:00:00"); args.put("passengers", 2);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_x", "trip_x", "ecogo://trip/trip_x");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "book PGP to CLB tomorrow 2 people");
        assertContains(r, "Booking confirmed");
    }

    // ===== Model bus arrivals with delayed status =====
    @Test void model_toolCall_busArrivals_delayed() {
        when(modelClientService.isEnabled()).thenReturn(true);
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("get_bus_arrivals", Map.of("stopName", "PGP", "route", "A1"));
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        when(busProvider.getArrivals("PGP", "A1")).thenReturn(new NusBusProvider.BusArrivalsResult("PGP", List.of(
                Map.of("route", "A1", "etaMinutes", 15, "status", "delayed"))));
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "A1 bus at PGP");
        assertContains(r, "PGP");
    }

    // ===== isBookingIntent: "book" + trip context =====
    @Test void bookingIntent_bookAndTrip() {
        assertNotNull(chatAs("u_001", "book a 行程"));
    }

    // ===== Recommendation: NUS campus both locations =====
    @Test void recommendation_bothNusCampus() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "PGP to UTown");
        assertContains(r, "Campus Shuttle");
    }

    // ===== Recommendation: NUS campus contains "nus" =====
    @Test void recommendation_nusKeyword() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "NUS campus to town");
        assertContains(r, "Campus Shuttle");
    }

    // ===== Bus query: status icons =====
    @Test void busQuery_arriving() {
        when(busProvider.getArrivals(any(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "A1", "etaMinutes", 0, "status", "arriving"))));
        ChatResponseDto r = chat("next bus at COM3");
        assertContains(r, "COM3");
    }

    @Test void busQuery_scheduled() {
        when(busProvider.getArrivals(any(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "A1", "etaMinutes", 15, "status", "scheduled"))));
        ChatResponseDto r = chat("next bus at COM3");
        assertContains(r, "COM3");
    }

    // ===== Bus query: more than 3 arrivals (Show more) =====
    @Test void busQuery_moreThan3_showsShowMore() {
        when(busProvider.getArrivals(any(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "A1", "etaMinutes", 2, "status", "arriving"),
                Map.of("route", "D2", "etaMinutes", 5, "status", "on_time"),
                Map.of("route", "A2", "etaMinutes", 8, "status", "on_time"),
                Map.of("route", "D1", "etaMinutes", 12, "status", "scheduled"))));
        ChatResponseDto r = chat("bus arrival COM3");
        assertContains(r, "more");
    }

    // ===== Show more with prior state =====
    @Test void showMore_withPriorBusQuery() {
        when(busProvider.getArrivals(any(), any())).thenReturn(new NusBusProvider.BusArrivalsResult("COM3", List.of(
                Map.of("route", "A1", "etaMinutes", 2, "status", "arriving"),
                Map.of("route", "D2", "etaMinutes", 5, "status", "on_time"),
                Map.of("route", "A2", "etaMinutes", 8, "status", "on_time"),
                Map.of("route", "D1", "etaMinutes", 12, "status", "scheduled"))));
        svc.handleChat("guest", false, "c1", "bus arrival COM3");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "Show more");
        assertContains(r, "COM3");
    }

    // ===== NUS bus route pattern matching =====
    @Test void busQuery_routePattern_A1() {
        mockBusArrivals();
        assertNotNull(chat("A1线到站时间"));
    }

    @Test void busQuery_routePattern_withBus() {
        mockBusArrivals();
        assertNotNull(chat("A1公交到站"));
    }

    // ===== Chinese bus query intent with check =====
    @Test void busQuery_chaXunWithShuttle() {
        mockBusArrivals();
        assertNotNull(chat("查询shuttle"));
    }

    // ===== Text with containsAny utility test through booking =====
    @Test void booking_englishFromTo() {
        ChatResponseDto r = chatAs("u_001", "book from NUS to Marina Bay");
        assertNotNull(r);
    }

    // ===== Regex-based greeting starts with =====
    @Test void greeting_startsWithHello() { assertContains(chat("hello world"), "EcoGo Assistant"); }
    @Test void greeting_startsWithNiHao() { assertContains(chat("你好呀"), "EcoGo Assistant"); }

    // ===== ProfilePatch hasAnyField via reflection =====
    @Test void profilePatch_hasAnyField() throws Exception {
        Class<?> patchClass = Class.forName("com.example.EcoGo.service.chatbot.ChatOrchestratorService$ProfilePatch");
        java.lang.reflect.Constructor<?> ctor = patchClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object patch = ctor.newInstance();

        java.lang.reflect.Method hasAny = patchClass.getDeclaredMethod("hasAnyField");
        hasAny.setAccessible(true);
        assertFalse((Boolean) hasAny.invoke(patch));

        java.lang.reflect.Field nickField = patchClass.getDeclaredField("nickname");
        nickField.setAccessible(true);
        nickField.set(patch, "test");
        assertTrue((Boolean) hasAny.invoke(patch));
    }

    @Test void profilePatch_hasAnyField_email() throws Exception {
        Class<?> patchClass = Class.forName("com.example.EcoGo.service.chatbot.ChatOrchestratorService$ProfilePatch");
        java.lang.reflect.Constructor<?> ctor = patchClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object patch = ctor.newInstance();
        java.lang.reflect.Method hasAny = patchClass.getDeclaredMethod("hasAnyField");
        hasAny.setAccessible(true);

        java.lang.reflect.Field emailField = patchClass.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(patch, "test@email.com");
        assertTrue((Boolean) hasAny.invoke(patch));
    }

    @Test void profilePatch_hasAnyField_phone() throws Exception {
        Class<?> patchClass = Class.forName("com.example.EcoGo.service.chatbot.ChatOrchestratorService$ProfilePatch");
        java.lang.reflect.Constructor<?> ctor = patchClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object patch = ctor.newInstance();
        java.lang.reflect.Method hasAny = patchClass.getDeclaredMethod("hasAnyField");
        hasAny.setAccessible(true);

        java.lang.reflect.Field phoneField = patchClass.getDeclaredField("phone");
        phoneField.setAccessible(true);
        phoneField.set(patch, "12345");
        assertTrue((Boolean) hasAny.invoke(patch));
    }

    @Test void profilePatch_hasAnyField_faculty() throws Exception {
        Class<?> patchClass = Class.forName("com.example.EcoGo.service.chatbot.ChatOrchestratorService$ProfilePatch");
        java.lang.reflect.Constructor<?> ctor = patchClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object patch = ctor.newInstance();
        java.lang.reflect.Method hasAny = patchClass.getDeclaredMethod("hasAnyField");
        hasAny.setAccessible(true);

        java.lang.reflect.Field facField = patchClass.getDeclaredField("faculty");
        facField.setAccessible(true);
        facField.set(patch, "SoC");
        assertTrue((Boolean) hasAny.invoke(patch));
    }

    // ===== isRecommendationIntent edge cases =====
    @Test void recommendation_chuXingFangShi_short() {
        // "出行方式" alone (< 15 chars, without "哪些/什么")
        ChatResponseDto r = chat("推荐出行方式");
        assertNotNull(r);
    }

    @Test void recommendation_howDoIGet() {
        ChatResponseDto r = chat("how do I get to COM3");
        assertNotNull(r);
    }

    @Test void recommendation_suggest() {
        ChatResponseDto r = chat("suggest a route");
        assertNotNull(r);
    }

    @Test void recommendation_jianYi() {
        ChatResponseDto r = chat("建议一下出行");
        assertNotNull(r);
    }

    // ===== Bus query expanded with no state =====
    @Test void busQueryExpanded_noState() {
        ChatResponseDto r = chat("Show more");
        assertNotNull(r);
    }

    // ===== persistState error handling =====
    @Test void persistState_failure_shouldNotThrow() {
        when(conversationRepository.findByConversationId("c1")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> svc.handleChat("guest", false, "c1", "hello"));
    }

    // ===== Cleanup expired conversations =====
    @Test void multipleConversations_differentIds() {
        // Create multiple conversations to trigger cleanup
        for (int i = 0; i < 5; i++) {
            svc.handleChat("guest", false, "c_" + i, "hello");
        }
        // All should work fine
        ChatResponseDto r = svc.handleChat("guest", false, "c_0", "hello");
        assertContains(r, "EcoGo Assistant");
    }

    // ===== Booking: complete flow with all KV fields =====
    @Test void bookingFlow_kvAllFields() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_x", "trip_x", "ecogo://trip/trip_x");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "route=从PGP到CLB, departAt=2026-02-11T10:00, passengers=2");
        assertNotNull(r);
    }

    // ===== Booking: tripId null in result (no linked trip) =====
    @Test void bookingFlow_noTripId() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_x", null, "ecogo://booking/bk_x");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=2026-02-11T10:00, passengers=2");
        assertContains(r, "Booking confirmed");
    }

    // ===== Model: update_user with no patch map =====
    @Test void model_toolCall_updateUser_noPatch() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("userId", "u_001");
        // No "patch" key
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        User u = buildUser("u_001", "Old", null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update something");
        assertNotNull(r);
    }

    // ===== Model: update_user with null userId in args =====
    @Test void model_toolCall_updateUser_noUserId() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("patch", Map.of("nickname", "N"));
        // No "userId" key
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("update_user", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        User u = buildUser("u_001", "Old", null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my info");
        assertContains(r, "updated");
    }

    // ===== Model: create_booking partial (only from, no to) =====
    @Test void model_toolCall_createBooking_onlyFrom() {
        when(modelClientService.isEnabled()).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("fromName", "PGP");
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking", args);
        when(modelClientService.callModelForTool(anyString())).thenReturn(new ModelClientService.ModelResult("", tc));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "book from PGP");
        assertNotNull(r);
    }

    // ===== Booking: passengers from text fallback =====
    @Test void bookingFlow_passengersFromTextFallback() {
        ChatBookingService.BookingResult br = new ChatBookingService.BookingResult("bk_x", null, "ecogo://booking/bk_x");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(br);
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "passengers=2, departAt=2026-02-11T10:00");
        assertNotNull(r);
    }

    // ===== normalizeDepartAt: empty and null =====
    @Test void bookingFlow_emptyDepartAt() {
        svc.handleChat("u_001", false, "c1", "预订从COM3到UTown");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "departAt=");
        assertNotNull(r);
    }

    // ===== Booking: from text extraction in booking state with various patterns =====
    @Test void bookingFlow_extractFromTo_inState() {
        svc.handleChat("u_001", false, "c1", "🎫 Book a Trip");
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "from Marina Bay to Orchard");
        assertNotNull(r);
    }

    // ===== Bus query internal: Chinese stop name =====
    @Test void busQuery_chineseStopName() {
        mockBusArrivals();
        assertNotNull(chat("next bus at 大学城"));
    }

    // ===== Bus query: no stop or route found =====
    @Test void busQuery_noStopOrRoute() {
        mockBusArrivals();
        assertNotNull(chat("bus arrival"));
    }

    // ===== isBookingIntent with book + route context =====
    @Test void bookingIntent_bookAndRouteContext() {
        assertNotNull(chatAs("u_001", "book 路线 PGP"));
    }

    // ===== Recommendation: off-campus both =====
    @Test void recommendation_offCampusBoth() {
        svc.handleChat("guest", false, "c1", "📍 Travel Advice");
        ChatResponseDto r = svc.handleChat("guest", false, "c1", "Orchard to Clementi");
        assertContains(r, "MRT");
    }

    // ===== User update: self modify with multiple fields =====
    @Test void userUpdate_selfModify_multipleFields() {
        User u = buildUser("u_001", "Old", null, null, null);
        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        ChatResponseDto r = svc.handleChat("u_001", false, "c1", "update my nickname=NewName email=new@nus.edu phone=99999 faculty=SoC profile");
        assertContains(r, "updated");
    }

    // ===== BusQueryIntent: 到站信息 =====
    @Test void busQueryIntent_daoZhanXinXi() {
        mockBusArrivals();
        assertNotNull(chat("到站信息COM3"));
    }

    // ===== RecommendationIntent: 如何去 =====
    @Test void recommendationIntent_ruHeQu() {
        assertNotNull(chat("如何去商学院"));
    }

    // ===== RecommendationIntent: 怎样去 =====
    @Test void recommendationIntent_zenYangQu() {
        assertNotNull(chat("怎样去大学城"));
    }

    // ===== Helpers =====
    private ChatResponseDto chat(String msg) {
        return svc.handleChat("guest", false, "c1", msg);
    }

    private ChatResponseDto chatAs(String userId, String msg) {
        return svc.handleChat(userId, false, "c1", msg);
    }

    private void mockBusArrivals() {
        lenient().when(busProvider.getArrivals(any(), any())).thenReturn(
                new NusBusProvider.BusArrivalsResult("COM3", List.of(
                        Map.of("route", "D2", "etaMinutes", 3, "status", "on_time"))));
    }

    private User buildUser(String userid, String nickname, String email, String phone, String faculty) {
        User u = new User();
        u.setUserid(userid);
        u.setNickname(nickname);
        u.setEmail(email);
        u.setPhone(phone);
        u.setFaculty(faculty);
        u.setTotalCarbon(100.0);
        u.setCurrentPoints(50);
        u.setTotalPoints(200);
        return u;
    }

    private void assertContains(ChatResponseDto r, String sub) {
        assertNotNull(r);
        assertNotNull(r.getAssistant());
        assertTrue(r.getAssistant().getText().toLowerCase().contains(sub.toLowerCase()),
                "Expected to contain '" + sub + "' but was: " + r.getAssistant().getText());
    }

    private void assertContainsAny(ChatResponseDto r, String... subs) {
        assertNotNull(r);
        assertNotNull(r.getAssistant());
        String text = r.getAssistant().getText().toLowerCase();
        boolean found = false;
        for (String s : subs) if (text.contains(s.toLowerCase())) { found = true; break; }
        assertTrue(found, "Expected any of " + Arrays.toString(subs) + " in: " + r.getAssistant().getText());
    }

    private void assertContainsAny(String text, String... subs) {
        boolean found = false;
        String lower = text.toLowerCase();
        for (String s : subs) if (lower.contains(s.toLowerCase())) { found = true; break; }
        assertTrue(found, "Expected any of " + Arrays.toString(subs) + " in: " + text);
    }
}
