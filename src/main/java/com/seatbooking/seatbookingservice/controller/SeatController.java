package com.seatbooking.seatbookingservice.controller;

import com.seatbooking.seatbookingservice.dto.BookingMessage;
import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.service.MessageProducer;
import com.seatbooking.seatbookingservice.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;
    private final MessageProducer producer;

    public SeatController(SeatService seatService,
                          MessageProducer producer) {
        this.seatService = seatService;
        this.producer = producer;
    }

    // 1 Get All Seats

    @GetMapping
    public ResponseEntity<List<Seat>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }
    // 2️ Reserve single Seat
    @PostMapping("/reserve")
    public ResponseEntity<String> reserveSeat(
            @RequestParam String seatNumber,
            @RequestParam String userId) {

        seatService.reserveSeat(seatNumber, userId);

        long duration = seatService.getLockDuration();

        return ResponseEntity.ok(
                "Seat " + seatNumber +
                        " reserved successfully. Complete payment within " +duration + " minute(s). ");
    }

    @PostMapping("/reserve-multiple")
    public ResponseEntity<String> reserveMultipleSeats(
            @RequestParam List<String> seatNumbers,
            @RequestParam String userId) {

        seatService.reserveMultipleSeats(seatNumbers, userId);
        long duration = seatService.getLockDuration();

        return ResponseEntity.ok(
                "Seats " + seatNumbers +
                        " reserved successfully. Complete payment within " +duration + " minute(s). ");
    }
    // 3️ Confirm Single Seat
    @PostMapping("/confirm-single")
    public ResponseEntity<String> confirmSingleSeat(
            @RequestParam String seatNumber,
            @RequestParam String userId) {

        // Validate before sending payment
        seatService.validateBeforeConfirm(seatNumber, userId);

        BookingMessage message =
                new BookingMessage(List.of(seatNumber), userId);

        producer.sendPaymentMessage(message);

        return ResponseEntity.ok(
                "Payment processing started for seat: " + seatNumber);
    }

    // 4. Confirm multiple seats
    @PostMapping("/confirm-multiple")
    public ResponseEntity<String> confirmMultipleSeats(
            @RequestParam List<String> seatNumbers,
            @RequestParam String userId) {

        seatService.validateBeforeConfirmMultiple(seatNumbers, userId);

        BookingMessage message =
                new BookingMessage(seatNumbers, userId);

        producer.sendPaymentMessage(message);

        return ResponseEntity.ok(
                "Payment processing started for seat: " + seatNumbers);
    }

    // 4️ Cancel Booking
    // =========================
    @PostMapping("/cancel")
    public ResponseEntity<Seat> cancelBooking(
            @RequestParam String seatNumber) {

        return ResponseEntity.ok(
                seatService.cancelBooking(seatNumber));
    }
}