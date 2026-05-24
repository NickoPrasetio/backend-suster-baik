package com.tukangku.booking.repository;

import com.tukangku.booking.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<BookingEntity> findByWorkerIdOrderByCreatedAtDesc(String workerId);

    boolean existsByWorkerIdAndBookingDateAndStatusNot(
            String workerId, LocalDate bookingDate, String status);
}
