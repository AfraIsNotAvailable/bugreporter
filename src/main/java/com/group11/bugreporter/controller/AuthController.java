package com.group11.bugreporter.controller;

import com.group11.bugreporter.dto.request.LoginRequest;
import com.group11.bugreporter.dto.request.RegisterRequest;
import com.group11.bugreporter.dto.response.AuthResponse;
import com.group11.bugreporter.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Public auth request: POST /api/auth/login for username='{}'", request.getUsername());
        String token = authService.authenticate(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Public auth request: POST /api/auth/register for username='{}', email='{}'",
                request.getUsername(), request.getEmail());
        String token = authService.register(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
