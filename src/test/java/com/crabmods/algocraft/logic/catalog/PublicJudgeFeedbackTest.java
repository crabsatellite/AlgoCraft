package com.crabmods.algocraft.logic.catalog;
import com.crabmods.algocraft.logic.SubmissionResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicJudgeFeedbackTest {
    private SubmissionResult result(String status) {
        SubmissionResult result = new SubmissionResult(); result.setMessage(status);
        var detail = new SubmissionResult.TestCaseResult(); detail.setInput("SECRET_INPUT"); detail.setExpected("SECRET_EXPECTED");
        detail.setActual("SECRET_OUTPUT"); detail.setError("ERROR: Syntax error at line 3");result.addDetail(detail);return result;
    }
    @Test void compilerDiagnosticRoundTripsWithoutInputsOrExpectedValues() {
        String wire = PublicJudgeFeedback.encode(result("Compilation Error"));
        assertTrue(wire.contains("Syntax error at line 3")); assertFalse(wire.contains("SECRET"));
        SubmissionResult client = new SubmissionResult(); PublicJudgeFeedback.decode(client,wire);
        assertEquals("Compilation Error",client.getMessage()); assertTrue(client.getDetails().get(0).getError().contains("Syntax error"));
        assertEquals("",client.getDetails().get(0).getInput()); assertEquals("",client.getDetails().get(0).getExpected());
    }
    @Test void hiddenRuntimeErrorsAndOutputsAreNotPublished() {
        assertEquals("Runtime Error",PublicJudgeFeedback.encode(result("Runtime Error")));
        assertEquals("Wrong Answer",PublicJudgeFeedback.encode(result("Wrong Answer")));
    }
    @Test void diagnosticsAreBounded() {
        var result = result("Compilation Error");result.getDetails().get(0).setError("x".repeat(10000));
        assertEquals(4096,PublicJudgeFeedback.encode(result).length());
    }
}
