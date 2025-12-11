package com.jeanfrias.ordering.domain;

import java.math.BigDecimal;

public record OrderRequest(
    String orderId,
    String restaurantId,
    BigDecimal amount,
    String idempotencyKey
) {}