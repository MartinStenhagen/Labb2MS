package com.example.payment;

public class PaymentProcessor {
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final NotificationClient notificationClient;
    private static final String STATUS_SUCCESS = "SUCCESS";

    public PaymentProcessor(PaymentGateway paymentGateway,
                              PaymentRepository paymentRepository,
                              NotificationClient notificationClient) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.notificationClient = notificationClient;
    }

    public boolean processPayment(double amount, String recipient) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient must not be blank");
        }
        PaymentApiResponse response = paymentGateway.charge(amount);

        if (response.isSuccess()) {
            paymentRepository.savePayment(amount, STATUS_SUCCESS);
            notificationClient.sendPaymentConfirmation(recipient, amount);
        }

        return response.isSuccess();
    }
}

