package com.tukangku.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@DisplayName("ReviewController")
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ReviewService reviewService;

    private ReviewDto mockReview;

    @BeforeEach
    void setUp() {
        mockReview = ReviewDto.builder()
                .id("review-1")
                .workerId("worker-1")
                .userName("Budi Santoso")
                .rating(5)
                .comment("Sangat profesional dan ramah")
                .date("2026-05-25")
                .build();
    }

    // ─── GET /api/reviews/worker/{workerId} ───────────────────────────────────

    @Nested
    @DisplayName("GET /api/reviews/worker/{workerId}")
    class GetByWorker {

        @Test
        @DisplayName("200 OK with list of reviews for the worker")
        void returns200WithReviewList() throws Exception {
            when(reviewService.getByWorkerId("worker-1")).thenReturn(List.of(mockReview));

            mockMvc.perform(get("/api/reviews/worker/worker-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("review-1"))
                    .andExpect(jsonPath("$[0].workerId").value("worker-1"))
                    .andExpect(jsonPath("$[0].rating").value(5));
        }

        @Test
        @DisplayName("200 OK with empty list when worker has no reviews")
        void noReviews_returnsEmptyList() throws Exception {
            when(reviewService.getByWorkerId("worker-99")).thenReturn(List.of());

            mockMvc.perform(get("/api/reviews/worker/worker-99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─── POST /api/reviews ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/reviews")
    class CreateReview {

        private ReviewRequest buildValidRequest() {
            ReviewRequest req = new ReviewRequest();
            req.setWorkerId("worker-1");
            req.setUserName("Budi Santoso");
            req.setRating(5);
            req.setComment("Pelayanan sangat baik dan profesional");
            return req;
        }

        @Test
        @DisplayName("201 Created with ReviewDto on valid request")
        void validRequest_returns201() throws Exception {
            when(reviewService.create(any(ReviewRequest.class))).thenReturn(mockReview);

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("review-1"))
                    .andExpect(jsonPath("$.rating").value(5))
                    .andExpect(jsonPath("$.comment").value("Sangat profesional dan ramah"));
        }

        @Test
        @DisplayName("400 Bad Request when workerId is blank")
        void blankWorkerId_returns400() throws Exception {
            ReviewRequest req = buildValidRequest();
            req.setWorkerId("");

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when rating is out of range (0)")
        void ratingTooLow_returns400() throws Exception {
            ReviewRequest req = buildValidRequest();
            req.setRating(0); // must be 1-5

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when rating is out of range (6)")
        void ratingTooHigh_returns400() throws Exception {
            ReviewRequest req = buildValidRequest();
            req.setRating(6); // must be 1-5

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when comment is too short (< 5 chars)")
        void shortComment_returns400() throws Exception {
            ReviewRequest req = buildValidRequest();
            req.setComment("Bad"); // less than 5 chars

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when userName is blank")
        void blankUserName_returns400() throws Exception {
            ReviewRequest req = buildValidRequest();
            req.setUserName("");

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when request body is empty")
        void emptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
