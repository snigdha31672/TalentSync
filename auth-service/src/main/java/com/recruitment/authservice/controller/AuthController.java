package com.recruitment.authservice.controller;

import com.recruitment.authservice.dto.AuthResponse;
import com.recruitment.authservice.dto.LoginRequest;
import com.recruitment.authservice.dto.RegisterRequest;
import com.recruitment.authservice.dto.UserProfileResponse;
import com.recruitment.authservice.entity.AppUser;
import com.recruitment.authservice.repository.AppUserRepository;
import com.recruitment.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Set<String> VALID_ROLES = Set.of("CANDIDATE", "RECRUITER");

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        if (request.password() == null || request.password().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 4 characters long"));
        }
        String role = request.role() == null ? "" : request.role().trim().toUpperCase();
        if (!VALID_ROLES.contains(role)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role must be CANDIDATE or RECRUITER"));
        }
        if (userRepository.existsByUsername(request.username().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }
        if (request.email() != null && !request.email().trim().isEmpty() && userRepository.existsByEmail(request.email().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already exists"));
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName() != null ? request.fullName().trim() : request.username().trim());
        user.setUsername(request.username().trim());
        user.setEmail(request.email() != null && !request.email().trim().isEmpty() 
            ? request.email().trim() 
            : request.username().trim() + "@talentsync.local");
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        AppUser saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "User registered successfully",
            "userId", saved.getId(),
            "username", saved.getUsername(),
            "email", saved.getEmail(),
            "fullName", saved.getFullName(),
            "role", saved.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        var userOpt = userRepository.findByUsername(request.username().trim());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        AppUser user = userOpt.get();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getEmail(), user.getId());

        return ResponseEntity.ok(new AuthResponse(
            token,
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
        }

        String username = jwtUtil.extractUsername(token);
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        AppUser user = userOpt.get();
        return ResponseEntity.ok(new UserProfileResponse(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        ));
    }
}
