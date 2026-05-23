package com.susterku.review.controller;

import com.susterku.review.dto.ReviewDto;
import com.susterku.review.dto.ReviewRequest;
import com.susterku.review.service.ReviewService;
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

    @GetMapping("/nurse/{nurseId}")
    public ResponseEntity<List<ReviewDto>> getByNurse(@PathVariable String nurseId) {
        return ResponseEntity.ok(reviewService.getByNurseId(nurseId));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }
}
