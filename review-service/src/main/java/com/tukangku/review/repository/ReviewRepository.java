package com.tukangku.review.repository;

import com.tukangku.review.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, String> {
    List<ReviewEntity> findByWorkerIdOrderByDateDesc(String workerId);
    long countByWorkerId(String workerId);
}
