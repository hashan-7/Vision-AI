package com.example.vision_ai.service;

import com.example.vision_ai.model.Image;
import com.example.vision_ai.model.User;
import com.example.vision_ai.repository.ImageRepository;
import com.example.vision_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${h7.ai.engine.url}")
    private String engineUrl;

    @Value("${h7.ai.engine.key}")
    private String engineSecretKey;

    @Value("${app.image.upload-dir}")
    private String uploadDir;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void clearOldDataOnStartup() {
        System.out.println(">>> Render Restart Detected: Cleaning up old image records...");
        imageRepository.deleteAll(); 
    }

    @Transactional
    public String generateImage(Long userId, String prompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        checkAndRefreshCredits(user);

        if (user.getDailyCreditsRemaining() <= 0) {
            LocalDateTime refreshTime = user.getLastImageGeneratedAt().plusHours(24);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            throw new RuntimeException("Daily limit reached. Refresh on: " + refreshTime.format(formatter));
        }

        user.setDailyCreditsRemaining(user.getDailyCreditsRemaining() - 1);
        if (user.getDailyCreditsRemaining() == 0) {
            user.setLastImageGeneratedAt(LocalDateTime.now());
        }
        userRepository.saveAndFlush(user); 
        String cleanUrl = engineUrl.trim();
        if (cleanUrl.contains("=")) {
            cleanUrl = cleanUrl.substring(cleanUrl.lastIndexOf("=") + 1).trim();
        }
        cleanUrl = cleanUrl.replace("[", "").replace("]", "");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", engineSecretKey.trim());
            headers.set("Accept", "image/png");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String finalUrl = UriComponentsBuilder.fromHttpUrl(cleanUrl)
                    .queryParam("prompt", prompt.trim())
                    .build().toUriString();

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    finalUrl, HttpMethod.POST, entity, byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return saveImageToDisk(user.getId(), prompt, response.getBody());
            } else {
                user.setDailyCreditsRemaining(user.getDailyCreditsRemaining() + 1);
                userRepository.save(user);
                throw new RuntimeException("AI engine is busy. Credit refunded.");
            }

        } catch (Exception e) {
            user.setDailyCreditsRemaining(user.getDailyCreditsRemaining() + 1);
            userRepository.save(user);
            throw new RuntimeException("Generation failed: " + e.getMessage());
        }
    }

    private void checkAndRefreshCredits(User user) {
        if (user.getDailyCreditsRemaining() <= 0 && user.getLastImageGeneratedAt() != null) {
            if (LocalDateTime.now().isAfter(user.getLastImageGeneratedAt().plusHours(24))) {
                user.setDailyCreditsRemaining(10);
                user.setLastImageGeneratedAt(null);
                userRepository.save(user);
            }
        }
    }

    private String saveImageToDisk(Long userId, String prompt, byte[] imageBytes) throws Exception {
        String fileName = UUID.randomUUID().toString() + ".png";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageBytes);
        }

        String imageUrl = "/uploads/" + fileName;

        Image image = new Image();
        image.setUserId(userId);
        image.setPrompt(prompt);
        image.setImageUrl(imageUrl);
        imageRepository.save(image);

        return imageUrl;
    }

    public List<Image> getUserImages(Long userId) {
        return imageRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Image::getId).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(Long id) {
        imageRepository.deleteById(id);
    }
}