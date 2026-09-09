package tests.report;

import org.testng.ITestResult;

import java.util.Map;

/**
 * Maps each test method to the business story the report should tell.
 * Titles and results describe verified behaviour, not raw API traffic.
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
            Map.entry("addSizeCustomizationUpdatesNameAndTotal", new TestInfo(
                    "Size Customization",
                    "Validates that a size option updates the product name and cart total.",
                    "Customization",
                    "Size customization applied and total validated."
            )),
            Map.entry("addAddonCustomizationIncreasesCartTotal", new TestInfo(
                    "Addon Customization",
                    "Validates that an add-on increases the cart total.",
                    "Customization",
                    "Add-on applied and total validated."
            )),
            Map.entry("singleItemCartUsesItem1Total", new TestInfo(
                    "Single Item Cart",
                    "Validates cart totals when only the first baseline product is added.",
                    "Cart",
                    "Single-item cart total validated."
            )),
            Map.entry("increasingItem1QuantityIncreasesTotal", new TestInfo(
                    "Increase Item Quantity",
                    "Validates that raising quantity increases the cart total.",
                    "Cart",
                    "Quantity change increased the cart total."
            )),
            Map.entry("baselineItemsPlusSizeCustomization", new TestInfo(
                    "Cart With Customization",
                    "Validates baseline products plus a size customization in one cart.",
                    "Cart",
                    "Combined cart totals validated."
            )),
            Map.entry("placeOrderWithCustomerDetails", new TestInfo(
                    "Customer Details",
                    "Validates that guest name and mobile variants can start payment without changing item totals.",
                    "Customer",
                    "Customer details accepted and payment session created."
            )),
            Map.entry("shortMobileIsRejected", new TestInfo(
                    "Invalid Mobile Number",
                    "Validates that a mobile number below the accepted length is rejected.",
                    "Negative",
                    "Short mobile number was rejected."
            )),
            Map.entry("itemInstructionIsStoredAsNotes", new TestInfo(
                    "Item Instruction",
                    "Validates that an item instruction is stored as notes without changing totals.",
                    "Instructions",
                    "Item instruction stored as notes."
            )),
            Map.entry("orderLevelCookingDetailsCanInitiatePayment", new TestInfo(
                    "Order Cooking Details",
                    "Validates that order-level cooking details are accepted when payment is initiated.",
                    "Instructions",
                    "Cooking details accepted and payment session created."
            )),
            Map.entry("menuCartAndInitiatePaymentStayAligned", new TestInfo(
                    "Menu, Cart And Payment",
                    "Validates that menu, cart totals, and Telr payment initiation stay aligned.",
                    "Regression",
                    "Menu, cart, and payment initiation stayed aligned."
            )),
            Map.entry("verifyQrMenuCartAndInitiatePayment", new TestInfo(
                    "Complete Payment & Create Order",
                    "Validates the dine-in flow from menu and cart through Telr payment and order acceptance.",
                    "E2E Order",
                    "Payment completed successfully and order was accepted."
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
