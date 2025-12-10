package com.jeanfrias.ordering.domain;

public final class OrderConstants {

    private OrderConstants() {
    }

    // Provider Names
    public static final String PROVIDER_STRIPE = "stripe";
    public static final String PROVIDER_ADYEN = "adyen";
    public static final String PROVIDER_EPOS = "epos";
    public static final String PROVIDER_SIMPHONY = "simphony";
    public static final String PROVIDER_LOGGI = "loggi";
    public static final String PROVIDER_ORDERLORD = "orderlord";

    // Order Status
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";

    // Failure Reasons
    public static final String REASON_AUTH_FAIL = "AUTH_FAIL";
    public static final String REASON_CAPTURE_FAIL = "CAPTURE_FAIL";
    public static final String REASON_POS_UNAVAILABLE = "POS_UNAVAILABLE";
    public static final String REASON_DISPATCH_UNAVAILABLE = "DISPATCH_UNAVAILABLE";
}
