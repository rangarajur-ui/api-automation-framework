package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CartResponse {

    private CartData data;

    public CartData getData() {
        return data;
    }

    public void setData(CartData data) {
        this.data = data;
    }
}
