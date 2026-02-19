package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.dto.BookingMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${seatbooking.rabbitmq.exchange}")
    private String exchange;

    @Value("${seatbooking.rabbitmq.request-routing-key}")
    private String requestRoutingKey;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPaymentMessage(BookingMessage message) {
        System.out.println("Sending booking message for user: " + message.getUserId());
        rabbitTemplate.convertAndSend(exchange, requestRoutingKey, message);
    }
}