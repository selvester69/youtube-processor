package com.local.yt.service;

import com.local.yt.adapter.SocialPlatformAdapter;
import com.local.yt.adapter.SocialPlatformRegistry;
import com.local.yt.model.VideoItem;
import com.local.yt.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final SocialPlatformRegistry platformRegistry;
    private final OllamaService ollamaService;
    private final VideoRepository videoRepository;

    public WorkflowService(SocialPlatformRegistry platformRegistry, OllamaService ollamaService, VideoRepository videoRepository) {
        this.platformRegistry = platformRegistry;
        this.ollamaService = ollamaService;
        this.videoRepository = videoRepository;
    }

    public List<VideoItem> fetchAndSyncVideos() {
        SocialPlatformAdapter adapter = platformRegistry.getAdapter("YOUTUBE");
        List<VideoItem> ytVideos = adapter.fetchContent();
        for (VideoItem video : ytVideos) {
            Optional<VideoItem> existing = videoRepository.findById(video.getVideoId());
            if (existing.isPresent()) {
                VideoItem dbVideo = existing.get();
                dbVideo.setTitle(video.getTitle());
                dbVideo.setDescription(video.getDescription());
                dbVideo.setThumbnailUrl(video.getThumbnailUrl());
                dbVideo.setShort(video.isShort());
                dbVideo.setViewCount(video.getViewCount());
                dbVideo.setLikeCount(video.getLikeCount());
                videoRepository.save(dbVideo);
            } else {
                videoRepository.save(video);
            }
        }
        return videoRepository.findAll();
    }

    public VideoItem analyzeAndGenerateHashtags(String videoId) {
        VideoItem video = videoRepository.findById(videoId)
                .orElseGet(() -> {
                    SocialPlatformAdapter adapter = platformRegistry.getAdapter("YOUTUBE");
                    return adapter.fetchContent().stream()
                            .filter(v -> v.getVideoId().equals(videoId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Video ID not found: " + videoId));
                });

        logger.info("Analyzing video ID: {} ({}) for hashtags...", videoId, video.getTitle());
        String generatedTags = ollamaService.generateHashtags(video.getTitle(), video.getDescription(), video.isShort());

        video.setGeneratedHashtags(generatedTags);
        video.setLastAnalyzedAt(LocalDateTime.now());
        return videoRepository.save(video);
    }

    public VideoItem updateHashtagsOnYouTube(String videoId) {
        VideoItem video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video ID not found: " + videoId));

        if (video.getGeneratedHashtags() == null || video.getGeneratedHashtags().trim().isEmpty()) {
            analyzeAndGenerateHashtags(videoId);
            video = videoRepository.findById(videoId).orElseThrow();
        }

        SocialPlatformAdapter adapter = platformRegistry.getAdapter("YOUTUBE");
        boolean updated = adapter.updateDescription(videoId, video.getGeneratedHashtags());
        video.setUpdatedOnYoutube(updated);
        return videoRepository.save(video);
    }

    public List<VideoItem> getAllSavedVideos() {
        return videoRepository.findAll();
    }

    public boolean isYouTubeConfigured() {
        return platformRegistry.getAdapter("YOUTUBE").isConfigured();
    }

    public List<String> getRegisteredPlatforms() {
        return platformRegistry.getRegisteredPlatforms();
    }
}
