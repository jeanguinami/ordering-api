package com.jeanfrias.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jeanfrias.ordering.domain.OrderConstants;
import com.jeanfrias.ordering.domain.OrderRequest;
import com.jeanfrias.ordering.domain.OrderResponse;
import com.jeanfrias.ordering.provider.DispatchProvider;
import com.jeanfrias.ordering.provider.PaymentProvider;
import com.jeanfrias.ordering.provider.PosProvider;
import com.jeanfrias.ordering.service.OrderProcessor;

class OrderProcessorTest {

    private OrderProcessor orderProcessor;

    @Mock
    private PaymentProvider paymentMock;
    @Mock
    private PosProvider posMock;
    @Mock
    private DispatchProvider dispatchMock;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        orderProcessor = new OrderProcessor(
                Map.of(OrderConstants.PROVIDER_STRIPE, paymentMock),
                Map.of(OrderConstants.PROVIDER_EPOS, posMock),
                Map.of(OrderConstants.PROVIDER_LOGGI, dispatchMock));
    }

    @Test
    void testPosFailure_ShouldVoidAndFail() {
        // Arrange
        when(paymentMock.authorize(any(), any())).thenReturn(true);
        when(posMock.sendOrder(any(), any())).thenReturn(false);

        var req = new OrderRequest("ORD-FAIL", "REST-1", BigDecimal.TEN, "KEY-FAIL");

        // Act
        OrderResponse resp = orderProcessor.processOrder(req);

        // Assert
        assertEquals(OrderConstants.STATUS_FAILED, resp.status());
        assertEquals(OrderConstants.REASON_POS_UNAVAILABLE, resp.reason());
        assertTrue(resp.payment().voided(), "Payment should be voided if POS fails");
        assertFalse(resp.payment().captured(), "Payment should not be captured if POS fails");

        verify(paymentMock).voidPayment(any());
        verify(dispatchMock, never()).schedule(any());
    }

    @Test
    void testHappyPath() {
        // Arrange
        when(paymentMock.authorize(any(), any())).thenReturn(true);
        when(posMock.sendOrder(any(), any())).thenReturn(true);
        when(paymentMock.capture(any())).thenReturn(true);
        when(dispatchMock.schedule(any())).thenReturn(true);

        var req = new OrderRequest("ORD-OK", "REST-1", BigDecimal.TEN, "KEY-OK");

        // Act
        OrderResponse resp = orderProcessor.processOrder(req);

        // Assert
        assertEquals(OrderConstants.STATUS_PROCESSED, resp.status());
        assertTrue(resp.payment().captured());

    }

}