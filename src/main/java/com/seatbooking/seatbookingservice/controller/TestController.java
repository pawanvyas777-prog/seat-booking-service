package com.seatbooking.seatbookingservice.controller;

import com.seatbooking.seatbookingservice.dto.BookingMessage;
import com.seatbooking.seatbookingservice.service.MessageProducer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev")
public class TestController {

    private final MessageProducer producer;

    public TestController(MessageProducer producer) {
        this.producer = producer;
    }

    // Only for RabbitMQ connectivity testing
    @GetMapping("/test-mq")
    public String testRabbitMQ() {

        BookingMessage bookingMessage =
                new BookingMessage(
                        java.util.List.of("TEST_SEAT"),
                        "devUser"
                );

        producer.sendPaymentMessage(bookingMessage);

        return "RabbitMQ test message sent successfully.";
    }
}
