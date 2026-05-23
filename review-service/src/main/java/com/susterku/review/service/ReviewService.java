package com.susterku.review.service;

import com.susterku.review.client.NurseClient;
import com.susterku.review.dto.ReviewDto;
import com.susterku.review.dto.ReviewRequest;
import com.susterku.review.entity.ReviewEntity;
import com.susterku.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final NurseClient nurseClient;

    public List<ReviewDto> getByNurseId(String nurseId) {
        return reviewRepository.findByNurseIdOrderByDateDesc(nurseId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ReviewDto create(ReviewRequest request) {
        ReviewEntity review = ReviewEntity.builder()
                .nurseId(request.getNurseId())
                .userName(request.getUserName())
                .rating(request.getRating())
                .comment(request.getComment())
                .date(LocalDate.now())
                .build();

        review = reviewRepository.save(review);

        // Recalculate and sync rating to nurse-service
        List<ReviewEntity> allReviews = reviewRepository.findByNurseIdOrderByDateDesc(request.getNurseId());
        double avgRating = allReviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(request.getRating());
        double roundedRating = Math.round(avgRating * 10.0) / 10.0;

        nurseClient.updateNurseRating(request.getNurseId(), roundedRating, allReviews.size());

        return toDto(review);
    }

    private ReviewDto toDto(ReviewEntity e) {
        return ReviewDto.builder()
                .id(e.getId())
                .nurseId(e.getNurseId())
                .userId(e.getUserId())
                .userName(e.getUserName())
                .rating(e.getRating())
                .comment(e.getComment())
                .date(e.getDate().toString())
                .build();
    }
}
