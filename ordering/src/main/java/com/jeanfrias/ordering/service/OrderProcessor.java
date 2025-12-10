package com.jeanfrias.ordering.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.jeanfrias.ordering.domain.OrderRequest;
import com.jeanfrias.ordering.domain.OrderResponse;
import com.jeanfrias.ordering.domain.PaymentStatus;
import com.jeanfrias.ordering.provider.DispatchProvider;
import com.jeanfrias.ordering.provider.PaymentProvider;
import com.jeanfrias.ordering.provider.PosProvider;

@Service
public class OrderProcessor {

    private final Map<String, OrderResponse> idempotencyStore = new ConcurrentHashMap<>();

    private Map<String, PaymentProvider> payments = new ConcurrentHashMap<>();
    private Map<String, PosProvider> poses = new ConcurrentHashMap<>();
    private Map<String, DispatchProvider> dispatches = new ConcurrentHashMap<>();

    public OrderProcessor(
            Map<String, PaymentProvider> payments,
            Map<String, PosProvider> poses,
            Map<String, DispatchProvider> dispatches) {

        this.payments = payments;
        this.poses = poses;
        this.dispatches = dispatches;
    }

    public OrderResponse processOrder(OrderRequest orderRequest) {
        if (idempotencyStore.containsKey(orderRequest.idempotencyKey())) {
            var cached = idempotencyStore.get(orderRequest.idempotencyKey());
            return new OrderResponse(
                    cached.orderId(),
                    cached.status(),
                    cached.reason(),
                    cached.payment(),
                    cached.pos(),
                    cached.dispatch(),
                    true);
        }

        var payment = payments.getOrDefault("stripe", payments.values().iterator().next());
        var pos = poses.getOrDefault("epos", poses.values().iterator().next());
        var dispatch = dispatches.getOrDefault("loggi", dispatches.values().iterator().next());

        boolean isAuthorized = false;
        boolean isVoided = false;
        String posStatus = "PENDING";
        String dispatchStatus = "PENDING";

        if (!payment.authorize(orderRequest.orderId(), orderRequest.amount())) {
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), "FAILED", "AUTH_FAIL",
                    new PaymentStatus(false, false, false),
                    "SKIPPED", "SKIPPED", false));
        }

        isAuthorized = true;

        if (!pos.sendOrder(orderRequest.orderId(), orderRequest.amount())) {
            payment.voidPayment(orderRequest.orderId());
            isVoided = true;
            posStatus = "ERROR";

            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), "FAILED", "POS_UNAVAILABLE",
                    new PaymentStatus(isAuthorized, false, isVoided),
                    posStatus, "SKIPPED", false));
        }

        posStatus = "OK";

        if (!payment.capture(orderRequest.orderId())) {
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), "FAILED", "CAPTURE_FAIL",
                    new PaymentStatus(isAuthorized, false, isVoided),
                    posStatus, "SKIPPED", false));
        }

        if (dispatch.schedule(orderRequest.orderId())) {
            dispatchStatus = "SCHEDULED";
        } else {
            dispatchStatus = "FAILED";
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), "FAILED", "DISPATCH_UNAVAILABLE",
                    new PaymentStatus(isAuthorized, true, false),
                    posStatus, dispatchStatus, false));
        }

        return save(orderRequest.idempotencyKey(), new OrderResponse(
                orderRequest.orderId(), "PROCESSED", null,
                new PaymentStatus(isAuthorized, true, false),
                posStatus, dispatchStatus, false));
    }

    private OrderResponse save(String idempotencyKey, OrderResponse orderResponse) {
        idempotencyStore.put(idempotencyKey, orderResponse);
        return orderResponse;
    }
}
