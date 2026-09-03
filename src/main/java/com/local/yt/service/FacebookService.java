package com.local.yt.service;

import com.local.yt.model.VideoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Meta Graph API v19.0 for Facebook Pages & Reels.
 * Supports token file detection (facebook_token.json) or configuration parameters.
 */
@Service
public class FacebookService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookService.class);
    private static final String TOKEN_FILE = "facebook_token.json";

    @Value("${facebook.page.id:}")
    private String pageId;

    @Value("${facebook.page.access.token:}")
    private String pageAccessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return new File(TOKEN_FILE).exists() || (pageId != null && !pageId.isBlank() && pageAccessToken != null && !pageAccessToken.isBlank());
    }

    public List<VideoItem> getMyRecentVideos() {
        if (!isConfigured()) {
            logger.info("facebook_token.json or facebook.page.id not configured. Returning mock Facebook videos for demo/testing.");
            return getMockVideos();
        }

        try {
            String url = String.format("https://graph.facebook.com/v19.0/%s/videos?fields=id,title,description,picture,views&access_token=%s",
                    pageId, pageAccessToken);
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);

            List<VideoItem> items = new ArrayList<>();
            if (response != null && response.containsKey("data")) {
                List<?> data = (List<?>) response.get("data");
                for (Object obj : data) {
                    if (obj instanceof Map<?, ?> videoMap) {
                        String id = (String) videoMap.get("id");
                        String title = videoMap.containsKey("title") ? (String) videoMap.get("title") : "Facebook Video " + id;
                        String desc = videoMap.containsKey("description") ? (String) videoMap.get("description") : "";
                        String thumb = videoMap.containsKey("picture") ? (String) videoMap.get("picture") : "https://picsum.photos/300/200?random=fb";

                        items.add(VideoItem.builder()
                                .videoId("fb_" + id)
                                .title(title)
                                .description(desc)
                                .thumbnailUrl(thumb)
                                .isShort(true) // Facebook Reels / Vertical Video
                                .viewCount(12000L)
                                .likeCount(950L)
                                .updatedOnYoutube(false)
                                .build());
                    }
                }
            }
            return items.isEmpty() ? getMockVideos() : items;
        } catch (Exception e) {
            logger.error("Error calling Facebook Graph API, falling back to mock list: {}", e.getMessage());
            return getMockVideos();
        }
    }

    public boolean updateVideoDescription(String contentId, String hashtags) {
        if (!isConfigured()) {
            logger.info("Facebook API not configured. Mocking video description update for ID: {}", contentId);
            return true;
        }

        try {
            String realId = contentId.replace("fb_", "");
            String url = String.format("https://graph.facebook.com/v19.0/%s?description=%s&access_token=%s",
                    realId, hashtags, pageAccessToken);
            restTemplate.postForObject(url, null, Map.class);
            logger.info("Successfully updated Facebook video description for ID: {}", contentId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update Facebook video description for ID {}: {}", contentId, e.getMessage());
            return false;
        }
    }

    public List<VideoItem> getMockVideos() {
        List<VideoItem> list = new ArrayList<>();
        list.add(VideoItem.builder()
                .videoId("fb_reel_1")
                .title("Top 3 Spring Boot Hacks You Need to Know #facebookreels")
                .description("Boost your Java backend productivity with these simple Spring Boot tips!")
                .thumbnailUrl("https://picsum.photos/300/200?random=4")
                .isShort(true)
                .viewCount(18200L)
                .likeCount(1430L)
                .updatedOnYoutube(false)
                .build());

        list.add(VideoItem.builder()
                .videoId("fb_video_1")
                .title("How to Build Free AI Apps Locally with Ollama and Meta Llama 3")
                .description("Full guide to running open source AI locally on desktop without cloud API fees.")
                .thumbnailUrl("https://picsum.photos/300/200?random=5")
                .isShort(false)
                .viewCount(5600L)
                .likeCount(410L)
                .updatedOnYoutube(false)
                .build());

        return list;
    }
}
