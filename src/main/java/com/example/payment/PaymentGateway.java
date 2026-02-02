package com.example.payment;

public interface PaymentGateway {
    PaymentApiResponse charge(double amount);
}
