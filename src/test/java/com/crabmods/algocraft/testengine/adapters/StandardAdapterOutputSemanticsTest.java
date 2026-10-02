package com.crabmods.algocraft.testengine.adapters;

import com.crabmods.algocraft.testengine.TestResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardAdapterOutputSemanticsTest {
    @Test
    void acceptsZeroBasedTwoSumIndicesInEitherOrder() {
        TestResult result = StandardAdapter.run(ReversedTwoSum.class,
                "nums = [-1,-2,-3,-4,-5], target = -8",
                "[2,4]",
                "1");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void rejectsZeroBasedTwoSumIndicesForOneBasedTwoSumProblem() {
        TestResult result = StandardAdapter.run(ZeroBasedTwoSumForOneBasedProblem.class,
                "numbers = [2,7,11,15], target = 9",
                "[1,2]",
                "22");

        assertFalse(result.passed, result::toString);
    }

    @Test
    void acceptsOneBasedTwoSumIndicesForOneBasedTwoSumProblem() {
        TestResult result = StandardAdapter.run(OneBasedTwoSum.class,
                "numbers = [2,7,11,15], target = 9",
                "[1,2]",
                "22");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void acceptsGroupAnagramsWithReorderedGroupsAndMembers() {
        TestResult result = StandardAdapter.run(ReorderedGroupAnagrams.class,
                "strs = [\"eat\",\"tea\",\"tan\",\"ate\",\"nat\",\"bat\"]",
                "[[\"bat\"],[\"nat\",\"tan\"],[\"ate\",\"eat\",\"tea\"]]",
                "4");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void stillRejectsReorderedNestedArraysForOrderSensitiveMethods() {
        TestResult result = StandardAdapter.run(ReorderedInsertIntervals.class,
                "intervals = [[1,3],[6,9]], newInterval = [2,5]",
                "[[1,5],[6,9]]",
                "182");

        assertFalse(result.passed, result::toString);
    }

    @Test
    void prefersInputCompatibleEntrypointOverPublicHelperMethod() {
        TestResult result = StandardAdapter.run(PublicHelperBeforeEntrypoint.class,
                "nums = [2,7,11,15], target = 9",
                "[0,1]",
                "1");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void removeDuplicatesRejectsCorrectLengthWithoutMutatedPrefix() {
        TestResult result = StandardAdapter.run(RemoveDuplicatesReturnsLengthOnly.class,
                "nums = [1,1,2]",
                "2",
                "26");

        assertFalse(result.passed,
                "p26 official-bank adapter must verify nums[0..k), not only the returned k");
    }

    @Test
    void removeDuplicatesAcceptsReturnedLengthAndMutatedPrefix() {
        TestResult result = StandardAdapter.run(RemoveDuplicatesCompactsPrefix.class,
                "nums = [0,0,1,1,1,2,2,3,3,4]",
                "5",
                "26");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void removeElementRejectsCorrectLengthWithoutMutatedPrefix() {
        TestResult result = StandardAdapter.run(RemoveElementReturnsLengthOnly.class,
                "nums = [3,2,2,3], val = 3",
                "2",
                "27");

        assertFalse(result.passed,
                "p27 official-bank adapter must verify nums[0..k), not only the returned k");
    }

    @Test
    void removeElementAcceptsReturnedLengthAndUnorderedMutatedPrefix() {
        TestResult result = StandardAdapter.run(RemoveElementSwapsWithTail.class,
                "nums = [0,1,2,2,3,0,4,2], val = 2",
                "5",
                "27");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void alienDictionaryAcceptsAnyValidTopologicalOrder() {
        TestResult result = StandardAdapter.run(AlienDictionaryDifferentValidOrder.class,
                "words = [\"za\",\"zb\",\"ca\",\"cb\"]",
                "\"azbc\"",
                "146");

        assertTrue(result.passed, result::toString);
    }

    @Test
    void alienDictionaryRejectsMissingCharacters() {
        TestResult result = StandardAdapter.run(AlienDictionaryMissingCharacter.class,
                "words = [\"abc\"]",
                "\"abc\"",
                "146");

        assertFalse(result.passed,
                "p146 adapter must require exactly the distinct characters that appear in words");
    }

    @Test
    void alienDictionaryRejectsNonEmptyOrderForCycle() {
        TestResult result = StandardAdapter.run(AlienDictionaryAlwaysZx.class,
                "words = [\"z\",\"x\",\"z\"]",
                "\"\"",
                "146");

        assertFalse(result.passed,
                "p146 adapter must reject non-empty output when constraints form a cycle");
    }

    @Test
    void alienDictionaryRejectsNonEmptyOrderForInvalidPrefix() {
        TestResult result = StandardAdapter.run(AlienDictionaryAlwaysAbc.class,
                "words = [\"abc\",\"ab\"]",
                "\"\"",
                "146");

        assertFalse(result.passed,
                "p146 adapter must reject non-empty output for invalid prefix ordering");
    }

    @Test
    void crackingSafeAcceptsAnyMinimumDeBruijnSequence() {
        TestResult result = StandardAdapter.run(CrackingSafeDifferentValidSequence.class,
                "n = 2, k = 2",
                "\"01100\"",
                "376");

        assertTrue(result.passed,
                "p376 adapter must accept any minimum string that covers every length-n password");
    }

    @Test
    void crackingSafeRejectsShortOrIncompleteSequence() {
        TestResult result = StandardAdapter.run(CrackingSafeIncompleteSequence.class,
                "n = 2, k = 2",
                "\"01100\"",
                "376");

        assertFalse(result.passed,
                "p376 adapter must reject strings that do not cover every length-n password exactly once");
    }

    public static class ReversedTwoSum {
        public int[] twoSum(int[] nums, int target) {
            return new int[]{4, 2};
        }
    }

    public static class ZeroBasedTwoSumForOneBasedProblem {
        public int[] twoSum(int[] numbers, int target) {
            return new int[]{0, 1};
        }
    }

    public static class OneBasedTwoSum {
        public int[] twoSum(int[] numbers, int target) {
            return new int[]{1, 2};
        }
    }

    public static class ReorderedGroupAnagrams {
        public List<List<String>> groupAnagrams(String[] strs) {
            return List.of(
                    List.of("tan", "nat"),
                    List.of("bat"),
                    List.of("eat", "tea", "ate")
            );
        }
    }

    public static class ReorderedInsertIntervals {
        public int[][] insert(int[][] intervals, int[] newInterval) {
            return new int[][]{{6, 9}, {1, 5}};
        }
    }

    public static class PublicHelperBeforeEntrypoint {
        public int helper(int[] nums) {
            return 999;
        }

        int[] twoSum(int[] nums, int target) {
            return new int[]{0, 1};
        }
    }

    public static class RemoveDuplicatesReturnsLengthOnly {
        public int removeDuplicates(int[] nums) {
            return 2;
        }
    }

    public static class RemoveDuplicatesCompactsPrefix {
        public int removeDuplicates(int[] nums) {
            int write = 0;
            for (int value : nums) {
                if (write == 0 || nums[write - 1] != value) {
                    nums[write] = value;
                    write++;
                }
            }
            return write;
        }
    }

    public static class RemoveElementReturnsLengthOnly {
        public int removeElement(int[] nums, int val) {
            return 2;
        }
    }

    public static class RemoveElementSwapsWithTail {
        public int removeElement(int[] nums, int val) {
            int left = 0;
            int right = nums.length;
            while (left < right) {
                if (nums[left] == val) {
                    nums[left] = nums[right - 1];
                    right--;
                } else {
                    left++;
                }
            }
            return right;
        }
    }

    public static class AlienDictionaryDifferentValidOrder {
        public String alienOrder(String[] words) {
            return "zacb";
        }
    }

    public static class AlienDictionaryMissingCharacter {
        public String alienOrder(String[] words) {
            return "ab";
        }
    }

    public static class AlienDictionaryAlwaysZx {
        public String alienOrder(String[] words) {
            return "zx";
        }
    }

    public static class AlienDictionaryAlwaysAbc {
        public String alienOrder(String[] words) {
            return "abc";
        }
    }

    public static class CrackingSafeDifferentValidSequence {
        public String crackSafe(int n, int k) {
            return "00110";
        }
    }

    public static class CrackingSafeIncompleteSequence {
        public String crackSafe(int n, int k) {
            return "0011";
        }
    }
}
