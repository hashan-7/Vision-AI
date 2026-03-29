package com.example.vision_ai.controller;

import com.example.vision_ai.model.User;
import com.example.vision_ai.repository.UserRepository;
import com.example.vision_ai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            return ResponseEntity.status(401).body(response);
        }

        String googleId = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPictureUrl(picture);
            newUser.setDailyCreditsRemaining(10);
            return userRepository.save(newUser);
        });

        response.put("success", true);
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("pictureUrl", user.getPictureUrl());
        response.put("credits", user.getDailyCreditsRemaining());
        
        if (user.getLastImageGeneratedAt() != null) {
            response.put("nextRefreshAt", user.getLastImageGeneratedAt().plusHours(24).toString());
        } else {
            response.put("nextRefreshAt", null);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteAccount(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal == null) {
            response.put("success", false);
            return ResponseEntity.status(401).body(response);
        }
        

        try {
            String googleId = principal.getAttribute("sub");
            userRepository.findByGoogleId(googleId).ifPresent(user -> {
                userService.deleteUserAccount(user.getId());
            });
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}