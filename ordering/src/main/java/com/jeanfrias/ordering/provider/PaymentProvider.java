package com.jeanfrias.ordering.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    boolean authorize(String orderId, BigDecimal amount);
    boolean capture(String orderId);
    boolean voidPayment(String orderId);
}