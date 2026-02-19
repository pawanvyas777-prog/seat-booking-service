package com.seatbooking.seatbookingservice.config;

import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import com.seatbooking.seatbookingservice.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeatMapGenerator {

    @Bean
    CommandLineRunner setupTheaterLayout(SeatRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                System.out.println("--- Building the Theater Rows ---");

                // Outer loop for Rows (A to K)
                for (char row = 'A'; row <= 'K'; row++) {

                    String category;
                    Double price;

                    // Grouping rows into categories
                    if (row >= 'I') { // I, J, K
                        category = "Platinum";
                        price = 1000.0;
                    } else if (row >= 'F') { // F, G, H
                        category = "Gold";
                        price = 700.0;
                    } else { // A, B, C, D, E
                        category = "Silver";
                        price = 400.0;
                    }

                    // Inner loop for Seats in each row (1 to 10)
                    for (int num = 1; num <= 10; num++) {
                        Seat seat = new Seat();
                        seat.setSeatNumber(row + String.valueOf(num)); // e.g., "A1", "B5"
                        seat.setCategory(category);
                        seat.setPrice(price);
                        seat.setStatus(SeatStatus.AVAILABLE);
                        repository.save(seat);
                    }
                }
                System.out.println("--- Theater Ready: Rows A-K Created! ---");
            }
        };
    }
}