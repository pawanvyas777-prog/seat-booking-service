package com.seatbooking.seatbookingservice.repository;

import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByStatusAndLockTimestampBefore(SeatStatus status, Instant time);

    Optional<Seat> findBySeatNumber(String seatNumber);


    List<Seat> findBySeatNumberIn(List<String> seatNumbers);
}
