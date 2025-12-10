package com.jeanfrias.ordering.domain;

public record PaymentStatus(boolean authorized, boolean captured, boolean voided) {
}