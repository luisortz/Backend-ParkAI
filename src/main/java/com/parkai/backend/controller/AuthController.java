package com.parkai.backend.controller;

import com.parkai.backend.dto.AuthResponse;
import com.parkai.backend.dto.RegisterRequest;
import com.parkai.backend.dto.ResendCodeRequest;
import com.parkai.backend.dto.UserResponse;
import com.parkai.backend.dto.VerifyRequest;
import com.parkai.backend.model.User;
import com.parkai.backend.repository.UserRepository;
import com.parkai.backend.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(
            @RequestBody @Valid RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    public AuthResponse verify(
        @RequestBody VerifyRequest request
    ) {
    return authService.verify(request);
    }

    @PostMapping("/resend-code")
    public void resendCode(
            @RequestBody @Valid
            ResendCodeRequest request
    ) {

        authService.resendVerificationCode(
                request
        );
    }
}