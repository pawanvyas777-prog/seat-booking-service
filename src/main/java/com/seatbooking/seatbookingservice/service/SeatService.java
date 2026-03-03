package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.exception.SeatBookingException;
import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import com.seatbooking.seatbookingservice.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);

    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;

    public SeatService(SeatRepository seatRepository,
                       SeatLockService seatLockService) {
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
    }

    // ==============================
    // 1️⃣ View all seats
    // ==============================
    public List<Seat> getAllSeats() {
        return seatRepository.findAll(Sort.by("seatNumber"));
    }

    // ==============================
    // 2️⃣ Reserve Seat (Before Payment)
    // ==============================
    @Transactional
    public void reserveSeat(String seatNumber, String userId) {

        // Check seat exists
        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() ->
                        new SeatBookingException(
                                "Seat does not exist",
                                HttpStatus.BAD_REQUEST));

        // Check not already booked
        if (SeatStatus.BOOKED.equals(seat.getStatus())) {
            throw new SeatBookingException(
                    "Seat already booked",
                    HttpStatus.CONFLICT);
        }

        // Try Redis lock
        boolean locked = seatLockService.lockSeat(seatNumber, userId);

        if (!locked) {
            throw new SeatBookingException(
                    "Seat already reserved by another user",
                    HttpStatus.CONFLICT);
        }
    }

    // ==============================
    // 3️⃣ Validate Before Confirm (All 4 checks)
    // ==============================
    @Transactional
    public void validateBeforeConfirm(String seatNumber, String userId) {

        // 1️⃣ Check seat exists
        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() ->
                        new SeatBookingException(
                                "Seat does not exist",
                                HttpStatus.BAD_REQUEST));

        // 2️⃣ Check not already booked
        if (SeatStatus.BOOKED.equals(seat.getStatus())) {
            throw new SeatBookingException(
                    "Seat already booked",
                    HttpStatus.CONFLICT);
        }

        // 3️⃣ Check Redis lock exists
        String owner = seatLockService.getLockOwner(seatNumber);

        if (owner == null) {
            throw new SeatBookingException(
                    "Reservation expired",
                    HttpStatus.GONE);
        }

        // 4️⃣ Validate lock owner
        if (!owner.equalsIgnoreCase(userId)) {
            throw new SeatBookingException(
                    "Seat " +seatNumber +  " locked by another user",
                    HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void validateBeforeConfirmMultiple(List<String> seatNumbers, String userId) {

        // 1️⃣ Fetch seats
        List<Seat> seats = seatRepository.findBySeatNumberIn(seatNumbers);

        if (seats.size() != seatNumbers.size()) {
            throw new SeatBookingException(
                    "One or more seats do not exist",
                    HttpStatus.BAD_REQUEST);
        }

        // 2️⃣ Check none already BOOKED
        for (Seat seat : seats) {
            if (SeatStatus.BOOKED.equals(seat.getStatus())) {
                throw new SeatBookingException(
                        "Seat " + seat.getSeatNumber() + " is already booked",
                        HttpStatus.CONFLICT);
            }
        }

        // 3️⃣ Validate Redis lock for each
        for (String seatNumber : seatNumbers) {

            String owner = seatLockService.getLockOwner(seatNumber);

            if (owner == null) {
                throw new SeatBookingException(
                        "Reservation expired for seat " + seatNumbers,
                        HttpStatus.GONE);
            }

            if (!owner.equalsIgnoreCase(userId)) {
                throw new SeatBookingException(
                        "Seat(s) " + seatNumbers + " locked by another user",
                        HttpStatus.CONFLICT);
            }
        }
    }

    // ==============================
    // 4️⃣ Confirm Booking (Called by Consumer AFTER Payment Success)
    // ==============================
    @Transactional
    public void confirmBooking(List<String> seatNumbers, String userId) {

        List<Seat> seats = seatRepository.findBySeatNumberIn(seatNumbers);

        // Safety check
        if (seats.size() != seatNumbers.size()) {
            throw new SeatBookingException(
                    "One or more seats do not exist",
                    HttpStatus.BAD_REQUEST);
        }

        for (Seat seat : seats) {

            if (SeatStatus.BOOKED.equals(seat.getStatus())) {
                throw new SeatBookingException(
                        "Seat " + seat.getSeatNumber() + " is already booked",
                        HttpStatus.CONFLICT);
            }

            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedBy(userId);
            seat.setLockTimestamp(null);
        }
        seatRepository.saveAll(seats);
        log.info("Seats {} successfully booked for user {}", seatNumbers, userId);

        // Remove Redis locks after successful DB update
        for (String seatNumber : seatNumbers) {
            seatLockService.unlockSeat(seatNumber);
        }
    }

    @Transactional
    public void reserveMultipleSeats(List<String> seatNumbers, String userId) {

        // 1️⃣ Fetch all seats from DB
        List<Seat> seats = seatRepository.findBySeatNumberIn(seatNumbers);

        // Safety check: ensure all seats exist
        if (seats.size() != seatNumbers.size()) {
            throw new SeatBookingException(
                    "One or more seats do not exist",
                    HttpStatus.BAD_REQUEST);
        }

        // 2️⃣ Check none are already BOOKED
        for (Seat seat : seats) {
            if (SeatStatus.BOOKED.equals(seat.getStatus())) {
                throw new SeatBookingException(
                        "Seat " + seat.getSeatNumber() + " is already booked",
                        HttpStatus.CONFLICT);
            }
        }

        // 3️⃣ Try locking all in Redis (atomic behavior)
        List<String> successfullyLocked = new ArrayList<>();

        for (String seatNumber : seatNumbers) {

            boolean locked = seatLockService.lockSeat(seatNumber, userId);

            if (!locked) {
                // Rollback previously locked seats
                for (String lockedSeat : successfullyLocked) {
                    seatLockService.unlockSeat(lockedSeat);
                }

                throw new SeatBookingException(
                        "One or more seats are already reserved",
                        HttpStatus.CONFLICT);
            }

            successfullyLocked.add(seatNumber);
        }
    }

    // ==============================
    // 5️⃣ Cancel Booking
    // ==============================
    @Transactional
    public Seat cancelBooking(String seatNumber) {

        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() ->
                        new SeatBookingException(
                                "Seat does not exist",
                                HttpStatus.BAD_REQUEST));

        if (!SeatStatus.BOOKED.equals(seat.getStatus())) {
            throw new SeatBookingException(
                    "Only BOOKED seats can be cancelled",
                    HttpStatus.CONFLICT);
        }

        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setLockedBy(null);
        seat.setLockTimestamp(null);

        return seatRepository.save(seat);
    }

    public long getLockDuration() {
        return seatLockService.getLockDuration();
    }
}