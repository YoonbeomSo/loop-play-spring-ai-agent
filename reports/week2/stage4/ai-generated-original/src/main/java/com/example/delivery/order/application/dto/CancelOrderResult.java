package com.example.delivery.order.application.dto;

public record CancelOrderResult(
    boolean success,
    Long orderId,
    String orderStatus,
    String message
) {
    public static CancelOrderResult success(Long orderId, String orderStatus, String message) {
        return new CancelOrderResult(true, orderId, orderStatus, message);
    }

    public static CancelOrderResult fail(Long orderId, String orderStatus, String message) {
        return new CancelOrderResult(false, orderId, orderStatus, message);
    }
}
