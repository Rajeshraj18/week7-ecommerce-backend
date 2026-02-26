package com.ecommerce.controller;

import com.ecommerce.model.dto.UserDTO;
import com.ecommerce.model.entity.User;
import com.ecommerce.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Simulating authenticated user ID as 1 for demonstration
    private final Long FAKE_AUTH_USER_ID = 1L;

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile() {
        User user = userService.getUserProfile(FAKE_AUTH_USER_ID);
        return ResponseEntity.ok(userService.convertToDto(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        User user = userService.updateUserProfile(FAKE_AUTH_USER_ID, request.getName());
        return ResponseEntity.ok(userService.convertToDto(user));
    }

    @Data
    public static class UpdateProfileRequest {
        private String name;
    }
}
