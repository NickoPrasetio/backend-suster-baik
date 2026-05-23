package com.tukangku.worker.repository;

import com.tukangku.worker.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {

    @Query("SELECT DISTINCT n FROM WorkerEntity n LEFT JOIN n.specializations s " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.location) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:available IS NULL OR n.isAvailable = :available)")
    List<WorkerEntity> findBySearchAndAvailability(
            @Param("search") String search,
            @Param("available") Boolean available);
}
