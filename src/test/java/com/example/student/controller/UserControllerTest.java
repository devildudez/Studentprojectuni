package com.example.student.controller;

import com.example.student.configuration.SecurityConfiguration;
import com.example.student.dto.user.*;
import com.example.student.model.Role;
import com.example.student.security.JwtFilter;
import com.example.student.security.JwtService;
import com.example.student.service.CustomUserDetailsService;
import com.example.student.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, JwtFilter.class})
class UserControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    private static org.springframework.security.core.userdetails.UserDetails superAdmin() {
        return org.springframework.security.core.userdetails.User.withUsername("root").password("x").roles("SUPER_ADMIN").build();
    }

    @Test
    void getAllUsersRequiresSuperAdminAndReturnsList() throws Exception {
        when(userService.findAll()).thenReturn(new UserResponseList(
                List.of(new UserResponse(1L, "teacher@example.com", Set.of(Role.TEACHER))), 1));
        mvc.perform(get("/api/v1/users").with(user(superAdmin())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.responses[0].email").value("teacher@example.com"));
        verify(userService).findAll();
    }

    @Test
    void getAllUsersForbidsAdminRole() throws Exception {
        mvc.perform(get("/api/v1/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void updateUserPassesRequestToService() throws Exception {
        when(userService.updateUser(any())).thenReturn(new UserResponse(2L, "updated@example.com", Set.of(Role.ADMIN)));
        mvc.perform(put("/api/v1/users/2").with(user(superAdmin())).contentType("application/json")
                        .content("{\"email\":\"updated@example.com\",\"password\":\"pw\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
        verify(userService).updateUser(any(UserRequest.class));
    }

    @Test
    void saveUserPassesRequestToService() throws Exception {
        when(userService.save(any())).thenReturn(new UserResponse(3L, "new@example.com", Set.of(Role.TEACHER)));
        mvc.perform(post("/api/v1/users/save").with(user(superAdmin())).contentType("application/json")
                        .content("{\"email\":\"new@example.com\",\"password\":\"pw\",\"roles\":[\"TEACHER\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        verify(userService).save(any(UserRequest.class));
    }

    @Test
    void deleteUserPassesIdToService() throws Exception {
        mvc.perform(delete("/api/v1/users/4").with(user(superAdmin())))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(userService).deleteById(4L);
    }

    @Test
    void usersEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }
}
