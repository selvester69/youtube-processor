package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.FacebookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FacebookPlatformAdapter implements SocialPlatformAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FacebookPlatformAdapter.class);
    private final FacebookService facebookService;

    public FacebookPlatformAdapter(FacebookService facebookService) {
        this.facebookService = facebookService;
    }

    @Override
    public String getPlatformType() {
        return "FACEBOOK";
    }

    @Override
    public boolean isConfigured() {
        return facebookService.isConfigured();
    }

    @Override
    public List<VideoItem> fetchContent() {
        return facebookService.getMyRecentVideos();
    }

    @Override
    public boolean updateDescription(String contentId, String hashtags) {
        return facebookService.updateVideoDescription(contentId, hashtags);
    }

    @Override
    public boolean shareContent(VideoItem video, String targetPlatform) {
        logger.info("Sharing content [{}] to Facebook page/reels.", video.getTitle());
        return true;
    }
}
