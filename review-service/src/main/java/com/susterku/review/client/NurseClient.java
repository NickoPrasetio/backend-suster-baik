package com.susterku.review.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NurseClient {

    private final RestTemplate restTemplate;

    @Value("${nurse.service.url}")
    private String nurseServiceUrl;

    public void updateNurseRating(String nurseId, double newRating, int totalReviews) {
        try {
            String url = nurseServiceUrl + "/internal/nurses/" + nurseId + "/rating";
            restTemplate.put(url, Map.of("newRating", newRating, "totalReviews", totalReviews));
        } catch (Exception e) {
            log.error("Failed to update nurse rating for nurseId={}: {}", nurseId, e.getMessage());
        }
    }
}
