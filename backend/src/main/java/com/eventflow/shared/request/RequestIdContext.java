package com.eventflow.shared.request;

public final class RequestIdContext {

    private static final String FALLBACK_REQUEST_ID = "unknown";
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestIdContext() {}

    public static String current() {
        String requestId = REQUEST_ID.get();
        return requestId == null ? FALLBACK_REQUEST_ID : requestId;
    }

    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
