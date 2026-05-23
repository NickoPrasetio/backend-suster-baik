package com.tukangku.review.service;

import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.entity.ReviewEntity;
import com.tukangku.review.kafka.ReviewEventProducer;
import com.tukangku.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewEventProducer reviewEventProducer;

    public List<ReviewDto> getByWorkerId(String workerId) {
        return reviewRepository.findByWorkerIdOrderByDateDesc(workerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ReviewDto create(ReviewRequest request) {
        ReviewEntity review = ReviewEntity.builder()
                .workerId(request.getWorkerId())
                .userName(request.getUserName())
                .rating(request.getRating())
                .comment(request.getComment())
                .date(LocalDate.now())
                .build();

        review = reviewRepository.save(review);

        List<ReviewEntity> allReviews = reviewRepository.findByWorkerIdOrderByDateDesc(request.getWorkerId());
        double avgRating = allReviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(request.getRating());
        double roundedRating = Math.round(avgRating * 10.0) / 10.0;

        reviewEventProducer.publishRatingUpdate(request.getWorkerId(), roundedRating, allReviews.size());

        return toDto(review);
    }

    private ReviewDto toDto(ReviewEntity e) {
        return ReviewDto.builder()
                .id(e.getId())
                .workerId(e.getWorkerId())
                .userId(e.getUserId())
                .userName(e.getUserName())
                .rating(e.getRating())
                .comment(e.getComment())
                .date(e.getDate().toString())
                .build();
    }
}
