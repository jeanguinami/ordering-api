package com.jeanfrias.ordering.infrastructure.pos;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.PosProvider;

@Component("epos")
public class EposProvider implements PosProvider {

    @Override
    public boolean sendOrder(String orderId, BigDecimal amount) {
        System.out.println("Epos - Sending order " + orderId);
        return true;
    }
}