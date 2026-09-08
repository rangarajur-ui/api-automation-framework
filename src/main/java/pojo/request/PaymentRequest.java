package pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Initiate-payment body is the cart payload plus a few payment fields.
 * order_items stays in the same positional-array format as CartRequest.
 */
public class PaymentRequest extends CartRequest {

    @JsonProperty("pay_mode")
    private int payMode;

    @JsonProperty("table_preference_value")
    private int tablePreferenceValue;

    @JsonProperty("order_type")
    private String orderType;

    @JsonProperty("cooking_details")
    private String cookingDetails;

    public static PaymentRequest fromCart(CartRequest cart) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setCode(cart.getCode());
        paymentRequest.setOrderSource(cart.getOrderSource());
        paymentRequest.setDeliveryType(cart.getDeliveryType());
        paymentRequest.setOrderMode(cart.getOrderMode());
        paymentRequest.setUserName(cart.getUserName());
        paymentRequest.setUserMobileNumber(cart.getUserMobileNumber());
        paymentRequest.setUserCountryCode(cart.getUserCountryCode());
        paymentRequest.setOrderItems(cart.getOrderItems());
        return paymentRequest;
    }

    public int getPayMode() {
        return payMode;
    }

    public void setPayMode(int payMode) {
        this.payMode = payMode;
    }

    public int getTablePreferenceValue() {
        return tablePreferenceValue;
    }

    public void setTablePreferenceValue(int tablePreferenceValue) {
        this.tablePreferenceValue = tablePreferenceValue;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCookingDetails() {
        return cookingDetails;
    }

    public void setCookingDetails(String cookingDetails) {
        this.cookingDetails = cookingDetails;
    }
}
