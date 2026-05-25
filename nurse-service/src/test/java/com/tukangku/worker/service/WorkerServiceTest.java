package com.tukangku.worker.service;

import com.tukangku.worker.dto.*;
import com.tukangku.worker.entity.WorkerEntity;
import com.tukangku.worker.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService")
class WorkerServiceTest {

    @Mock WorkerRepository workerRepository;
    @InjectMocks WorkerService workerService;

    private WorkerEntity openWorker;

    @BeforeEach
    void setUp() {
        openWorker = WorkerEntity.builder()
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
                .latitude(-6.2)
                .longitude(106.816)
                .build();
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns WorkerDto when worker exists")
        void success_returnsWorkerDto() {
            when(workerRepository.findById("worker-1")).thenReturn(Optional.of(openWorker));

            WorkerDto result = workerService.findById("worker-1");

            assertThat(result.getId()).isEqualTo("worker-1");
            assertThat(result.getName()).isEqualTo("Rizky Pratama");
            assertThat(result.getWorkStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when worker not found")
        void notFound_throwsIllegalArgument() {
            when(workerRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workerService.findById("bad-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns all workers from repository")
        void returnsAllWorkers() {
            when(workerRepository.findBySearchAndAvailability(null, null))
                    .thenReturn(List.of(openWorker));

            List<WorkerDto> result = workerService.findAll(null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("worker-1");
        }

        @Test
        @DisplayName("passes search and available parameters to repository")
        void passesSearchAndAvailableToRepository() {
            when(workerRepository.findBySearchAndAvailability("Rizky", true))
                    .thenReturn(List.of(openWorker));

            List<WorkerDto> result = workerService.findAll("Rizky", true);

            assertThat(result).hasSize(1);
            verify(workerRepository).findBySearchAndAvailability("Rizky", true);
        }
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves worker and returns mapped WorkerDto")
        void success_savesAndReturnsDto() {
            WorkerCreateRequest req = new WorkerCreateRequest();
            req.setName("Baru Tukang");
            req.setLocation("Bandung");
            req.setPricePerDay(new BigDecimal("250000"));
            req.setAge(25);
            req.setExperience(2);

            when(workerRepository.save(any(WorkerEntity.class))).thenReturn(openWorker);

            WorkerDto result = workerService.create(req);

            assertThat(result).isNotNull();
            verify(workerRepository).save(any(WorkerEntity.class));
        }

        @Test
        @DisplayName("defaults workStatus to OPEN when not specified")
        void nullWorkStatus_defaultsToOpen() {
            WorkerCreateRequest req = new WorkerCreateRequest();
            req.setName("Tukang");
            // workStatus is null

            when(workerRepository.save(any(WorkerEntity.class))).thenAnswer(inv -> {
                WorkerEntity saved = inv.getArgument(0);
                assertThat(saved.getWorkStatus()).isEqualTo("OPEN");
                assertThat(saved.getIsAvailable()).isTrue();
                return saved;
            });

            workerService.create(req);
        }
    }

    // ─── bookWorker ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("bookWorker()")
    class BookWorker {

        @Test
        @DisplayName("sets workStatus to BOOKED and isAvailable to false on success")
        void success_setsStatusBooked() {
            when(workerRepository.findById("worker-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any(WorkerEntity.class))).thenReturn(openWorker);

            WorkerDto result = workerService.bookWorker("worker-1");

            assertThat(openWorker.getWorkStatus()).isEqualTo("BOOKED");
            assertThat(openWorker.getIsAvailable()).isFalse();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws IllegalStateException when worker is already BOOKED")
        void alreadyBooked_throwsIllegalState() {
            openWorker.setWorkStatus("BOOKED");
            when(workerRepository.findById("worker-1")).thenReturn(Optional.of(openWorker));

            assertThatThrownBy(() -> workerService.bookWorker("worker-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pekerjaan lain");
        }

        @Test
        @DisplayName("throws IllegalStateException when worker is CLOSED")
        void closed_throwsIllegalState() {
            openWorker.setWorkStatus("CLOSED");
            when(workerRepository.findById("worker-1")).thenReturn(Optional.of(openWorker));

            assertThatThrownBy(() -> workerService.bookWorker("worker-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("tidak menerima pekerjaan");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when worker not found")
        void notFound_throwsIllegalArgument() {
            when(workerRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workerService.bookWorker("missing"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }

    // ─── updateWorkStatus ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateWorkStatus()")
    class UpdateWorkStatus {

        @Test
        @DisplayName("OPEN status sets isAvailable to true")
        void openStatus_setsAvailableTrue() {
            openWorker.setWorkStatus("CLOSED");
            openWorker.setIsAvailable(false);
            when(workerRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkStatusUpdateRequest req = new WorkStatusUpdateRequest();
            req.setStatus("OPEN");
            WorkerDto result = workerService.updateWorkStatus("auth-1", req);

            assertThat(openWorker.getIsAvailable()).isTrue();
            assertThat(openWorker.getWorkStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("CLOSED status sets isAvailable to false")
        void closedStatus_setsAvailableFalse() {
            when(workerRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkStatusUpdateRequest req = new WorkStatusUpdateRequest();
            req.setStatus("CLOSED");
            workerService.updateWorkStatus("auth-1", req);

            assertThat(openWorker.getIsAvailable()).isFalse();
            assertThat(openWorker.getWorkStatus()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when tukang profile not linked")
        void notFound_throwsIllegalArgument() {
            when(workerRepository.findByAuthUserId("bad-auth")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workerService.updateWorkStatus("bad-auth", new WorkStatusUpdateRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }

    // ─── updateSalary ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSalary()")
    class UpdateSalary {

        @Test
        @DisplayName("updates pricePerDay to new value")
        void updatesSalary() {
            when(workerRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TukangSalaryUpdateRequest req = new TukangSalaryUpdateRequest();
            req.setPricePerDay(new BigDecimal("500000"));

            WorkerDto result = workerService.updateSalary("auth-1", req);

            assertThat(openWorker.getPricePerDay()).isEqualByComparingTo(new BigDecimal("500000"));
        }
    }

    // ─── updateLocation ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("updates latitude and longitude")
        void updatesCoordinates() {
            when(workerRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TukangLocationUpdateRequest req = new TukangLocationUpdateRequest();
            req.setLatitude(-7.25);
            req.setLongitude(112.75);

            workerService.updateLocation("auth-1", req);

            assertThat(openWorker.getLatitude()).isEqualTo(-7.25);
            assertThat(openWorker.getLongitude()).isEqualTo(112.75);
        }
    }

    // ─── updateRating ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRating()")
    class UpdateRating {

        @Test
        @DisplayName("updates rating and totalReviews on existing worker")
        void updatesRatingAndTotalReviews() {
            when(workerRepository.findById("worker-1")).thenReturn(Optional.of(openWorker));
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RatingUpdateRequest req = new RatingUpdateRequest(4.8, 15);
            workerService.updateRating("worker-1", req);

            assertThat(openWorker.getRating()).isEqualTo(4.8);
            assertThat(openWorker.getTotalReviews()).isEqualTo(15);
        }
    }

    // ─── getByAuthUserId ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByAuthUserId()")
    class GetByAuthUserId {

        @Test
        @DisplayName("returns WorkerDto for linked auth user")
        void success_returnsWorkerDto() {
            when(workerRepository.findByAuthUserId("auth-1")).thenReturn(Optional.of(openWorker));

            WorkerDto result = workerService.getByAuthUserId("auth-1");

            assertThat(result.getAuthUserId()).isEqualTo("auth-1");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when no worker linked to auth user")
        void notLinked_throwsIllegalArgument() {
            when(workerRepository.findByAuthUserId("unlinked")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workerService.getByAuthUserId("unlinked"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }
}
