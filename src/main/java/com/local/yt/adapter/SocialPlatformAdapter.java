package com.local.yt.adapter;

import com.local.yt.model.VideoItem;

import java.util.List;

/**
 * Strategy & Adapter Interface adhering to SOLID Principles:
 * - Single Responsibility Principle (SRP): Each adapter handles interactions for one platform.
 * - Open/Closed Principle (OCP): New platforms (e.g. Instagram, Facebook) can be added without altering core service code.
 * - Liskov Substitution Principle (LSP): Implementations can be substituted interchangeably via this interface.
 * - Interface Segregation Principle (ISP): Clean, cohesive contract focused exclusively on social video analysis & sharing.
 * - Dependency Inversion Principle (DIP): Workflow Services depend on this abstraction rather than concrete platform classes.
 */
public interface SocialPlatformAdapter {

    /**
     * Returns the target platform identifier (e.g., "YOUTUBE", "INSTAGRAM", "FACEBOOK").
     */
    String getPlatformType();

    /**
     * Checks if the platform adapter credentials and configuration are ready.
     */
    boolean isConfigured();

    /**
     * Fetches recent videos/content for analysis.
     */
    List<VideoItem> fetchContent();

    /**
     * Updates video description or metadata on the target platform with generated hashtags.
     */
    boolean updateDescription(String contentId, String hashtags);

    /**
     * Shares content across to target platform.
     */
    boolean shareContent(VideoItem video, String targetPlatform);
}
