package tests.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts completed checkout outcomes from real CheckoutFlow calls.
 */
public final class CheckoutStats {

    private static final AtomicInteger PAYMENTS_SUCCESS = new AtomicInteger();
    private static final AtomicInteger PAYMENTS_FAILED = new AtomicInteger();
    private static final AtomicInteger ORDERS_CREATED = new AtomicInteger();
    private static final AtomicInteger CUSTOMIZATION_ORDERS = new AtomicInteger();
    private static final AtomicInteger INSTRUCTION_ORDERS = new AtomicInteger();
    private static final AtomicInteger COOKING_ORDERS = new AtomicInteger();

    private CheckoutStats() {
    }

    public static void reset() {
        PAYMENTS_SUCCESS.set(0);
        PAYMENTS_FAILED.set(0);
        ORDERS_CREATED.set(0);
        CUSTOMIZATION_ORDERS.set(0);
        INSTRUCTION_ORDERS.set(0);
        COOKING_ORDERS.set(0);
    }

    public static void paymentSuccess() {
        PAYMENTS_SUCCESS.incrementAndGet();
    }

    public static void paymentFailed() {
        PAYMENTS_FAILED.incrementAndGet();
    }

    public static void orderCreated() {
        ORDERS_CREATED.incrementAndGet();
    }

    public static void customizationOrder() {
        CUSTOMIZATION_ORDERS.incrementAndGet();
    }

    public static void instructionOrder() {
        INSTRUCTION_ORDERS.incrementAndGet();
    }

    public static void cookingOrder() {
        COOKING_ORDERS.incrementAndGet();
    }

    public static int paymentsSuccess() {
        return PAYMENTS_SUCCESS.get();
    }

    public static int paymentsFailed() {
        return PAYMENTS_FAILED.get();
    }

    public static int ordersCreated() {
        return ORDERS_CREATED.get();
    }

    public static int customizationOrders() {
        return CUSTOMIZATION_ORDERS.get();
    }

    public static int instructionOrders() {
        return INSTRUCTION_ORDERS.get();
    }

    public static int cookingOrders() {
        return COOKING_ORDERS.get();
    }
}
