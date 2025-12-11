package com.jeanfrias.ordering.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jeanfrias.ordering.domain.OrderRequest;
import com.jeanfrias.ordering.domain.OrderResponse;
import com.jeanfrias.ordering.service.OrderProcessor;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderProcessor orderProcessor;

    public OrderController(OrderProcessor orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> processOrder(@RequestBody OrderRequest orderRequest) {
        if (orderRequest.orderId() == null || orderRequest.idempotencyKey() == null) {
            return ResponseEntity.badRequest().build();
        }

        OrderResponse orderResponse = orderProcessor.processOrder(orderRequest);
        return ResponseEntity.ok(orderResponse);
    }
}