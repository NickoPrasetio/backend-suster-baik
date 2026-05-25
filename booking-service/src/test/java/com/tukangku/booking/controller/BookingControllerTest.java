package com.tukangku.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukangku.booking.dto.BookingDto;
import com.tukangku.booking.dto.CreateBookingRequest;
import com.tukangku.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@DisplayName("BookingController")
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookingService bookingService;

    private BookingDto mockBooking;

    @BeforeEach
    void setUp() {
        mockBooking = BookingDto.builder()
                .id("booking-1")
                .workerId("worker-1")
                .workerName("Rizky Tukang")
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
                .build();
    }

    // ─── GET /api/bookings/server-time ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/bookings/server-time returns date and dateTime")
    void serverTime_returns200WithDateFields() throws Exception {
        mockMvc.perform(get("/api/bookings/server-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.dateTime").exists());
    }

    // ─── POST /api/bookings ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/bookings")
    class CreateBooking {

        private CreateBookingRequest buildValidRequest() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setWorkerId("worker-1");
            req.setCustomerName("Budi Santoso");
            req.setAddress("Jl. Test No. 1");
            req.setCity("Jakarta");
            req.setLatitude(-6.2);
            req.setLongitude(106.816);
            req.setBookingDate(LocalDate.now().plusDays(1));
            req.setStartTime("08:00");
            req.setDurationDays(1);
            req.setPaymentMethod("CASH");
            return req;
        }

        @Test
        @DisplayName("201 Created with booking on valid request")
        void validRequest_returns201() throws Exception {
            when(bookingService.create(eq("customer-1"), any(CreateBookingRequest.class)))
                    .thenReturn(mockBooking);

            mockMvc.perform(post("/api/bookings")
                            .header("X-User-Id", "customer-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("booking-1"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 Bad Request when required fields are missing")
        void missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("X-User-Id", "customer-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 Conflict when tukang already has booking on that date")
        void alreadyBooked_returns409() throws Exception {
            when(bookingService.create(any(), any()))
                    .thenThrow(new IllegalStateException("Tukang sudah ada booking pada tanggal tersebut"));

            mockMvc.perform(post("/api/bookings")
                            .header("X-User-Id", "customer-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Tukang sudah ada booking pada tanggal tersebut"));
        }
    }

    // ─── GET /api/bookings/my ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/bookings/my")
    class GetMyBookings {

        @Test
        @DisplayName("200 OK with booking list for authenticated customer")
        void authenticated_returns200WithList() throws Exception {
            when(bookingService.getMyBookings("customer-1")).thenReturn(List.of(mockBooking));

            mockMvc.perform(get("/api/bookings/my")
                            .header("X-User-Id", "customer-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("booking-1"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(get("/api/bookings/my"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── GET /api/bookings/my-orders ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/bookings/my-orders")
    class GetMyWorkerOrders {

        @Test
        @DisplayName("200 OK with order list for authenticated tukang")
        void authenticated_returns200() throws Exception {
            when(bookingService.getWorkerOrders("auth-1")).thenReturn(List.of(mockBooking));

            mockMvc.perform(get("/api/bookings/my-orders")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].workerId").value("worker-1"));
        }

        @Test
        @DisplayName("404 Not Found when tukang profile is not linked")
        void notLinked_returns404() throws Exception {
            when(bookingService.getWorkerOrders("auth-1"))
                    .thenThrow(new IllegalArgumentException("Profil tukang tidak ditemukan"));

            mockMvc.perform(get("/api/bookings/my-orders")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Profil tukang tidak ditemukan"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(get("/api/bookings/my-orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── PATCH /api/bookings/{id}/confirm ────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/bookings/{id}/confirm")
    class ConfirmBooking {

        @Test
        @DisplayName("200 OK with CONFIRMED booking on success")
        void success_returns200WithConfirmed() throws Exception {
            BookingDto confirmed = BookingDto.builder()
                    .id("booking-1").status("CONFIRMED").build();
            when(bookingService.confirmByTukang("booking-1", "auth-1")).thenReturn(confirmed);

            mockMvc.perform(patch("/api/bookings/booking-1/confirm")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(patch("/api/bookings/booking-1/confirm"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found when booking does not exist")
        void bookingNotFound_returns404() throws Exception {
            when(bookingService.confirmByTukang("bad-id", "auth-1"))
                    .thenThrow(new IllegalArgumentException("Booking tidak ditemukan"));

            mockMvc.perform(patch("/api/bookings/bad-id/confirm")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 Conflict when booking was already processed")
        void alreadyProcessed_returns409() throws Exception {
            when(bookingService.confirmByTukang("booking-1", "auth-1"))
                    .thenThrow(new IllegalStateException("Booking sudah diproses sebelumnya (status: CONFIRMED)"));

            mockMvc.perform(patch("/api/bookings/booking-1/confirm")
                            .header("X-User-Id", "auth-1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Booking sudah diproses sebelumnya (status: CONFIRMED)"));
        }
    }

    // ─── GET /api/bookings/{id} ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/bookings/{id}")
    class GetBookingById {

        @Test
        @DisplayName("200 OK when requester is the customer or worker")
        void authorized_returns200() throws Exception {
            when(bookingService.getById("booking-1", "customer-1")).thenReturn(mockBooking);

            mockMvc.perform(get("/api/bookings/booking-1")
                            .header("X-User-Id", "customer-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("booking-1"));
        }

        @Test
        @DisplayName("403 Forbidden when requester is not associated with the booking")
        void notAssociated_returns403() throws Exception {
            when(bookingService.getById("booking-1", "stranger"))
                    .thenThrow(new IllegalStateException("Tidak diizinkan mengakses booking ini"));

            mockMvc.perform(get("/api/bookings/booking-1")
                            .header("X-User-Id", "stranger"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(get("/api/bookings/booking-1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
