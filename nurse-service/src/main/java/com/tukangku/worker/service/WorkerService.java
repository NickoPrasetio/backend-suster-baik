package com.tukangku.worker.service;

import com.tukangku.worker.dto.WorkerCreateRequest;
import com.tukangku.worker.dto.WorkerDto;
import com.tukangku.worker.dto.RatingUpdateRequest;
import com.tukangku.worker.dto.WorkStatusUpdateRequest;
import com.tukangku.worker.dto.TukangSalaryUpdateRequest;
import com.tukangku.worker.dto.TukangLocationUpdateRequest;
import com.tukangku.worker.entity.WorkerEntity;
import com.tukangku.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;

    public List<WorkerDto> findAll(String search, Boolean available) {
        return workerRepository.findBySearchAndAvailability(search, available)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public WorkerDto findById(String id) {
        return workerRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Tukang tidak ditemukan"));
    }

    public WorkerDto create(WorkerCreateRequest request) {
        WorkerEntity worker = WorkerEntity.builder()
                .name(request.getName())
                .avatar(request.getAvatar() != null ? request.getAvatar() : "")
                .age(request.getAge())
                .experience(request.getExperience())
                .rating(0.0)
                .totalReviews(0)
                .specializations(request.getSpecializations() != null ? request.getSpecializations() : List.of())
                .location(request.getLocation())
                .pricePerDay(request.getPricePerDay())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .workStatus(request.getWorkStatus() != null ? request.getWorkStatus() : "OPEN")
                .authUserId(request.getAuthUserId())
                .bio(request.getBio())
                .build();
        return toDto(workerRepository.save(worker));
    }

    public WorkerDto updateAvatar(String id, String avatarUrl) {
        WorkerEntity worker = workerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tukang tidak ditemukan"));
        worker.setAvatar(avatarUrl);
        return toDto(workerRepository.save(worker));
    }

    public void updateRating(String id, RatingUpdateRequest request) {
        WorkerEntity worker = workerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tukang tidak ditemukan"));
        worker.setRating(request.getNewRating());
        worker.setTotalReviews(request.getTotalReviews());
        workerRepository.save(worker);
    }

    // ─── Tukang self-service endpoints ────────────────────────────────────────

    public WorkerDto updateWorkStatus(String authUserId, WorkStatusUpdateRequest request) {
        WorkerEntity worker = workerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Profil tukang tidak ditemukan"));
        worker.setWorkStatus(request.getStatus());
        worker.setIsAvailable("OPEN".equals(request.getStatus()));
        return toDto(workerRepository.save(worker));
    }

    public WorkerDto updateSalary(String authUserId, TukangSalaryUpdateRequest request) {
        WorkerEntity worker = workerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Profil tukang tidak ditemukan"));
        worker.setPricePerDay(request.getPricePerDay());
        return toDto(workerRepository.save(worker));
    }

    public WorkerDto updateLocation(String authUserId, TukangLocationUpdateRequest request) {
        WorkerEntity worker = workerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Profil tukang tidak ditemukan"));
        worker.setLatitude(request.getLatitude());
        worker.setLongitude(request.getLongitude());
        return toDto(workerRepository.save(worker));
    }

    public WorkerDto getByAuthUserId(String authUserId) {
        return workerRepository.findByAuthUserId(authUserId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Profil tukang tidak ditemukan"));
    }

    /**
     * Dipanggil oleh booking-service melalui endpoint internal.
     * Atomik: validasi workStatus OPEN → set BOOKED → return WorkerDto.
     * Melempar exception jika tukang tidak dapat dibooking.
     */
    @Transactional
    public WorkerDto bookWorker(String workerId) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Tukang tidak ditemukan"));

        if ("BOOKED".equals(worker.getWorkStatus())) {
            throw new IllegalStateException("Tukang sedang dalam pekerjaan lain");
        }
        if ("CLOSED".equals(worker.getWorkStatus())) {
            throw new IllegalStateException("Tukang tidak menerima pekerjaan saat ini");
        }

        worker.setWorkStatus("BOOKED");
        worker.setIsAvailable(false);
        workerRepository.save(worker);

        return toDto(worker);
    }

    private WorkerDto toDto(WorkerEntity e) {
        return WorkerDto.builder()
                .id(e.getId())
                .name(e.getName())
                .avatar(e.getAvatar())
                .age(e.getAge())
                .experience(e.getExperience())
                .rating(e.getRating())
                .totalReviews(e.getTotalReviews())
                .specializations(e.getSpecializations())
                .location(e.getLocation())
                .pricePerDay(e.getPricePerDay())
                .isAvailable(e.getIsAvailable())
                .workStatus(e.getWorkStatus())
                .authUserId(e.getAuthUserId())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .bio(e.getBio())
                .build();
    }
}
