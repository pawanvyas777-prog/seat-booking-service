package com.seatbooking.seatbookingservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "seats")
@Data // This automatically generates Getters, Setters, and toString()
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;
    private String category; // e.g., Platinum, Gold, Silver

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;
    private Double price;

    @Version
    private Integer version;

    private Instant lockTimestamp;

    // The Setter - To save the user ID when locking
    private String lockedBy;

}

