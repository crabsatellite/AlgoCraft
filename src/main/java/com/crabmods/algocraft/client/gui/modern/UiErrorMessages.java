package com.crabmods.algocraft.client.gui.modern;

public final class UiErrorMessages {
    public static final int MAX_ERROR_MESSAGE_CHARS = 240;
    private static final String TRUNCATED_MARKER = " [truncated]";

    private UiErrorMessages() {
    }

    public static String fromThrowable(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        String message = cause == null ? null : cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause == null ? "Unknown error" : cause.getClass().getSimpleName();
        }
        return bounded(message);
    }

    public static String bounded(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_ERROR_MESSAGE_CHARS) {
            return normalized;
        }

        int prefixLength = Math.max(1, MAX_ERROR_MESSAGE_CHARS - TRUNCATED_MARKER.length());
        return normalized.substring(0, prefixLength) + TRUNCATED_MARKER;
    }
}
