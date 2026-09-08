package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentData {

    private boolean success;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonProperty("pg_name")
    private String pgName;

    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("payment_log_id")
    private Integer paymentLogId;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPgName() {
        return pgName;
    }

    public void setPgName(String pgName) {
        this.pgName = pgName;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getPaymentLogId() {
        return paymentLogId;
    }

    public void setPaymentLogId(Integer paymentLogId) {
        this.paymentLogId = paymentLogId;
    }
}
