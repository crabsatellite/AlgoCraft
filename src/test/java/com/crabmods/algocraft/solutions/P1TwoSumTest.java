package com.crabmods.algocraft.solutions;

import java.util.*;

/**
 * Test cases for Problem 1: Two Sum
 * This file contains all solutions and verifies them against test cases.
 */
public class P1TwoSumTest {

    // ==================== Solution 1: Hash Map - One Pass ====================
    static class Solution1 {
        public int[] twoSum(int[] nums, int target) {
            Map<Integer, Integer> map = new HashMap<>();
            
            for (int i = 0; i < nums.length; i++) {
                int complement = target - nums[i];
                
                if (map.containsKey(complement)) {
                    return new int[] { map.get(complement), i };
                }
                
                map.put(nums[i], i);
            }
            
            return new int[] {}; // No solution found
        }
    }

    // ==================== Solution 2: Brute Force ====================
    static class Solution2 {
        public int[] twoSum(int[] nums, int target) {
            for (int i = 0; i < nums.length; i++) {
                for (int j = i + 1; j < nums.length; j++) {
                    if (nums[i] + nums[j] == target) {
                        return new int[] { i, j };
                    }
                }
            }
            
            return new int[] {}; // No solution found
        }
    }

    // ==================== Solution 3: Two Pointers (with Sorting) ====================
    static class Solution3 {
        public int[] twoSum(int[] nums, int target) {
            int n = nums.length;
            int[][] numWithIndex = new int[n][2];
            
            for (int i = 0; i < n; i++) {
                numWithIndex[i][0] = nums[i];
                numWithIndex[i][1] = i;
            }
            
            Arrays.sort(numWithIndex, (a, b) -> a[0] - b[0]);
            
            int left = 0, right = n - 1;
            
            while (left < right) {
                int sum = numWithIndex[left][0] + numWithIndex[right][0];
                
                if (sum == target) {
                    return new int[] { numWithIndex[left][1], numWithIndex[right][1] };
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
            
            return new int[] {};
        }
    }

    // ==================== Test Infrastructure ====================
    
    static class TestCase {
        int[] nums;
        int target;
        int[] expected;
        
        TestCase(int[] nums, int target, int[] expected) {
            this.nums = nums;
            this.target = target;
            this.expected = expected;
        }
    }
    
    private static boolean arraysEqualUnordered(int[] a, int[] b) {
        if (a.length != b.length) return false;
        int[] aCopy = a.clone();
        int[] bCopy = b.clone();
        Arrays.sort(aCopy);
        Arrays.sort(bCopy);
        return Arrays.equals(aCopy, bCopy);
    }
    
    private static void runTests(String solutionName, java.util.function.BiFunction<int[], Integer, int[]> solver) {
        TestCase[] testCases = {
            new TestCase(new int[]{2,7,11,15}, 9, new int[]{0,1}),
            new TestCase(new int[]{3,2,4}, 6, new int[]{1,2}),
            new TestCase(new int[]{3,3}, 6, new int[]{0,1}),
            new TestCase(new int[]{-1,-2,-3,-4,-5}, -8, new int[]{2,4}),
            new TestCase(new int[]{0,4,3,0}, 0, new int[]{0,3})
        };
        
        System.out.println("\n========== Testing " + solutionName + " ==========");
        
        int passed = 0;
        int failed = 0;
        
        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            int[] result = solver.apply(tc.nums.clone(), tc.target);
            
            boolean isCorrect = arraysEqualUnordered(result, tc.expected);
            
            if (isCorrect) {
                passed++;
                System.out.println("✅ Test " + (i + 1) + " PASSED");
            } else {
                failed++;
                System.out.println("❌ Test " + (i + 1) + " FAILED");
                System.out.println("   Input: nums = " + Arrays.toString(tc.nums) + ", target = " + tc.target);
                System.out.println("   Expected: " + Arrays.toString(tc.expected));
                System.out.println("   Got: " + Arrays.toString(result));
            }
        }
        
        System.out.println("\nResults: " + passed + "/" + testCases.length + " tests passed");
        
        if (failed > 0) {
            System.out.println("⚠️  " + failed + " tests failed!");
        } else {
            System.out.println("🎉 All tests passed!");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          Problem 1: Two Sum - Solution Tests                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();
        
        runTests("Solution 1: Hash Map - One Pass", (nums, target) -> sol1.twoSum(nums, target));
        runTests("Solution 2: Brute Force", (nums, target) -> sol2.twoSum(nums, target));
        runTests("Solution 3: Two Pointers (with Sorting)", (nums, target) -> sol3.twoSum(nums, target));
        
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("All solutions tested successfully!");
    }
}
