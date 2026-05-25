package com.tukangku.booking.service;

import com.tukangku.booking.client.WorkerClient;
import com.tukangku.booking.client.WorkerInfoDto;
import com.tukangku.booking.dto.BookingDto;
import com.tukangku.booking.dto.CreateBookingRequest;
import com.tukangku.booking.entity.BookingEntity;
import com.tukangku.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService")
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock WorkerClient workerClient;
    @InjectMocks BookingService bookingService;

    private BookingEntity pendingBooking;
    private WorkerInfoDto mockWorker;

    @BeforeEach
    void setUp() {
        pendingBooking = BookingEntity.builder()
                .id("booking-1")
                .workerId("worker-1")
                .customerId("customer-1")
                .customerName("Budi Santoso")
                .address("Jl. Test No. 1")
                .city("Jakarta")
                .latitude(-6.2)
                .longitude(106.816)
                .bookingDate(LocalDate.now())
                .startTime("08:00")
                .durationDays(1)
                .paymentMethod("CASH")
                .status("PENDING")
                .workerName("Rizky Tukang")
                .workerAvatar("https://cdn.test/avatar.jpg")
                .build();

        mockWorker = new WorkerInfoDto();
        mockWorker.setId("worker-1");
        mockWorker.setName("Rizky Tukang");
        mockWorker.setAvatar("https://cdn.test/avatar.jpg");
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        private CreateBookingRequest buildRequest() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setWorkerId("worker-1");
            req.setCustomerName("Budi Santoso");
            req.setAddress("Jl. Test No. 1");
            req.setCity("Jakarta");
            req.setLatitude(-6.2);
            req.setLongitude(106.816);
            req.setBookingDate(LocalDate.now());
            req.setStartTime("08:00");
            req.setDurationDays(1);
            req.setPaymentMethod("CASH");
            return req;
        }

        @Test
        @DisplayName("returns BookingDto with PENDING status on success")
        void success_returnsPendingBooking() {
            when(bookingRepository.existsByWorkerIdAndBookingDateAndStatusNot(any(), any(), any()))
                    .thenReturn(false);
            when(workerClient.bookWorker("worker-1")).thenReturn(mockWorker);
            when(bookingRepository.save(any(BookingEntity.class))).thenReturn(pendingBooking);

            BookingDto result = bookingService.create("customer-1", buildRequest());

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getWorkerId()).isEqualTo("worker-1");
            assertThat(result.getWorkerName()).isEqualTo("Rizky Tukang");
        }

        @Test
        @DisplayName("caches worker name and avatar from WorkerClient response")
        void cachesWorkerInfo() {
            when(bookingRepository.existsByWorkerIdAndBookingDateAndStatusNot(any(), any(), any()))
                    .thenReturn(false);
            when(workerClient.bookWorker("worker-1")).thenReturn(mockWorker);
            when(bookingRepository.save(any(BookingEntity.class))).thenReturn(pendingBooking);

            ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
            bookingService.create("customer-1", buildRequest());

            verify(bookingRepository).save(captor.capture());
            assertThat(captor.getValue().getWorkerName()).isEqualTo("Rizky Tukang");
            assertThat(captor.getValue().getWorkerAvatar()).isEqualTo("https://cdn.test/avatar.jpg");
        }

        @Test
        @DisplayName("throws IllegalStateException when tukang already has a booking on same date")
        void alreadyBooked_throwsIllegalState() {
            when(bookingRepository.existsByWorkerIdAndBookingDateAndStatusNot(any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> bookingService.create("customer-1", buildRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sudah ada booking");

            verify(workerClient, never()).bookWorker(any());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("does not persist booking when WorkerClient throws")
        void workerClientThrows_doesNotSave() {
            when(bookingRepository.existsByWorkerIdAndBookingDateAndStatusNot(any(), any(), any()))
                    .thenReturn(false);
            when(workerClient.bookWorker("worker-1"))
                    .thenThrow(new IllegalStateException("Tukang tidak tersedia"));

            assertThatThrownBy(() -> bookingService.create("customer-1", buildRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("tidak tersedia");

            verify(bookingRepository, never()).save(any());
        }
    }

    // ─── getMyBookings ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyBookings()")
    class GetMyBookings {

        @Test
        @DisplayName("returns list of bookings filtered by customerId")
        void returnsBookingsForCustomer() {
            when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-1"))
                    .thenReturn(List.of(pendingBooking));

            List<BookingDto> result = bookingService.getMyBookings("customer-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCustomerId()).isEqualTo("customer-1");
        }

        @Test
        @DisplayName("returns empty list when customer has no bookings")
        void noBookings_returnsEmpty() {
            when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-99"))
                    .thenReturn(List.of());

            assertThat(bookingService.getMyBookings("customer-99")).isEmpty();
        }
    }

    // ─── getWorkerOrders ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWorkerOrders()")
    class GetWorkerOrders {

        @Test
        @DisplayName("resolves authUserId to workerId then returns matching bookings")
        void resolvesAuthUserAndReturnsOrders() {
            when(workerClient.getWorkerIdByAuthUserId("auth-1")).thenReturn("worker-1");
            when(bookingRepository.findByWorkerIdOrderByCreatedAtDesc("worker-1"))
                    .thenReturn(List.of(pendingBooking));

            List<BookingDto> result = bookingService.getWorkerOrders("auth-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkerId()).isEqualTo("worker-1");
        }

        @Test
        @DisplayName("propagates IllegalArgumentException from WorkerClient for unknown tukang")
        void unknownAuthUser_propagatesException() {
            when(workerClient.getWorkerIdByAuthUserId("bad-auth"))
                    .thenThrow(new IllegalArgumentException("Profil tukang tidak ditemukan"));

            assertThatThrownBy(() -> bookingService.getWorkerOrders("bad-auth"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profil tukang tidak ditemukan");
        }
    }

    // ─── confirmByTukang ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmByTukang()")
    class ConfirmByTukang {

        @Test
        @DisplayName("changes status to CONFIRMED and returns updated BookingDto")
        void success_statusBecomesConfirmed() {
            when(workerClient.getWorkerIdByAuthUserId("auth-1")).thenReturn("worker-1");
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            BookingDto result = bookingService.confirmByTukang("booking-1", "auth-1");

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            verify(bookingRepository).save(pendingBooking);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when booking not found")
        void bookingNotFound_throwsIllegalArgument() {
            when(workerClient.getWorkerIdByAuthUserId("auth-1")).thenReturn("worker-1");
            when(bookingRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.confirmByTukang("bad-id", "auth-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }

        @Test
        @DisplayName("throws IllegalStateException when tukang does not own the booking")
        void notOwner_throwsIllegalState() {
            when(workerClient.getWorkerIdByAuthUserId("other-auth")).thenReturn("worker-99");
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.confirmByTukang("booking-1", "other-auth"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Tidak diizinkan");
        }

        @Test
        @DisplayName("throws IllegalStateException when booking is already CONFIRMED")
        void alreadyConfirmed_throwsIllegalState() {
            pendingBooking.setStatus("CONFIRMED");
            when(workerClient.getWorkerIdByAuthUserId("auth-1")).thenReturn("worker-1");
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.confirmByTukang("booking-1", "auth-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sudah diproses");
        }

        @Test
        @DisplayName("throws IllegalStateException when booking is CANCELLED")
        void cancelled_throwsIllegalState() {
            pendingBooking.setStatus("CANCELLED");
            when(workerClient.getWorkerIdByAuthUserId("auth-1")).thenReturn("worker-1");
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.confirmByTukang("booking-1", "auth-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sudah diproses");
        }
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("customer who owns the booking can access it")
        void customerOwner_canAccess() {
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            BookingDto result = bookingService.getById("booking-1", "customer-1");

            assertThat(result.getId()).isEqualTo("booking-1");
        }

        @Test
        @DisplayName("worker assigned to the booking can access it")
        void workerOwner_canAccess() {
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            BookingDto result = bookingService.getById("booking-1", "worker-1");

            assertThat(result.getId()).isEqualTo("booking-1");
        }

        @Test
        @DisplayName("throws IllegalStateException when requester is neither customer nor worker of booking")
        void stranger_throwsIllegalState() {
            when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(pendingBooking));

            assertThatThrownBy(() -> bookingService.getById("booking-1", "stranger-id"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Tidak diizinkan");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when booking not found")
        void bookingNotFound_throwsIllegalArgument() {
            when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getById("missing", "customer-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }
}
