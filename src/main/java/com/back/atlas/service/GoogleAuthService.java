package com.back.atlas.service;

import com.back.atlas.dto.GoogleAuthResponse;
import com.back.atlas.dto.GoogleLoginRequest;
import com.back.atlas.dto.UserResponse;
import com.back.atlas.entity.User;
import com.back.atlas.enums.AuthProvider;
import com.back.atlas.repository.UserRepository;
import com.back.atlas.security.GoogleTokenVerifier;
import com.back.atlas.security.GoogleTokenVerifier.GoogleUser;
import com.back.atlas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleTokenVerifier tokenVerifier;

    public GoogleAuthResponse authenticate(GoogleLoginRequest request) {
        GoogleUser googleUser = tokenVerifier.verify(request.getAccessToken());
        User user = userRepository.findByEmail(googleUser.email())
                .map(existing -> updateFromGoogle(existing, googleUser))
                .orElseGet(() -> createFromGoogle(googleUser));

        return new GoogleAuthResponse(jwtService.generateToken(user), "Bearer", mapToUserResponse(user));
    }

    /** Links an existing account to Google (safe: Google verified the email address). */
    private User updateFromGoogle(User user, GoogleUser googleUser) {
        if (user.getProvider() != AuthProvider.GOOGLE) {
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(googleUser.sub());
        }
        if (user.getAvatar() == null && googleUser.picture() != null) {
            user.setAvatar(googleUser.picture());
        }
        if (user.getFirstName() == null && googleUser.name() != null) {
            user.setFirstName(googleUser.name());
        }
        return userRepository.save(user);
    }

    private User createFromGoogle(GoogleUser googleUser) {
        User user = new User();
        user.setEmail(googleUser.email());
        user.setFirstName(googleUser.givenName() != null ? googleUser.givenName() : googleUser.name());
        user.setLastName(googleUser.familyName() != null ? googleUser.familyName() : "");
        user.setAvatar(googleUser.picture());
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(googleUser.sub());
        return userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setAvatar(user.getAvatar());
        if (user.getCreatedAt() != null) {
            userResponse.setCreatedAt(user.getCreatedAt().toString());
        }
        return userResponse;
    }
}