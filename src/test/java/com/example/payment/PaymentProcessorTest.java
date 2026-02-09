package com.example.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationClient notificationClient;

    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        paymentProcessor = new PaymentProcessor(paymentGateway, paymentRepository, notificationClient);
    }

    @Test
    @DisplayName("Should return true and perform actions when payment is successful")
    void processPayment_shouldReturnTrue_whenPaymentIsSuccessful() {
        // Arrange
        double amount = 100.0;
        String recipient = "test@example.com";
        PaymentApiResponse successfulResponse = new PaymentApiResponse(true);

        when(paymentGateway.charge(amount)).thenReturn(successfulResponse);

        // Act
        boolean result = paymentProcessor.processPayment(amount, recipient);

        // Assert
        assertThat(result).isTrue();

        // Verify
        verify(paymentGateway).charge(amount);
        verify(paymentRepository).savePayment(amount, "SUCCESS");
        verify(notificationClient).sendPaymentConfirmation(recipient, amount);

        verifyNoMoreInteractions(paymentGateway, paymentRepository, notificationClient);
    }

    @Test
    @DisplayName("Should return false and not perform actions when payment fails")
    void processPayment_shouldReturnFalse_whenPaymentFails() {
        // Arrange
        double amount = 150.0;
        String recipient = "fail@example.com";
        PaymentApiResponse failedResponse = new PaymentApiResponse(false);

        when(paymentGateway.charge(amount)).thenReturn(failedResponse);

        // Act
        boolean result = paymentProcessor.processPayment(amount, recipient);

        // Assert
        assertThat(result).isFalse();

        // Verify
        verify(paymentGateway).charge(amount);
        verifyNoInteractions(paymentRepository, notificationClient);
    }

    @Test
    @DisplayName("processPayment: amount <= 0 should throw IllegalArgumentException")
    void processPayment_whenAmountIsZeroOrNegative_shouldThrowException() {
        assertThatThrownBy(() ->
                paymentProcessor.processPayment(0, "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                paymentProcessor.processPayment(-10, "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}