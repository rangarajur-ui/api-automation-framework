package pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body for the paid-order confirmation cart (view=customer).
 * Matches the live request: no order_items — the order is loaded via ct.
 */
public class ConfirmationCartRequest {

    @JsonProperty("order_source")
    private int orderSource;

    @JsonProperty("order_type")
    private String orderType;

    @JsonProperty("delivery_type")
    private String deliveryType;

    @JsonProperty("order_mode")
    private String orderMode;

    public int getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(int orderSource) {
        this.orderSource = orderSource;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
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
}
