package com.seatbooking.seatbookingservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SeatBookingServiceApplication {

    public static void main(String[] args) {

        // FORCE the JVM to use UTC before anything else happens
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SeatBookingServiceApplication.class, args);
    }

}
