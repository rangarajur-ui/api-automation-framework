package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private PaymentData data;

    public PaymentData getData() {
        return data;
    }

    public void setData(PaymentData data) {
        this.data = data;
    }
}
