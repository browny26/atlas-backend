package com.back.atlas.service;

import com.back.atlas.dto.GoogleAuthResponse;
import com.back.atlas.dto.GoogleLoginRequest;
import com.back.atlas.dto.UserResponse;
import com.back.atlas.entity.User;
import com.back.atlas.enums.AuthProvider;
import com.back.atlas.repository.UserRepository;
import com.back.atlas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleAuthResponse authenticateWithGoogleData(GoogleLoginRequest request) {
        try {
            User user = findOrCreateUserFromGoogleData(request);

            String jwtToken = jwtService.generateToken(user);

            return new GoogleAuthResponse(jwtToken, "Bearer", mapToUserResponse(user));

        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    private User findOrCreateUserFromGoogleData(GoogleLoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(existingUser -> updateUserWithGoogleData(existingUser, request))
                .orElseGet(() -> createNewUserFromGoogleData(request));
    }

    private User updateUserWithGoogleData(User user, GoogleLoginRequest request) {
        if (user.getProvider() != AuthProvider.GOOGLE) {
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(request.getSub());
        }

        if (user.getAvatar() == null && request.getPicture() != null) {
            user.setAvatar(request.getPicture());
        }

        if (user.getFirstName() == null && request.getName() != null) {
            user.setFirstName(request.getName());
        }

        return userRepository.save(user);
    }

    private User createNewUserFromGoogleData(GoogleLoginRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getGiven_name());
        user.setLastName(request.getFamily_name());
        user.setAvatar(request.getPicture());
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(request.getSub());

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