package com.tukangku.worker.kafka;

import com.tukangku.worker.dto.RatingUpdateRequest;
import com.tukangku.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingUpdateConsumer {

    private final WorkerService workerService;

    @KafkaListener(topics = "rating-update", groupId = "worker-service-group")
    public void consume(RatingUpdateEvent event) {
        log.info("Received rating-update event: workerId={}, rating={}", event.getWorkerId(), event.getNewRating());
        RatingUpdateRequest request = new RatingUpdateRequest(event.getNewRating(), event.getTotalReviews());
        workerService.updateRating(event.getWorkerId(), request);
    }
}
