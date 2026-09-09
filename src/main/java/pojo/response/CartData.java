package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CartData {

    @JsonProperty("order_items_total")
    private OrderItemsTotal orderItemsTotal;

    @JsonProperty("order_items")
    private List<OrderItem> orderItems;

    @JsonProperty("new_order_items")
    private List<NewOrderItem> newOrderItems;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("payment_provider_name")
    private String paymentProviderName;

    @JsonProperty("cooking_details")
    private String cookingDetails;

    @JsonProperty("user_name")
    private String userName;

    public OrderItemsTotal getOrderItemsTotal() {
        return orderItemsTotal;
    }

    public void setOrderItemsTotal(OrderItemsTotal orderItemsTotal) {
        this.orderItemsTotal = orderItemsTotal;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public List<NewOrderItem> getNewOrderItems() {
        return newOrderItems;
    }

    public void setNewOrderItems(List<NewOrderItem> newOrderItems) {
        this.newOrderItems = newOrderItems;
    }

    /**
     * After payment, order_number is on each new_order_items row, not on data itself.
     */
    public String getOrderNumber() {
        if (newOrderItems == null || newOrderItems.isEmpty()) {
            return null;
        }
        return newOrderItems.get(0).getOrderNumber();
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentProviderName() {
        return paymentProviderName;
    }

    public void setPaymentProviderName(String paymentProviderName) {
        this.paymentProviderName = paymentProviderName;
    }

    public String getCookingDetails() {
        return cookingDetails;
    }

    public void setCookingDetails(String cookingDetails) {
        this.cookingDetails = cookingDetails;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
