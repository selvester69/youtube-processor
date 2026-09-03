package com.local.yt.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SocialPlatformRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SocialPlatformRegistry.class);
    private final Map<String, SocialPlatformAdapter> adapterRegistry = new HashMap<>();

    public SocialPlatformRegistry(List<SocialPlatformAdapter> adapters) {
        for (SocialPlatformAdapter adapter : adapters) {
            String platformKey = adapter.getPlatformType().toUpperCase();
            adapterRegistry.put(platformKey, adapter);
            logger.info("Registered SocialPlatformAdapter for platform: {}", platformKey);
        }
    }

    public SocialPlatformAdapter getAdapter(String platform) {
        String key = platform.toUpperCase();
        SocialPlatformAdapter adapter = adapterRegistry.get(key);
        if (adapter == null) {
            throw new IllegalArgumentException("No SocialPlatformAdapter registered for platform: " + platform);
        }
        return adapter;
    }

    public boolean hasAdapter(String platform) {
        return adapterRegistry.containsKey(platform.toUpperCase());
    }

    public List<String> getRegisteredPlatforms() {
        return List.copyOf(adapterRegistry.keySet());
    }
}
