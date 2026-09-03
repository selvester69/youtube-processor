package com.local.yt.controller;

import com.local.yt.model.VideoItem;
import com.local.yt.service.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AppController {

    private final WorkflowService workflowService;

    public AppController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("youtubeConfigured", workflowService.isYouTubeConfigured());
        status.put("registeredPlatforms", workflowService.getRegisteredPlatforms());
        status.put("status", "UP");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/videos")
    public ResponseEntity<List<VideoItem>> getVideos() {
        List<VideoItem> videos = workflowService.fetchAndSyncVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VideoItem>> getHistory() {
        return ResponseEntity.ok(workflowService.getAllSavedVideos());
    }

    @PostMapping("/analyze/{videoId}")
    public ResponseEntity<VideoItem> analyzeVideo(@PathVariable String videoId) {
        VideoItem video = workflowService.analyzeAndGenerateHashtags(videoId);
        return ResponseEntity.ok(video);
    }

    @PostMapping("/analyze-and-update/{videoId}")
    public ResponseEntity<VideoItem> analyzeAndUpdate(@PathVariable String videoId) {
        workflowService.analyzeAndGenerateHashtags(videoId);
        VideoItem updatedVideo = workflowService.updateHashtagsOnYouTube(videoId);
        return ResponseEntity.ok(updatedVideo);
    }
}
