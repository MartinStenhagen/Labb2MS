# Refactoring `PaymentProcessor` for Testability

### **1. Summary**

The primary goal of this refactoring was to decouple the `PaymentProcessor` class from its external dependencies to make it fully testable in isolation. The original implementation had hard-coded dependencies on a payment API, a database connection, and an email service, which made it impossible to unit test without triggering real-world side effects (network calls, database writes, sending emails).

The strategy employed was **Dependency Inversion** and **Dependency Injection**.

*   **Dependency Inversion:** We made `PaymentProcessor` depend on abstractions (interfaces) rather than concrete implementations.
*   **Dependency Injection:** We provided these abstractions to `PaymentProcessor` through its constructor.

### **2. Analysis of the "Before" State**

Here is the original `PaymentProcessor` code:

```java
// Original PaymentProcessor.java
package com.example.payment;

public class PaymentProcessor {
    private static final String API_KEY = "sk_test_123456";

    public boolean processPayment(double amount) {
        // Anropar extern betaltjänst direkt
        PaymentApiResponse response = PaymentApi.charge(API_KEY, amount);

        // Skriver till databas direkt
        if (response.isSuccess()) {
            DatabaseConnection.getInstance()
                    .executeUpdate("INSERT INTO payments...");
        }

        // Skickar e-post direkt
        if (response.isSuccess()) {
            EmailService.sendPaymentConfirmation("user@example.com", amount);
        }

        return response.isSuccess();
    }
}
```

**Identified Problems:**

*   **Static `PaymentApi.charge` call:** This is a direct, static call to an external payment service. In a unit test, this would trigger a real network request, making the test slow, unreliable, and dependent on external factors. It also makes it impossible to simulate failure scenarios.
*   **`DatabaseConnection.getInstance()` Singleton:** This is a direct call to a database. Unit tests should never rely on a live database, as it makes them fragile and difficult to set up. We cannot verify *what* was saved without querying the database.
*   **Static `EmailService.sendPaymentConfirmation` call:** This is a direct call to an email service, which would attempt to send a real email during a test run. This prevents us from verifying that the method was called with the correct recipient and amount.

### **3. Step-by-Step Refactoring Decisions**

To solve these problems, a layer of abstraction for each dependency was introduced.

#### **Decision 1: Abstract the Payment Gateway**

*   **Problem:** The hard-coded, static call to `PaymentApi.charge`.
*   **Solution:**  Created a `PaymentGateway` interface to represent the action of charging a payment.
    ```java
    // New Interface: PaymentGateway.java
    public interface PaymentGateway {
        PaymentApiResponse charge(double amount);
    }
    ```
*   **Justification:** `PaymentProcessor` no longer needs to know *how* a payment is processed. This allows us to provide a mock `PaymentGateway` in the tests to simulate both successful and failed payments instantly, without any network calls.

#### **Decision 2: Abstract the Database Persistence**

*   **Problem:** The direct call to the `DatabaseConnection` singleton.
*   **Solution:** Created a `PaymentRepository` interface to represent the action of saving a payment record.
    ```java
    // New Interface: PaymentRepository.java
    public interface PaymentRepository {
        void savePayment(double amount, String status);
    }
    ```
*   **Justification:** This isolates `PaymentProcessor` from database concerns. Its responsibility is now simply to delegate the saving action. In tests, we can provide a mock `PaymentRepository` and verify that `savePayment()` was called with the correct arguments.

#### **Decision 3: Abstract the Notification Service**

*   **Problem:** The static call to `EmailService.sendPaymentConfirmation`.
*   **Solution:** A `NotificationClient` interface was created to represent the action of sending a notification.
    ```java
    // New Interface: NotificationClient.java
    public interface NotificationClient {
        void sendPaymentConfirmation(String recipient, double amount);
    }
    ```
*   **Justification:** This decouples `PaymentProcessor` from the specific method of notification. In  the tests, we can use a mock `NotificationClient` to verify that a notification was attempted with the correct recipient and amount, without sending any actual emails.

#### **4. The "After" State: A Testable `PaymentProcessor`**

By applying these decisions, the refactored `PaymentProcessor` now looks like this:

```java
// Refactored PaymentProcessor.java
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
```
In addition to dependency injection, the refactored implementation now includes input validation to enforce a clear method contract. Invalid input is rejected before any interaction with external dependencies occurs.

#### **5. Testing Strategy**

The refactored PaymentProcessor was tested in isolation using mocked dependencies.

The tests cover:

Successful payment flow: Verifies that the payment is charged, persisted, and a notification is sent.

Failed payment flow: Verifies that no side effects occur when the payment fails.

Input validation edge cases: Ensures that invalid amounts and recipients are rejected with clear exceptions.

By mocking all external dependencies, the tests are fast, deterministic, and focused solely on the behavior of PaymentProcessor.

#### **Conclusion:**

The `PaymentProcessor` is now a simple orchestrator. It coordinates calls to the dependencies it is given but contains no complex logic or external dependencies itself. This makes it extremely easy to unit test, as we proved by writing tests that could fully control and verify its behavior using mocks.
