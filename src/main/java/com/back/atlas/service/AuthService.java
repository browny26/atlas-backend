package com.back.atlas.service;

import com.back.atlas.dto.*;
import com.back.atlas.entity.PasswordResetToken;
import com.back.atlas.entity.User;
import com.back.atlas.enums.AuthProvider;
import com.back.atlas.repository.PasswordResetTokenRepository;
import com.back.atlas.repository.UserRepository;
import com.back.atlas.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        // Same error for unknown email and wrong password: do not reveal which accounts exist.
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        log.debug("user found - ID: " + user.getId());

        log.debug("Before finding expired tokens");
        List<PasswordResetToken> expiredTokens = passwordResetTokenRepository
                .findByUserIdAndExpiryDateBefore(user.getId(), LocalDateTime.now());

        log.debug("expiredTokens: " + expiredTokens);

        if (!expiredTokens.isEmpty()) {
            passwordResetTokenRepository.deleteAll(expiredTokens);
            log.debug("Deleted " + expiredTokens.size() + " expired tokens");
        }

        log.debug("Before finding active tokens");
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findByUserIdAndExpiryDateAfter(user.getId(), LocalDateTime.now());

        log.debug("activeTokens: " + activeTokens.size());

        if (!activeTokens.isEmpty()) {
            log.debug("A password reset token has already been generated for this user. Please check your email or wait for it to expire.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A password reset token has already been generated for this user. Please check your email or wait for it to expire.");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));

        passwordResetTokenRepository.save(resetToken);

        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .toUriString();
        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
    }


    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
}