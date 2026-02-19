package com.seatbooking.seatbookingservice.controller;

import com.seatbooking.seatbookingservice.dto.BookingMessage;
import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.service.MessageProducer;
import com.seatbooking.seatbookingservice.service.SeatLockService;
import com.seatbooking.seatbookingservice.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;
    private final SeatLockService seatLockService;
    private final MessageProducer producer;

    // Dependency Injection via Constructor
    public SeatController(SeatService seatService, SeatLockService seatLockService,MessageProducer producer) {
        this.seatService = seatService;
        this.seatLockService = seatLockService;
        this.producer = producer;
    }

    // 1. Get all seats (To see your 110 seats in the browser)
    @GetMapping
    public ResponseEntity<List<Seat>> getAllSeats() {

        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveSeat(
            @RequestParam String seatNumber,
            @RequestParam String userId) {

        boolean locked = seatLockService.lockSeat(seatNumber, userId);
        // 1. Get the duration from the Service first
        long duration = seatLockService.getLockDuration();

        if (locked) {
            return ResponseEntity.ok("Seat  " + seatNumber +" has been reserved for you for "+ duration +" minute(s). Please complete your payment.");
        } else {
            return ResponseEntity.badRequest()
                    .body("Seat not reserved because either it is already reserved/Booked or doest not exist in DB");
        }
    }

    @PostMapping("/reserve-multiple")
    public ResponseEntity<String> reserveMultipleSeats(
            @RequestParam List<String> seatNumbers,
            @RequestParam String userId) {

        boolean locked = seatLockService.reserveMultipleSeats(seatNumbers, userId);

        if (locked) {
            return ResponseEntity.ok("All seats "  + seatNumbers +" reserved successfully for " + userId);
        } else {
            return ResponseEntity.badRequest()
                    .body("One or more seats are already reserved. Reservation failed.");
        }
    }

    @PostMapping("/confirm-single")
    public ResponseEntity<String> confirmSingleSeat(
            @RequestParam String seatNumber,
            @RequestParam String userId) {

        // Validate Redis lock for the single seat
        String owner = seatLockService.getLockOwner(seatNumber);
        if (owner == null || !owner.equalsIgnoreCase(userId)) {
            return ResponseEntity.badRequest()
                    .body("You do not own reservation for seat " + seatNumber);
        }

        BookingMessage message = new BookingMessage(List.of(seatNumber), userId);
        producer.sendPaymentMessage(message);

        return ResponseEntity.ok("Payment processing started for seat: " + seatNumber);
    }


    @PostMapping("/confirm-multiple")
    public ResponseEntity<String> confirmMultipleSeats(
            @RequestParam List<String> seatNumbers,
            @RequestParam String userId) {

        // Validate Redis lock for ALL seats in the list
        for (String seatNumber : seatNumbers) {
            String owner = seatLockService.getLockOwner(seatNumber);
            if (owner == null || !owner.equalsIgnoreCase(userId)) {
                return ResponseEntity.badRequest()
                        .body("Reservation expired or invalid for seat: " + seatNumber);
            }
        }

        BookingMessage message = new BookingMessage(seatNumbers, userId);
        producer.sendPaymentMessage(message);

        return ResponseEntity.ok("Payment processing started for seats: " + seatNumbers);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Seat> cancelBooking(@RequestParam String seatNumber) {
        return ResponseEntity.ok(seatService.cancelBooking(seatNumber));
    }

}