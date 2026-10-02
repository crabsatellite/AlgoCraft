package com.crabmods.algocraft.client.gui.modern;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiErrorMessagesTest {
    @Test
    void blankThrowableMessageFallsBackToThrowableClassName() {
        assertEquals("IllegalStateException",
                UiErrorMessages.fromThrowable(new IllegalStateException("   ")));
    }

    @Test
    void completionExceptionUsesBoundedCauseMessage() {
        String message = UiErrorMessages.fromThrowable(
                new CompletionException(new IllegalArgumentException("manifest\nsignature\tmismatch")));

        assertEquals("manifest signature mismatch", message);
        assertFalse(message.contains("\n"), "UI error messages should not inject extra console/status lines");
        assertFalse(message.contains("\t"), "UI error messages should normalize whitespace");
    }

    @Test
    void oversizedExternalErrorMessagesAreTruncatedForGameUi() {
        String marker = "manifest totalProblems mismatch ";
        String hostileTail = "REMOTE_ERROR_TAIL_" + "x".repeat(2_000);

        String message = UiErrorMessages.fromThrowable(new IllegalStateException(marker + hostileTail));

        assertTrue(message.startsWith(marker), "bounded message should keep the actionable prefix");
        assertTrue(message.endsWith("[truncated]"), "bounded message should identify truncation");
        assertTrue(message.length() <= UiErrorMessages.MAX_ERROR_MESSAGE_CHARS,
                "bounded message should fit in the in-game status/console");
        assertFalse(message.contains(hostileTail),
                "bounded message must not retain the full external error payload");
    }
}
