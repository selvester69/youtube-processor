package com.local.yt.controller;

import com.local.yt.model.VideoItem;
import com.local.yt.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppController.class)
class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowService workflowService;

    @Test
    void testGetStatus() throws Exception {
        when(workflowService.isYouTubeConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.youtubeConfigured").value(false))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testGetVideos() throws Exception {
        VideoItem video = VideoItem.builder()
                .videoId("v1")
                .title("Test Video")
                .description("Test Description")
                .isShort(false)
                .build();

        when(workflowService.fetchAndSyncVideos()).thenReturn(List.of(video));

        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].videoId").value("v1"))
                .andExpect(jsonPath("$[0].title").value("Test Video"));
    }

    @Test
    void testAnalyzeVideo() throws Exception {
        VideoItem video = VideoItem.builder()
                .videoId("v1")
                .title("Test Video")
                .generatedHashtags("#Test #Java")
                .build();

        when(workflowService.analyzeAndGenerateHashtags("v1")).thenReturn(video);

        mockMvc.perform(post("/api/analyze/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value("v1"))
                .andExpect(jsonPath("$.generatedHashtags").value("#Test #Java"));
    }
}
