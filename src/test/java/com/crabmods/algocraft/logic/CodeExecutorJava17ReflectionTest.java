package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeExecutorJava17ReflectionTest {
    @Test void repeatedDesignMethodsAndConstructorsStayInsideTheSandbox() {
        String operations = "[\"Counter\"," + String.join(",", Collections.nCopies(40, "\"next\"")) + "]";
        String arguments = "[[0]," + String.join(",", Collections.nCopies(40, "[]")) + "]";
        var expected = new ArrayList<String>();
        expected.add("null");
        for (int i = 1; i <= 40; i++) expected.add(Integer.toString(i));
        String output = "[" + String.join(",", expected) + "]";
        var cases = Collections.nCopies(20, new CodeExecutor.TestCase(operations + "\n" + arguments, output));
        var results = CodeExecutor.executeBatch("""
                class Counter {
                    private int count;
                    public Counter(int initial) { count = initial; }
                    public int next() { return ++count; }
                }
                """, cases);
        assertTrue(results.size() == 20 && results.stream().allMatch(result -> result.passed),
                "Repeated reflective judge calls must pass without allowing JDK internal classes");
    }
}
