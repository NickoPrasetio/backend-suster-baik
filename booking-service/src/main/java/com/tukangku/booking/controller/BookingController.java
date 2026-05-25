package com.tukangku.booking.controller;

import com.tukangku.booking.dto.BookingDto;
import com.tukangku.booking.dto.CreateBookingRequest;
import com.tukangku.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Mengembalikan tanggal & waktu server saat ini.
     * Dipakai frontend agar validasi tanggal tidak bisa dimanipulasi lewat jam perangkat.
     */
    @GetMapping("/api/bookings/server-time")
    public ResponseEntity<?> getServerTime() {
        return ResponseEntity.ok(Map.of(
                "date",     LocalDate.now().toString(),
                "dateTime", LocalDateTime.now().toString()
        ));
    }

    /**
     * Tukang memulai order (PENDING → CONFIRMED).
     * Validasi GPS & tanggal dilakukan di frontend; backend hanya cek otorisasi & status.
     */
    @PatchMapping("/api/bookings/{id}/confirm")
    public ResponseEntity<?> confirmBooking(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        try {
            BookingDto booking = bookingService.confirmByTukang(id, userId);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<?> createBooking(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateBookingRequest request) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        try {
            BookingDto booking = bookingService.create(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/bookings/my")
    public ResponseEntity<?> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        List<BookingDto> bookings = bookingService.getMyBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    /** Ambil order (booking masuk) untuk tukang yang sedang login */
    @GetMapping("/api/bookings/my-orders")
    public ResponseEntity<?> getMyWorkerOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        try {
            List<BookingDto> orders = bookingService.getWorkerOrders(userId);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<?> getBooking(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(bookingService.getById(id, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
