package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void bookRoom_shouldSucceed_whenRoomIsAvailable() throws NotificationException { // Removed throws NotificationException
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
    void bookRoom_shouldReturnFalse_whenRoomIsNotAvailable() throws NotificationException { // Removed throws NotificationException
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

    @ParameterizedTest(name = "Should throw exception when {0} is null")
    @MethodSource("nullArgumentsForBookRoom")
    void bookRoom_shouldThrowException_whenArgumentsAreNull(String testName, String roomId, LocalDateTime startTime, LocalDateTime endTime) throws NotificationException { // Removed throws NotificationException
        // Arrange - No specific timeProvider stubbing needed as the null check occurs before timeProvider is accessed

        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.bookRoom(roomId, startTime, endTime);
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bokning kräver giltiga start- och sluttider samt rum-id");

        // Verify that no booking was made because the validation failed first
        verify(roomRepository, never()).findById(anyString());
        verify(mockRoom, never()).addBooking(any());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    private static Stream<Arguments> nullArgumentsForBookRoom() {
        LocalDateTime futureTime1 = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime futureTime2 = LocalDateTime.of(2026, 2, 1, 11, 0);

        return Stream.of(
            Arguments.of("Room ID", null, futureTime1, futureTime2),
            Arguments.of("Start Time", "roomID_test", null, futureTime2),
            Arguments.of("End Time", "roomID_test", futureTime1, null)
        );
    }

    @Test
    @DisplayName("Should throw exception when room is not found")
    void bookRoom_shouldThrowException_whenRoomNotFound() throws NotificationException { // Removed throws NotificationException
        // Arrange
        String roomId = "nonExistentRoom";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(2);

        when(timeProvider.getCurrentTime()).thenReturn(now);
        // Configure roomRepository to return an empty Optional, simulating room not found
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.bookRoom(roomId, startTime, endTime);
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Rummet existerar inte");

        // Verify that findById was called, but no further interactions occurred
        verify(roomRepository).findById(roomId);
        verify(mockRoom, never()).isAvailable(any(), any()); // Should not reach this
        verify(mockRoom, never()).addBooking(any());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    @Test
    @DisplayName("Should still succeed when notification service fails")
    void bookRoom_shouldStillSucceed_whenNotificationFails() throws NotificationException {
        // Arrange
        String roomId = "roomNotifFail";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime startTime = now.plusHours(1);
        LocalDateTime endTime = now.plusHours(2);

        when(timeProvider.getCurrentTime()).thenReturn(now);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(mockRoom.isAvailable(startTime, endTime)).thenReturn(true);
        // Configure notificationService to throw an exception
        doThrow(new NotificationException("Notification failed")).when(notificationService).sendBookingConfirmation(any(Booking.class));

        // Act
        boolean result = bookingSystem.bookRoom(roomId, startTime, endTime);

        // Assert
        assertThat(result).isTrue(); // Booking should still be true

        // Verify that all steps leading to successful booking were called
        verify(roomRepository).findById(roomId);
        verify(mockRoom).isAvailable(startTime, endTime);
        verify(mockRoom).addBooking(any(Booking.class));
        verify(roomRepository).save(mockRoom);
        verify(notificationService).sendBookingConfirmation(any(Booking.class)); // Notification was attempted
    }

    @Test
    @DisplayName("Should return only rooms that are available for the given time slot")
    void getAvailableRooms_shouldReturnOnlyAvailableRooms() {
        // Arrange
        LocalDateTime queryStartTime = LocalDateTime.of(2026, 2, 1, 9, 0);
        LocalDateTime queryEndTime = LocalDateTime.of(2026, 2, 1, 10, 0);

        // Create mock rooms and stub their isAvailable method
        Room availableRoom1 = mock(Room.class);
        when(availableRoom1.isAvailable(queryStartTime, queryEndTime)).thenReturn(true);

        Room unavailableRoom = mock(Room.class);
        when(unavailableRoom.isAvailable(queryStartTime, queryEndTime)).thenReturn(false);

        Room availableRoom2 = mock(Room.class);
        when(availableRoom2.isAvailable(queryStartTime, queryEndTime)).thenReturn(true);

        // Configure roomRepository to return all mock rooms
        when(roomRepository.findAll()).thenReturn(java.util.Arrays.asList(availableRoom1, unavailableRoom, availableRoom2));

        // Act
        java.util.List<Room> result = bookingSystem.getAvailableRooms(queryStartTime, queryEndTime);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(availableRoom1, availableRoom2);

        // Verify interactions
        verify(roomRepository).findAll();
        verify(availableRoom1).isAvailable(queryStartTime, queryEndTime);
        verify(unavailableRoom).isAvailable(queryStartTime, queryEndTime);
        verify(availableRoom2).isAvailable(queryStartTime, queryEndTime);
    }

    @ParameterizedTest(name = "getAvailableRooms: should throw exception for {0}")
    @MethodSource("invalidTimeRangesForGetAvailableRooms")
    void getAvailableRooms_shouldThrowException_forInvalidTimeRanges(String testName, LocalDateTime startTime, LocalDateTime endTime, String expectedMessage) {
        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.getAvailableRooms(startTime, endTime);
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);

        // Verify no repository interaction
        verify(roomRepository, never()).findAll();
    }

    private static Stream<Arguments> invalidTimeRangesForGetAvailableRooms() {
        LocalDateTime time1 = LocalDateTime.of(2026, 2, 1, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2026, 2, 1, 11, 0);

        return Stream.of(
            Arguments.of("null start time", null, time2, "Måste ange både start- och sluttid"),
            Arguments.of("null end time", time1, null, "Måste ange både start- och sluttid"),
            Arguments.of("end time before start time", time2, time1, "Sluttid måste vara efter starttid")
        );
    }

    @Test
    @DisplayName("Should successfully cancel a booking that exists and is in the future")
    void cancelBooking_shouldSucceed_whenBookingExistsAndIsInTheFuture() throws NotificationException {
        // Arrange
        String bookingId = "booking123";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime bookingStartTime = now.plusDays(1); // Booking is in the future

        // Mock a Booking object
        Booking mockBooking = mock(Booking.class);
        when(mockBooking.getStartTime()).thenReturn(bookingStartTime);

        // Configure mockRoom to have this booking
        when(mockRoom.hasBooking(bookingId)).thenReturn(true);
        when(mockRoom.getBooking(bookingId)).thenReturn(mockBooking);
        // Configure roomRepository to return this room
        when(roomRepository.findAll()).thenReturn(java.util.Arrays.asList(mockRoom));

        // Configure timeProvider
        when(timeProvider.getCurrentTime()).thenReturn(now);

        // Act
        boolean result = bookingSystem.cancelBooking(bookingId);

        // Assert
        assertThat(result).isTrue();

        // Verify interactions
        verify(roomRepository).findAll();
        verify(mockRoom).hasBooking(bookingId);
        verify(mockRoom).getBooking(bookingId);
        verify(mockBooking).getStartTime();
        verify(timeProvider).getCurrentTime();
        verify(mockRoom).removeBooking(bookingId);
        verify(roomRepository).save(mockRoom);
        verify(notificationService).sendCancellationConfirmation(mockBooking);
    }

    @Test
    @DisplayName("Should return false when trying to cancel a booking that does not exist")
    void cancelBooking_shouldReturnFalse_whenBookingNotFound() throws NotificationException {
        // Arrange
        String nonExistentBookingId = "nonExistentBooking";
        Room anotherMockRoom = mock(Room.class); // Another room to ensure filtering works

        // Configure mockRoom to NOT have the booking
        when(mockRoom.hasBooking(nonExistentBookingId)).thenReturn(false);
        // Configure anotherMockRoom to NOT have the booking either
        when(anotherMockRoom.hasBooking(nonExistentBookingId)).thenReturn(false);

        // Configure roomRepository to return a list of rooms, none of which have the booking
        when(roomRepository.findAll()).thenReturn(java.util.Arrays.asList(mockRoom, anotherMockRoom));

        // Act
        boolean result = bookingSystem.cancelBooking(nonExistentBookingId);

        // Assert
        assertThat(result).isFalse();

        // Verify interactions
        verify(roomRepository).findAll();
        verify(mockRoom).hasBooking(nonExistentBookingId);
        verify(anotherMockRoom).hasBooking(nonExistentBookingId); // Ensure all rooms were checked
        verify(mockRoom, never()).getBooking(anyString());
        verify(anotherMockRoom, never()).getBooking(anyString());
        verify(mockRoom, never()).removeBooking(anyString());
        verify(anotherMockRoom, never()).removeBooking(anyString());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendCancellationConfirmation(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel a booking that has already started")
    void cancelBooking_shouldThrowException_whenBookingIsInThePast() throws NotificationException {
        // Arrange
        String bookingId = "pastBooking123";
        LocalDateTime now = LocalDateTime.of(2026, 1, 30, 10, 0);
        LocalDateTime bookingStartTimeInPast = now.minusHours(1); // Booking started an hour ago

        Booking mockBooking = mock(Booking.class);
        when(mockBooking.getStartTime()).thenReturn(bookingStartTimeInPast);

        when(mockRoom.hasBooking(bookingId)).thenReturn(true);
        when(mockRoom.getBooking(bookingId)).thenReturn(mockBooking);
        when(roomRepository.findAll()).thenReturn(java.util.Arrays.asList(mockRoom));
        when(timeProvider.getCurrentTime()).thenReturn(now);

        // Act & Assert
        assertThatThrownBy(() -> {
            bookingSystem.cancelBooking(bookingId);
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Kan inte avboka påbörjad eller avslutad bokning");

        // Verify that cancellation actions were not taken
        verify(mockRoom, never()).removeBooking(anyString());
        verify(roomRepository, never()).save(any());
        verify(notificationService, never()).sendCancellationConfirmation(any());
    }
}
