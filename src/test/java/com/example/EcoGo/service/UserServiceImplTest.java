package com.example.EcoGo.service;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.dto.UserProfileDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.JwtUtils;
import com.example.EcoGo.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordUtils passwordUtils;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId("uuid");
        mockUser.setUserid("testUser");
        mockUser.setPassword("encodedPassword");
        mockUser.setAdmin(false);
    }

    // ========== register ==========

    @Test
    void register_success() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.userid = "newUser";
        request.email = "new@example.com";
        request.password = "password";
        request.repassword = "password";
        request.nickname = "New User";

        when(userRepository.findByEmail(request.email)).thenReturn(Optional.empty());
        when(userRepository.findByUserid(request.userid)).thenReturn(Optional.empty());
        when(passwordUtils.encode(request.password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AuthDto.RegisterResponse response = userService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordMismatch() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.password = "pass1";
        request.repassword = "pass2";

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void register_duplicateEmail() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.password = "pass";
        request.repassword = "pass";
        request.email = "existing@example.com";

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ErrorCode.USER_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void register_duplicateUserid() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.password = "pass";
        request.repassword = "pass";
        request.email = "new@example.com";
        request.userid = "existingUser";

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("existingUser")).thenReturn(Optional.of(new User()));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ErrorCode.USER_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    // ========== loginMobile ==========

    @Test
    void loginMobile_success() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        when(userRepository.findByUserid(request.userid)).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches(request.password, mockUser.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(mockUser.getUserid(), mockUser.isAdmin())).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginMobile(request);

        assertNotNull(response);
        assertEquals("token", response.token);
        verify(userRepository).save(mockUser);
    }

    @Test
    void loginMobile_userNotFound() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "unknown";

        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.loginMobile(request));
    }

    @Test
    void loginMobile_wrongPassword() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "wrong";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("wrong", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.loginMobile(request));
    }

    @Test
    void loginMobile_withExistingActivityMetrics_updatesCorrectly() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        // Set up existing activity metrics with login dates
        User.ActivityMetrics metrics = new User.ActivityMetrics();
        metrics.setLastTripDays(5);
        metrics.setLoginFrequency7d(3);
        List<LocalDate> dates = new ArrayList<>();
        dates.add(LocalDate.now().minusDays(2));
        dates.add(LocalDate.now().minusDays(5));
        dates.add(LocalDate.now().minusDays(35)); // old date, should be pruned
        metrics.setLoginDates(dates);
        mockUser.setActivityMetrics(metrics);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("testUser", false)).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginMobile(request);

        assertNotNull(response);
        // The old date (35 days ago) should be pruned
        assertFalse(mockUser.getActivityMetrics().getLoginDates().contains(LocalDate.now().minusDays(35)));
        // Today should be added
        assertTrue(mockUser.getActivityMetrics().getLoginDates().contains(LocalDate.now()));
        assertEquals(4, mockUser.getActivityMetrics().getLoginFrequency7d()); // 3+1
    }

    @Test
    void loginMobile_withNullActivityMetrics_createsNew() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        mockUser.setActivityMetrics(null);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("testUser", false)).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginMobile(request);

        assertNotNull(response);
        assertNotNull(mockUser.getActivityMetrics());
    }

    @Test
    void loginMobile_withNullLoginDates_createsNewList() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        User.ActivityMetrics metrics = new User.ActivityMetrics();
        metrics.setLoginDates(null);
        metrics.setLoginFrequency7d(0);
        mockUser.setActivityMetrics(metrics);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("testUser", false)).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginMobile(request);

        assertNotNull(response);
        assertNotNull(mockUser.getActivityMetrics().getLoginDates());
        assertTrue(mockUser.getActivityMetrics().getLoginDates().contains(LocalDate.now()));
    }

    @Test
    void loginMobile_duplicateLoginToday_doesNotAddTwice() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        User.ActivityMetrics metrics = new User.ActivityMetrics();
        List<LocalDate> dates = new ArrayList<>();
        dates.add(LocalDate.now()); // already logged in today
        metrics.setLoginDates(dates);
        metrics.setLoginFrequency7d(1);
        mockUser.setActivityMetrics(metrics);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("testUser", false)).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        userService.loginMobile(request);

        // Should still have only one entry for today
        long todayCount = mockUser.getActivityMetrics().getLoginDates().stream()
                .filter(d -> d.equals(LocalDate.now())).count();
        assertEquals(1, todayCount);
    }

    // ========== logoutMobile ==========

    @Test
    void logoutMobile_withUserId() {
        assertDoesNotThrow(() -> userService.logoutMobile("token", "user1"));
    }

    @Test
    void logoutMobile_extractFromToken() {
        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject("user1"));
        assertDoesNotThrow(() -> userService.logoutMobile("token", null));
    }

    @Test
    void logoutMobile_invalidToken() {
        when(jwtUtils.validateToken("invalid")).thenThrow(new RuntimeException("Invalid"));
        assertDoesNotThrow(() -> userService.logoutMobile("invalid", null));
    }

    // ========== getUserFromToken (tested indirectly) ==========

    @Test
    void getUserFromToken_invalidToken_throwsNoPermission() {
        when(jwtUtils.validateToken("bad")).thenThrow(new RuntimeException("bad token"));
        assertThrows(BusinessException.class, () -> userService.getUserDetail("bad"));
    }

    @Test
    void getUserFromToken_userNotFound() {
        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject("unknown"));
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.getUserDetail("token"));
    }

    @Test
    void getUserFromToken_deactivatedUser_throwsAccountDisabled() {
        mockUser.setDeactivated(true);
        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject("testUser"));
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getUserDetail("token"));
        assertEquals(ErrorCode.ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    // ========== updateProfile ==========

    @Test
    void updateProfile_fullCoverage() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "UpdatedNick";
        request.avatar = "new_avatar.png";
        request.phone = "123456789";
        request.faculty = "Engineering";

        UserProfileDto.PreferencesDto pref = new UserProfileDto.PreferencesDto();
        pref.preferredTransport = new ArrayList<>(List.of("metro"));
        pref.enablePush = false;
        pref.enableEmail = false;
        pref.enableBusReminder = false;
        pref.language = "en";
        pref.theme = "dark";
        pref.shareLocation = false;
        pref.showOnLeaderboard = false;
        pref.shareAchievements = false;
        pref.dormitoryOrResidence = "Dorm A";
        pref.mainTeachingBuilding = "Building B";
        pref.favoriteStudySpot = "Library";
        pref.interests = new ArrayList<>(List.of("Reading"));
        pref.weeklyGoals = 5;
        pref.newChallenges = true;
        pref.activityReminders = true;
        pref.friendActivity = true;

        request.preferences = pref;

        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateProfile("token", request);

        assertEquals("UpdatedNick", mockUser.getNickname());
        assertEquals("new_avatar.png", mockUser.getAvatar());
        assertEquals("123456789", mockUser.getPhone());
        assertEquals("Engineering", mockUser.getFaculty());

        User.Preferences savedPref = mockUser.getPreferences();
        assertNotNull(savedPref);
        assertEquals("metro", savedPref.getPreferredTransport().get(0));
        assertFalse(savedPref.isEnablePush());
        assertEquals("en", savedPref.getLanguage());
        assertEquals("dark", savedPref.getTheme());
        assertEquals("Dorm A", savedPref.getDormitoryOrResidence());
        assertEquals("Building B", savedPref.getMainTeachingBuilding());
        assertEquals("Library", savedPref.getFavoriteStudySpot());
        assertEquals(5, savedPref.getWeeklyGoals());
        assertTrue(savedPref.isNewChallenges());
        assertTrue(savedPref.isActivityReminders());
        assertTrue(savedPref.isFriendActivity());
    }

    @Test
    void updateProfile_nullPreferences_skipsPreferencesUpdate() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "OnlyNick";
        request.preferences = null;

        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateProfile("token", request);

        assertEquals("OnlyNick", mockUser.getNickname());
    }

    @Test
    void updateProfile_existingPreferences_updatesPartially() {
        User.Preferences existingPref = new User.Preferences();
        existingPref.setLanguage("zh");
        existingPref.setTheme("light");
        mockUser.setPreferences(existingPref);

        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        UserProfileDto.PreferencesDto pref = new UserProfileDto.PreferencesDto();
        pref.language = "en"; // only updating language
        request.preferences = pref;

        when(jwtUtils.validateToken("token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateProfile("token", request);

        assertEquals("en", mockUser.getPreferences().getLanguage());
        assertEquals("light", mockUser.getPreferences().getTheme()); // unchanged
    }

    // ========== resetPreferences ==========

    @Test
    void resetPreferences_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.PreferencesResetResponse response = userService.resetPreferences("mock-token");

        assertNotNull(response);
        assertNotNull(mockUser.getPreferences());
        assertEquals("zh", mockUser.getPreferences().getLanguage());
        assertEquals("light", mockUser.getPreferences().getTheme());
        assertTrue(mockUser.getPreferences().isEnablePush());
        assertTrue(mockUser.getPreferences().isEnableEmail());
        assertNull(mockUser.getPreferences().getDormitoryOrResidence());
        assertFalse(mockUser.getPreferences().isNewChallenges());
        verify(userRepository).save(mockUser);
    }

    // ========== deleteUser ==========

    @Test
    void deleteUser_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));

        userService.deleteUser("mock-token");
        verify(userRepository).delete(mockUser);
    }

    // ========== getUserDetail ==========

    @Test
    void getUserDetail_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid()));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));

        UserProfileDto.UserDetailResponse response = userService.getUserDetail("mock-token");
        assertEquals(mockUser.getUserid(), response.user_info.getUserid());
    }

    // ========== getUserDetailAdmin ==========

    @Test
    void getUserDetailAdmin_foundById() {
        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));
        UserProfileDto.UserDetailResponse response = userService.getUserDetailAdmin("uuid");
        assertEquals(mockUser.getUserid(), response.user_info.getUserid());
    }

    @Test
    void getUserDetailAdmin_foundByUserid_fallback() {
        when(userRepository.findById("testUser")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        UserProfileDto.UserDetailResponse response = userService.getUserDetailAdmin("testUser");
        assertEquals(mockUser.getUserid(), response.user_info.getUserid());
    }

    @Test
    void getUserDetailAdmin_notFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.getUserDetailAdmin("unknown"));
    }

    // ========== updateUserStatus ==========

    @Test
    void updateUserStatus_success() {
        UserProfileDto.UserStatusRequest request = new UserProfileDto.UserStatusRequest();
        request.isDeactivated = true;

        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.UpdateProfileResponse response = userService.updateUserStatus("uuid", request);

        assertTrue(mockUser.isDeactivated());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserStatus_foundByUserid_fallback() {
        UserProfileDto.UserStatusRequest request = new UserProfileDto.UserStatusRequest();
        request.isDeactivated = false;

        when(userRepository.findById("testUser")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.UpdateProfileResponse response = userService.updateUserStatus("testUser", request);
        assertNotNull(response);
    }

    @Test
    void updateUserStatus_notFound() {
        UserProfileDto.UserStatusRequest request = new UserProfileDto.UserStatusRequest();
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.updateUserStatus("unknown", request));
    }

    // ========== getAllUsers ==========

    @Test
    void getAllUsers_success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page = new org.springframework.data.domain.PageImpl<>(
                java.util.Arrays.asList(new User(), new User()));

        when(userRepository.findByIsAdminFalse(pageable)).thenReturn(page);

        com.example.EcoGo.dto.PageResponse<User> response = userService.getAllUsers(1, 10);

        assertEquals(2, response.getList().size());
        assertEquals(2, response.getTotal());
    }

    // ========== loginWeb ==========

    @Test
    void loginWeb_success() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "adminUser";
        request.password = "password";

        User adminUser = new User();
        adminUser.setUserid("adminUser");
        adminUser.setPassword("encodedPassword");
        adminUser.setAdmin(true);
        adminUser.setDeactivated(false);

        when(userRepository.findByUserid("adminUser")).thenReturn(Optional.of(adminUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("adminUser", true)).thenReturn("adminToken");
        when(jwtUtils.getExpirationDate("adminToken")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginWeb(request);

        assertNotNull(response);
        assertEquals("adminToken", response.token);
        verify(userRepository).save(adminUser);
    }

    @Test
    void loginWeb_userNotFound() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "unknown";
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void loginWeb_wrongPassword() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "adminUser";
        request.password = "wrong";

        User adminUser = new User();
        adminUser.setUserid("adminUser");
        adminUser.setPassword("encodedPassword");
        adminUser.setAdmin(true);

        when(userRepository.findByUserid("adminUser")).thenReturn(Optional.of(adminUser));
        when(passwordUtils.matches("wrong", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void loginWeb_notAdmin() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "normalUser";
        request.password = "password";

        User normalUser = new User();
        normalUser.setUserid("normalUser");
        normalUser.setPassword("encodedPassword");
        normalUser.setAdmin(false);

        when(userRepository.findByUserid("normalUser")).thenReturn(Optional.of(normalUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void loginWeb_deactivated() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "deactivatedUser";
        request.password = "password";

        User deactivatedUser = new User();
        deactivatedUser.setUserid("deactivatedUser");
        deactivatedUser.setPassword("encodedPassword");
        deactivatedUser.setAdmin(true);
        deactivatedUser.setDeactivated(true);

        when(userRepository.findByUserid("deactivatedUser")).thenReturn(Optional.of(deactivatedUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    // ========== logoutWeb ==========

    @Test
    void logoutWeb_success() {
        assertDoesNotThrow(() -> userService.logoutWeb("token"));
    }

    // ========== authenticateUser ==========

    @Test
    void authenticateUser_success() {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims();
        claims.put("isAdmin", true);
        when(jwtUtils.validateToken("valid-token")).thenReturn(claims);

        UserProfileDto.AuthCheckResponse response = userService.authenticateUser("valid-token");

        assertTrue(response.is_admin);
        assertEquals("ALL", response.permissions.get(0));
    }

    @Test
    void authenticateUser_invalidToken() {
        when(jwtUtils.validateToken("bad-token")).thenThrow(new RuntimeException("Invalid"));
        assertThrows(BusinessException.class, () -> userService.authenticateUser("bad-token"));
    }

    // ========== manageUser ==========

    @Test
    void manageUser_success() {
        UserProfileDto.AdminManageUserRequest request = new UserProfileDto.AdminManageUserRequest();
        request.isAdmin = true;
        request.vip_status = true;
        request.isDeactivated = false;
        request.remark = "Promoted";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.UpdateProfileResponse response = userService.manageUser("testUser", request);

        assertNotNull(response);
        assertTrue(mockUser.isAdmin());
        assertTrue(mockUser.getVip().isActive());
        verify(userRepository).save(mockUser);
    }

    @Test
    void manageUser_nullVip_createsNew() {
        mockUser.setVip(null);
        UserProfileDto.AdminManageUserRequest request = new UserProfileDto.AdminManageUserRequest();
        request.isAdmin = false;
        request.vip_status = true;
        request.isDeactivated = false;

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.manageUser("testUser", request);
        assertNotNull(mockUser.getVip());
        assertTrue(mockUser.getVip().isActive());
    }

    @Test
    void manageUser_notFound() {
        UserProfileDto.AdminManageUserRequest request = new UserProfileDto.AdminManageUserRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.manageUser("unknown", request));
    }

    // ========== activateVip ==========

    @Test
    void activateVip_newVip() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.activateVip("testUser", 30);

        assertTrue(mockUser.getVip().isActive());
        assertNotNull(mockUser.getVip().getStartDate());
        assertNotNull(mockUser.getVip().getExpiryDate());
        assertEquals("MONTHLY", mockUser.getVip().getPlan());
        verify(userRepository).save(mockUser);
    }

    @Test
    void activateVip_extendExistingVip() {
        User.Vip existingVip = new User.Vip();
        existingVip.setActive(true);
        existingVip.setStartDate(LocalDateTime.now().minusDays(10));
        existingVip.setExpiryDate(LocalDateTime.now().plusDays(20));
        mockUser.setVip(existingVip);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.activateVip("testUser", 30);

        // Expiry should be extended by 30 days from current expiry (now+20+30 = now+50)
        assertTrue(mockUser.getVip().getExpiryDate().isAfter(LocalDateTime.now().plusDays(45)));
        verify(userRepository).save(mockUser);
    }

    @Test
    void activateVip_nullVip_createsNew() {
        mockUser.setVip(null);
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.activateVip("testUser", 30);

        assertNotNull(mockUser.getVip());
        assertTrue(mockUser.getVip().isActive());
    }

    @Test
    void activateVip_userNotFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.activateVip("unknown", 30));
    }

    // ========== updateUserInfoAdmin ==========

    @Test
    void updateUserInfoAdmin_success() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.nickname = "AdminUpdated";
        request.isVipActive = true;
        request.vipPlan = "Yearly";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.UpdateProfileResponse response = userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(response);
        assertEquals("AdminUpdated", mockUser.getNickname());
        assertTrue(mockUser.getVip().isActive());
        assertEquals("Yearly", mockUser.getVip().getPlan());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserInfoAdmin_emptyUserId() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin("", request));
        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin(null, request));
    }

    @Test
    void updateUserInfoAdmin_userNotFound() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin("unknown", request));
    }

    @Test
    void updateUserInfoAdmin_vipActivation_wasNotActive() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.isVipActive = true;

        User.Vip vip = new User.Vip();
        vip.setActive(false); // was not active
        mockUser.setVip(vip);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertTrue(mockUser.getVip().isActive());
        assertNotNull(mockUser.getVip().getStartDate()); // startDate reset
    }

    @Test
    void updateUserInfoAdmin_vipActivation_wasActiveButExpired() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.isVipActive = true;

        User.Vip vip = new User.Vip();
        vip.setActive(true);
        vip.setExpiryDate(LocalDateTime.now().minusDays(1)); // expired
        vip.setStartDate(LocalDateTime.now().minusDays(31));
        mockUser.setVip(vip);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertTrue(mockUser.getVip().isActive());
        // startDate should be reset because it was expired
        assertNotNull(mockUser.getVip().getStartDate());
    }

    @Test
    void updateUserInfoAdmin_vipDeactivation() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.isVipActive = false;

        User.Vip vip = new User.Vip();
        vip.setActive(true);
        mockUser.setVip(vip);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertFalse(mockUser.getVip().isActive());
    }

    @Test
    void updateUserInfoAdmin_nullVip_createsNewOnActivation() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.isVipActive = true;
        mockUser.setVip(null);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(mockUser.getVip());
        assertTrue(mockUser.getVip().isActive());
    }

    @Test
    void updateUserInfoAdmin_vipPlan_nullVip_createsNew() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.vipPlan = "Quarterly";
        mockUser.setVip(null);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(mockUser.getVip());
        assertEquals("Quarterly", mockUser.getVip().getPlan());
    }

    @Test
    void updateUserInfoAdmin_vipExpiryDate_validDateFormat() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.vipExpiryDate = "2026-12-31";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(mockUser.getVip());
        assertEquals(2026, mockUser.getVip().getExpiryDate().getYear());
        assertEquals(12, mockUser.getVip().getExpiryDate().getMonthValue());
        assertEquals(31, mockUser.getVip().getExpiryDate().getDayOfMonth());
        assertEquals(23, mockUser.getVip().getExpiryDate().getHour());
        assertEquals(59, mockUser.getVip().getExpiryDate().getMinute());
    }

    @Test
    void updateUserInfoAdmin_vipExpiryDate_isoFormat() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.vipExpiryDate = "2026-12-31T12:00:00";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(mockUser.getVip());
        assertEquals(12, mockUser.getVip().getExpiryDate().getHour());
    }

    @Test
    void updateUserInfoAdmin_vipExpiryDate_invalidFormat_logWarning() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.vipExpiryDate = "not-a-date";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Should not throw, just log warning
        assertDoesNotThrow(() -> userService.updateUserInfoAdmin("testUser", request));
    }

    @Test
    void updateUserInfoAdmin_vipExpiryDate_nullVip_createsNew() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.vipExpiryDate = "2026-06-30";
        mockUser.setVip(null);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertNotNull(mockUser.getVip());
        assertNotNull(mockUser.getVip().getExpiryDate());
    }

    @Test
    void updateUserInfoAdmin_emailUpdate() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.email = "newemail@test.com";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertEquals("newemail@test.com", mockUser.getEmail());
    }

    @Test
    void updateUserInfoAdmin_isDeactivated() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.isDeactivated = true;

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateUserInfoAdmin("testUser", request);

        assertTrue(mockUser.isDeactivated());
    }

    // ========== updateProfileAdmin ==========

    @Test
    void updateProfileAdmin_success() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "New Nick";

        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto.UpdateProfileResponse response = userService.updateProfileAdmin("uuid", request);

        assertEquals("New Nick", mockUser.getNickname());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateProfileAdmin_notFound() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.updateProfileAdmin("unknown", request));
    }

    // ========== updateMobileProfileByUserId ==========

    @Test
    void updateMobileProfileByUserId_success() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "MobileNick";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateMobileProfileByUserId("testUser", request);
        assertEquals("MobileNick", mockUser.getNickname());
    }

    @Test
    void updateMobileProfileByUserId_notFound() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.updateMobileProfileByUserId("unknown", request));
    }

    // ========== getUserByUserid ==========

    @Test
    void getUserByUserid_success() {
        mockUser.setNickname("MapMe");
        mockUser.setPhone("987654321");
        mockUser.setEmail("map@example.com");

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        com.example.EcoGo.dto.UserResponseDto response = userService.getUserByUserid("testUser");

        assertNotNull(response);
        assertEquals("testUser", response.getUserid());
        assertEquals("MapMe", response.getNickname());
    }

    @Test
    void getUserByUserid_notFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.getUserByUserid("unknown"));
    }

    // ========== getUserDetailByUserid ==========

    @Test
    void getUserDetailByUserid_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        User result = userService.getUserDetailByUserid("testUser");
        assertEquals(mockUser, result);
    }

    @Test
    void getUserDetailByUserid_notFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.getUserDetailByUserid("unknown"));
    }
}
