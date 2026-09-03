package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.InstagramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InstagramPlatformAdapter implements SocialPlatformAdapter {

    private static final Logger logger = LoggerFactory.getLogger(InstagramPlatformAdapter.class);
    private final InstagramService instagramService;

    public InstagramPlatformAdapter(InstagramService instagramService) {
        this.instagramService = instagramService;
    }

    @Override
    public String getPlatformType() {
        return "INSTAGRAM";
    }

    @Override
    public boolean isConfigured() {
        return instagramService.isConfigured();
    }

    @Override
    public List<VideoItem> fetchContent() {
        return instagramService.getMyRecentVideos();
    }

    @Override
    public boolean updateDescription(String contentId, String hashtags) {
        return instagramService.updateVideoDescription(contentId, hashtags);
    }

    @Override
    public boolean shareContent(VideoItem video, String targetPlatform) {
        logger.info("Sharing content [{}] to Instagram reels/feed.", video.getTitle());
        return true;
    }
}
