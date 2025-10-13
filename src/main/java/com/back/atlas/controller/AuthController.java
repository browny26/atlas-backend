package com.back.atlas.controller;

import com.back.atlas.dto.*;
import com.back.atlas.entity.User;
import com.back.atlas.enums.AuthProvider;
import com.back.atlas.security.JwtService;
import com.back.atlas.service.AuthService;
import com.back.atlas.service.GoogleAuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non loggato");
        }

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName()
        );

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/request-reset-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        try {
            authService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok().body(Map.of("message", "Password reset link sent to email."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.ok().body(Map.of("message", "If the email exists, a reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while processing your request."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password updated successfully.");
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLoginDirect(@RequestBody GoogleLoginRequest request) {
        try {
            GoogleAuthResponse response = googleAuthService.authenticateWithGoogleData(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }
}