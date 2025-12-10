package com.jeanfrias.ordering.infrastructure.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.PaymentProvider;

@Component("stripe")
public class StripePayment implements PaymentProvider {

    @Override
    public boolean authorize(String orderId, BigDecimal amount) {
        System.out.println("Stripe - Authorizing order " + orderId);
        return true;
    }

    @Override
    public boolean capture(String orderId) {
        System.out.println("Stripe - Capturing order " + orderId);
        return true;
    }

    @Override
    public boolean voidPayment(String orderId) {
        System.out.println("Stripe - Voiding order " + orderId);
        return true;
    }
}
