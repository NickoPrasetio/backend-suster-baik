package com.tukangku.review.controller;

import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int  MAX_PHOTOS    = 5;
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB

    private final ReviewService reviewService;

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<ReviewDto>> getByWorker(@PathVariable String workerId) {
        return ResponseEntity.ok(reviewService.getByWorkerId(workerId));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    /**
     * Review dari customer setelah order selesai — mendukung upload foto (maks. 5 foto, 50 MB/foto).
     * Field teks dikirim sebagai @RequestParam, file sebagai @RequestPart.
     */
    @PostMapping(value = "/with-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createWithPhotos(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam("workerId")                              String workerId,
            @RequestParam("bookingId")                             String bookingId,
            @RequestParam("userName")                              String userName,
            @RequestParam("rating")                                Integer rating,
            @RequestParam(value = "comment", required = false)     String comment,
            @RequestPart(value = "photos",   required = false)     List<MultipartFile> photos) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rating harus antara 1–5"));
        }

        // Validasi foto di sisi server sebagai defence-in-depth
        if (photos != null) {
            if (photos.size() > MAX_PHOTOS) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Maksimal " + MAX_PHOTOS + " foto"));
            }
            for (MultipartFile photo : photos) {
                if (photo.getSize() > MAX_SIZE_BYTES) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Ukuran setiap foto maksimal 50 MB"));
                }
                String ct = photo.getContentType();
                if (ct == null || !ct.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "File harus berupa gambar"));
                }
            }
        }

        ReviewDto result = reviewService.createWithPhotos(
                workerId, bookingId, userId, userName, rating, comment, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

