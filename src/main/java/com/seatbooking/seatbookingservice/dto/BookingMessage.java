package com.seatbooking.seatbookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingMessage {

    private List<String> seatNumbers;
    private  String userId;

}
