package com.local.yt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoItem {

    @Id
    private String videoId;

    private String title;

    @Column(length = 2000)
    private String description;

    private String thumbnailUrl;

    private boolean isShort;

    private Long viewCount;

    private Long likeCount;

    @Column(length = 1000)
    private String generatedHashtags;

    private boolean updatedOnYoutube;

    private LocalDateTime lastAnalyzedAt;
}
