package com.local.yt.adapter;

import com.local.yt.model.VideoItem;
import com.local.yt.service.FacebookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FacebookPlatformAdapterTest {

    private FacebookService facebookService;
    private FacebookPlatformAdapter adapter;

    @BeforeEach
    void setUp() {
        facebookService = mock(FacebookService.class);
        adapter = new FacebookPlatformAdapter(facebookService);
    }

    @Test
    void testGetPlatformType() {
        assertEquals("FACEBOOK", adapter.getPlatformType());
    }

    @Test
    void testIsConfigured() {
        when(facebookService.isConfigured()).thenReturn(true);
        assertTrue(adapter.isConfigured());
    }

    @Test
    void testFetchContent() {
        VideoItem item = VideoItem.builder().videoId("fb_1").title("FB Reel").build();
        when(facebookService.getMyRecentVideos()).thenReturn(List.of(item));

        List<VideoItem> result = adapter.fetchContent();
        assertEquals(1, result.size());
        assertEquals("fb_1", result.get(0).getVideoId());
    }

    @Test
    void testUpdateDescription() {
        when(facebookService.updateVideoDescription("fb_1", "#fb #viral")).thenReturn(true);
        assertTrue(adapter.updateDescription("fb_1", "#fb #viral"));
    }
}
