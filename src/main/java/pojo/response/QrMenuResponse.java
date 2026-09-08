package pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QrMenuResponse {

    private QrMenuData data;

    public QrMenuData getData() {
        return data;
    }

    public void setData(QrMenuData data) {
        this.data = data;
    }
}
