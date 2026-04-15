package com.group11.bugreporter.service;

import com.group11.bugreporter.dto.request.LoginRequest;
import com.group11.bugreporter.dto.request.RegisterRequest;
import com.group11.bugreporter.entity.User;
import com.group11.bugreporter.entity.enums.Role;
import com.group11.bugreporter.exception.ConflictException;
import com.group11.bugreporter.exception.InvalidCredentialsException;
import com.group11.bugreporter.repository.UserRepository;
import com.group11.bugreporter.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return jwtService.generateToken(user.getUsername());
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .banned(false)
                .build();
        userRepository.save(user);
        return jwtService.generateToken(user.getUsername());
    }
}
