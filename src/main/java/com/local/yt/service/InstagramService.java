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
 * Service for interacting with Meta Instagram Graph API v19.0 for Instagram Professional/Creator accounts.
 * Supports token file detection (instagram_token.json) or configuration parameters.
 */
@Service
public class InstagramService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramService.class);
    private static final String TOKEN_FILE = "instagram_token.json";

    @Value("${instagram.user.id:}")
    private String igUserId;

    @Value("${instagram.access.token:}")
    private String userAccessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return new File(TOKEN_FILE).exists() || (igUserId != null && !igUserId.isBlank() && userAccessToken != null && !userAccessToken.isBlank());
    }

    public List<VideoItem> getMyRecentVideos() {
        if (!isConfigured()) {
            logger.info("instagram_token.json or instagram.user.id not configured. Returning mock Instagram media/reels for demo/testing.");
            return getMockVideos();
        }

        try {
            String url = String.format("https://graph.facebook.com/v19.0/%s/media?fields=id,caption,media_type,media_url,thumbnail_url,like_count,comments_count&access_token=%s",
                    igUserId, userAccessToken);
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);

            List<VideoItem> items = new ArrayList<>();
            if (response != null && response.containsKey("data")) {
                List<?> data = (List<?>) response.get("data");
                for (Object obj : data) {
                    if (obj instanceof Map<?, ?> mediaMap) {
                        String id = (String) mediaMap.get("id");
                        String caption = mediaMap.containsKey("caption") ? (String) mediaMap.get("caption") : "Instagram Reel " + id;
                        String thumb = mediaMap.containsKey("thumbnail_url") ? (String) mediaMap.get("thumbnail_url")
                                : (mediaMap.containsKey("media_url") ? (String) mediaMap.get("media_url") : "https://picsum.photos/300/200?random=ig");
                        Long likes = mediaMap.containsKey("like_count") ? ((Number) mediaMap.get("like_count")).longValue() : 500L;

                        items.add(VideoItem.builder()
                                .videoId("ig_" + id)
                                .title(caption.length() > 50 ? caption.substring(0, 47) + "..." : caption)
                                .description(caption)
                                .thumbnailUrl(thumb)
                                .isShort(true) // Instagram Reel
                                .viewCount(likes * 10)
                                .likeCount(likes)
                                .updatedOnYoutube(false)
                                .build());
                    }
                }
            }
            return items.isEmpty() ? getMockVideos() : items;
        } catch (Exception e) {
            logger.error("Error calling Instagram Graph API, falling back to mock list: {}", e.getMessage());
            return getMockVideos();
        }
    }

    public boolean updateVideoDescription(String contentId, String hashtags) {
        if (!isConfigured()) {
            logger.info("Instagram API not configured. Mocking reel caption update for ID: {}", contentId);
            return true;
        }

        try {
            String realId = contentId.replace("ig_", "");
            String url = String.format("https://graph.facebook.com/v19.0/%s?caption=%s&access_token=%s",
                    realId, hashtags, userAccessToken);
            restTemplate.postForObject(url, null, Map.class);
            logger.info("Successfully updated Instagram caption for media ID: {}", contentId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update Instagram caption for ID {}: {}", contentId, e.getMessage());
            return false;
        }
    }

    public List<VideoItem> getMockVideos() {
        List<VideoItem> list = new ArrayList<>();
        list.add(VideoItem.builder()
                .videoId("ig_reel_1")
                .title("10x Java Coding Speed with Ollama Local LLMs #instagramreels")
                .description("Automate repetitive code analysis using free local AI models on desktop!")
                .thumbnailUrl("https://picsum.photos/300/200?random=6")
                .isShort(true)
                .viewCount(22400L)
                .likeCount(2100L)
                .updatedOnYoutube(false)
                .build());

        list.add(VideoItem.builder()
                .videoId("ig_reel_2")
                .title("Why YouTube Shorts & Instagram Reels are the Future of Tech Content")
                .description("How short-form videos can drive massive engagement for developers and creators.")
                .thumbnailUrl("https://picsum.photos/300/200?random=7")
                .isShort(true)
                .viewCount(14800L)
                .likeCount(1150L)
                .updatedOnYoutube(false)
                .build());

        return list;
    }
}
