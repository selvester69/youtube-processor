package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class YouTubePlatformAdapter implements SocialPlatformAdapter {

    private static final Logger logger = LoggerFactory.getLogger(YouTubePlatformAdapter.class);
    private final YouTubeService youtubeService;

    public YouTubePlatformAdapter(YouTubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    @Override
    public String getPlatformType() {
        return "YOUTUBE";
    }

    @Override
    public boolean isConfigured() {
        return youtubeService.isConfigured();
    }

    @Override
    public List<VideoItem> fetchContent() {
        return youtubeService.getMyRecentVideos();
    }

    @Override
    public boolean updateDescription(String contentId, String hashtags) {
        return youtubeService.updateVideoDescription(contentId, hashtags);
    }

    @Override
    public boolean shareContent(VideoItem video, String targetPlatform) {
        logger.info("Sharing YouTube video [{}] to target platform: {}", video.getTitle(), targetPlatform);
        // Extensible hook for cross-posting YouTube content to other platforms
        return true;
    }
}
