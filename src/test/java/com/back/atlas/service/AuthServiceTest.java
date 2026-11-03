package com.back.atlas.service;

import com.back.atlas.dto.RegisterRequest;
import com.back.atlas.entity.User;
import com.back.atlas.repository.PasswordResetTokenRepository;
import com.back.atlas.repository.UserRepository;
import com.back.atlas.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, mock(JwtService.class), encoder,
            mock(PasswordResetTokenRepository.class), mock(EmailService.class));

    private static RegisterRequest request(String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("ada@example.com");
        request.setPassword(password);
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        return request;
    }

    @Test
    void passwordsOver72BytesAreRejectedEvenIfShortInCharacters() {
        String password = "é".repeat(40); // 40 characters, 80 bytes in UTF-8
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(password)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(encoder);
    }

    @Test
    void concurrentDuplicateRegistrationReturns409() {
        when(users.findByEmail(any())).thenReturn(Optional.empty()); // both requests passed the check
        when(encoder.encode(any())).thenReturn("hash");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("users_email_key"));

        assertThatThrownBy(() -> service.register(request("supersecret")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
