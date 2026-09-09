package pojo.response.oms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmsOrderDetailsResponse {

    private OmsOrderData data;

    public OmsOrderData getData() {
        return data;
    }

    public void setData(OmsOrderData data) {
        this.data = data;
    }
}
