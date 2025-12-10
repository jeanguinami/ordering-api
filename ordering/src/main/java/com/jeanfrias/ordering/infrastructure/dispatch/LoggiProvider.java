package com.jeanfrias.ordering.infrastructure.dispatch;

import org.springframework.stereotype.Component;

import com.jeanfrias.ordering.provider.DispatchProvider;

@Component("loggi")
public class LoggiProvider implements DispatchProvider {

    @Override
    public boolean dispatch(String orderId) {
        System.out.println("Loggi - Dispatching order " + orderId);
        return true;
    }
}