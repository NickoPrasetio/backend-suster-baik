package com.tukangku.review.controller;

import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<ReviewDto>> getByWorker(@PathVariable String workerId) {
        return ResponseEntity.ok(reviewService.getByWorkerId(workerId));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }
}
