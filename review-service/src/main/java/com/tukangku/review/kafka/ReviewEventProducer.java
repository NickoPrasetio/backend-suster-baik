package com.tukangku.review.kafka;

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

    public void publishRatingUpdate(String workerId, double newRating, int totalReviews) {
        RatingUpdateEvent event = new RatingUpdateEvent(workerId, newRating, totalReviews);
        kafkaTemplate.send(TOPIC, workerId, event);
        log.info("Published rating-update event: workerId={}, rating={}", workerId, newRating);
    }
}
