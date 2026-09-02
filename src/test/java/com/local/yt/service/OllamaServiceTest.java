package com.local.yt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class OllamaServiceTest {

    private RestTemplate restTemplate;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        ollamaService = new OllamaService(restTemplate);
    }

    @Test
    void testGenerateHashtagsSuccess() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("response", "#Java #Spring #Coding #Tutorial #Developer");

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(mockResponse);

        String hashtags = ollamaService.generateHashtags("Java Tutorial", "Learn Java programming", false);
        assertNotNull(hashtags);
        assertTrue(hashtags.contains("#Java"));
    }

    @Test
    void testGenerateHashtagsFallbackWhenOllamaFails() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        String hashtags = ollamaService.generateHashtags("Java Tutorial", "Learn Java programming", true);
        assertNotNull(hashtags);
        assertTrue(hashtags.contains("#Shorts"));
        assertTrue(hashtags.contains("#Java"));
    }
}
