package pojo.response.oms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmsAuditEvent {

    private String event;
    private String title;
    private String status;

    @JsonProperty("time_user_stamp")
    private String timeUserStamp;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeUserStamp() {
        return timeUserStamp;
    }

    public void setTimeUserStamp(String timeUserStamp) {
        this.timeUserStamp = timeUserStamp;
    }
}
