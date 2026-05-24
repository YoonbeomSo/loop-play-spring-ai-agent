package com.example.delivery.order.application.dto;

public record CancelOrderCommand(
    Long orderId,
    Long customerId,
    String cancelReason,
    String requestedBy
) {
}
