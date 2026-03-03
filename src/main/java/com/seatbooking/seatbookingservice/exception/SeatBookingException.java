package com.seatbooking.seatbookingservice.exception;

import org.springframework.http.HttpStatus;

public class SeatBookingException extends RuntimeException {

    private final HttpStatus status;

    public SeatBookingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}