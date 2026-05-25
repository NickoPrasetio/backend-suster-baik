package com.tukangku.review.service;

import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.entity.ReviewEntity;
import com.tukangku.review.kafka.ReviewEventProducer;
import com.tukangku.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewEventProducer reviewEventProducer;
    @InjectMocks ReviewService reviewService;

    private ReviewEntity reviewEntity;

    @BeforeEach
    void setUp() {
        reviewEntity = ReviewEntity.builder()
                .id("review-1")
                .workerId("worker-1")
                .userName("Budi Santoso")
                .rating(5)
                .comment("Sangat profesional dan ramah")
                .date(LocalDate.now())
                .build();
    }

    // ─── getByWorkerId ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByWorkerId()")
    class GetByWorkerId {

        @Test
        @DisplayName("returns list of ReviewDto ordered by date desc")
        void returnsReviewList() {
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity));

            List<ReviewDto> result = reviewService.getByWorkerId("worker-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkerId()).isEqualTo("worker-1");
            assertThat(result.get(0).getUserName()).isEqualTo("Budi Santoso");
            assertThat(result.get(0).getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("returns empty list when worker has no reviews")
        void noReviews_returnsEmpty() {
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-99"))
                    .thenReturn(List.of());

            assertThat(reviewService.getByWorkerId("worker-99")).isEmpty();
        }
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        private ReviewRequest buildRequest(int rating) {
            ReviewRequest req = new ReviewRequest();
            req.setWorkerId("worker-1");
            req.setUserName("Budi Santoso");
            req.setRating(rating);
            req.setComment("Pelayanan sangat baik sekali");
            return req;
        }

        @Test
        @DisplayName("saves review and publishes Kafka rating-update event")
        void savesReviewAndPublishesKafkaEvent() {
            when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity));

            ReviewDto result = reviewService.create(buildRequest(5));

            assertThat(result.getId()).isEqualTo("review-1");
            verify(reviewRepository).save(any(ReviewEntity.class));
            verify(reviewEventProducer).publishRatingUpdate(eq("worker-1"), anyDouble(), anyInt());
        }

        @Test
        @DisplayName("publishes Kafka event with correct average rating (single review)")
        void singleReview_publishesExactRating() {
            when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity)); // only one review with rating 5

            reviewService.create(buildRequest(5));

            ArgumentCaptor<Double> ratingCaptor = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Integer> countCaptor  = ArgumentCaptor.forClass(Integer.class);
            verify(reviewEventProducer).publishRatingUpdate(eq("worker-1"), ratingCaptor.capture(), countCaptor.capture());

            assertThat(ratingCaptor.getValue()).isEqualTo(5.0);
            assertThat(countCaptor.getValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("calculates rounded average across multiple reviews")
        void multipleReviews_calculatesRoundedAverage() {
            // Two existing reviews: ratings 4 and 3 → average = 3.5
            ReviewEntity review4 = ReviewEntity.builder().id("r2").workerId("worker-1")
                    .userName("User A").rating(4).comment("ok").date(LocalDate.now()).build();
            ReviewEntity review3 = ReviewEntity.builder().id("r3").workerId("worker-1")
                    .userName("User B").rating(3).comment("ok").date(LocalDate.now()).build();

            when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity, review4, review3)); // ratings: 5, 4, 3

            reviewService.create(buildRequest(5));

            ArgumentCaptor<Double> ratingCaptor = ArgumentCaptor.forClass(Double.class);
            verify(reviewEventProducer).publishRatingUpdate(eq("worker-1"), ratingCaptor.capture(), eq(3));

            // average(5, 4, 3) = 4.0; rounded to 1 decimal = 4.0
            assertThat(ratingCaptor.getValue()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("rounds average to 1 decimal place (e.g. 4.333 → 4.3)")
        void averageRating_isRoundedToOneDecimal() {
            // Three reviews: 5, 4, 4 → average = 4.333... → should be rounded to 4.3
            ReviewEntity r2 = ReviewEntity.builder().id("r2").workerId("worker-1")
                    .userName("U").rating(4).comment("ok").date(LocalDate.now()).build();
            ReviewEntity r3 = ReviewEntity.builder().id("r3").workerId("worker-1")
                    .userName("U").rating(4).comment("ok").date(LocalDate.now()).build();

            when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity, r2, r3)); // ratings: 5, 4, 4

            reviewService.create(buildRequest(5));

            ArgumentCaptor<Double> ratingCaptor = ArgumentCaptor.forClass(Double.class);
            verify(reviewEventProducer).publishRatingUpdate(any(), ratingCaptor.capture(), any());

            // (5+4+4)/3 = 4.333... → rounded to 4.3
            assertThat(ratingCaptor.getValue()).isEqualTo(4.3);
        }

        @Test
        @DisplayName("sets review date to today")
        void setsDateToToday() {
            ArgumentCaptor<ReviewEntity> entityCaptor = ArgumentCaptor.forClass(ReviewEntity.class);
            when(reviewRepository.save(entityCaptor.capture())).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity));

            reviewService.create(buildRequest(4));

            assertThat(entityCaptor.getValue().getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("publishes total review count after new review is added")
        void publishesCorrectTotalCount() {
            ReviewEntity existing = ReviewEntity.builder().id("existing").workerId("worker-1")
                    .userName("U").rating(4).comment("ok").date(LocalDate.now()).build();

            when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewEntity);
            when(reviewRepository.findByWorkerIdOrderByDateDesc("worker-1"))
                    .thenReturn(List.of(reviewEntity, existing));

            reviewService.create(buildRequest(5));

            ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(reviewEventProducer).publishRatingUpdate(any(), anyDouble(), countCaptor.capture());
            assertThat(countCaptor.getValue()).isEqualTo(2);
        }
    }
}
