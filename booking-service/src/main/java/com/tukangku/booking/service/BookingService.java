package com.tukangku.booking.service;

import com.tukangku.booking.client.WorkerClient;
import com.tukangku.booking.client.WorkerInfoDto;
import com.tukangku.booking.dto.BookingDto;
import com.tukangku.booking.dto.CreateBookingRequest;
import com.tukangku.booking.entity.BookingEntity;
import com.tukangku.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WorkerClient      workerClient;

    /**
     * Buat booking baru.
     * Alur:
     * 1. Cek duplikat booking pada tanggal yang sama di booking_db
     * 2. Panggil worker-service /internal/workers/{id}/book (atomic: validasi + set BOOKED)
     * 3. Simpan booking ke booking_db dengan cache nama & avatar tukang
     */
    @Transactional
    public BookingDto create(String customerId, CreateBookingRequest request) {
        // 1. Cek apakah sudah ada booking aktif untuk tukang pada tanggal ini
        boolean alreadyBooked = bookingRepository.existsByWorkerIdAndBookingDateAndStatusNot(
                request.getWorkerId(), request.getBookingDate(), "CANCELLED");
        if (alreadyBooked) {
            throw new IllegalStateException("Tukang sudah ada booking pada tanggal tersebut");
        }

        // 2. Panggil worker-service: validasi status OPEN dan tandai BOOKED (atomic)
        WorkerInfoDto worker = workerClient.bookWorker(request.getWorkerId());

        // 3. Simpan booking — cache nama & avatar agar read tidak perlu cross-service call
        BookingEntity booking = BookingEntity.builder()
                .workerId(request.getWorkerId())
                .customerId(customerId)
                .customerName(request.getCustomerName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .durationDays(request.getDurationDays())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .workerName(worker.getName())
                .workerAvatar(worker.getAvatar())
                .status("PENDING")
                .build();

        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    public List<BookingDto> getMyBookings(String customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Ambil semua order (booking) yang ditujukan ke tukang yang sedang login.
     * authUserId di-resolve ke workerId via nurse-service internal call.
     */
    public List<BookingDto> getWorkerOrders(String authUserId) {
        String workerId = workerClient.getWorkerIdByAuthUserId(authUserId);
        return bookingRepository.findByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Tukang memulai/konfirmasi order miliknya.
     * Validasi: status harus PENDING, requester harus tukang pemilik booking.
     */
    @Transactional
    public BookingDto confirmByTukang(String bookingId, String authUserId) {
        String workerId = workerClient.getWorkerIdByAuthUserId(authUserId);

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking tidak ditemukan"));

        if (!booking.getWorkerId().equals(workerId)) {
            throw new IllegalStateException("Tidak diizinkan mengubah status booking ini");
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking sudah diproses sebelumnya (status: " + booking.getStatus() + ")");
        }

        booking.setStatus("CONFIRMED");
        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    /**
     * Customer menandai order selesai: CONFIRMED → COMPLETED.
     * Setelah sukses, worker-service dipanggil untuk melepas status BOOKED → OPEN.
     */
    @Transactional
    public BookingDto completeByCustomer(String bookingId, String customerId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking tidak ditemukan"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new IllegalStateException("Tidak diizinkan mengubah status booking ini");
        }
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalStateException(
                    "Order belum dikonfirmasi tukang (status: " + booking.getStatus() + ")");
        }

        booking.setStatus("COMPLETED");
        booking = bookingRepository.save(booking);

        // Non-fatal: release worker back to OPEN
        workerClient.releaseWorker(booking.getWorkerId());

        return toDto(booking);
    }

    public BookingDto getById(String bookingId, String requesterId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking tidak ditemukan"));

        if (!booking.getCustomerId().equals(requesterId) &&
            !booking.getWorkerId().equals(requesterId)) {
            throw new IllegalStateException("Tidak diizinkan mengakses booking ini");
        }

        return toDto(booking);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private BookingDto toDto(BookingEntity b) {
        return BookingDto.builder()
                .id(b.getId())
                .workerId(b.getWorkerId())
                .workerName(b.getWorkerName())
                .workerAvatar(b.getWorkerAvatar())
                .customerId(b.getCustomerId())
                .customerName(b.getCustomerName())
                .address(b.getAddress())
                .city(b.getCity())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .durationDays(b.getDurationDays())
                .paymentMethod(b.getPaymentMethod())
                .status(b.getStatus())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
