package com.example.payment;

public interface NotificationClient {
    void sendPaymentConfirmation(String recipient, double amount);
}
