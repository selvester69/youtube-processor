package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.InstagramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstagramPlatformAdapterTest {

    private InstagramService instagramService;
    private InstagramPlatformAdapter adapter;

    @BeforeEach
    void setUp() {
        instagramService = mock(InstagramService.class);
        adapter = new InstagramPlatformAdapter(instagramService);
    }

    @Test
    void testGetPlatformType() {
        assertEquals("INSTAGRAM", adapter.getPlatformType());
    }

    @Test
    void testIsConfigured() {
        when(instagramService.isConfigured()).thenReturn(true);
        assertTrue(adapter.isConfigured());
    }

    @Test
    void testFetchContent() {
        VideoItem item = VideoItem.builder().videoId("ig_1").title("IG Reel").build();
        when(instagramService.getMyRecentVideos()).thenReturn(List.of(item));

        List<VideoItem> result = adapter.fetchContent();
        assertEquals(1, result.size());
        assertEquals("ig_1", result.get(0).getVideoId());
    }

    @Test
    void testUpdateDescription() {
        when(instagramService.updateVideoDescription("ig_1", "#reels #ig")).thenReturn(true);
        assertTrue(adapter.updateDescription("ig_1", "#reels #ig"));
    }
}
