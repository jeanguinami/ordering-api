package com.jeanfrias.ordering.domain;

public record OrderResponse(
    String orderId,
    String status,
    String reason,
    PaymentStatus payment,
    String pos,
    String dispatch,
    boolean idempotent
) {
    
}