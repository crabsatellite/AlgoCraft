package com.crabmods.algocraft.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeExecutorOutputEquivalenceTest {
    @Test
    void stringArrayParsingPreservesTrailingEmptyString() {
        String code = """
                class Solution {
                    public java.util.List<String> echo(String[] words) {
                        return java.util.Arrays.asList(words);
                    }
                }
                """;

        assertEquals("PASS", CodeExecutor.execute(code, "words = [\"a\",\"\"]", "[\"a\",\"\"]"));
    }

    @Test
    void nestedStringListComparisonAcceptsEmptyStringGroups() {
        String code = """
                class Solution {
                    public java.util.List<java.util.List<String>> groupAnagrams(String[] strs) {
                        java.util.Map<String, java.util.List<String>> groups = new java.util.HashMap<>();
                        for (String s : strs) {
                            char[] chars = s.toCharArray();
                            java.util.Arrays.sort(chars);
                            groups.computeIfAbsent(new String(chars), ignored -> new java.util.ArrayList<>()).add(s);
                        }
                        return new java.util.ArrayList<>(groups.values());
                    }
                }
                """;

        assertEquals("PASS", CodeExecutor.execute(code, "strs = [\"\", \"\", \"\"]", "[[\"\",\"\",\"\"]]"));
    }
}
