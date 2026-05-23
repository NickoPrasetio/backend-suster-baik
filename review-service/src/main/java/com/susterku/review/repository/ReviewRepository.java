package com.susterku.review.repository;

import com.susterku.review.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, String> {
    List<ReviewEntity> findByNurseIdOrderByDateDesc(String nurseId);
    long countByNurseId(String nurseId);
}
