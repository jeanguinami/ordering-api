package com.jeanfrias.ordering.provider;

public interface DispatchProvider {
    boolean dispatch(String orderId);
}