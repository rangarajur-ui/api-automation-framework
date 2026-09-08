package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItem {

    @JsonProperty("item_object_id")
    private Integer itemObjectId;

    private String name;
    private double quantity;
    private double price;

    @JsonProperty("net_price")
    private double netPrice;

    private String notes;

    public Integer getItemObjectId() {
        return itemObjectId;
    }

    public void setItemObjectId(Integer itemObjectId) {
        this.itemObjectId = itemObjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(double netPrice) {
        this.netPrice = netPrice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
