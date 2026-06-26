package com.ashutosh.cyberhelper.web;

import com.ashutosh.cyberhelper.dto.auth.AuthRequest;
import com.ashutosh.cyberhelper.dto.auth.AuthResponse;
import com.ashutosh.cyberhelper.dto.auth.UserRegisterRequest;
import com.ashutosh.cyberhelper.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }
}
