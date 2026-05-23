package com.susterku.nurse.service;

import com.susterku.nurse.dto.NurseDto;
import com.susterku.nurse.dto.RatingUpdateRequest;
import com.susterku.nurse.entity.NurseEntity;
import com.susterku.nurse.repository.NurseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NurseService {

    private final NurseRepository nurseRepository;

    public List<NurseDto> findAll(String search, Boolean available) {
        return nurseRepository.findBySearchAndAvailability(search, available)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public NurseDto findById(String id) {
        return nurseRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Suster tidak ditemukan"));
    }

    public void updateRating(String id, RatingUpdateRequest request) {
        NurseEntity nurse = nurseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suster tidak ditemukan"));
        nurse.setRating(request.getNewRating());
        nurse.setTotalReviews(request.getTotalReviews());
        nurseRepository.save(nurse);
    }

    private NurseDto toDto(NurseEntity e) {
        return NurseDto.builder()
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
