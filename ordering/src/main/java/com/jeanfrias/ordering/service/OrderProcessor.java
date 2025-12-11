package com.jeanfrias.ordering.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jeanfrias.ordering.domain.OrderConstants;
import com.jeanfrias.ordering.domain.OrderRequest;
import com.jeanfrias.ordering.domain.OrderResponse;
import com.jeanfrias.ordering.domain.PaymentStatus;
import com.jeanfrias.ordering.provider.DispatchProvider;
import com.jeanfrias.ordering.provider.PaymentProvider;
import com.jeanfrias.ordering.provider.PosProvider;

@Service
public class OrderProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);

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
        logger.info("Processing order: orderId={}, amount={}, idempotencyKey={}",
                orderRequest.orderId(), orderRequest.amount(), orderRequest.idempotencyKey());

        if (idempotencyStore.containsKey(orderRequest.idempotencyKey())) {
            logger.info("Idempotency cache hit for orderId={}, returning cached response", orderRequest.orderId());
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

        var payment = payments.getOrDefault(OrderConstants.PROVIDER_STRIPE, payments.values().iterator().next());
        var pos = poses.getOrDefault(OrderConstants.PROVIDER_EPOS, poses.values().iterator().next());
        var dispatch = dispatches.getOrDefault(OrderConstants.PROVIDER_LOGGI, dispatches.values().iterator().next());

        boolean isAuthorized = false;
        boolean isVoided = false;
        String posStatus = OrderConstants.STATUS_PENDING;
        String dispatchStatus = OrderConstants.STATUS_PENDING;

        logger.debug("Authorizing payment for orderId={}", orderRequest.orderId());
        if (!payment.authorize(orderRequest.orderId(), orderRequest.amount())) {
            logger.warn("Payment authorization failed for orderId={}", orderRequest.orderId());
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), OrderConstants.STATUS_FAILED, OrderConstants.REASON_AUTH_FAIL,
                    new PaymentStatus(false, false, false),
                    OrderConstants.STATUS_SKIPPED, OrderConstants.STATUS_SKIPPED, false));
        }

        isAuthorized = true;
        logger.info("Payment authorized successfully for orderId={}", orderRequest.orderId());

        logger.debug("Sending order to POS for orderId={}", orderRequest.orderId());
        if (!pos.sendOrder(orderRequest.orderId(), orderRequest.amount())) {
            logger.warn("POS order failed for orderId={}, voiding payment", orderRequest.orderId());
            payment.voidPayment(orderRequest.orderId());
            isVoided = true;
            posStatus = OrderConstants.STATUS_ERROR;

            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), OrderConstants.STATUS_FAILED, OrderConstants.REASON_POS_UNAVAILABLE,
                    new PaymentStatus(isAuthorized, false, isVoided),
                    posStatus, OrderConstants.STATUS_SKIPPED, false));
        }

        posStatus = OrderConstants.STATUS_OK;
        logger.info("POS order sent successfully for orderId={}", orderRequest.orderId());

        logger.debug("Capturing payment for orderId={}", orderRequest.orderId());
        if (!payment.capture(orderRequest.orderId())) {
            logger.error("Payment capture failed for orderId={}", orderRequest.orderId());
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), OrderConstants.STATUS_FAILED, OrderConstants.REASON_CAPTURE_FAIL,
                    new PaymentStatus(isAuthorized, false, isVoided),
                    posStatus, OrderConstants.STATUS_SKIPPED, false));
        }
        logger.info("Payment captured successfully for orderId={}", orderRequest.orderId());

        logger.debug("Scheduling dispatch for orderId={}", orderRequest.orderId());
        if (dispatch.schedule(orderRequest.orderId())) {
            dispatchStatus = OrderConstants.STATUS_SCHEDULED;
            logger.info("Dispatch scheduled successfully for orderId={}", orderRequest.orderId());
        } else {
            dispatchStatus = OrderConstants.STATUS_FAILED;
            logger.warn("Dispatch scheduling failed for orderId={}", orderRequest.orderId());
            return save(orderRequest.idempotencyKey(), new OrderResponse(
                    orderRequest.orderId(), OrderConstants.STATUS_FAILED, OrderConstants.REASON_DISPATCH_UNAVAILABLE,
                    new PaymentStatus(isAuthorized, true, false),
                    posStatus, dispatchStatus, false));
        }

        logger.info("Order processed successfully: orderId={}, status={}", orderRequest.orderId(),
                OrderConstants.STATUS_PROCESSED);
        return save(orderRequest.idempotencyKey(), new OrderResponse(
                orderRequest.orderId(), OrderConstants.STATUS_PROCESSED, null,
                new PaymentStatus(isAuthorized, true, false),
                posStatus, dispatchStatus, false));
    }

    private OrderResponse save(String idempotencyKey, OrderResponse orderResponse) {
        idempotencyStore.put(idempotencyKey, orderResponse);
        return orderResponse;
    }
}
