package com.crabmods.algocraft.logic.catalog;
import com.crabmods.algocraft.logic.SubmissionResult;

/** Compiler diagnostics describe the submitted code. Hidden inputs and runtime outputs never leave the judge. */
public final class PublicJudgeFeedback {
    public static String encode(SubmissionResult result) {
        String status = result.getMessage().replace('\n', ' ').replace('\r', ' ');
        String message = status;
        if (status.equals("Compilation Error")) {
            for (var detail : result.getDetails()) {
                if (detail.getError() != null) { message += "\n" + detail.getError(); break; }
            }
        }
        return message.substring(0, Math.min(message.length(), 4096));
    }
    public static void decode(SubmissionResult result, String message) {
        int newline = message.indexOf('\n');
        result.setMessage(newline < 0 ? message : message.substring(0, newline));
        if (newline >= 0 && result.getMessage().equals("Compilation Error")) {
            var detail = new SubmissionResult.TestCaseResult();
            detail.setPassed(false); detail.setError(message.substring(newline + 1));
            result.addDetail(detail);
        }
    }
}
