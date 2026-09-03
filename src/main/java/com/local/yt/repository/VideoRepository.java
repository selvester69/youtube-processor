package com.local.yt.repository;

import com.local.yt.model.VideoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<VideoItem, String> {
    List<VideoItem> findByIsShort(boolean isShort);
}
