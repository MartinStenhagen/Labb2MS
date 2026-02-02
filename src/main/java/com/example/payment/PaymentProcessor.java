package com.example.payment;

public class PaymentProcessor {
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final NotificationClient notificationClient;

    public PaymentProcessor(PaymentGateway paymentGateway,
                              PaymentRepository paymentRepository,
                              NotificationClient notificationClient) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.notificationClient = notificationClient;
    }

    public boolean processPayment(double amount, String recipient) {
        PaymentApiResponse response = paymentGateway.charge(amount);

        if (response.isSuccess()) {
            paymentRepository.savePayment(amount, "SUCCESS");
            notificationClient.sendPaymentConfirmation(recipient, amount);
        }

        return response.isSuccess();
    }
}

