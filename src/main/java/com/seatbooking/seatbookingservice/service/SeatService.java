package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import com.seatbooking.seatbookingservice.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    // 1️ View all seats
    public List<Seat> getAllSeats() {
        //return seatRepository.findAll();
        return seatRepository.findAll(Sort.by("seatNumber"));
    }

    // 2️ Confirm booking (Called ONLY from RabbitMQ Consumer)
    /* @Transactional
    public void confirmBooking(String seatNumber, String userId) {

        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Safety check (Optimistic Locking already handles concurrency)
        if (SeatStatus.BOOKED.equals(seat.getStatus())) {
            System.out.println("Seat already booked, skipping: " + seatNumber);
            return;
           // throw new RuntimeException("Seat already booked!");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setLockedBy(userId);
        seat.setLockTimestamp(null); // clear old lock time

        seatRepository.save(seat);

        System.out.println(" Seat " + seatNumber + " confirmed for " + userId);
    } */

    /* @Transactional
    public void confirmBooking(List<String> seatNumbers, String userId) {

        List<Seat> seats = seatRepository.findBySeatNumberIn(seatNumbers);

        // Validate first (atomic rule)
        for (Seat seat : seats) {
            if (SeatStatus.BOOKED.equals(seat.getStatus())) {
                throw new RuntimeException("One or more seats already booked!");
            }
        }

        // Update all
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedBy(userId);
            seat.setLockTimestamp(null);
        }

        seatRepository.saveAll(seats);

        System.out.println("All seats confirmed for " + userId);
    } */

    @Transactional
    public void confirmBooking(List<String> seatNumbers, String userId) {
        List<Seat> seats = seatRepository.findBySeatNumberIn(seatNumbers);

        for (Seat seat : seats) {
            if (SeatStatus.BOOKED.equals(seat.getStatus())) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is already booked!");
            }
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedBy(userId);
            seat.setLockTimestamp(null);
        }
        seatRepository.saveAll(seats);
        System.out.println("seats(s) : " + seatNumbers +  "confirmed for " + userId);
    }

    // 3️ Cancel booking (Direct DB update)
    @Transactional
    public Seat cancelBooking(String seatNumber) {

        Seat seat = seatRepository.findBySeatNumber(seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (!SeatStatus.BOOKED.equals(seat.getStatus())) {
            throw new RuntimeException("Only BOOKED seats can be cancelled.");
        }

        System.out.println("Initiating refund for seat " + seat.getSeatNumber());

        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setLockedBy(null);
        seat.setLockTimestamp(null);

        return seatRepository.save(seat);
    }
}
