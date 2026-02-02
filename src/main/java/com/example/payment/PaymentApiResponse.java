package com.example.payment;

// A simple response object to represent the result from a payment API.
public class PaymentApiResponse {
    private final boolean success;

    public PaymentApiResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
