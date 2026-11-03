package com.back.atlas.controller;

import com.back.atlas.dto.UserRequest;
import com.back.atlas.dto.UserResponse;
import com.back.atlas.entity.User;
import com.back.atlas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Profile endpoints: a user can only read, update or delete their own account. */
@RestController
@RequestMapping("/v1/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        requireSelf(id, currentUser);
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                   @RequestBody UserRequest userRequest,
                                   @AuthenticationPrincipal User currentUser) {
        requireSelf(id, currentUser);
        return userService.updateUser(id, userRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        requireSelf(id, currentUser);
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    private static void requireSelf(Long id, User currentUser) {
        if (!currentUser.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own account");
        }
    }
}
