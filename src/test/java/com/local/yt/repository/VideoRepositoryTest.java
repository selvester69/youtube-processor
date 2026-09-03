package com.local.yt.repository;

import com.local.yt.model.VideoItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VideoRepositoryTest {

    @Autowired
    private VideoRepository videoRepository;

    @Test
    void testSaveAndFindByIsShort() {
        VideoItem shortVideo = VideoItem.builder()
                .videoId("s1")
                .title("Short 1")
                .isShort(true)
                .build();

        VideoItem standardVideo = VideoItem.builder()
                .videoId("v1")
                .title("Video 1")
                .isShort(false)
                .build();

        videoRepository.save(shortVideo);
        videoRepository.save(standardVideo);

        List<VideoItem> shorts = videoRepository.findByIsShort(true);
        assertEquals(1, shorts.size());
        assertEquals("s1", shorts.get(0).getVideoId());

        List<VideoItem> videos = videoRepository.findByIsShort(false);
        assertEquals(1, videos.size());
        assertEquals("v1", videos.get(0).getVideoId());
    }
}
