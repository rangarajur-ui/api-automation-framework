package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Customer-facing line on the after-payment page (data.new_order_items).
 * order_number lives here, not at the data root.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewOrderItem {

    private String name;

    @JsonProperty("item_name")
    private String itemName;

    private double quantity;

    @JsonProperty("order_number")
    private String orderNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
