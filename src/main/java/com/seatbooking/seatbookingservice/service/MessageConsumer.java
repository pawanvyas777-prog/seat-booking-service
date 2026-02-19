package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.dto.BookingMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    private final SeatService seatService;
    private final SeatLockService seatLockService;

    public MessageConsumer(SeatService seatService,SeatLockService seatLockService) {
        this.seatService = seatService;
        this.seatLockService = seatLockService;
    }


    @RabbitListener(queues = "payment_success_queue")
    public void receiveMessage(BookingMessage message) {
        System.out.println("Consumer received DTO: " + message);

        try {
            // Pass the entire List<String> directly to the Service
            // This makes the DB update atomic (all or nothing)
            seatService.confirmBooking(message.getSeatNumbers(), message.getUserId());

            // After DB success, clear Redis locks for these seats
            for (String seat : message.getSeatNumbers()) {
                seatLockService.unlockSeat(seat);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error: Payment succeeded but DB update failed
        }
    }
}
