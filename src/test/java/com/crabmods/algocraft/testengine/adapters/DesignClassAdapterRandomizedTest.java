package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.TestResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignClassAdapterRandomizedTest {
    @Test
    void randomizedSetGetRandomRejectsValuesOutsideTheCurrentSet() {
        TestResult result = DesignClassAdapter.run(BadRandomizedSet.class,
                randomizedSetInput(),
                "[null,true,false,true,2,true,false,2]");

        assertFalse(result.passed,
                "official design-class adapter must not skip getRandom or accept arbitrary non-null values");
    }

    @Test
    void randomizedSetGetRandomAcceptsAnyCurrentSetMember() {
        TestResult result = DesignClassAdapter.run(ValidRandomizedSet.class,
                randomizedSetInput(),
                "[null,true,false,true,2,true,false,2]");

        assertTrue(result.passed,
                "official design-class adapter should accept any value currently present in RandomizedSet");
    }

    @Test
    void randomizedCollectionGetRandomAcceptsAnyCurrentMultisetMember() {
        TestResult result = DesignClassAdapter.run(ValidRandomizedCollection.class,
                """
                        ["RandomizedCollection", "insert", "insert", "insert", "getRandom", "remove", "getRandom"]
                        [[], [1], [1], [2], [], [1], []]
                        """,
                "[null,true,false,true,2,true,1]");

        assertTrue(result.passed,
                "official design-class adapter should validate RandomizedCollection membership with duplicates");
    }

    private static String randomizedSetInput() {
        return """
                ["RandomizedSet", "insert", "remove", "insert", "getRandom", "remove", "insert", "getRandom"]
                [[], [1], [2], [2], [], [1], [2], []]
                """;
    }

    static class BadRandomizedSet {
        private final Set<Integer> values = new HashSet<>();

        public boolean insert(int val) {
            return values.add(val);
        }

        public boolean remove(int val) {
            return values.remove(val);
        }

        public int getRandom() {
            return 999;
        }
    }

    static class ValidRandomizedSet {
        private final Set<Integer> values = new HashSet<>();

        public boolean insert(int val) {
            return values.add(val);
        }

        public boolean remove(int val) {
            return values.remove(val);
        }

        public int getRandom() {
            return values.contains(1) ? 1 : values.iterator().next();
        }
    }

    static class ValidRandomizedCollection {
        private final List<Integer> values = new ArrayList<>();

        public boolean insert(int val) {
            boolean isNew = !values.contains(val);
            values.add(val);
            return isNew;
        }

        public boolean remove(int val) {
            return values.remove(Integer.valueOf(val));
        }

        public int getRandom() {
            return values.contains(1) ? 1 : values.getFirst();
        }
    }
}
