package com.tukangku.worker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukangku.worker.dto.*;
import com.tukangku.worker.service.MinioService;
import com.tukangku.worker.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkerController.class)
@DisplayName("WorkerController")
class WorkerControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean WorkerService workerService;
    @MockBean MinioService minioService;

    private WorkerDto mockWorker;

    @BeforeEach
    void setUp() {
        mockWorker = WorkerDto.builder()
                .id("worker-1")
                .name("Rizky Pratama")
                .avatar("https://cdn.test/avatar.jpg")
                .age(30)
                .experience(5)
                .rating(4.5)
                .totalReviews(10)
                .specializations(List.of("Perawat", "Lansia"))
                .location("Jakarta Selatan")
                .pricePerDay(new BigDecimal("300000"))
                .isAvailable(true)
                .workStatus("OPEN")
                .authUserId("auth-1")
                .build();
    }

    // ─── GET /api/workers ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/workers")
    class GetWorkers {

        @Test
        @DisplayName("200 OK with list of all workers")
        void returnsAllWorkers() throws Exception {
            when(workerService.findAll(null, null)).thenReturn(List.of(mockWorker));

            mockMvc.perform(get("/api/workers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("worker-1"))
                    .andExpect(jsonPath("$[0].name").value("Rizky Pratama"));
        }

        @Test
        @DisplayName("200 OK with filtered list when search and available params given")
        void withParams_callsServiceWithParams() throws Exception {
            when(workerService.findAll("Rizky", true)).thenReturn(List.of(mockWorker));

            mockMvc.perform(get("/api/workers")
                            .param("search", "Rizky")
                            .param("available", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(workerService).findAll("Rizky", true);
        }
    }

    // ─── GET /api/workers/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/workers/{id}")
    class GetWorkerById {

        @Test
        @DisplayName("200 OK with worker detail when id exists")
        void existingId_returns200() throws Exception {
            when(workerService.findById("worker-1")).thenReturn(mockWorker);

            mockMvc.perform(get("/api/workers/worker-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("worker-1"))
                    .andExpect(jsonPath("$.workStatus").value("OPEN"));
        }
    }

    // ─── POST /api/workers ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/workers")
    class CreateWorker {

        private WorkerCreateRequest buildRequest() {
            WorkerCreateRequest req = new WorkerCreateRequest();
            req.setName("Tukang Baru");
            req.setLocation("Bandung");
            req.setPricePerDay(new BigDecimal("250000"));
            req.setAge(25);
            req.setExperience(3);
            return req;
        }

        @Test
        @DisplayName("201 Created when called with ROLE_ADMIN")
        void adminRole_returns201() throws Exception {
            when(workerService.create(any(WorkerCreateRequest.class))).thenReturn(mockWorker);

            mockMvc.perform(post("/api/workers")
                            .header("X-User-Role", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("worker-1"));
        }

        @Test
        @DisplayName("403 Forbidden when called without ROLE_ADMIN")
        void nonAdminRole_returns403() throws Exception {
            mockMvc.perform(post("/api/workers")
                            .header("X-User-Role", "ROLE_USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isForbidden());

            verify(workerService, never()).create(any());
        }

        @Test
        @DisplayName("403 Forbidden when X-User-Role header is missing")
        void missingRole_returns403() throws Exception {
            mockMvc.perform(post("/api/workers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/tukang/profile ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/tukang/profile")
    class GetTukangProfile {

        @Test
        @DisplayName("200 OK with tukang profile when X-User-Id is provided")
        void authenticated_returns200() throws Exception {
            when(workerService.getByAuthUserId("auth-1")).thenReturn(mockWorker);

            mockMvc.perform(get("/api/tukang/profile")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authUserId").value("auth-1"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(get("/api/tukang/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found when no worker profile linked to auth user")
        void notLinked_returns404() throws Exception {
            when(workerService.getByAuthUserId("auth-999"))
                    .thenThrow(new IllegalArgumentException("Profil tukang tidak ditemukan"));

            mockMvc.perform(get("/api/tukang/profile")
                            .header("X-User-Id", "auth-999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── PATCH /api/tukang/status ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/tukang/status")
    class UpdateStatus {

        @Test
        @DisplayName("200 OK with updated worker when status is valid")
        void validStatus_returns200() throws Exception {
            WorkerDto updated = WorkerDto.builder()
                    .id("worker-1").workStatus("CLOSED").isAvailable(false).build();
            when(workerService.updateWorkStatus(eq("auth-1"), any(WorkStatusUpdateRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/tukang/status")
                            .header("X-User-Id", "auth-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"CLOSED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workStatus").value("CLOSED"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(patch("/api/tukang/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"OPEN\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── PATCH /api/tukang/salary ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/tukang/salary")
    class UpdateSalary {

        @Test
        @DisplayName("200 OK with updated pricePerDay")
        void validSalary_returns200() throws Exception {
            WorkerDto updated = WorkerDto.builder()
                    .id("worker-1").pricePerDay(new BigDecimal("500000")).build();
            when(workerService.updateSalary(eq("auth-1"), any(TukangSalaryUpdateRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/tukang/salary")
                            .header("X-User-Id", "auth-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pricePerDay\":500000}"))
                    .andExpect(status().isOk());
        }
    }

    // ─── PATCH /api/tukang/location ───────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/tukang/location")
    class UpdateLocation {

        @Test
        @DisplayName("200 OK with updated coordinates")
        void validLocation_returns200() throws Exception {
            WorkerDto updated = WorkerDto.builder()
                    .id("worker-1").latitude(-7.25).longitude(112.75).build();
            when(workerService.updateLocation(eq("auth-1"), any(TukangLocationUpdateRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/tukang/location")
                            .header("X-User-Id", "auth-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"latitude\":-7.25,\"longitude\":112.75}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(patch("/api/tukang/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"latitude\":-6.2,\"longitude\":106.8}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
