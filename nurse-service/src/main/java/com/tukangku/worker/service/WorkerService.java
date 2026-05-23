package com.tukangku.worker.service;

import com.tukangku.worker.dto.WorkerCreateRequest;
import com.tukangku.worker.dto.WorkerDto;
import com.tukangku.worker.dto.RatingUpdateRequest;
import com.tukangku.worker.entity.WorkerEntity;
import com.tukangku.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .bio(e.getBio())
                .build();
    }
}
