package com.seatbooking.seatbookingservice.service;

import com.seatbooking.seatbookingservice.model.Seat;
import com.seatbooking.seatbookingservice.model.SeatStatus;
import com.seatbooking.seatbookingservice.repository.SeatRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    @Getter
    @Value("${seatbooking.lock.duration-minutes}")
    private long lockDuration;

    public SeatLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean lockSeat(String seatNumber, String userId) {

        String key = "seat_lock:" + seatNumber;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId, lockDuration, TimeUnit.MINUTES);

        return success != null && success;
    }

    public boolean reserveMultipleSeats(List<String> seatNumbers, String userId) {

        List<String> successfullyLocked = new ArrayList<>();

        for (String seatNumber : seatNumbers) {

            boolean locked = lockSeat(seatNumber, userId);

            if (!locked) {
                for (String lockedSeat : successfullyLocked) {
                    unlockSeat(lockedSeat);
                }
                return false;
            }

            successfullyLocked.add(seatNumber);
        }

        return true;
    }

    public void unlockSeat(String seatId) {
        redisTemplate.delete("seat_lock:" + seatId);
    }

    public String getLockOwner(String seatId) {
        return redisTemplate.opsForValue().get("seat_lock:" + seatId);
    }
}