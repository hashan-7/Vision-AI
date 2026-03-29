package com.example.vision_ai.controller;

import com.example.vision_ai.model.Image;
import com.example.vision_ai.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String prompt = body.get("prompt").toString();

            String url = imageService.generateImage(userId, prompt);

            response.put("success", true);
            response.put("imageUrl", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Image>> getAll(@RequestParam Long userId) {
        return ResponseEntity.ok(imageService.getUserImages(userId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}