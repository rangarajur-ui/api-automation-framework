package pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QrMenuRequest {

    @JsonProperty("skip_item_options")
    private boolean skipItemOptions;

    @JsonProperty("issue_qr_scan_token")
    private boolean issueQrScanToken;

    public boolean isSkipItemOptions() {
        return skipItemOptions;
    }

    public void setSkipItemOptions(boolean skipItemOptions) {
        this.skipItemOptions = skipItemOptions;
    }

    public boolean isIssueQrScanToken() {
        return issueQrScanToken;
    }

    public void setIssueQrScanToken(boolean issueQrScanToken) {
        this.issueQrScanToken = issueQrScanToken;
    }
}
