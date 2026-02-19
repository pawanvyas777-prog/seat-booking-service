package com.seatbooking.seatbookingservice.model;

public enum SeatStatus {
    AVAILABLE,
    LOCKED,   // This is for the 5-minute payment window
    BOOKED
}
