package com.local.yt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String modelName;

    private final RestTemplate restTemplate;

    public OllamaService() {
        this.restTemplate = new RestTemplate();
    }

    public OllamaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateHashtags(String title, String description, boolean isShort) {
        String contentType = isShort ? "YouTube Short (9:16 vertical)" : "YouTube Video";

        String prompt = String.format("""
            You are a YouTube viral growth and SEO expert. Analyze this %s content:
            Title: %s
            Description: %s

            Generate 10 trending, high-reach hashtags designed to increase views and discoverability.
            Format the response ONLY as a single line of space-separated hashtags starting with #.
            Do not include introductory text, bullet points, numbers, or explanations.
            """, contentType, title, description);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            logger.info("Sending request to Ollama endpoint: {} for model: {}", ollamaUrl, modelName);
            Map<?, ?> response = restTemplate.postForObject(ollamaUrl, requestBody, Map.class);
            if (response != null && response.containsKey("response")) {
                String result = ((String) response.get("response")).trim();
                logger.info("Successfully generated hashtags from Ollama AI");
                return result;
            }
        } catch (Exception e) {
            logger.warn("Ollama AI endpoint unavailable at {} ({}), using rule-based hashtag generation fallback.", ollamaUrl, e.getMessage());
        }

        return generateFallbackHashtags(title, isShort);
    }

    private String generateFallbackHashtags(String title, boolean isShort) {
        StringBuilder sb = new StringBuilder();
        if (isShort) {
            sb.append("#Shorts #YouTubeShorts #ViralShorts ");
        } else {
            sb.append("#YouTube #Trending #VideoOfTheDay ");
        }

        String[] words = title.replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");
        int added = 0;
        for (String word : words) {
            if (word.length() > 3 && added < 7) {
                sb.append("#").append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
                added++;
            }
        }

        return sb.toString().trim();
    }
}
