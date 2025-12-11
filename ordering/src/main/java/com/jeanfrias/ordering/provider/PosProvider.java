package com.jeanfrias.ordering.provider;

import java.math.BigDecimal;

public interface PosProvider {
    boolean sendOrder(String orderId, BigDecimal amount);   
}