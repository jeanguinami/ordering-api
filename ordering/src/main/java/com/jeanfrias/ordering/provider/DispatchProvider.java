package com.jeanfrias.ordering.provider;

public interface DispatchProvider {
    boolean schedule(String orderId);
}