package com.jeanfrias.ordering.infrastructure.dispatch;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.DispatchProvider;

@Component("orderlord")
public class OrderLordProvider implements DispatchProvider {

    @Override
    public boolean schedule(String orderId) {
        System.out.println("OrderLord - Dispatching order " + orderId);
        return true;
    }
}