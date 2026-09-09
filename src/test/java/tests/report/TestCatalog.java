package tests.report;

import org.testng.ITestResult;

import java.util.Map;

/**
 * Maps each test method to the business story the report should tell.
 */
public final class TestCatalog {

    private static final TestInfo FALLBACK = new TestInfo(
            "API Test",
            "Validates an API business flow.",
            "API",
            "Business flow completed."
    );

    private static final Map<String, TestInfo> TESTS = Map.ofEntries(
            Map.entry("qrMenuReturnsSessionToken", new TestInfo(
                    "QR Menu Session",
                    "Validates that a dine-in QR menu session can be opened.",
                    "QR Menu",
                    "QR menu session opened successfully."
            )),
            Map.entry("baselineCartReturnsExpectedTotal", new TestInfo(
                    "Create Baseline Cart",
                    "Validates that the baseline two-item cart totals 20.00.",
                    "Cart",
                    "Baseline cart total validated."
            )),
            Map.entry("initiatePaymentReturnsTelrUrl", new TestInfo(
                    "Initiate Telr Payment",
                    "Validates that payment can be initiated with Telr as the provider.",
                    "Payment",
                    "Telr payment session created."
            )),
            Map.entry("qrMenuReturnsExpectedAccount", new TestInfo(
                    "Account Validation",
                    "Validates that the QR code belongs to the expected account and channel.",
                    "Account",
                    "Account and channel validated."
            )),
            Map.entry("baselineCartHasBothItemsAndPendingTelr", new TestInfo(
                    "Create Cart",
                    "Validates that both baseline products are added and the cart waits for Telr payment.",
                    "Cart",
                    "Cart created and pending Telr payment."
            )),
            Map.entry("menuWithOptionsIncludesCustomizableItem", new TestInfo(
                    "Menu Customizable Item",
                    "Validates that the menu exposes the expected customizable product.",
                    "Catalog",
                    "Customizable product is available on the menu."
            )),
            Map.entry("invalidQrCodeIsRejected", new TestInfo(
                    "Invalid QR Code",
                    "Validates that an unknown QR code is rejected.",
                    "Negative",
                    "Invalid QR code was rejected."
            )),
            Map.entry("unknownItemCannotBeAddedToCart", new TestInfo(
                    "Invalid Product ID",
                    "Validates that an unknown product cannot be added to the cart.",
                    "Negative",
                    "Unknown product was rejected."
            )),
            Map.entry("emptyCartIsRejected", new TestInfo(
                    "Empty Cart",
                    "Validates that checkout is blocked when the cart has no items.",
                    "Negative",
                    "Empty cart was rejected."
            )),
            Map.entry("invalidPaymentCtIsRejected", new TestInfo(
                    "Invalid Payment Link",
                    "Validates that payment status cannot be fetched with an invalid return link.",
                    "Negative",
                    "Invalid payment link was rejected."
            )),
            Map.entry("shortMobileIsRejected", new TestInfo(
                    "Invalid Mobile Number",
                    "Validates that a mobile number below the accepted length is rejected.",
                    "Negative",
                    "Short mobile number was rejected."
            )),
            Map.entry("shouldCreateOrderWithSizeCustomization", new TestInfo(
                    "Size Customization → Complete Order",
                    "Validates that a size option is preserved through checkout and final order creation.",
                    "Cart → Customization → Payment → Order",
                    "Customized product successfully ordered."
            )),
            Map.entry("shouldCreateOrderWithAddonCustomization", new TestInfo(
                    "Addon Customization → Complete Order",
                    "Validates that an add-on is preserved through checkout and final order creation.",
                    "Cart → Add-on → Payment → Order",
                    "Add-on product successfully ordered."
            )),
            Map.entry("shouldCreateOrderWithSingleItem", new TestInfo(
                    "Single Item → Complete Order",
                    "Validates that a single-item cart can be paid and accepted.",
                    "Cart → Payment → Order",
                    "Single-item order created and accepted."
            )),
            Map.entry("shouldCreateOrderAfterIncreasingQuantity", new TestInfo(
                    "Increase Quantity → Complete Order",
                    "Validates that a quantity change is paid and persisted on the accepted order.",
                    "Cart → Quantity → Payment → Order",
                    "Quantity change was paid and persisted on the order."
            )),
            Map.entry("shouldCreateOrderWithBaselineAndSizeCustomization", new TestInfo(
                    "Cart With Customization → Complete Order",
                    "Validates baseline products plus a size customization through payment and order creation.",
                    "Cart → Customization → Payment → Order",
                    "Combined cart was paid and accepted."
            )),
            Map.entry("shouldCreateOrderWithCustomerDetails", new TestInfo(
                    "Customer Details → Complete Order",
                    "Validates that guest details can complete Telr payment and create an accepted order.",
                    "Customer → Cart → Payment → Order",
                    "Customer details accepted and order was created and accepted."
            )),
            Map.entry("shouldCreateOrderWithItemInstruction", new TestInfo(
                    "Item Instruction → Complete Order",
                    "Validates that an item instruction is preserved through checkout and final order creation.",
                    "Cart → Instruction → Customer → Payment → Order",
                    "Instruction successfully persisted through checkout and order creation."
            )),
            Map.entry("shouldCreateOrderWithCookingDetails", new TestInfo(
                    "Cooking Details → Complete Order",
                    "Validates that cooking details are sent through Telr payment and the order is accepted.",
                    "Cart → Cooking details → Payment → Order",
                    "Cooking details accepted and order was created and accepted."
            )),
            Map.entry("shouldCreateOrderFromMenuCartAndPayment", new TestInfo(
                    "Menu, Cart And Payment → Complete Order",
                    "Validates menu, cart, Telr payment, and an accepted order stay aligned.",
                    "Menu → Cart → Telr Payment → Order",
                    "Payment completed successfully and order was created and accepted."
            )),
            Map.entry("shouldCreateAcceptedOrderAfterTelrPayment", new TestInfo(
                    "Complete Payment & Create Order",
                    "Validates the dine-in journey from menu through Telr payment to an accepted order.",
                    "Menu → Cart → Customer → Telr Payment → Order",
                    "Payment completed successfully and order was created and accepted."
            ))
    );

    private TestCatalog() {
    }

    public static TestInfo info(ITestResult result) {
        String method = result.getMethod().getMethodName();
        TestInfo info = TESTS.getOrDefault(method, FALLBACK);
        Object[] params = result.getParameters();
        if (params != null && params.length > 0 && params[0] != null) {
            String suffix = " [" + params[0] + "]";
            return new TestInfo(info.title() + suffix, info.purpose(), info.flow(), info.passResult());
        }
        return info;
    }
}
