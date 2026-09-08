package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentStatusResponse {

    private PaymentStatusData data;

    public PaymentStatusData getData() {
        return data;
    }

    public void setData(PaymentStatusData data) {
        this.data = data;
    }
}
