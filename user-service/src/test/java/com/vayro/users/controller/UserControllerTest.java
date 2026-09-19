package com.vayro.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.users.dto.AuthResponse;
import com.vayro.users.dto.LoginRequest;
import com.vayro.users.dto.RegisterRequest;
import com.vayro.users.entity.Role;
import com.vayro.users.entity.User;
import com.vayro.users.repository.UserRepository;
import com.vayro.users.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testGetCurrentUserWithValidJwtReturns200() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Sarah", "Connor", "sarah@vayro.com", "Password123!", "+919876543210");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + authResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sarah@vayro.com"))
                .andExpect(jsonPath("$.firstName").value("Sarah"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testGetCurrentUserWithoutJwtReturnsForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllUsersAsRegularUserReturns403Forbidden() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Regular", "User", "regular@vayro.com", "Password123!", "+919876543210");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + authResponse.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllUsersAsAdminReturns200Ok() throws Exception {
        User admin = new User("Admin", "Vayro", "admin@vayro.com", passwordEncoder.encode("AdminPass123!"), "+919820011223", Role.ADMIN);
        userRepository.save(admin);

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("admin@vayro.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }
}
