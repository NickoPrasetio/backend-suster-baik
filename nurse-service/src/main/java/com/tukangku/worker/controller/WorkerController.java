package com.tukangku.worker.controller;

import com.tukangku.worker.dto.WorkerCreateRequest;
import com.tukangku.worker.dto.WorkerDto;
import com.tukangku.worker.dto.RatingUpdateRequest;
import com.tukangku.worker.service.MinioService;
import com.tukangku.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final MinioService minioService;

    @GetMapping("/api/workers")
    public ResponseEntity<List<WorkerDto>> getWorkers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(workerService.findAll(search, available));
    }

    @GetMapping("/api/workers/{id}")
    public ResponseEntity<WorkerDto> getWorker(@PathVariable String id) {
        return ResponseEntity.ok(workerService.findById(id));
    }

    @PostMapping("/api/workers")
    public ResponseEntity<WorkerDto> createWorker(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestBody WorkerCreateRequest request) {
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(workerService.create(request));
    }

    @PostMapping(value = "/api/workers/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File tidak boleh kosong"));
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ukuran file maksimal 5MB"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "File harus berupa gambar"));
        }
        try {
            String ext = getExtension(file.getOriginalFilename());
            String objectName = "worker-" + id + "-" + System.currentTimeMillis() + ext;
            String url = minioService.uploadPhoto(file, objectName);
            WorkerDto updated = workerService.updateAvatar(id, url);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Gagal upload foto"));
        }
    }

    @PutMapping("/internal/workers/{id}/rating")
    public ResponseEntity<Void> updateRating(
            @PathVariable String id,
            @RequestBody RatingUpdateRequest request) {
        workerService.updateRating(id, request);
        return ResponseEntity.ok().build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
