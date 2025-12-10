package com.jeanfrias.ordering.infrastructure.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.PaymentProvider;

@Component("adyen")
public class AdyenPayment implements PaymentProvider {

    @Override
    public boolean authorize(String orderId, BigDecimal amount) {
        System.out.println("Adyen - Authorizing order " + orderId);
        return true;
    }

    @Override
    public boolean capture(String orderId) {
        System.out.println("Adyen - Capturing order " + orderId);
        return true;
    }

    @Override
    public boolean voidPayment(String orderId) {
        System.out.println("Adyen - Voiding order " + orderId);
        return true;
    }
}
