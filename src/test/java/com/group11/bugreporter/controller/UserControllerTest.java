package com.group11.bugreporter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group11.bugreporter.dto.request.UserRequest;
import com.group11.bugreporter.dto.response.UserResponse;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.GlobalExceptionHandler;
import com.group11.bugreporter.exception.ResourceNotFoundException;
import com.group11.bugreporter.security.JwtAuthenticationFilter;
import com.group11.bugreporter.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//asta proneste numai ce e necesar din aplicatie, nu toata aplicatia
//adica se conecteaza pe controller
@WebMvcTest(UserController.class)
//asta ii ca sa aduca handler-u global de exceptii
//alea cu status 404 "User not found"
@Import(GlobalExceptionHandler.class)
//asta ca sa ignor filtrele si sa nu imi dea 401 unothorized
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    //asta ii obiectul cu care simulez request-urile HTTP fara sa sornesc serverul real
    @Autowired
    private MockMvc mockMvc;

    //face un mock pentru service
    @MockitoBean
    private UserService userService;

    //ca sa nu crape
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    //asta ii ca sa converteasca obiectele in json atunci cand fac POST si PUT
    private final ObjectMapper objectMapper = new ObjectMapper();


    //asta ii metoda helper ca sa nu repet codul la fiecare test
    private UserResponse buildUserResponse() {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("alex");
        response.setEmail("alex@test.com");
        response.setRole(Role.USER);
        response.setBanned(false);
        response.setCreatedAt(LocalDateTime.of(2026, 3, 29, 12, 0));
        return response;
    }

    //tot un helper dar asta face request
    private UserRequest buildUserRequest() {
        UserRequest request = new UserRequest();
        request.setUsername("alex");
        request.setEmail("alex@test.com");
        request.setPassword("123456");
        return request;
    }

    @Test
    @DisplayName("POST /api/users should create user")
    void shouldCreateUser() throws Exception {

        //pregatesc datele
        UserRequest request = buildUserRequest();
        UserResponse response = buildUserResponse();

        //definesc comportamentul mock-ului
        when(userService.createUser(any(UserRequest.class))).thenReturn(response);


        //fac request-ul HTTP
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        //aici pun body-u la request
                        .content(objectMapper.writeValueAsString(request)))
                //verific raspunsu (isOk => status 200 ii asteptat)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alex"))
                .andExpect(jsonPath("$.email").value("alex@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    @DisplayName("GET /api/users should return all users")
    void shouldReturnAllUsers() throws Exception {
        UserResponse response = buildUserResponse();

        when(userService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("alex"))
                .andExpect(jsonPath("$[0].email").value("alex@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return user by id")
    void shouldReturnUserById() throws Exception {
        UserResponse response = buildUserResponse();

        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alex"))
                .andExpect(jsonPath("$.email").value("alex@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when user not found")
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} should update user")
    void shouldUpdateUser() throws Exception {
        UserRequest request = buildUserRequest();
        request.setUsername("alexandra");
        request.setEmail("alexandra@test.com");
        request.setPassword("newpass");

        UserResponse response = buildUserResponse();
        response.setUsername("alexandra");
        response.setEmail("alexandra@test.com");

        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alexandra"))
                .andExpect(jsonPath("$.email").value("alexandra@test.com"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} should delete user")
    void shouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} should return 404 when deleting missing user")
    void shouldReturnNotFoundWhenDeletingMissingUser() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/ban should ban user")
    void shouldBanUser() throws Exception {
        UserResponse response = buildUserResponse();
        response.setBanned(true);

        when(userService.banUser(1L)).thenReturn(response);

        mockMvc.perform(put("/api/users/1/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.banned").value(true));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/unban should unban user")
    void shouldUnbanUser() throws Exception {
        UserResponse response = buildUserResponse();
        response.setBanned(false);

        when(userService.unbanUser(1L)).thenReturn(response);

        mockMvc.perform(put("/api/users/1/unban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.banned").value(false));
    }
}
