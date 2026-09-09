package config;

import java.util.Properties;

/**
 * Reads static QR-order scenario values for the active environment.
 * Default is testdata.properties (test). Staging is testdata-staging.properties.
 * Dynamic values (session token, payment/order IDs) must come from API responses.
 */
public class TestDataReader {

    private static final String DATA_FILE = ConfigReader.testdataFileName();
    private static final Properties DATA = ConfigReader.load(DATA_FILE);

    public static String getQrCode() {
        return required("qr.code");
    }

    public static int getOrderSource() {
        return Integer.parseInt(required("order.source"));
    }

    public static String getDeliveryType() {
        return required("delivery.type");
    }

    public static String getOrderMode() {
        return required("order.mode");
    }

    public static String getUserName() {
        return required("user.name");
    }

    public static String getUserMobile() {
        return required("user.mobile");
    }

    public static String getUserCountryCode() {
        return required("user.country.code");
    }

    public static String getItem1Id() {
        return required("item1.id");
    }

    public static int getItem1Price() {
        return Integer.parseInt(required("item1.price"));
    }

    public static int getItem1Quantity() {
        return Integer.parseInt(required("item1.quantity"));
    }

    public static String getItem2Id() {
        return required("item2.id");
    }

    public static int getItem2Price() {
        return Integer.parseInt(required("item2.price"));
    }

    public static int getItem2Quantity() {
        return Integer.parseInt(required("item2.quantity"));
    }

    public static int getExpectedAccountId() {
        return Integer.parseInt(required("expected.account.id"));
    }

    public static String getExpectedAccountName() {
        return required("expected.account.name");
    }

    public static double getExpectedTotalAmount() {
        return Double.parseDouble(required("expected.total.amount"));
    }

    public static double getExpectedTax() {
        return Double.parseDouble(required("expected.tax"));
    }

    public static double getExpectedNetPayable() {
        return Double.parseDouble(required("expected.net.payable"));
    }

    public static String getExpectedPaymentStatus() {
        return required("expected.payment.status");
    }

    public static String getExpectedPaymentProvider() {
        return required("expected.payment.provider");
    }

    public static int getPayMode() {
        return Integer.parseInt(required("pay.mode"));
    }

    public static int getTablePreferenceValue() {
        return Integer.parseInt(required("table.preference.value"));
    }

    public static String getOrderType() {
        return required("order.type");
    }

    public static String getPaymentUserMobile() {
        return required("payment.user.mobile");
    }

    public static String getExpectedAfterPaymentStatus() {
        return required("expected.after.payment.status");
    }

    public static String getExpectedAfterOrderStatus() {
        return required("expected.after.order.status");
    }

    public static String getExpectedOmsPaymentStatus() {
        return required("expected.oms.payment.status");
    }

    public static String getExpectedOmsPaymentMethod() {
        return required("expected.oms.payment.method");
    }

    public static String getExpectedOmsOrderType() {
        return required("expected.oms.order.type");
    }

    public static String getCustomItemId() {
        return required("custom.item.id");
    }

    public static double getCustomItemPrice() {
        return Double.parseDouble(required("custom.item.price"));
    }

    public static int getCustomItemQuantity() {
        return Integer.parseInt(required("custom.item.quantity"));
    }

    public static int getCustomOptionId() {
        return Integer.parseInt(required("custom.option.id"));
    }

    public static double getCustomOptionPrice() {
        return Double.parseDouble(required("custom.option.price"));
    }

    public static int getCustomOptionMappingId() {
        return Integer.parseInt(required("custom.option.mapping.id"));
    }

    public static double getCustomExpectedTotal() {
        return Double.parseDouble(required("custom.expected.total"));
    }

    public static String getCustomExpectedNameContains() {
        return required("custom.expected.name.contains");
    }

    public static String getCustomAddonItemId() {
        return required("custom.addon.item.id");
    }

    public static double getCustomAddonItemPrice() {
        return Double.parseDouble(required("custom.addon.item.price"));
    }

    public static int getCustomAddonItemQuantity() {
        return Integer.parseInt(required("custom.addon.item.quantity"));
    }

    public static int getCustomAddonOptionId() {
        return Integer.parseInt(required("custom.addon.option.id"));
    }

    public static double getCustomAddonOptionPrice() {
        return Double.parseDouble(required("custom.addon.option.price"));
    }

    public static int getCustomAddonOptionMappingId() {
        return Integer.parseInt(required("custom.addon.option.mapping.id"));
    }

    public static double getCustomAddonExpectedTotal() {
        return Double.parseDouble(required("custom.addon.expected.total"));
    }

    public static String getInvalidQrCode() {
        return required("invalid.qr.code");
    }

    public static String getInvalidItemId() {
        return required("invalid.item.id");
    }

    public static double getItem1OnlyExpectedTotal() {
        return Double.parseDouble(required("item1.only.expected.total"));
    }

    public static int getItem1Qty3() {
        return Integer.parseInt(required("item1.qty3"));
    }

    public static double getItem1Qty3ExpectedTotal() {
        return Double.parseDouble(required("item1.qty3.expected.total"));
    }

    public static double getBaselinePlusCustomExpectedTotal() {
        return Double.parseDouble(required("baseline.plus.custom.expected.total"));
    }

    /**
     * Rows for the customer-details DataProvider: name, country code, mobile.
     */
    public static Object[][] customerVariants() {
        return new Object[][] {
                {required("customer.1.name"), required("customer.1.country.code"), required("customer.1.mobile")},
                {required("customer.2.name"), required("customer.2.country.code"), required("customer.2.mobile")},
                {required("customer.3.name"), required("customer.3.country.code"), required("customer.3.mobile")}
        };
    }

    public static String getInvalidShortMobile() {
        return required("invalid.mobile.short");
    }

    public static String getItemInstruction() {
        return required("item.instruction");
    }

    public static String getOrderInstruction() {
        return required("order.instruction");
    }

    private static String required(String key) {
        String value = DATA.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing key in " + DATA_FILE + ": " + key);
        }
        return value.trim();
    }
}
