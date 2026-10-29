package com.recruitment.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.authservice.dto.LoginRequest;
import com.recruitment.authservice.dto.RegisterRequest;
import com.recruitment.authservice.entity.AppUser;
import com.recruitment.authservice.repository.AppUserRepository;
import com.recruitment.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private AppUser sampleCandidate;
    private AppUser sampleRecruiter;

    @BeforeEach
    void setUp() {
        sampleCandidate = new AppUser();
        sampleCandidate.setId(1L);
        sampleCandidate.setFullName("John Doe");
        sampleCandidate.setUsername("johndoe");
        sampleCandidate.setEmail("john@example.com");
        sampleCandidate.setPassword("$2a$10$hashedCandidatePassword");
        sampleCandidate.setRole("CANDIDATE");

        sampleRecruiter = new AppUser();
        sampleRecruiter.setId(2L);
        sampleRecruiter.setFullName("Sarah Smith");
        sampleRecruiter.setUsername("sarah_recruiter");
        sampleRecruiter.setEmail("sarah@company.com");
        sampleRecruiter.setPassword("$2a$10$hashedRecruiterPassword");
        sampleRecruiter.setRole("RECRUITER");
    }

    @Test
    void testRegisterRecruiter_Success() throws Exception {
        when(userRepository.existsByUsername("sarah_recruiter")).thenReturn(false);
        when(userRepository.existsByEmail("sarah@company.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(AppUser.class))).thenReturn(sampleRecruiter);

        RegisterRequest request = new RegisterRequest(
                "Sarah Smith", "sarah_recruiter", "sarah@company.com", "pass1234", "RECRUITER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("sarah_recruiter"))
                .andExpect(jsonPath("$.role").value("RECRUITER"));
    }

    @Test
    void testRegisterCandidate_Success() throws Exception {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(AppUser.class))).thenReturn(sampleCandidate);

        RegisterRequest request = new RegisterRequest(
                "John Doe", "johndoe", "john@example.com", "pass1234", "CANDIDATE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    @Test
    void testRegister_DuplicateUsername_ReturnsConflict() throws Exception {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "John Doe", "johndoe", "john@example.com", "pass1234", "CANDIDATE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void testRegister_DuplicateEmail_ReturnsConflict() throws Exception {
        when(userRepository.existsByUsername("differentUser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "John Doe", "differentUser", "john@example.com", "pass1234", "CANDIDATE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void testRegister_InvalidRole_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Admin User", "admin1", "admin@example.com", "pass1234", "SUPERADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role must be CANDIDATE or RECRUITER"));
    }

    @Test
    void testLogin_Success_ReturnsJwtToken() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleCandidate));
        when(passwordEncoder.matches("pass1234", sampleCandidate.getPassword())).thenReturn(true);

        LoginRequest request = new LoginRequest("johndoe", "pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    @Test
    void testLogin_WrongPassword_ReturnsUnauthorized() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleCandidate));
        when(passwordEncoder.matches("wrongpass", sampleCandidate.getPassword())).thenReturn(false);

        LoginRequest request = new LoginRequest("johndoe", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        String token = jwtUtil.generateToken("johndoe", "CANDIDATE", "john@example.com", 1L);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleCandidate));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }
}
