package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import com.seatbooking.seatbookingservice.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class SeatLockScheduler {

    @Value("${seatbooking.lock.duration-minutes}")
    private int lockDuration;

    private final SeatRepository seatRepository;

    public SeatLockScheduler(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    // Runs every 60,000 milliseconds (1 minute)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredLocks() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(lockDuration));

        // Find seats that are LOCKED and have a timestamp older than 5 mins
        List<Seat> expiredSeats = seatRepository.findByStatusAndLockTimestampBefore(
                SeatStatus.LOCKED, threshold);

        if (!expiredSeats.isEmpty()) {
            System.out.println("Scheduler: Releasing " + expiredSeats.size() + " expired seats.");
            for (Seat seat : expiredSeats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockTimestamp(null);
                seat.setLockedBy(null);
            }
            seatRepository.saveAll(expiredSeats);
        }
    }
}