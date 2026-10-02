package com.crabmods.algocraft.testengine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputComparatorTest {
    @Test
    void intArrayComparisonPreservesOrderByDefault() {
        assertTrue(OutputComparator.compare("[1,2,3]", "[1,2,3]", int[].class));
        assertFalse(OutputComparator.compare("[3,2,1]", "[1,2,3]", int[].class));
    }

    @Test
    void nestedArrayComparisonPreservesRowOrderByDefault() {
        assertTrue(OutputComparator.compare("[[1,2],[3,4]]", "[[1,2],[3,4]]", int[][].class));
        assertFalse(OutputComparator.compare("[[3,4],[1,2]]", "[[1,2],[3,4]]", int[][].class));
    }

    @Test
    void listComparisonPreservesOrderByDefault() {
        assertTrue(OutputComparator.compare("[\"a\",\"b\",\"c\"]", "[\"a\",\"b\",\"c\"]", List.class));
        assertFalse(OutputComparator.compare("[\"c\",\"b\",\"a\"]", "[\"a\",\"b\",\"c\"]", List.class));
    }
}
