package com.tukangku.review.service;

import com.tukangku.review.dto.ReviewDto;
import com.tukangku.review.dto.ReviewRequest;
import com.tukangku.review.entity.ReviewEntity;
import com.tukangku.review.kafka.ReviewEventProducer;
import com.tukangku.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository   reviewRepository;
    private final ReviewEventProducer reviewEventProducer;
    private final MinioService        minioService;

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

    /**
     * Buat review dari customer setelah order selesai, dengan opsional upload foto.
     * Foto diupload ke MinIO bucket "review-photos"; jika upload gagal, review tetap disimpan
     * tanpa foto (non-fatal agar UX tidak terganggu).
     *
     * @param workerId  ID tukang yang di-review
     * @param bookingId ID booking yang selesai
     * @param userId    ID customer dari JWT header
     * @param userName  Nama customer (dari frontend / auth store)
     * @param rating    1–5
     * @param comment   Komentar teks (opsional)
     * @param photos    Daftar foto (maks. 5, maks. 50 MB per file — divalidasi frontend)
     */
    public ReviewDto createWithPhotos(
            String workerId,
            String bookingId,
            String userId,
            String userName,
            int rating,
            String comment,
            List<MultipartFile> photos) {

        // Upload semua foto ke MinIO, kumpulkan URL
        List<String> photoUrls = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo == null || photo.isEmpty()) continue;
                try {
                    String ext        = getExtension(photo.getOriginalFilename());
                    String objectName = "review-" + workerId + "-" + System.currentTimeMillis()
                                        + "-" + photoUrls.size() + ext;
                    String url = minioService.uploadPhoto(photo, objectName);
                    photoUrls.add(url);
                } catch (Exception e) {
                    log.warn("Gagal upload foto review untuk workerId={}: {}", workerId, e.getMessage());
                }
            }
        }

        ReviewEntity review = ReviewEntity.builder()
                .workerId(workerId)
                .bookingId(bookingId)
                .userId(userId)
                .userName(userName)
                .rating(rating)
                .comment(comment != null ? comment : "")
                .date(LocalDate.now())
                .photoUrls(photoUrls)
                .build();

        review = reviewRepository.save(review);

        // Publish Kafka event agar worker-service update rating rata-rata
        List<ReviewEntity> allReviews = reviewRepository.findByWorkerIdOrderByDateDesc(workerId);
        double avg = allReviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(rating);
        double rounded = Math.round(avg * 10.0) / 10.0;
        reviewEventProducer.publishRatingUpdate(workerId, rounded, allReviews.size());

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
                .bookingId(e.getBookingId())
                .photoUrls(e.getPhotoUrls())
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
