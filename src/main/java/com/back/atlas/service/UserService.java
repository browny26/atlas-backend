package com.back.atlas.service;

import com.back.atlas.dto.UserRequest;
import com.back.atlas.dto.UserResponse;
import com.back.atlas.entity.User;
import com.back.atlas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    UserResponse response = new UserResponse();
                    response.setId(user.getId());
                    response.setEmail(user.getEmail());
                    response.setFirstName(user.getFirstName());
                    response.setLastName(user.getLastName());
                    response.setPhoneNumber(user.getPhoneNumber());
                    response.setBio(user.getBio());
                    response.setFacebookLink(user.getFacebookLink());
                    response.setInstagramLink(user.getInstagramLink());
                    response.setXLink(user.getXLink());
                    response.setLinkedinLink(user.getLinkedinLink());
                    response.setAvatar(user.getAvatar());
                    response.setProvider(user.getProvider());
                    if (user.getCreatedAt() != null) {
                        response.setCreatedAt(user.getCreatedAt().toString());
                    }
                    return response;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    UserResponse response = new UserResponse();
                    response.setId(user.getId());
                    response.setEmail(user.getEmail());
                    response.setFirstName(user.getFirstName());
                    response.setLastName(user.getLastName());
                    response.setPhoneNumber(user.getPhoneNumber());
                    response.setBio(user.getBio());
                    response.setFacebookLink(user.getFacebookLink());
                    response.setInstagramLink(user.getInstagramLink());
                    response.setXLink(user.getXLink());
                    response.setLinkedinLink(user.getLinkedinLink());
                    response.setAvatar(user.getAvatar());
                    response.setProvider(user.getProvider());
                    if (user.getCreatedAt() != null) {
                        response.setCreatedAt(user.getCreatedAt().toString());
                    }
                    return response;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getFacebookLink() != null) user.setFacebookLink(request.getFacebookLink());
        if (request.getInstagramLink() != null) user.setInstagramLink(request.getInstagramLink());
        if (request.getXLink() != null) user.setXLink(request.getXLink());
        if (request.getLinkedinLink() != null) user.setLinkedinLink(request.getLinkedinLink());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        userRepository.save(user);
        return new UserResponse(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userRepository.delete(user);
    }
}
