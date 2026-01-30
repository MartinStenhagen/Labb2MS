package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSystemTest {

    @Mock
    private TimeProvider timeProvider;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private NotificationService notificationService;

    private BookingSystem bookingSystem;

    @BeforeEach
    void setUp() {
        bookingSystem = new BookingSystem(timeProvider, roomRepository, notificationService);
    }

    @Test
    @DisplayName("Should successfully book a room when it is available")
    void bookRoom_shouldSucceed_whenRoomIsAvailable() throws NotificationException {
        // Arrange
        String roomId = "room1";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(2);

        // Assuming Room has a constructor like Room(String id, String name, double price)
        Room mockRoom = new Room(roomId, "Test Room");

        // Configure mocks
        when(timeProvider.getCurrentTime()).thenReturn(now);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(mockRoom.isAvailable(startTime, endTime)).thenReturn(true);

        // Act
        boolean result = bookingSystem.bookRoom(roomId, startTime, endTime);

        // Assert
        assertThat(result).isTrue();

        // Verify interactions with mocks
        verify(roomRepository).findById(roomId);
        verify(mockRoom).isAvailable(startTime, endTime);
        // Verify that addBooking was called on the mockRoom with any Booking instance
        verify(mockRoom).addBooking(any(Booking.class));
        verify(roomRepository).save(mockRoom);
        verify(notificationService).sendBookingConfirmation(any(Booking.class));
    }
}
