package pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CartRequest {

    private String code;

    @JsonProperty("order_source")
    private int orderSource;

    @JsonProperty("delivery_type")
    private String deliveryType;

    @JsonProperty("order_mode")
    private String orderMode;

    // The API expects order_items as positional arrays, not named JSON objects.
    // Row 0 is the column header. Every following row must use the same column order.
    // Keep this mapping here so tests can call addItem(...) and never build these arrays.
    private static final List<Object> ORDER_ITEM_COLUMNS = Arrays.asList(
            "id",
            "item_id",
            "price",
            "quantity",
            "total_price",
            "offer_id",
            "notes",
            "net_price",
            "variants",
            "customizations",
            "time_slot_id",
            "delivery_option_id",
            "item_instruction",
            "origin"
    );

    // Nested table used inside the customizations column. Inspected from the live QR menu JS.
    private static final List<Object> CUSTOMIZATION_COLUMNS = Arrays.asList(
            "id",
            "item_id",
            "price",
            "quantity",
            "total_price",
            "offer_id",
            "notes",
            "net_price",
            "is_primary",
            "variants",
            "customizations",
            "time_slot_id",
            "delivery_option_id",
            "customization_mapping_id"
    );

    @JsonProperty("order_items")
    private List<List<Object>> orderItems;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_mobile_number")
    private String userMobileNumber;

    @JsonProperty("user_country_code")
    private String userCountryCode;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(int orderSource) {
        this.orderSource = orderSource;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getOrderMode() {
        return orderMode;
    }

    public void setOrderMode(String orderMode) {
        this.orderMode = orderMode;
    }

    public List<List<Object>> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<List<Object>> orderItems) {
        this.orderItems = orderItems;
    }

    /**
     * Adds one cart line in the format the API requires.
     * total_price and net_price are price * quantity (no offer applied).
     * Empty / null trailing values match the working API 2 request.
     */
    public void addItem(String itemId, int price, int quantity) {
        addItem(itemId, price, quantity, null);
    }

    /**
     * Same as addItem, plus item_instruction (curl column).
     * The cart response stores that text on notes.
     */
    public void addItem(String itemId, int price, int quantity, String itemInstruction) {
        ensureHeaderRow();

        int totalPrice = price * quantity;
        orderItems.add(Arrays.asList(
                "",
                itemId,
                price,
                quantity,
                totalPrice,
                "",
                null,
                totalPrice,
                Collections.emptyMap(),
                Collections.emptyList(),
                null,
                null,
                itemInstruction,
                "menu"
        ));
    }

    /**
     * Adds a parent item plus one customization option.
     * The UI sends customizations as a nested positional table (not JSON objects).
     * Parent price stays the menu base price; the option row carries the extra amount.
     */
    public void addCustomizedItem(
            String itemId,
            double price,
            int quantity,
            int optionId,
            double optionPrice,
            int mappingId
    ) {
        ensureHeaderRow();

        double parentTotal = price * quantity;
        double optionTotal = optionPrice * quantity;

        List<List<Object>> customizationTable = new ArrayList<>();
        customizationTable.add(CUSTOMIZATION_COLUMNS);
        customizationTable.add(Arrays.asList(
                optionId,
                itemId,
                optionPrice,
                quantity,
                optionTotal,
                null,
                null,
                optionTotal,
                null,
                null,
                null,
                null,
                null,
                mappingId
        ));

        orderItems.add(Arrays.asList(
                "",
                itemId,
                price,
                quantity,
                parentTotal,
                "",
                null,
                parentTotal,
                Collections.emptyMap(),
                customizationTable,
                null,
                null,
                null,
                "menu"
        ));
    }

    /**
     * Header row only — used to assert the empty-cart error.
     */
    public void addHeaderRowOnly() {
        orderItems = new ArrayList<>();
        orderItems.add(ORDER_ITEM_COLUMNS);
    }

    private void ensureHeaderRow() {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        if (orderItems.isEmpty()) {
            orderItems.add(ORDER_ITEM_COLUMNS);
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobileNumber() {
        return userMobileNumber;
    }

    public void setUserMobileNumber(String userMobileNumber) {
        this.userMobileNumber = userMobileNumber;
    }

    public String getUserCountryCode() {
        return userCountryCode;
    }

    public void setUserCountryCode(String userCountryCode) {
        this.userCountryCode = userCountryCode;
    }
}
