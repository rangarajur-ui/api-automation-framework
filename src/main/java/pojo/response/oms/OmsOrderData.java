package pojo.response.oms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OmsOrderData {

    private Integer id;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("kot_number")
    private String kotNumber;

    @JsonProperty("order_type")
    private String orderType;

    private String status;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("table_numbers")
    private String tableNumbers;

    @JsonProperty("outlet_name")
    private String outletName;

    @JsonProperty("total_amount")
    private double totalAmount;

    @JsonProperty("tenant_id")
    private Integer tenantId;

    private OmsCustomer customer;
    private List<OmsOrderItem> items;

    @JsonProperty("order_timeline")
    private OmsOrderTimeline orderTimeline;

    @JsonProperty("audit_timeline")
    private List<OmsAuditEvent> auditTimeline;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getKotNumber() {
        return kotNumber;
    }

    public void setKotNumber(String kotNumber) {
        this.kotNumber = kotNumber;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTableNumbers() {
        return tableNumbers;
    }

    public void setTableNumbers(String tableNumbers) {
        this.tableNumbers = tableNumbers;
    }

    public String getOutletName() {
        return outletName;
    }

    public void setOutletName(String outletName) {
        this.outletName = outletName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public OmsCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(OmsCustomer customer) {
        this.customer = customer;
    }

    public List<OmsOrderItem> getItems() {
        return items;
    }

    public void setItems(List<OmsOrderItem> items) {
        this.items = items;
    }

    public OmsOrderTimeline getOrderTimeline() {
        return orderTimeline;
    }

    public void setOrderTimeline(OmsOrderTimeline orderTimeline) {
        this.orderTimeline = orderTimeline;
    }

    public List<OmsAuditEvent> getAuditTimeline() {
        return auditTimeline;
    }

    public void setAuditTimeline(List<OmsAuditEvent> auditTimeline) {
        this.auditTimeline = auditTimeline;
    }

    public boolean hasAuditEvent(String eventName) {
        if (auditTimeline == null) {
            return false;
        }
        for (OmsAuditEvent event : auditTimeline) {
            if (eventName.equals(event.getEvent()) && "done".equals(event.getStatus())) {
                return true;
            }
        }
        return false;
    }
}
