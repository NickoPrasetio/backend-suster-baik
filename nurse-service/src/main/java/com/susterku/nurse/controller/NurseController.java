package com.susterku.nurse.controller;

import com.susterku.nurse.dto.NurseDto;
import com.susterku.nurse.dto.RatingUpdateRequest;
import com.susterku.nurse.service.NurseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    @GetMapping("/api/nurses")
    public ResponseEntity<List<NurseDto>> getNurses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(nurseService.findAll(search, available));
    }

    @GetMapping("/api/nurses/{id}")
    public ResponseEntity<NurseDto> getNurse(@PathVariable String id) {
        return ResponseEntity.ok(nurseService.findById(id));
    }

    /** Internal endpoint called by review-service to sync rating */
    @PutMapping("/internal/nurses/{id}/rating")
    public ResponseEntity<Void> updateRating(
            @PathVariable String id,
            @RequestBody RatingUpdateRequest request) {
        nurseService.updateRating(id, request);
        return ResponseEntity.ok().build();
    }
}
