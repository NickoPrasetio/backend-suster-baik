package com.susterku.nurse.kafka;

import com.susterku.nurse.dto.RatingUpdateRequest;
import com.susterku.nurse.service.NurseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingUpdateConsumer {

    private final NurseService nurseService;

    @KafkaListener(topics = "rating-update", groupId = "nurse-service-group")
    public void consume(RatingUpdateEvent event) {
        log.info("Received rating-update event: nurseId={}, rating={}", event.getNurseId(), event.getNewRating());
        RatingUpdateRequest request = new RatingUpdateRequest(event.getNewRating(), event.getTotalReviews());
        nurseService.updateRating(event.getNurseId(), request);
    }
}
