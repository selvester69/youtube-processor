package com.local.yt.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.local.yt.model.VideoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class YouTubeService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeService.class);
    private static final String CLIENT_SECRET_FILE = "client_secret.json";
    private static final String TOKENS_DIRECTORY = "tokens";

    public boolean isConfigured() {
        return new File(CLIENT_SECRET_FILE).exists();
    }

    private YouTube getYouTubeClient() throws GeneralSecurityException, IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("client_secret.json not found in root directory. Please configure Google OAuth credentials.");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new FileReader(CLIENT_SECRET_FILE));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singletonList(YouTubeScopes.YOUTUBE_FORCE_SSL))
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("LocalYoutubeHashtagApp")
                .build();
    }

    public List<VideoItem> getMyRecentVideos() {
        if (!isConfigured()) {
            logger.info("client_secret.json not present. Returning mock YouTube video list for demo/testing.");
            return getMockVideos();
        }

        try {
            YouTube youtube = getYouTubeClient();

            // Fetch uploaded playlist ID from channel snippet/contentDetails
            YouTube.Channels.List channelRequest = youtube.channels().list(Collections.singletonList("contentDetails"));
            channelRequest.setMine(true);
            var channelResponse = channelRequest.execute();
            if (channelResponse.getItems() == null || channelResponse.getItems().isEmpty()) {
                return getMockVideos();
            }

            String uploadsPlaylistId = channelResponse.getItems().get(0)
                    .getContentDetails().getRelatedPlaylists().getUploads();

            // Fetch playlist items
            YouTube.PlaylistItems.List playlistRequest = youtube.playlistItems().list(Collections.singletonList("snippet"));
            playlistRequest.setPlaylistId(uploadsPlaylistId);
            playlistRequest.setMaxResults(25L);
            var playlistResponse = playlistRequest.execute();

            if (playlistResponse.getItems() == null || playlistResponse.getItems().isEmpty()) {
                return Collections.emptyList();
            }

            List<String> videoIds = new ArrayList<>();
            for (var item : playlistResponse.getItems()) {
                videoIds.add(item.getSnippet().getResourceId().getVideoId());
            }

            // Fetch video details by IDs
            YouTube.Videos.List request = youtube.videos()
                    .list(Collections.singletonList("snippet,statistics,contentDetails"));
            request.setId(videoIds);
            VideoListResponse response = request.execute();

            List<VideoItem> items = new ArrayList<>();
            if (response.getItems() != null) {
                for (Video v : response.getItems()) {
                    boolean isShort = isShortVideo(v);
                    Long views = v.getStatistics() != null && v.getStatistics().getViewCount() != null ? v.getStatistics().getViewCount().longValue() : 0L;
                    Long likes = v.getStatistics() != null && v.getStatistics().getLikeCount() != null ? v.getStatistics().getLikeCount().longValue() : 0L;
                    String thumb = v.getSnippet() != null && v.getSnippet().getThumbnails() != null && v.getSnippet().getThumbnails().getMedium() != null
                            ? v.getSnippet().getThumbnails().getMedium().getUrl() : "";

                    VideoItem item = VideoItem.builder()
                            .videoId(v.getId())
                            .title(v.getSnippet() != null ? v.getSnippet().getTitle() : "Untitled")
                            .description(v.getSnippet() != null ? v.getSnippet().getDescription() : "")
                            .thumbnailUrl(thumb)
                            .isShort(isShort)
                            .viewCount(views)
                            .likeCount(likes)
                            .updatedOnYoutube(false)
                            .build();
                    items.add(item);
                }
            }
            return items;
        } catch (Exception e) {
            logger.error("Error fetching YouTube videos, falling back to mock list: {}", e.getMessage());
            return getMockVideos();
        }
    }

    public boolean updateVideoDescription(String videoId, String hashtags) {
        if (!isConfigured()) {
            logger.info(" client_secret.json not present. Mocking description update for videoId: {}", videoId);
            return true;
        }

        try {
            YouTube youtube = getYouTubeClient();
            VideoListResponse response = youtube.videos()
                    .list(Collections.singletonList("snippet"))
                    .setId(Collections.singletonList(videoId))
                    .execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {
                logger.warn("Video with ID {} not found on YouTube.", videoId);
                return false;
            }

            Video video = response.getItems().get(0);
            VideoSnippet snippet = video.getSnippet();

            String currentDesc = snippet.getDescription() != null ? snippet.getDescription() : "";
            if (!currentDesc.contains(hashtags)) {
                String updatedDesc = currentDesc + "\n\n" + hashtags;
                snippet.setDescription(updatedDesc);

                youtube.videos().update(Collections.singletonList("snippet"), video).execute();
                logger.info("Successfully updated YouTube description for video ID: {}", videoId);
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to update YouTube video description for ID {}: {}", videoId, e.getMessage());
            return false;
        }
    }

    private boolean isShortVideo(Video video) {
        if (video.getContentDetails() == null || video.getContentDetails().getDuration() == null) {
            return false;
        }
        try {
            String isoDuration = video.getContentDetails().getDuration();
            Duration duration = Duration.parse(isoDuration);
            return duration.getSeconds() <= 60;
        } catch (Exception e) {
            return false;
        }
    }

    public List<VideoItem> getMockVideos() {
        List<VideoItem> list = new ArrayList<>();
        list.add(VideoItem.builder()
                .videoId("demo_short_1")
                .title("5 Mind-Blowing Java Tricks in 60 Seconds #shorts")
                .description("Quick Java tips for faster development and cleaner code!")
                .thumbnailUrl("https://picsum.photos/300/200?random=1")
                .isShort(true)
                .viewCount(15400L)
                .likeCount(1200L)
                .updatedOnYoutube(false)
                .build());

        list.add(VideoItem.builder()
                .videoId("demo_video_1")
                .title("Building a Complete Local YouTube Analyzer with Java & Ollama")
                .description("Step-by-step tutorial on how to use Java 17, Spring Boot, and local LLMs like Llama3 for free YouTube analysis.")
                .thumbnailUrl("https://picsum.photos/300/200?random=2")
                .isShort(false)
                .viewCount(3400L)
                .likeCount(280L)
                .updatedOnYoutube(false)
                .build());

        list.add(VideoItem.builder()
                .videoId("demo_short_2")
                .title("Why Local AI (Ollama) is a Game Changer for Developers")
                .description("Run Llama 3 locally on your PC for 100% free AI automation without subscription fees.")
                .thumbnailUrl("https://picsum.photos/300/200?random=3")
                .isShort(true)
                .viewCount(8900L)
                .likeCount(750L)
                .updatedOnYoutube(false)
                .build());

        return list;
    }
}
