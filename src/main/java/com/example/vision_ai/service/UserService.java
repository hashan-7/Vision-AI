package com.example.vision_ai.service;

import com.example.vision_ai.model.Image;
import com.example.vision_ai.model.User;
import com.example.vision_ai.repository.ImageRepository;
import com.example.vision_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    public User getOrCreateUser(org.springframework.security.oauth2.core.user.OAuth2User principal) {
        String googleId = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        return userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPictureUrl(picture);
            newUser.setDailyCreditsRemaining(10);
            return userRepository.save(newUser);
        });
    }

    @Transactional
    public void deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        List<Image> userImages = imageRepository.findByUserId(userId);
        
        for (Image image : userImages) {
            deletePhysicalFile(image.getImageUrl());
        }

        imageRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private void deletePhysicalFile(String imageUrl) {
        try {
            String fileName = imageUrl.replace("/uploads/", "");
            Path filePath = Paths.get("uploads").resolve(fileName).toAbsolutePath();
            File file = filePath.toFile();
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("Could not remove file: " + imageUrl);
        }
    }

    public void deleteUserByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> deleteUserAccount(user.getId()));
    }
}