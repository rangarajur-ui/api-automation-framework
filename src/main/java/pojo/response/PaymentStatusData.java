package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentStatusData {

    @JsonProperty("is_terminal_status")
    private boolean terminalStatus;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("order_status")
    private String orderStatus;

    @JsonProperty("table_preference_value")
    private Integer tablePreferenceValue;

    @JsonProperty("cooking_details")
    private String cookingDetails;

    public boolean isTerminalStatus() {
        return terminalStatus;
    }

    public void setTerminalStatus(boolean terminalStatus) {
        this.terminalStatus = terminalStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getTablePreferenceValue() {
        return tablePreferenceValue;
    }

    public void setTablePreferenceValue(Integer tablePreferenceValue) {
        this.tablePreferenceValue = tablePreferenceValue;
    }

    public String getCookingDetails() {
        return cookingDetails;
    }

    public void setCookingDetails(String cookingDetails) {
        this.cookingDetails = cookingDetails;
    }
}
