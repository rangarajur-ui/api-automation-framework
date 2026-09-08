package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemsTotal {

    @JsonProperty("total_amount")
    private double totalAmount;

    @JsonProperty("original_tax_total")
    private double originalTaxTotal;

    @JsonProperty("net_payable_amount")
    private double netPayableAmount;

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getOriginalTaxTotal() {
        return originalTaxTotal;
    }

    public void setOriginalTaxTotal(double originalTaxTotal) {
        this.originalTaxTotal = originalTaxTotal;
    }

    public double getNetPayableAmount() {
        return netPayableAmount;
    }

    public void setNetPayableAmount(double netPayableAmount) {
        this.netPayableAmount = netPayableAmount;
    }
}
