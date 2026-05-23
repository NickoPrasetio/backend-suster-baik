package com.susterku.nurse.repository;

import com.susterku.nurse.entity.NurseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NurseRepository extends JpaRepository<NurseEntity, String> {

    @Query("SELECT DISTINCT n FROM NurseEntity n LEFT JOIN n.specializations s " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.location) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:available IS NULL OR n.isAvailable = :available)")
    List<NurseEntity> findBySearchAndAvailability(
            @Param("search") String search,
            @Param("available") Boolean available);
}
