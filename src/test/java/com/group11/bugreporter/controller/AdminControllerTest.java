package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.response.UserResponse;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.repository.BugRepository;
import com.group11.bugreporter.repository.CommentRepository;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.security.JwtAuthenticationFilter;
import com.group11.bugreporter.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BugRepository bugRepository;

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserResponse buildUserResponse(boolean banned) {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("alex");
        response.setEmail("alex@test.com");
        response.setRole(Role.USER);
        response.setBanned(banned);
        response.setCreatedAt(LocalDateTime.of(2026, 3, 29, 12, 0));
        return response;
    }

    @Test
    @DisplayName("GET /api/admin/users should include banned status")
    void shouldReturnUsersWithBannedStatus() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(buildUserResponse(true)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("alex"))
                .andExpect(jsonPath("$[0].banned").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/ban?banned=true should ban user")
    void shouldBanUserFromAdminEndpoint() throws Exception {
        when(userService.banUser(1L)).thenReturn(buildUserResponse(true));

        mockMvc.perform(put("/api/admin/users/1/ban").param("banned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.banned").value(true));

        verify(userService).banUser(1L);
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/ban?banned=false should unban user")
    void shouldUnbanUserFromAdminEndpoint() throws Exception {
        when(userService.unbanUser(1L)).thenReturn(buildUserResponse(false));

        mockMvc.perform(put("/api/admin/users/1/ban").param("banned", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.banned").value(false));

        verify(userService).unbanUser(1L);
    }
}
