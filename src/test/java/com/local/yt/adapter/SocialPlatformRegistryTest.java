package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.YouTubeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialPlatformRegistryTest {

    private YouTubeService youtubeService;
    private YouTubePlatformAdapter youtubeAdapter;
    private SocialPlatformRegistry registry;

    @BeforeEach
    void setUp() {
        youtubeService = Mockito.mock(YouTubeService.class);
        youtubeAdapter = new YouTubePlatformAdapter(youtubeService);
        registry = new SocialPlatformRegistry(List.of(youtubeAdapter));
    }

    @Test
    void testRegistryHasAndGetAdapter() {
        assertTrue(registry.hasAdapter("YOUTUBE"));
        assertTrue(registry.hasAdapter("youtube"));

        SocialPlatformAdapter adapter = registry.getAdapter("YOUTUBE");
        assertNotNull(adapter);
        assertEquals("YOUTUBE", adapter.getPlatformType());
    }

    @Test
    void testRegistryThrowsExceptionForUnknownPlatform() {
        assertFalse(registry.hasAdapter("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> registry.getAdapter("UNKNOWN"));
    }

    @Test
    void testYouTubeAdapterDelegation() {
        when(youtubeService.isConfigured()).thenReturn(true);
        when(youtubeService.getMyRecentVideos()).thenReturn(List.of(VideoItem.builder().videoId("v1").build()));

        assertTrue(youtubeAdapter.isConfigured());
        assertEquals(1, youtubeAdapter.fetchContent().size());

        verify(youtubeService, times(1)).isConfigured();
        verify(youtubeService, times(1)).getMyRecentVideos();
    }
}
