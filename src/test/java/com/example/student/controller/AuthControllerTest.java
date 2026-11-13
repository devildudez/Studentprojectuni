package com.example.student.controller;

import com.example.student.configuration.SecurityConfiguration;
import com.example.student.dto.AuthResponse;
import com.example.student.model.Role;
import com.example.student.security.JwtFilter;
import com.example.student.security.JwtService;
import com.example.student.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfiguration.class, JwtFilter.class})
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void loginReturnsBearerTokenAndRoles() throws Exception {
        var principal = User.withUsername("admin@example.com").password("hashed")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).build();
        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        doReturn(principal.getAuthorities()).when(authentication).getAuthorities();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN))).thenReturn("jwt-token");

        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateToken("admin@example.com", Set.of(Role.ADMIN));
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginValidatesRequiredFields() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authenticationManager, jwtService);
    }
}
