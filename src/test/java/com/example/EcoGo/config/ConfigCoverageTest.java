package com.example.EcoGo.config;

import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.JwtUtils;
import com.example.EcoGo.utils.PasswordUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for config classes: JwtAuthenticationFilter, CustomAccessDeniedHandler,
 * CustomAuthenticationEntryPoint, DatabaseSeeder, WebConfig.
 */
class ConfigCoverageTest {

    // ==================== JwtAuthenticationFilter ====================
    private final JwtUtils jwtUtils = new JwtUtils();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtFilter_noAuthHeader_passesThrough() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // No Authorization header
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void jwtFilter_invalidBearerPrefix_passesThrough() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void jwtFilter_validToken_userRole() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        String token = jwtUtils.generateToken("user1", false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user1", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void jwtFilter_validToken_adminRole() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        String token = jwtUtils.generateToken("admin", true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void jwtFilter_invalidToken_noAuthSet() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ==================== CustomAccessDeniedHandler ====================
    @Test
    void accessDeniedHandler_writesJsonResponse() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response,
                new org.springframework.security.access.AccessDeniedException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("Access Denied"));
    }

    // ==================== CustomAuthenticationEntryPoint ====================
    @Test
    void authenticationEntryPoint_writesJsonResponse() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new org.springframework.security.authentication.BadCredentialsException("Bad creds"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("Unauthorized"));
    }

    // ==================== DatabaseSeeder ====================
    @Test
    void databaseSeeder_createsAdmin_whenNotExists() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        PasswordUtils pwUtils = mock(PasswordUtils.class);
        TransportModeRepository tmRepo = mock(TransportModeRepository.class);

        DatabaseSeeder seeder = new DatabaseSeeder(userRepo, pwUtils, tmRepo);

        // Set adminDefaultPassword via reflection
        Field pwField = DatabaseSeeder.class.getDeclaredField("adminDefaultPassword");
        pwField.setAccessible(true);
        pwField.set(seeder, "admin123");

        when(userRepo.findByUserid("admin")).thenReturn(Optional.empty());
        when(pwUtils.encode("admin123")).thenReturn("encoded_password");
        when(tmRepo.count()).thenReturn(0L);

        seeder.run();

        verify(userRepo).save(any(User.class));
        verify(tmRepo).saveAll(anyList());
    }

    @Test
    void databaseSeeder_skipsAdmin_whenExists() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        PasswordUtils pwUtils = mock(PasswordUtils.class);
        TransportModeRepository tmRepo = mock(TransportModeRepository.class);

        DatabaseSeeder seeder = new DatabaseSeeder(userRepo, pwUtils, tmRepo);

        Field pwField = DatabaseSeeder.class.getDeclaredField("adminDefaultPassword");
        pwField.setAccessible(true);
        pwField.set(seeder, "admin123");

        User existingAdmin = new User();
        existingAdmin.setUserid("admin");
        when(userRepo.findByUserid("admin")).thenReturn(Optional.of(existingAdmin));
        when(tmRepo.count()).thenReturn(6L); // transport modes already seeded

        seeder.run();

        verify(userRepo, never()).save(any(User.class));
        verify(tmRepo, never()).saveAll(anyList());
    }

    // ==================== WebConfig ====================
    @Test
    void webConfig_addViewControllers() {
        WebConfig config = new WebConfig();
        org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry =
                mock(org.springframework.web.servlet.config.annotation.ViewControllerRegistry.class);
        org.springframework.web.servlet.config.annotation.ViewControllerRegistration registration =
                mock(org.springframework.web.servlet.config.annotation.ViewControllerRegistration.class);
        when(registry.addViewController("/")).thenReturn(registration);

        config.addViewControllers(registry);

        verify(registry).addViewController("/");
        verify(registration).setViewName("forward:/index.html");
    }

    @Test
    void webConfig_addResourceHandlers_callsRegistry() {
        WebConfig config = new WebConfig();
        org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry =
                mock(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry.class);
        org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration registration =
                mock(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration.class);
        org.springframework.web.servlet.config.annotation.ResourceChainRegistration chainRegistration =
                mock(org.springframework.web.servlet.config.annotation.ResourceChainRegistration.class);

        when(registry.addResourceHandler("/**")).thenReturn(registration);
        when(registration.addResourceLocations("classpath:/static/")).thenReturn(registration);
        when(registration.resourceChain(true)).thenReturn(chainRegistration);
        when(chainRegistration.addResolver(any())).thenReturn(chainRegistration);

        config.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/**");
        verify(registration).addResourceLocations("classpath:/static/");
        verify(registration).resourceChain(true);
        verify(chainRegistration).addResolver(any(org.springframework.web.servlet.resource.PathResourceResolver.class));
    }

    @Test
    void webConfig_pathResourceResolver_existingResource_returnsIt() throws Exception {
        org.springframework.core.io.Resource location = mock(org.springframework.core.io.Resource.class);
        org.springframework.core.io.Resource requestedResource = mock(org.springframework.core.io.Resource.class);

        when(location.createRelative("existing.js")).thenReturn(requestedResource);
        when(requestedResource.exists()).thenReturn(true);
        when(requestedResource.isReadable()).thenReturn(true);

        WebConfig.SpaFallbackResourceResolver resolver = new WebConfig.SpaFallbackResourceResolver();
        org.springframework.core.io.Resource result = resolver.resolve("existing.js", location);

        assertEquals(requestedResource, result);
    }

    @Test
    void webConfig_pathResourceResolver_missingResource_returnsFallback() throws Exception {
        org.springframework.core.io.Resource location = mock(org.springframework.core.io.Resource.class);
        org.springframework.core.io.Resource requestedResource = mock(org.springframework.core.io.Resource.class);

        when(location.createRelative("missing.js")).thenReturn(requestedResource);
        when(requestedResource.exists()).thenReturn(false);

        WebConfig.SpaFallbackResourceResolver resolver = new WebConfig.SpaFallbackResourceResolver();
        org.springframework.core.io.Resource result = resolver.resolve("missing.js", location);

        // Should return ClassPathResource fallback (index.html)
        assertNotNull(result);
        assertTrue(result instanceof org.springframework.core.io.ClassPathResource);
    }

    @Test
    void webConfig_pathResourceResolver_existsButNotReadable_returnsFallback() throws Exception {
        org.springframework.core.io.Resource location = mock(org.springframework.core.io.Resource.class);
        org.springframework.core.io.Resource requestedResource = mock(org.springframework.core.io.Resource.class);

        when(location.createRelative("unreadable.js")).thenReturn(requestedResource);
        when(requestedResource.exists()).thenReturn(true);
        when(requestedResource.isReadable()).thenReturn(false);

        WebConfig.SpaFallbackResourceResolver resolver = new WebConfig.SpaFallbackResourceResolver();
        org.springframework.core.io.Resource result = resolver.resolve("unreadable.js", location);

        assertNotNull(result);
        assertTrue(result instanceof org.springframework.core.io.ClassPathResource);
    }
}
