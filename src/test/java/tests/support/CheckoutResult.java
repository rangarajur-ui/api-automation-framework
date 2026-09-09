package tests.support;

import io.restassured.response.Response;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.PaymentResponse;
import pojo.response.PaymentStatusResponse;
import pojo.response.oms.OmsOrderData;
import pojo.response.oms.OmsOrderDetailsResponse;
import pojo.response.oms.OmsOrderItem;

/**
 * Live payment + confirmation + OMS payloads after Telr returns.
 */
public final class CheckoutResult {

    private final PaymentResponse payment;
    private final PaymentStatusResponse paymentStatus;
    private final CartResponse confirmation;
    private final Response confirmationHttp;
    private final OmsOrderDetailsResponse oms;
    private final Response omsHttp;

    CheckoutResult(
            PaymentResponse payment,
            PaymentStatusResponse paymentStatus,
            CartResponse confirmation,
            Response confirmationHttp,
            OmsOrderDetailsResponse oms,
            Response omsHttp
    ) {
        this.payment = payment;
        this.paymentStatus = paymentStatus;
        this.confirmation = confirmation;
        this.confirmationHttp = confirmationHttp;
        this.oms = oms;
        this.omsHttp = omsHttp;
    }

    public PaymentResponse payment() {
        return payment;
    }

    public PaymentStatusResponse paymentStatus() {
        return paymentStatus;
    }

    public CartResponse confirmation() {
        return confirmation;
    }

    public Response confirmationHttp() {
        return confirmationHttp;
    }

    public OmsOrderDetailsResponse oms() {
        return oms;
    }

    public OmsOrderData omsData() {
        return oms == null ? null : oms.getData();
    }

    public Integer orderId() {
        return paymentStatus.getData().getOrderId();
    }

    public String orderNumber() {
        return paymentStatus.getData().getOrderNumber();
    }

    public OrderItem findItemByName(String name) {
        if (confirmation.getData().getOrderItems() == null) {
            return null;
        }
        for (OrderItem item : confirmation.getData().getOrderItems()) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    public OrderItem findItemContaining(String text) {
        if (confirmation.getData().getOrderItems() == null) {
            return null;
        }
        for (OrderItem item : confirmation.getData().getOrderItems()) {
            if (item.getName() != null && item.getName().contains(text)) {
                return item;
            }
        }
        return null;
    }

    public OmsOrderItem findOmsItemContaining(String text) {
        if (omsData() == null || omsData().getItems() == null) {
            return null;
        }
        for (OmsOrderItem item : omsData().getItems()) {
            if (item.getName() != null && item.getName().contains(text)) {
                return item;
            }
        }
        return null;
    }

    public String findPersistedText(String expected) {
        if (expected == null || expected.isBlank()) {
            return null;
        }
        if (omsData() != null && omsData().getItems() != null) {
            for (OmsOrderItem item : omsData().getItems()) {
                if (item.getNotes() != null && item.getNotes().contains(expected)) {
                    return item.getNotes();
                }
                if (item.getCustomizations() != null && item.getCustomizations().contains(expected)) {
                    return item.getCustomizations();
                }
            }
        }
        if (confirmation.getData().getCookingDetails() != null
                && confirmation.getData().getCookingDetails().contains(expected)) {
            return confirmation.getData().getCookingDetails();
        }
        if (omsHttp != null && omsHttp.asString() != null && omsHttp.asString().contains(expected)) {
            return expected;
        }
        if (confirmationHttp != null && confirmationHttp.asString() != null
                && confirmationHttp.asString().contains(expected)) {
            return expected;
        }
        return null;
    }
}
