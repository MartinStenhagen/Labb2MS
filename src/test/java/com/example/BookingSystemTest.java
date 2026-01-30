package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
    private Room mockRoom;

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

        // Configure mocks
        when(timeProvider.getCurrentTime()).thenReturn(now);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(mockRoom.isAvailable(startTime, endTime)).thenReturn(true);

        // Act
        boolean result = bookingSystem.bookRoom(roomId, startTime, endTime);

        // Assert
        assertThat(result).isTrue();

        // Verify interactions
        verify(roomRepository).findById(roomId);
        verify(mockRoom).addBooking(any(Booking.class));
        verify(roomRepository).save(mockRoom);
        verify(notificationService).sendBookingConfirmation(any(Booking.class));
    }

    @Test
    @DisplayName("Should return false when room is not available")
    void bookRoom_shouldReturnFalse_whenRoomIsNotAvailable() throws NotificationException {
        // Arrange
        String roomId = "room2";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(2);

        when(timeProvider.getCurrentTime()).thenReturn(now);
        // The room exists
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        // But it is not available
        when(mockRoom.isAvailable(startTime, endTime)).thenReturn(false);

        // Act
        boolean result = bookingSystem.bookRoom(roomId, startTime, endTime);

        // Assert
        assertThat(result).isFalse();

        // Verify that no booking was made
        verify(mockRoom, never()).addBooking(any());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    @Test
    @DisplayName("Should throw exception when booking in the past")
    void bookRoom_shouldThrowException_whenDateIsInThePast() {
        // Arrange
        String roomId = "room3";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTimeInThePast = now.minusHours(1);
        LocalDateTime endTime = now.plusHours(1);

        when(timeProvider.getCurrentTime()).thenReturn(now);

        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.bookRoom(roomId, startTimeInThePast, endTime);
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Kan inte boka tid i dåtid");

        // Verify that no booking was made because the validation failed first
        verify(roomRepository, never()).findById(anyString());
        verify(mockRoom, never()).addBooking(any());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw an exception when endtime is before starttime")
    void bookRoom_shouldThrowException_whenEndTimeIsBeforeStartTime() {
        String roomId = "room4";

        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTime = now.plusDays(4);
        LocalDateTime endTime = now.minusHours(2);

        when(timeProvider.getCurrentTime()).thenReturn(now);

        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.bookRoom(roomId, startTime, endTime);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sluttid måste vara efter starttid");

        // Verify that no booking was made because the validation failed first
        verify(roomRepository, never()).findById(anyString());
        verify(mockRoom, never()).addBooking(any());
        verify(roomRepository, never()).save(any());
    }
}
