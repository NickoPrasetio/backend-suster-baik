package com.susterku.review.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventProducer {

    private static final String TOPIC = "rating-update";

    private final KafkaTemplate<String, RatingUpdateEvent> kafkaTemplate;

    public void publishRatingUpdate(String nurseId, double newRating, int totalReviews) {
        RatingUpdateEvent event = new RatingUpdateEvent(nurseId, newRating, totalReviews);
        kafkaTemplate.send(TOPIC, nurseId, event);
        log.info("Published rating-update event: nurseId={}, rating={}", nurseId, newRating);
    }
}
