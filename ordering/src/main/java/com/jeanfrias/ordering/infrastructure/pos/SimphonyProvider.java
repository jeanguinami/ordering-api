package com.jeanfrias.ordering.infrastructure.pos;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.PosProvider;

@Component("simphony")
public class SimphonyProvider implements PosProvider {

    @Override
    public boolean sendOrder(String orderId, BigDecimal amount) {
        System.out.println("Simphony - Sending order " + orderId);
        return true;
    }
}