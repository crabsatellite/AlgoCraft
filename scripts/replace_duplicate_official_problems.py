#!/usr/bin/env python3
import json
from pathlib import Path

BASE = Path(__file__).resolve().parents[1] / "question_bank" / "official"
REPLACE_NUMBERS = [
    328, 308, 28, 23, 190, 309, 241, 317, 206, 318, 301, 345, 303, 469, 326,
    434, 436, 340, 484, 437, 339, 311, 307, 327, 483, 321, 360, 323, 316, 342,
    302, 474, 478, 492, 279, 278, 283, 286, 426, 453, 367, 370, 458, 432, 431,
    435, 433, 425, 424, 421, 422, 423, 427,
]


def t(inp, out):
    return {"input": inp, "output": out}


def s(name, code, time="O(n)", space="O(1)"):
    return {
        "name": name,
        "timeComplexity": time,
        "spaceComplexity": space,
        "description": "Reference implementation for this approach.",
        "code": code.strip(),
        "language": "java",
    }


def p(title, difficulty, signature, tags, summary, tests, solutions):
    return {
        "title": title,
        "description": f"# {title}\n\n{summary}",
        "difficulty": difficulty,
        "initialCode": f"class Solution {{\n    public {signature} {{\n        // Write your code here\n    }}\n}}",
        "tags": tags,
        "examples": tests[:2],
        "tests": tests,
        "solutions": solutions,
    }


PROBLEMS = [
    p("Longest Bounded Difference Window", "HARD", "int longestBoundedWindow(int[] nums, int limit)",
      ["Array", "Sliding Window", "Monotonic Queue"],
      "Return the length of the longest contiguous subarray whose maximum and minimum differ by at most limit.",
      [t("nums = [8,2,4,7], limit = 4", "2"), t("nums = [10,1,2,4,7,2], limit = 5", "4"),
       t("nums = [4,2,2,2,4,4,2,2], limit = 0", "3"), t("nums = [1,5,6,7,8,10,6,5,6], limit = 4", "5"),
       t("nums = [1], limit = 0", "1")],
      [s("Monotonic Deques", "class Solution { public int longestBoundedWindow(int[] nums, int limit) { Deque<Integer> max = new ArrayDeque<>(), min = new ArrayDeque<>(); int left = 0, best = 0; for (int right = 0; right < nums.length; right++) { while (!max.isEmpty() && nums[max.peekLast()] < nums[right]) max.pollLast(); while (!min.isEmpty() && nums[min.peekLast()] > nums[right]) min.pollLast(); max.addLast(right); min.addLast(right); while (nums[max.peekFirst()] - nums[min.peekFirst()] > limit) { if (max.peekFirst() == left) max.pollFirst(); if (min.peekFirst() == left) min.pollFirst(); left++; } best = Math.max(best, right - left + 1); } return best; } }", "O(n)", "O(n)"),
       s("TreeMap Window", "class Solution { public int longestBoundedWindow(int[] nums, int limit) { TreeMap<Integer,Integer> counts = new TreeMap<>(); int left = 0, best = 0; for (int right = 0; right < nums.length; right++) { counts.merge(nums[right], 1, Integer::sum); while (counts.lastKey() - counts.firstKey() > limit) { int value = nums[left++]; int next = counts.get(value) - 1; if (next == 0) counts.remove(value); else counts.put(value, next); } best = Math.max(best, right - left + 1); } return best; } }", "O(n log n)", "O(n)")]),

    p("Shortest Subarray Sum At Least Target", "HARD", "int shortestSubarray(int[] nums, int k)",
      ["Array", "Prefix Sum", "Monotonic Queue"],
      "The array can contain negative numbers. Return the shortest non-empty subarray length with sum at least k, or -1.",
      [t("nums = [1], k = 1", "1"), t("nums = [1,2], k = 4", "-1"), t("nums = [2,-1,2], k = 3", "3"),
       t("nums = [84,-37,32,40,95], k = 167", "3"), t("nums = [17,85,93,-45,-21], k = 150", "2")],
      [s("Prefix Deque", "class Solution { public int shortestSubarray(int[] nums, int k) { long[] pre = new long[nums.length + 1]; for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i]; Deque<Integer> dq = new ArrayDeque<>(); int best = nums.length + 1; for (int i = 0; i < pre.length; i++) { while (!dq.isEmpty() && pre[i] - pre[dq.peekFirst()] >= k) best = Math.min(best, i - dq.pollFirst()); while (!dq.isEmpty() && pre[i] <= pre[dq.peekLast()]) dq.pollLast(); dq.addLast(i); } return best == nums.length + 1 ? -1 : best; } }", "O(n)", "O(n)"),
       s("Quadratic Prefix Scan", "class Solution { public int shortestSubarray(int[] nums, int k) { long[] pre = new long[nums.length + 1]; for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i]; int best = nums.length + 1; for (int l = 0; l < nums.length; l++) for (int r = l + 1; r <= nums.length; r++) if (pre[r] - pre[l] >= k) { best = Math.min(best, r - l); break; } return best == nums.length + 1 ? -1 : best; } }", "O(n^2)", "O(n)")]),

    p("Count Equal Value Pairs", "EASY", "int countEqualPairs(int[] nums)", ["Array", "Hash Table", "Counting"],
      "Count pairs of indices i < j where the two values are equal.",
      [t("nums = [1,2,3,1,1,3]", "4"), t("nums = [1,1,1,1]", "6"), t("nums = [1,2,3]", "0"),
       t("nums = [-1,-1,2,-1]", "3"), t("nums = []", "0")],
      [s("Frequency Counting", "class Solution { public int countEqualPairs(int[] nums) { Map<Integer,Integer> seen = new HashMap<>(); int pairs = 0; for (int x : nums) { pairs += seen.getOrDefault(x, 0); seen.put(x, seen.getOrDefault(x, 0) + 1); } return pairs; } }", "O(n)", "O(n)"),
       s("Nested Pair Scan", "class Solution { public int countEqualPairs(int[] nums) { int pairs = 0; for (int i = 0; i < nums.length; i++) for (int j = i + 1; j < nums.length; j++) if (nums[i] == nums[j]) pairs++; return pairs; } }", "O(n^2)", "O(1)")]),

    p("Count Triplets With Smaller Sum", "MEDIUM", "int threeSumSmaller(int[] nums, int target)",
      ["Array", "Two Pointers", "Sorting"],
      "Return the number of index triplets whose values sum to less than target.",
      [t("nums = [-2,0,1,3], target = 2", "2"), t("nums = [0,0,0], target = 1", "1"),
       t("nums = [1,1,-2], target = 1", "1"), t("nums = [3,1,0,-2], target = 4", "3"),
       t("nums = [5,1,3,4,7], target = 12", "4")],
      [s("Sorted Two Pointers", "class Solution { public int threeSumSmaller(int[] nums, int target) { Arrays.sort(nums); int count = 0; for (int i = 0; i < nums.length - 2; i++) { int l = i + 1, r = nums.length - 1; while (l < r) { if (nums[i] + nums[l] + nums[r] < target) { count += r - l; l++; } else r--; } } return count; } }", "O(n^2)", "O(1)"),
       s("Brute Force Triplets", "class Solution { public int threeSumSmaller(int[] nums, int target) { int count = 0; for (int i = 0; i < nums.length; i++) for (int j = i + 1; j < nums.length; j++) for (int k = j + 1; k < nums.length; k++) if (nums[i] + nums[j] + nums[k] < target) count++; return count; } }", "O(n^3)", "O(1)")]),

    p("Minimum Right Rotations to Sort", "MEDIUM", "int minimumRightRotations(int[] nums)",
      ["Array", "Simulation"],
      "Return the fewest right rotations needed to make the array nondecreasing, or -1 if impossible.",
      [t("nums = [3,4,5,1,2]", "2"), t("nums = [1,3,5]", "0"), t("nums = [2,1,4]", "-1"),
       t("nums = [2,3,4,1]", "1"), t("nums = [1,1,2]", "0")],
      [s("Count Descents", "class Solution { public int minimumRightRotations(int[] nums) { int n = nums.length, drop = -1; for (int i = 0; i < n; i++) if (nums[i] > nums[(i + 1) % n]) { if (drop != -1) return -1; drop = i; } return drop == -1 ? 0 : n - drop - 1; } }"),
       s("Try Every Rotation", "class Solution { public int minimumRightRotations(int[] nums) { int n = nums.length; for (int rot = 0; rot < n; rot++) { boolean ok = true; for (int i = 1; i < n; i++) { int prev = nums[(i - 1 - rot + n) % n], cur = nums[(i - rot + n) % n]; if (prev > cur) { ok = false; break; } } if (ok) return rot; } return -1; } }", "O(n^2)", "O(1)")]),

    p("Maximum Submatrix Sum No More Than K", "HARD", "int maxSumSubmatrix(int[][] matrix, int k)",
      ["Array", "Matrix", "Prefix Sum", "Binary Search"],
      "Find the largest rectangular submatrix sum that is at most k.",
      [t("matrix = [[1,0,1],[0,-2,3]], k = 2", "2"), t("matrix = [[2,2,-1]], k = 3", "3"),
       t("matrix = [[5]], k = 5", "5"), t("matrix = [[2,2,-1]], k = 0", "-1"),
       t("matrix = [[1,2],[3,4]], k = 8", "7")],
      [s("Column Compression and TreeSet", "class Solution { public int maxSumSubmatrix(int[][] matrix, int k) { int rows = matrix.length, cols = matrix[0].length, best = Integer.MIN_VALUE; for (int left = 0; left < cols; left++) { int[] sums = new int[rows]; for (int right = left; right < cols; right++) { for (int r = 0; r < rows; r++) sums[r] += matrix[r][right]; TreeSet<Integer> set = new TreeSet<>(); set.add(0); int prefix = 0; for (int v : sums) { prefix += v; Integer ceil = set.ceiling(prefix - k); if (ceil != null) best = Math.max(best, prefix - ceil); set.add(prefix); } } } return best; } }", "O(c^2*r log r)", "O(r)"),
       s("Brute Force Rectangles", "class Solution { public int maxSumSubmatrix(int[][] matrix, int k) { int m = matrix.length, n = matrix[0].length, best = Integer.MIN_VALUE; int[][] pre = new int[m + 1][n + 1]; for (int r = 0; r < m; r++) for (int c = 0; c < n; c++) pre[r + 1][c + 1] = matrix[r][c] + pre[r][c + 1] + pre[r + 1][c] - pre[r][c]; for (int r1 = 0; r1 < m; r1++) for (int c1 = 0; c1 < n; c1++) for (int r2 = r1; r2 < m; r2++) for (int c2 = c1; c2 < n; c2++) { int sum = pre[r2 + 1][c2 + 1] - pre[r1][c2 + 1] - pre[r2 + 1][c1] + pre[r1][c1]; if (sum <= k) best = Math.max(best, sum); } return best; } }", "O(m^2*n^2)", "O(mn)")]),

    p("Count Negative Cells in Sorted Grid", "EASY", "int countNegatives(int[][] grid)",
      ["Array", "Matrix", "Binary Search"],
      "Rows and columns are sorted in non-increasing order. Count the negative values.",
      [t("grid = [[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]]", "8"), t("grid = [[3,2],[1,0]]", "0"),
       t("grid = [[-1]]", "1"), t("grid = [[5,1,0],[-1,-2,-3]]", "3"), t("grid = [[1,-1],[-1,-1]]", "3")],
      [s("Staircase Scan", "class Solution { public int countNegatives(int[][] grid) { int r = grid.length - 1, c = 0, ans = 0; while (r >= 0 && c < grid[0].length) { if (grid[r][c] < 0) { ans += grid[0].length - c; r--; } else c++; } return ans; } }", "O(m+n)", "O(1)"),
       s("Binary Search Per Row", "class Solution { public int countNegatives(int[][] grid) { int ans = 0; for (int[] row : grid) { int l = 0, r = row.length; while (l < r) { int mid = (l + r) >>> 1; if (row[mid] < 0) r = mid; else l = mid + 1; } ans += row.length - l; } return ans; } }", "O(m log n)", "O(1)")]),

    p("Count Subarrays With Fixed Bounds", "HARD", "long countSubarrays(int[] nums, int minK, int maxK)",
      ["Array", "Sliding Window"],
      "Count subarrays whose minimum is minK and maximum is maxK.",
      [t("nums = [1,3,5,2,7,5], minK = 1, maxK = 5", "2"), t("nums = [1,1,1,1], minK = 1, maxK = 1", "10"),
       t("nums = [2,1,3,5,4], minK = 1, maxK = 5", "2"), t("nums = [1,5,1,5], minK = 1, maxK = 5", "6"),
       t("nums = [2,2,2], minK = 1, maxK = 2", "0")],
      [s("Last Seen Bounds", "class Solution { public long countSubarrays(int[] nums, int minK, int maxK) { long ans = 0; int bad = -1, lastMin = -1, lastMax = -1; for (int i = 0; i < nums.length; i++) { if (nums[i] < minK || nums[i] > maxK) bad = i; if (nums[i] == minK) lastMin = i; if (nums[i] == maxK) lastMax = i; ans += Math.max(0, Math.min(lastMin, lastMax) - bad); } return ans; } }", "O(n)", "O(1)"),
       s("Enumerate Starts", "class Solution { public long countSubarrays(int[] nums, int minK, int maxK) { long ans = 0; for (int l = 0; l < nums.length; l++) { int mn = Integer.MAX_VALUE, mx = Integer.MIN_VALUE; for (int r = l; r < nums.length; r++) { mn = Math.min(mn, nums[r]); mx = Math.max(mx, nums[r]); if (mn < minK || mx > maxK) break; if (mn == minK && mx == maxK) ans++; } } return ans; } }", "O(n^2)", "O(1)")]),

    p("Maximum Width Ramp", "MEDIUM", "int maxWidthRamp(int[] nums)", ["Array", "Monotonic Stack"],
      "Return the largest j - i with i < j and nums[i] <= nums[j].",
      [t("nums = [6,0,8,2,1,5]", "4"), t("nums = [9,8,1,0,1,9,4,0,4,1]", "7"),
       t("nums = [1,2,3,4]", "3"), t("nums = [4,3,2,1]", "0"), t("nums = [2,2,2]", "2")],
      [s("Monotonic Candidate Stack", "class Solution { public int maxWidthRamp(int[] nums) { Deque<Integer> st = new ArrayDeque<>(); for (int i = 0; i < nums.length; i++) if (st.isEmpty() || nums[i] < nums[st.peek()]) st.push(i); int best = 0; for (int j = nums.length - 1; j >= 0; j--) while (!st.isEmpty() && nums[st.peek()] <= nums[j]) best = Math.max(best, j - st.pop()); return best; } }", "O(n)", "O(n)"),
       s("Brute Force Pairs", "class Solution { public int maxWidthRamp(int[] nums) { int best = 0; for (int i = 0; i < nums.length; i++) for (int j = i + 1; j < nums.length; j++) if (nums[i] <= nums[j]) best = Math.max(best, j - i); return best; } }", "O(n^2)", "O(1)")]),

    p("Maximum Score From Multipliers", "HARD", "int maximumScore(int[] nums, int[] multipliers)",
      ["Array", "Dynamic Programming"],
      "At each step choose the leftmost or rightmost number and multiply it by the current multiplier. Maximize the total.",
      [t("nums = [1,2,3], multipliers = [3,2,1]", "14"), t("nums = [-5,-3,-3,-2,7,1], multipliers = [-10,-5,3,4,6]", "102"),
       t("nums = [1], multipliers = [5]", "5"), t("nums = [2,3,1], multipliers = [3,1]", "9"),
       t("nums = [4,-2,5], multipliers = [-3,2]", "-2")],
      [s("Bottom Up DP", "class Solution { public int maximumScore(int[] nums, int[] multipliers) { int m = multipliers.length, n = nums.length; int[][] dp = new int[m + 1][m + 1]; for (int i = m - 1; i >= 0; i--) for (int left = i; left >= 0; left--) { int right = n - 1 - (i - left); dp[i][left] = Math.max(multipliers[i] * nums[left] + dp[i + 1][left + 1], multipliers[i] * nums[right] + dp[i + 1][left]); } return dp[0][0]; } }", "O(m^2)", "O(m^2)"),
       s("Memoized Choice", "class Solution { int[] nums, mult; Integer[][] memo; public int maximumScore(int[] nums, int[] multipliers) { this.nums = nums; this.mult = multipliers; memo = new Integer[multipliers.length][multipliers.length + 1]; return dfs(0, 0); } int dfs(int i, int left) { if (i == mult.length) return 0; if (memo[i][left] != null) return memo[i][left]; int right = nums.length - 1 - (i - left); int a = nums[left] * mult[i] + dfs(i + 1, left + 1); int b = nums[right] * mult[i] + dfs(i + 1, left); return memo[i][left] = Math.max(a, b); } }", "O(m^2)", "O(m^2)")]),

    p("Minimum Bouquet Days", "HARD", "int minDays(int[] bloomDay, int m, int k)",
      ["Array", "Binary Search"],
      "Each bouquet requires k adjacent bloomed flowers. Return the earliest day to make m bouquets, or -1.",
      [t("bloomDay = [1,10,3,10,2], m = 3, k = 1", "3"), t("bloomDay = [1,10,3,10,2], m = 3, k = 2", "-1"),
       t("bloomDay = [7,7,7,7,12,7,7], m = 2, k = 3", "12"), t("bloomDay = [1000000000,1000000000], m = 1, k = 1", "1000000000"),
       t("bloomDay = [1,2,4,9,3,4,1], m = 2, k = 2", "4")],
      [s("Binary Search Day", "class Solution { public int minDays(int[] bloomDay, int m, int k) { if ((long)m * k > bloomDay.length) return -1; int lo = 1, hi = 0; for (int d : bloomDay) hi = Math.max(hi, d); while (lo < hi) { int mid = lo + (hi - lo) / 2; if (can(bloomDay, m, k, mid)) hi = mid; else lo = mid + 1; } return lo; } boolean can(int[] days, int m, int k, int day) { int made = 0, run = 0; for (int d : days) { run = d <= day ? run + 1 : 0; if (run == k) { made++; run = 0; } } return made >= m; } }", "O(n log M)", "O(1)"),
       s("Sorted Candidate Days", "class Solution { public int minDays(int[] bloomDay, int m, int k) { if ((long)m * k > bloomDay.length) return -1; int[] days = bloomDay.clone(); Arrays.sort(days); for (int day : days) { int made = 0, run = 0; for (int b : bloomDay) { run = b <= day ? run + 1 : 0; if (run == k) { made++; run = 0; } } if (made >= m) return day; } return -1; } }", "O(n^2 log n)", "O(n)")]),

    p("Minimum Cost to Connect Sticks", "MEDIUM", "int connectSticks(int[] sticks)",
      ["Array", "Greedy", "Heap (Priority Queue)"],
      "Each operation connects two sticks and costs their combined length. Return the minimum total cost.",
      [t("sticks = [2,4,3]", "14"), t("sticks = [1,8,3,5]", "30"), t("sticks = [5]", "0"),
       t("sticks = [1,1,1,1]", "8"), t("sticks = [10,20,30]", "90")],
      [s("Min Heap Greedy", "class Solution { public int connectSticks(int[] sticks) { PriorityQueue<Integer> pq = new PriorityQueue<>(); for (int x : sticks) pq.add(x); int cost = 0; while (pq.size() > 1) { int sum = pq.poll() + pq.poll(); cost += sum; pq.add(sum); } return cost; } }", "O(n log n)", "O(n)"),
       s("Repeated Sorting", "class Solution { public int connectSticks(int[] sticks) { List<Integer> list = new ArrayList<>(); for (int x : sticks) list.add(x); int cost = 0; while (list.size() > 1) { Collections.sort(list); int sum = list.remove(0) + list.remove(0); cost += sum; list.add(sum); } return cost; } }", "O(n^2 log n)", "O(n)")]),

    p("Count All-One Submatrices", "HARD", "int numSubmat(int[][] mat)",
      ["Array", "Matrix", "Dynamic Programming", "Stack"],
      "Count rectangular submatrices whose cells are all 1.",
      [t("mat = [[1,0,1],[1,1,0],[1,1,0]]", "13"), t("mat = [[0,1,1,0],[0,1,1,1],[1,1,1,0]]", "24"),
       t("mat = [[1,1],[1,1]]", "9"), t("mat = [[0]]", "0"), t("mat = [[1,0,1]]", "2")],
      [s("Height Expansion", "class Solution { public int numSubmat(int[][] mat) { int m = mat.length, n = mat[0].length, ans = 0; int[] h = new int[n]; for (int r = 0; r < m; r++) { for (int c = 0; c < n; c++) h[c] = mat[r][c] == 0 ? 0 : h[c] + 1; for (int c = 0; c < n; c++) { int min = h[c]; for (int left = c; left >= 0 && min > 0; left--) { min = Math.min(min, h[left]); ans += min; } } } return ans; } }", "O(mn^2)", "O(n)"),
       s("Prefix Brute Force", "class Solution { public int numSubmat(int[][] mat) { int m = mat.length, n = mat[0].length, ans = 0; int[][] zero = new int[m + 1][n + 1]; for (int r = 0; r < m; r++) for (int c = 0; c < n; c++) zero[r + 1][c + 1] = (mat[r][c] == 0 ? 1 : 0) + zero[r][c + 1] + zero[r + 1][c] - zero[r][c]; for (int r1 = 0; r1 < m; r1++) for (int c1 = 0; c1 < n; c1++) for (int r2 = r1; r2 < m; r2++) for (int c2 = c1; c2 < n; c2++) if (zero[r2 + 1][c2 + 1] - zero[r1][c2 + 1] - zero[r2 + 1][c1] + zero[r1][c1] == 0) ans++; return ans; } }", "O(m^2n^2)", "O(mn)")]),

    p("Minimum Falling Path Sum Without Same Column", "HARD", "int minFallingPathSum(int[][] grid)",
      ["Array", "Matrix", "Dynamic Programming"],
      "Pick one value per row. Adjacent rows cannot use the same column. Return the minimum possible sum.",
      [t("grid = [[1,2,3],[4,5,6],[7,8,9]]", "13"), t("grid = [[7]]", "7"),
       t("grid = [[2,2,1],[3,4,5],[6,1,2]]", "5"), t("grid = [[-73,61,43],[-48,-36,10],[100,-90,-37]]", "-199"),
       t("grid = [[1,99],[99,1]]", "2")],
      [s("Track Two Minimums", "class Solution { public int minFallingPathSum(int[][] grid) { int n = grid.length; int[] dp = grid[0].clone(); for (int r = 1; r < n; r++) { int a = -1, b = -1; for (int c = 0; c < n; c++) if (a == -1 || dp[c] < dp[a]) { b = a; a = c; } else if (b == -1 || dp[c] < dp[b]) b = c; int[] next = new int[n]; for (int c = 0; c < n; c++) next[c] = grid[r][c] + dp[c == a ? b : a]; dp = next; } int best = Integer.MAX_VALUE; for (int v : dp) best = Math.min(best, v); return best; } }", "O(n^2)", "O(n)"),
       s("Quadratic DP", "class Solution { public int minFallingPathSum(int[][] grid) { int n = grid.length; int[] dp = grid[0].clone(); for (int r = 1; r < n; r++) { int[] next = new int[n]; Arrays.fill(next, Integer.MAX_VALUE / 4); for (int c = 0; c < n; c++) for (int pc = 0; pc < n; pc++) if (pc != c) next[c] = Math.min(next[c], dp[pc] + grid[r][c]); dp = next; } int best = Integer.MAX_VALUE; for (int v : dp) best = Math.min(best, v); return best; } }", "O(n^3)", "O(n)")]),

    p("Word Pattern Bijection", "MEDIUM", "boolean wordPattern(String pattern, String s)",
      ["Hash Table", "String"],
      "Return whether pattern characters and sentence words form a one-to-one mapping.",
      [t("pattern = \"abba\", s = \"dog cat cat dog\"", "true"), t("pattern = \"abba\", s = \"dog cat cat fish\"", "false"),
       t("pattern = \"aaaa\", s = \"dog cat cat dog\"", "false"), t("pattern = \"abba\", s = \"dog dog dog dog\"", "false"),
       t("pattern = \"abc\", s = \"one two three\"", "true")],
      [s("Two Maps", "class Solution { public boolean wordPattern(String pattern, String s) { String[] words = s.split(\" \"); if (words.length != pattern.length()) return false; Map<Character,String> a = new HashMap<>(); Map<String,Character> b = new HashMap<>(); for (int i = 0; i < words.length; i++) { char ch = pattern.charAt(i); String w = words[i]; if (a.containsKey(ch) && !a.get(ch).equals(w)) return false; if (b.containsKey(w) && b.get(w) != ch) return false; a.put(ch, w); b.put(w, ch); } return true; } }", "O(n)", "O(n)"),
       s("Encoded First Positions", "class Solution { public boolean wordPattern(String pattern, String s) { String[] words = s.split(\" \"); if (words.length != pattern.length()) return false; Map<Object,Integer> first = new HashMap<>(); for (int i = 0; i < words.length; i++) { Integer a = first.putIfAbsent(pattern.charAt(i), i); Integer b = first.putIfAbsent(words[i], i); if (!Objects.equals(a, b)) return false; } return true; } }", "O(n)", "O(n)")]),
]

PROBLEMS.extend([
    p("Shortest Alternating Color Paths", "MEDIUM", "int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges)",
      ["Graph", "Breadth-First Search", "Shortest Path"],
      "Return shortest distances from node 0 when edge colors must alternate between red and blue.",
      [t("n = 3, redEdges = [[0,1],[1,2]], blueEdges = []", "[0,1,-1]"), t("n = 3, redEdges = [[0,1]], blueEdges = [[1,2]]", "[0,1,2]"),
       t("n = 3, redEdges = [[1,0]], blueEdges = [[2,1]]", "[0,-1,-1]"), t("n = 4, redEdges = [[0,1],[1,2]], blueEdges = [[0,2],[2,3]]", "[0,1,1,2]"),
       t("n = 1, redEdges = [], blueEdges = []", "[0]")],
      [s("BFS With Last Color", "class Solution { public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) { List<Integer>[][] g = new ArrayList[2][n]; for (int c = 0; c < 2; c++) for (int i = 0; i < n; i++) g[c][i] = new ArrayList<>(); for (int[] e : redEdges) g[0][e[0]].add(e[1]); for (int[] e : blueEdges) g[1][e[0]].add(e[1]); int[][] d = new int[2][n]; for (int[] row : d) Arrays.fill(row, -1); Queue<int[]> q = new ArrayDeque<>(); q.add(new int[]{0,0}); q.add(new int[]{0,1}); d[0][0] = d[1][0] = 0; while (!q.isEmpty()) { int[] cur = q.poll(); int node = cur[0], color = cur[1], nextColor = 1 - color; for (int next : g[nextColor][node]) if (d[nextColor][next] == -1) { d[nextColor][next] = d[color][node] + 1; q.add(new int[]{next,nextColor}); } } int[] ans = new int[n]; for (int i = 0; i < n; i++) ans[i] = d[0][i] == -1 ? d[1][i] : d[1][i] == -1 ? d[0][i] : Math.min(d[0][i], d[1][i]); return ans; } }", "O(n+e)", "O(n+e)"),
       s("Layered Graph BFS", "class Solution { public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) { List<Integer>[] g = new ArrayList[2*n]; for (int i = 0; i < 2*n; i++) g[i] = new ArrayList<>(); for (int[] e : redEdges) g[n + e[0]].add(e[1]); for (int[] e : blueEdges) g[e[0]].add(n + e[1]); int[] d = new int[2*n]; Arrays.fill(d, -1); Queue<Integer> q = new ArrayDeque<>(); q.add(0); q.add(n); d[0] = d[n] = 0; while (!q.isEmpty()) { int cur = q.poll(); for (int next : g[cur]) if (d[next] == -1) { d[next] = d[cur] + 1; q.add(next); } } int[] ans = new int[n]; for (int i = 0; i < n; i++) ans[i] = d[i] == -1 ? d[n+i] : d[n+i] == -1 ? d[i] : Math.min(d[i], d[n+i]); return ans; } }", "O(n+e)", "O(n+e)")]),

    p("Count Words With Prefix", "EASY", "int prefixCount(String[] words, String pref)",
      ["Array", "String"],
      "Return how many words start with the given prefix.",
      [t("words = [\"pay\",\"attention\",\"practice\",\"attend\"], pref = \"at\"", "2"), t("words = [\"leetcode\",\"win\",\"loops\",\"success\"], pref = \"code\"", "0"),
       t("words = [\"a\",\"ab\",\"abc\"], pref = \"a\"", "3"), t("words = [\"dog\",\"cat\"], pref = \"z\"", "0"),
       t("words = [], pref = \"a\"", "0")],
      [s("StartsWith Scan", "class Solution { public int prefixCount(String[] words, String pref) { int count = 0; for (String w : words) if (w.startsWith(pref)) count++; return count; } }"),
       s("Manual Character Check", "class Solution { public int prefixCount(String[] words, String pref) { int ans = 0; outer: for (String w : words) { if (w.length() < pref.length()) continue; for (int i = 0; i < pref.length(); i++) if (w.charAt(i) != pref.charAt(i)) continue outer; ans++; } return ans; } }")]),

    p("Pair Words By Reversal", "EASY", "int maximumNumberOfStringPairs(String[] words)",
      ["Array", "Hash Table", "String"],
      "Count pairs of two-letter words where one word is the reverse of the other.",
      [t("words = [\"cd\",\"ac\",\"dc\",\"ca\",\"zz\"]", "2"), t("words = [\"ab\",\"ba\",\"cc\"]", "1"),
       t("words = [\"aa\",\"ab\"]", "0"), t("words = [\"ab\",\"ba\",\"ab\",\"ba\"]", "2"), t("words = []", "0")],
      [s("Hash Set Reverse Lookup", "class Solution { public int maximumNumberOfStringPairs(String[] words) { Set<String> seen = new HashSet<>(); int pairs = 0; for (String w : words) { String r = new StringBuilder(w).reverse().toString(); if (seen.remove(r)) pairs++; else seen.add(w); } return pairs; } }", "O(n)", "O(n)"),
       s("Nested Pair Check", "class Solution { public int maximumNumberOfStringPairs(String[] words) { int ans = 0; for (int i = 0; i < words.length; i++) for (int j = i + 1; j < words.length; j++) if (words[i].charAt(0) == words[j].charAt(1) && words[i].charAt(1) == words[j].charAt(0)) ans++; return ans; } }", "O(n^2)", "O(1)")]),

    p("Count Length-Three Palindromic Subsequences", "MEDIUM", "int countPalindromicSubsequence(String s)",
      ["Hash Table", "String", "Prefix Sum"],
      "Return the number of distinct palindromic subsequences of length three.",
      [t("s = \"aabca\"", "3"), t("s = \"adc\"", "0"), t("s = \"bbcbaba\"", "4"),
       t("s = \"aaaa\"", "1"), t("s = \"abcba\"", "3")],
      [s("First Last Window", "class Solution { public int countPalindromicSubsequence(String s) { int ans = 0; for (char ch = 'a'; ch <= 'z'; ch++) { int l = s.indexOf(ch), r = s.lastIndexOf(ch); if (l >= 0 && l < r) { Set<Character> mid = new HashSet<>(); for (int i = l + 1; i < r; i++) mid.add(s.charAt(i)); ans += mid.size(); } } return ans; } }", "O(26n)", "O(1)"),
       s("Prefix Presence", "class Solution { public int countPalindromicSubsequence(String s) { int n = s.length(); boolean[][] left = new boolean[n][26], right = new boolean[n][26]; for (int i = 1; i < n; i++) { left[i] = left[i - 1].clone(); left[i][s.charAt(i - 1) - 'a'] = true; } for (int i = n - 2; i >= 0; i--) { right[i] = right[i + 1].clone(); right[i][s.charAt(i + 1) - 'a'] = true; } boolean[][] seen = new boolean[26][26]; int ans = 0; for (int i = 0; i < n; i++) for (int c = 0; c < 26; c++) if (left[i][c] && right[i][c] && !seen[s.charAt(i)-'a'][c]) { seen[s.charAt(i)-'a'][c] = true; ans++; } return ans; } }", "O(26n)", "O(26n)")]),

    p("Minimum Window Subsequence", "HARD", "String minWindow(String s, String t)",
      ["String", "Dynamic Programming", "Two Pointers"],
      "Return the shortest substring of s that contains t as a subsequence, or the empty string.",
      [t("s = \"abcdebdde\", t = \"bde\"", "\"bcde\""), t("s = \"abc\", t = \"ac\"", "\"abc\""),
       t("s = \"abc\", t = \"d\"", "\"\""), t("s = \"abdbde\", t = \"bde\"", "\"bde\""),
       t("s = \"fgrqsqsnodwmxzkzxwqegkndaa\", t = \"kzed\"", "\"kzxwqegknd\"")],
      [s("Forward Backward Scan", "class Solution { public String minWindow(String s, String t) { int bestStart = -1, bestLen = Integer.MAX_VALUE, i = 0; while (i < s.length()) { int j = 0; while (i < s.length()) { if (s.charAt(i) == t.charAt(j) && ++j == t.length()) break; i++; } if (i == s.length()) break; int end = i + 1; j = t.length() - 1; while (i >= 0) { if (s.charAt(i) == t.charAt(j) && --j < 0) break; i--; } if (end - i < bestLen) { bestLen = end - i; bestStart = i; } i++; } return bestStart == -1 ? \"\" : s.substring(bestStart, bestStart + bestLen); } }", "O(st)", "O(1)"),
       s("Next Occurrence Table", "class Solution { public String minWindow(String s, String t) { int n = s.length(); int[][] next = new int[n + 1][128]; Arrays.fill(next[n], -1); for (int i = n - 1; i >= 0; i--) { next[i] = next[i + 1].clone(); next[i][s.charAt(i)] = i; } String best = \"\"; for (int start = 0; start < n; start++) { int pos = start; boolean ok = true; for (char ch : t.toCharArray()) { if (pos > n || next[pos][ch] == -1) { ok = false; break; } pos = next[pos][ch] + 1; } if (ok) { String cand = s.substring(start, pos); if (best.isEmpty() || cand.length() < best.length()) best = cand; } } return best; } }", "O(128s+st)", "O(128s)")]),

    p("Minimum Jumps to Target With Forbidden", "HARD", "int minimumJumps(int[] forbidden, int a, int b, int x)",
      ["Array", "Breadth-First Search"],
      "Starting at 0, jump forward by a or backward by b. Forbidden positions are blocked, and two backward jumps cannot be consecutive.",
      [t("forbidden = [14,4,18,1,15], a = 3, b = 15, x = 9", "3"), t("forbidden = [8,3,16,6,12,20], a = 15, b = 13, x = 11", "-1"),
       t("forbidden = [1,6,2,14,5,17,4], a = 16, b = 9, x = 7", "2"), t("forbidden = [], a = 2, b = 1, x = 4", "2"),
       t("forbidden = [2], a = 2, b = 1, x = 1", "-1")],
      [s("BFS With Direction State", "class Solution { public int minimumJumps(int[] forbidden, int a, int b, int x) { Set<Integer> bad = new HashSet<>(); int far = x; for (int f : forbidden) { bad.add(f); far = Math.max(far, f); } int limit = far + a + b + 2000; boolean[][] seen = new boolean[limit + 1][2]; Queue<int[]> q = new ArrayDeque<>(); q.add(new int[]{0,0,0}); seen[0][0] = true; while (!q.isEmpty()) { int[] cur = q.poll(); int pos = cur[0], back = cur[1], dist = cur[2]; if (pos == x) return dist; int next = pos + a; if (next <= limit && !bad.contains(next) && !seen[next][0]) { seen[next][0] = true; q.add(new int[]{next,0,dist+1}); } next = pos - b; if (back == 0 && next >= 0 && !bad.contains(next) && !seen[next][1]) { seen[next][1] = true; q.add(new int[]{next,1,dist+1}); } } return -1; } }", "O(limit)", "O(limit)"),
       s("Level BFS", "class Solution { public int minimumJumps(int[] forbidden, int a, int b, int x) { Set<Integer> bad = new HashSet<>(); int cap = x + a + b + 6000; for (int f : forbidden) { bad.add(f); cap = Math.max(cap, f + a + b + 100); } Set<String> seen = new HashSet<>(); Queue<int[]> q = new ArrayDeque<>(); q.offer(new int[]{0,0}); seen.add(\"0,0\"); for (int steps = 0; !q.isEmpty(); steps++) { for (int size = q.size(); size > 0; size--) { int[] cur = q.poll(); if (cur[0] == x) return steps; int next = cur[0] + a; if (next <= cap && !bad.contains(next) && seen.add(next + \",0\")) q.offer(new int[]{next,0}); next = cur[0] - b; if (cur[1] == 0 && next >= 0 && !bad.contains(next) && seen.add(next + \",1\")) q.offer(new int[]{next,1}); } } return -1; } }", "O(limit)", "O(limit)")]),

    p("Count Good Binary Strings", "MEDIUM", "int countGoodStrings(int low, int high, int zero, int one)",
      ["Dynamic Programming"],
      "Append blocks of zero zeroes or one ones. Count constructible lengths from low through high modulo 1000000007.",
      [t("low = 3, high = 3, zero = 1, one = 1", "8"), t("low = 2, high = 3, zero = 1, one = 2", "5"),
       t("low = 1, high = 1, zero = 1, one = 1", "2"), t("low = 1, high = 2, zero = 2, one = 3", "1"),
       t("low = 4, high = 4, zero = 2, one = 2", "4")],
      [s("Length DP", "class Solution { public int countGoodStrings(int low, int high, int zero, int one) { int mod = 1_000_000_007; int[] dp = new int[high + 1]; dp[0] = 1; long ans = 0; for (int len = 1; len <= high; len++) { long ways = 0; if (len >= zero) ways += dp[len - zero]; if (len >= one) ways += dp[len - one]; dp[len] = (int)(ways % mod); if (len >= low) ans = (ans + dp[len]) % mod; } return (int)ans; } }", "O(high)", "O(high)"),
       s("Memoized Length DFS", "class Solution { int low, high, zero, one; Integer[] memo; static final int MOD = 1_000_000_007; public int countGoodStrings(int low, int high, int zero, int one) { this.low = low; this.high = high; this.zero = zero; this.one = one; memo = new Integer[high + 1]; return dfs(0); } int dfs(int len) { if (len > high) return 0; if (memo[len] != null) return memo[len]; long ans = len >= low ? 1 : 0; ans += dfs(len + zero); ans += dfs(len + one); return memo[len] = (int)(ans % MOD); } }", "O(high)", "O(high)")]),

    p("Factorial Zeroes Preimage Size", "HARD", "int preimageSizeFZF(int k)",
      ["Math", "Binary Search", "Number Theory"],
      "Return how many non-negative integers x have exactly k trailing zeroes in x factorial.",
      [t("k = 0", "5"), t("k = 5", "0"), t("k = 3", "5"), t("k = 1", "5"), t("k = 10", "5")],
      [s("Binary Search Boundary", "class Solution { public int preimageSizeFZF(int k) { return (int)(first(k + 1L) - first(k)); } long first(long k) { long lo = 0, hi = 5L * (k + 1); while (lo < hi) { long mid = (lo + hi) >>> 1; if (zeros(mid) >= k) hi = mid; else lo = mid + 1; } return lo; } long zeros(long n) { long z = 0; while (n > 0) { n /= 5; z += n; } return z; } }", "O(log k log k)", "O(1)"),
       s("Search Exact Block", "class Solution { public int preimageSizeFZF(int k) { long lo = 0, hi = 5L * (k + 1); while (lo <= hi) { long mid = (lo + hi) >>> 1, z = count(mid); if (z == k) return 5; if (z < k) lo = mid + 1; else hi = mid - 1; } return 0; } long count(long n) { long total = 0; for (long d = 5; d <= n; d *= 5) total += n / d; return total; } }", "O(log k log k)", "O(1)")]),

    p("Alternating Digit Sum", "EASY", "int alternateDigitSum(int n)",
      ["Math"],
      "Starting at the most significant digit, alternately add and subtract digits.",
      [t("n = 521", "4"), t("n = 111", "1"), t("n = 886996", "0"), t("n = 7", "7"), t("n = 10", "1")],
      [s("String Digits", "class Solution { public int alternateDigitSum(int n) { String x = String.valueOf(n); int ans = 0; for (int i = 0; i < x.length(); i++) ans += (i % 2 == 0 ? 1 : -1) * (x.charAt(i) - '0'); return ans; } }"),
       s("Digit Array", "class Solution { public int alternateDigitSum(int n) { List<Integer> digits = new ArrayList<>(); while (n > 0) { digits.add(n % 10); n /= 10; } int ans = 0, sign = 1; for (int i = digits.size() - 1; i >= 0; i--) { ans += sign * digits.get(i); sign = -sign; } return ans; } }", "O(d)", "O(d)")]),

    p("Maximum Pair Sum After Sorting", "MEDIUM", "int minPairSum(int[] nums)",
      ["Array", "Greedy", "Sorting", "Two Pointers"],
      "Pair all numbers to minimize the maximum pair sum, and return that minimized maximum.",
      [t("nums = [3,5,2,3]", "7"), t("nums = [3,5,4,2,4,6]", "8"), t("nums = [1,1]", "2"),
       t("nums = [9,1,2,8]", "10"), t("nums = [4,4,4,4]", "8")],
      [s("Sort Opposite Ends", "class Solution { public int minPairSum(int[] nums) { Arrays.sort(nums); int ans = 0; for (int l = 0, r = nums.length - 1; l < r; l++, r--) ans = Math.max(ans, nums[l] + nums[r]); return ans; } }", "O(n log n)", "O(1)"),
       s("Deque Pairing", "class Solution { public int minPairSum(int[] nums) { Arrays.sort(nums); Deque<Integer> q = new ArrayDeque<>(); for (int x : nums) q.add(x); int ans = 0; while (!q.isEmpty()) ans = Math.max(ans, q.pollFirst() + q.pollLast()); return ans; } }", "O(n log n)", "O(n)")]),

    p("Laser Beams Between Security Rows", "MEDIUM", "int numberOfBeams(String[] bank)",
      ["Array", "String", "Counting"],
      "Rows with devices create beams with the next non-empty row. Empty rows are ignored.",
      [t("bank = [\"011001\",\"000000\",\"010100\",\"001000\"]", "8"), t("bank = [\"000\",\"111\",\"000\"]", "0"),
       t("bank = [\"1\",\"1\",\"1\"]", "2"), t("bank = [\"101\",\"111\",\"000\",\"010\"]", "9"), t("bank = []", "0")],
      [s("Previous Nonempty Row", "class Solution { public int numberOfBeams(String[] bank) { int prev = 0, ans = 0; for (String row : bank) { int cur = 0; for (char ch : row.toCharArray()) if (ch == '1') cur++; if (cur > 0) { ans += prev * cur; prev = cur; } } return ans; } }"),
       s("Collect Device Counts", "class Solution { public int numberOfBeams(String[] bank) { List<Integer> counts = new ArrayList<>(); for (String row : bank) { int c = 0; for (int i = 0; i < row.length(); i++) if (row.charAt(i) == '1') c++; if (c != 0) counts.add(c); } int beams = 0; for (int i = 1; i < counts.size(); i++) beams += counts.get(i - 1) * counts.get(i); return beams; } }", "O(nm)", "O(n)")]),
])

PROBLEMS.extend([
    p("Divide Players Into Teams", "MEDIUM", "long dividePlayers(int[] skill)",
      ["Array", "Hash Table", "Sorting", "Two Pointers"],
      "Pair players so every pair has the same total skill. Return the sum of pair chemistry products, or -1.",
      [t("skill = [3,2,5,1,3,4]", "22"), t("skill = [3,4]", "12"), t("skill = [1,1,2,3]", "-1"),
       t("skill = [2,2,2,2]", "8"), t("skill = [1,5,2,4]", "13")],
      [s("Sort Pair Ends", "class Solution { public long dividePlayers(int[] skill) { Arrays.sort(skill); int target = skill[0] + skill[skill.length - 1]; long ans = 0; for (int l = 0, r = skill.length - 1; l < r; l++, r--) { if (skill[l] + skill[r] != target) return -1; ans += (long)skill[l] * skill[r]; } return ans; } }", "O(n log n)", "O(1)"),
       s("Frequency Pairing", "class Solution { public long dividePlayers(int[] skill) { int sum = 0; for (int x : skill) sum += x; int pairs = skill.length / 2; if (sum % pairs != 0) return -1; int target = sum / pairs; Map<Integer,Integer> count = new HashMap<>(); for (int x : skill) count.put(x, count.getOrDefault(x,0)+1); long ans = 0; for (int x : skill) { if (count.getOrDefault(x,0) == 0) continue; count.put(x, count.get(x)-1); int y = target - x; if (count.getOrDefault(y,0) == 0) return -1; count.put(y, count.get(y)-1); ans += (long)x * y; } return ans; } }", "O(n)", "O(n)")]),

    p("Minimum Rounds to Finish Tasks", "MEDIUM", "int minimumRounds(int[] tasks)",
      ["Array", "Hash Table", "Greedy", "Counting"],
      "Each round completes exactly two or three tasks of the same difficulty. Return the minimum rounds, or -1.",
      [t("tasks = [2,2,3,3,2,4,4,4,4,4]", "4"), t("tasks = [2,3,3]", "-1"),
       t("tasks = [1,1]", "1"), t("tasks = [1,1,1,1]", "2"), t("tasks = []", "0")],
      [s("Frequency Formula", "class Solution { public int minimumRounds(int[] tasks) { Map<Integer,Integer> count = new HashMap<>(); for (int t : tasks) count.put(t, count.getOrDefault(t,0)+1); int ans = 0; for (int c : count.values()) { if (c == 1) return -1; ans += (c + 2) / 3; } return ans; } }", "O(n)", "O(n)"),
       s("Greedy Per Count", "class Solution { public int minimumRounds(int[] tasks) { Map<Integer,Integer> map = new HashMap<>(); for (int x : tasks) map.merge(x,1,Integer::sum); int rounds = 0; for (int c : map.values()) { if (c == 1) return -1; while (c > 0) { if (c % 3 == 0 || c >= 5) c -= 3; else c -= 2; rounds++; } } return rounds; } }", "O(n)", "O(n)")]),

    p("Longest Square Streak", "MEDIUM", "int longestSquareStreak(int[] nums)",
      ["Array", "Hash Table", "Sorting", "Dynamic Programming"],
      "Find the longest chain where each next value is the square of the previous value. Return -1 if no chain has length at least two.",
      [t("nums = [4,3,6,16,8,2]", "3"), t("nums = [2,3,5,6,7]", "-1"), t("nums = [2,4,16,256]", "4"),
       t("nums = [3,9,81,2]", "3"), t("nums = [5]", "-1")],
      [s("Hash Set Walk", "class Solution { public int longestSquareStreak(int[] nums) { Set<Long> set = new HashSet<>(); for (int x : nums) set.add((long)x); int best = 1; for (int x : nums) { long cur = x; int len = 0; while (set.contains(cur)) { len++; cur *= cur; if (cur > 1_000_000_000L) break; } best = Math.max(best, len); } return best < 2 ? -1 : best; } }", "O(n log V)", "O(n)"),
       s("Sorted DP", "class Solution { public int longestSquareStreak(int[] nums) { Arrays.sort(nums); Map<Integer,Integer> dp = new HashMap<>(); int best = 1; for (int x : nums) { int len = 1; int root = (int)Math.sqrt(x); if (root * root == x) len = dp.getOrDefault(root, 0) + 1; dp.put(x, Math.max(dp.getOrDefault(x, 0), len)); best = Math.max(best, dp.get(x)); } return best < 2 ? -1 : best; } }", "O(n log n)", "O(n)")]),

    p("Minimum Number Game", "EASY", "int[] numberGame(int[] nums)",
      ["Array", "Sorting", "Simulation"],
      "Repeatedly remove the two smallest values, append the second smallest, then the smallest.",
      [t("nums = [5,4,2,3]", "[3,2,5,4]"), t("nums = [2,5]", "[5,2]"), t("nums = [1,1]", "[1,1]"),
       t("nums = [9,8,7,6]", "[7,6,9,8]"), t("nums = []", "[]")],
      [s("Sort Pairs", "class Solution { public int[] numberGame(int[] nums) { Arrays.sort(nums); for (int i = 0; i + 1 < nums.length; i += 2) { int tmp = nums[i]; nums[i] = nums[i + 1]; nums[i + 1] = tmp; } return nums; } }", "O(n log n)", "O(1)"),
       s("Priority Queue", "class Solution { public int[] numberGame(int[] nums) { PriorityQueue<Integer> pq = new PriorityQueue<>(); for (int x : nums) pq.add(x); int[] ans = new int[nums.length]; for (int i = 0; i < ans.length; i += 2) { int a = pq.poll(), b = pq.poll(); ans[i] = b; ans[i + 1] = a; } return ans; } }", "O(n log n)", "O(n)")]),

    p("Distinct Difference Array", "EASY", "int[] distinctDifferenceArray(int[] nums)",
      ["Array", "Hash Table"],
      "For each index, return distinct(prefix ending here) minus distinct(suffix after here).",
      [t("nums = [1,2,3,4,5]", "[-3,-1,1,3,5]"), t("nums = [3,2,3,4,2]", "[-2,-1,0,2,3]"),
       t("nums = [1]", "[1]"), t("nums = [1,1,1]", "[0,0,1]"), t("nums = []", "[]")],
      [s("Prefix and Suffix Sets", "class Solution { public int[] distinctDifferenceArray(int[] nums) { int n = nums.length; int[] ans = new int[n], suffix = new int[n + 1]; Set<Integer> set = new HashSet<>(); for (int i = n - 1; i >= 0; i--) { set.add(nums[i]); suffix[i] = set.size(); } set.clear(); for (int i = 0; i < n; i++) { set.add(nums[i]); ans[i] = set.size() - suffix[i + 1]; } return ans; } }", "O(n)", "O(n)"),
       s("Recompute Each Cut", "class Solution { public int[] distinctDifferenceArray(int[] nums) { int[] ans = new int[nums.length]; for (int i = 0; i < nums.length; i++) { Set<Integer> a = new HashSet<>(), b = new HashSet<>(); for (int j = 0; j <= i; j++) a.add(nums[j]); for (int j = i + 1; j < nums.length; j++) b.add(nums[j]); ans[i] = a.size() - b.size(); } return ans; } }", "O(n^2)", "O(n)")]),

    p("Neighboring Bitwise XOR Validity", "MEDIUM", "boolean doesValidArrayExist(int[] derived)",
      ["Array", "Bit Manipulation"],
      "A circular binary array can produce derived[i] = original[i] XOR original[i+1]. Return whether at least one original exists.",
      [t("derived = [1,1,0]", "true"), t("derived = [1,1]", "true"), t("derived = [1,0]", "false"),
       t("derived = [0]", "true"), t("derived = [1,0,1]", "true")],
      [s("Xor Parity", "class Solution { public boolean doesValidArrayExist(int[] derived) { int x = 0; for (int v : derived) x ^= v; return x == 0; } }"),
       s("Try Starting Bit", "class Solution { public boolean doesValidArrayExist(int[] derived) { for (int start = 0; start <= 1; start++) { int cur = start; for (int d : derived) cur ^= d; if (cur == start) return true; } return false; } }")]),

    p("Count Complete Subarrays", "MEDIUM", "int countCompleteSubarrays(int[] nums)",
      ["Array", "Hash Table", "Sliding Window"],
      "A subarray is complete if it contains every distinct value present in the whole array. Count complete subarrays.",
      [t("nums = [1,3,1,2,2]", "4"), t("nums = [5,5,5,5]", "10"), t("nums = [1,2,3]", "1"),
       t("nums = []", "0"), t("nums = [1,2,1,2]", "6")],
      [s("Sliding Window", "class Solution { public int countCompleteSubarrays(int[] nums) { Set<Integer> all = new HashSet<>(); for (int x : nums) all.add(x); int need = all.size(), left = 0, ans = 0; Map<Integer,Integer> count = new HashMap<>(); for (int right = 0; right < nums.length; right++) { count.put(nums[right], count.getOrDefault(nums[right],0)+1); while (count.size() == need) { ans += nums.length - right; int v = nums[left++]; count.put(v, count.get(v)-1); if (count.get(v) == 0) count.remove(v); } } return ans; } }", "O(n)", "O(n)"),
       s("Enumerate Subarrays", "class Solution { public int countCompleteSubarrays(int[] nums) { Set<Integer> all = new HashSet<>(); for (int x : nums) all.add(x); int ans = 0; for (int l = 0; l < nums.length; l++) { Set<Integer> seen = new HashSet<>(); for (int r = l; r < nums.length; r++) { seen.add(nums[r]); if (seen.size() == all.size()) ans++; } } return ans; } }", "O(n^2)", "O(n)")]),

    p("Minimum Operations to Collect Elements", "EASY", "int minOperations(List<Integer> nums, int k)",
      ["Array", "Hash Table"],
      "Scan from the end until all values from 1 through k have been seen. Return the number of scanned elements.",
      [t("nums = [3,1,5,4,2], k = 2", "4"), t("nums = [3,1,5,4,2], k = 5", "5"), t("nums = [1], k = 1", "1"),
       t("nums = [2,2,2,1], k = 2", "2"), t("nums = [5,4,3,2,1], k = 3", "3")],
      [s("Reverse Set Scan", "class Solution { public int minOperations(List<Integer> nums, int k) { Set<Integer> need = new HashSet<>(); for (int v = 1; v <= k; v++) need.add(v); int ops = 0; for (int i = nums.size() - 1; i >= 0; i--) { ops++; need.remove(nums.get(i)); if (need.isEmpty()) return ops; } return ops; } }", "O(n+k)", "O(k)"),
       s("Boolean Seen", "class Solution { public int minOperations(List<Integer> nums, int k) { boolean[] seen = new boolean[k + 1]; int have = 0, ops = 0; for (int i = nums.size() - 1; i >= 0; i--) { ops++; int v = nums.get(i); if (v <= k && !seen[v]) { seen[v] = true; have++; if (have == k) return ops; } } return ops; } }", "O(n+k)", "O(k)")]),

    p("Semi-Ordered Permutation Moves", "EASY", "int semiOrderedPermutation(int[] nums)",
      ["Array", "Simulation"],
      "Using adjacent swaps, move value 1 to the front and value n to the end. Return the minimum swaps.",
      [t("nums = [2,1,4,3]", "2"), t("nums = [2,4,1,3]", "3"), t("nums = [1,3,4,2,5]", "0"),
       t("nums = [5,4,3,2,1]", "7"), t("nums = [1]", "0")],
      [s("Index Formula", "class Solution { public int semiOrderedPermutation(int[] nums) { int n = nums.length, p1 = 0, pn = 0; for (int i = 0; i < n; i++) { if (nums[i] == 1) p1 = i; if (nums[i] == n) pn = i; } return p1 + (n - 1 - pn) - (p1 > pn ? 1 : 0); } }"),
       s("Adjacent Swap Simulation", "class Solution { public int semiOrderedPermutation(int[] nums) { int swaps = 0; for (int i = 0; i < nums.length; i++) if (nums[i] == 1) { while (i > 0) { int t = nums[i]; nums[i] = nums[i-1]; nums[i-1] = t; i--; swaps++; } break; } for (int i = 0; i < nums.length; i++) if (nums[i] == nums.length) { while (i + 1 < nums.length) { int t = nums[i]; nums[i] = nums[i+1]; nums[i+1] = t; i++; swaps++; } break; } return swaps; } }", "O(n^2)", "O(1)")]),

    p("Prefix Common Array of Two Permutations", "MEDIUM", "int[] findThePrefixCommonArray(int[] A, int[] B)",
      ["Array", "Hash Table", "Bit Manipulation"],
      "For each prefix length, count how many values appear in both prefixes.",
      [t("A = [1,3,2,4], B = [3,1,2,4]", "[0,2,3,4]"), t("A = [2,3,1], B = [3,1,2]", "[0,1,3]"),
       t("A = [1], B = [1]", "[1]"), t("A = [1,2,3], B = [1,2,3]", "[1,2,3]"), t("A = [3,2,1], B = [1,2,3]", "[0,1,3]")],
      [s("Frequency Hits", "class Solution { public int[] findThePrefixCommonArray(int[] A, int[] B) { int n = A.length, common = 0; int[] seen = new int[n + 1], ans = new int[n]; for (int i = 0; i < n; i++) { if (++seen[A[i]] == 2) common++; if (++seen[B[i]] == 2) common++; ans[i] = common; } return ans; } }", "O(n)", "O(n)"),
       s("Set Intersection Per Prefix", "class Solution { public int[] findThePrefixCommonArray(int[] A, int[] B) { int[] ans = new int[A.length]; Set<Integer> sa = new HashSet<>(), sb = new HashSet<>(); for (int i = 0; i < A.length; i++) { sa.add(A[i]); sb.add(B[i]); int count = 0; for (int x : sa) if (sb.contains(x)) count++; ans[i] = count; } return ans; } }", "O(n^2)", "O(n)")]),

    p("Alternating Subarray Length", "EASY", "int alternatingSubarray(int[] nums)",
      ["Array", "Sliding Window"],
      "Return the longest subarray of length at least two whose differences are +1, -1, +1, ... starting with +1.",
      [t("nums = [2,3,4,3,4]", "4"), t("nums = [4,5,6]", "2"), t("nums = [1,2,1,2,1]", "5"),
       t("nums = [1,1,1]", "-1"), t("nums = [1]", "-1")],
      [s("Start Each Window", "class Solution { public int alternatingSubarray(int[] nums) { int best = -1; for (int i = 0; i < nums.length; i++) { int expect = 1; for (int j = i + 1; j < nums.length; j++) { if (nums[j] - nums[j - 1] != expect) break; best = Math.max(best, j - i + 1); expect = -expect; } } return best; } }", "O(n^2)", "O(1)"),
       s("Rolling Window", "class Solution { public int alternatingSubarray(int[] nums) { int best = -1, start = 0, expect = 1; for (int i = 1; i < nums.length; i++) { int diff = nums[i] - nums[i-1]; if (diff == expect) { best = Math.max(best, i - start + 1); expect = -expect; } else { start = diff == 1 ? i - 1 : i; expect = diff == 1 ? -1 : 1; if (diff == 1) best = Math.max(best, 2); } } return best; } }", "O(n)", "O(1)")]),

    p("Count Symmetric Integers", "EASY", "int countSymmetricIntegers(int low, int high)",
      ["Math", "Enumeration"],
      "Count integers with an even number of digits where the first half digit sum equals the second half digit sum.",
      [t("low = 1, high = 100", "9"), t("low = 1200, high = 1230", "4"), t("low = 10, high = 99", "9"),
       t("low = 1000, high = 1000", "0"), t("low = 11, high = 11", "1")],
      [s("String Enumeration", "class Solution { public int countSymmetricIntegers(int low, int high) { int ans = 0; for (int x = low; x <= high; x++) { String s = String.valueOf(x); if (s.length() % 2 == 1) continue; int a = 0, b = 0; for (int i = 0; i < s.length(); i++) if (i < s.length()/2) a += s.charAt(i)-'0'; else b += s.charAt(i)-'0'; if (a == b) ans++; } return ans; } }", "O(range*d)", "O(1)"),
       s("Numeric Digit Split", "class Solution { public int countSymmetricIntegers(int low, int high) { int count = 0; for (int x = low; x <= high; x++) if (ok(x)) count++; return count; } boolean ok(int x) { int digits = String.valueOf(x).length(); if (digits % 2 == 1) return false; int half = digits / 2, a = 0, b = 0; for (int i = 0; i < half; i++) { b += x % 10; x /= 10; } for (int i = 0; i < half; i++) { a += x % 10; x /= 10; } return a == b; } }", "O(range*d)", "O(1)")]),
])

PROBLEMS.extend([
    p("Time Needed to Buy Tickets", "EASY", "int timeRequiredToBuy(int[] tickets, int k)",
      ["Array", "Queue", "Simulation"],
      "People buy tickets in order. Return the time until person k finishes.",
      [t("tickets = [2,3,2], k = 2", "6"), t("tickets = [5,1,1,1], k = 0", "8"),
       t("tickets = [1], k = 0", "1"), t("tickets = [3,2,3], k = 1", "5"), t("tickets = [1,2,3,4], k = 2", "8")],
      [s("Formula Count", "class Solution { public int timeRequiredToBuy(int[] tickets, int k) { int time = 0; for (int i = 0; i < tickets.length; i++) time += Math.min(tickets[i], tickets[k] - (i > k ? 1 : 0)); return time; } }"),
       s("Queue Simulation", "class Solution { public int timeRequiredToBuy(int[] tickets, int k) { Queue<Integer> q = new ArrayDeque<>(); for (int i = 0; i < tickets.length; i++) q.add(i); int time = 0; while (true) { int i = q.poll(); tickets[i]--; time++; if (tickets[i] == 0 && i == k) return time; if (tickets[i] > 0) q.add(i); } } }", "O(total tickets)", "O(n)")]),

    p("Count Good Substrings of Length Three", "EASY", "int countGoodSubstrings(String s)",
      ["String", "Sliding Window"],
      "Count length-three substrings with all distinct characters.",
      [t("s = \"xyzzaz\"", "1"), t("s = \"aababcabc\"", "4"), t("s = \"abc\"", "1"), t("s = \"aaa\"", "0"), t("s = \"\"", "0")],
      [s("Fixed Window Check", "class Solution { public int countGoodSubstrings(String s) { int ans = 0; for (int i = 0; i + 2 < s.length(); i++) { char a = s.charAt(i), b = s.charAt(i + 1), c = s.charAt(i + 2); if (a != b && a != c && b != c) ans++; } return ans; } }"),
       s("Frequency Window", "class Solution { public int countGoodSubstrings(String s) { int[] f = new int[26]; int dup = 0, ans = 0; for (int i = 0; i < s.length(); i++) { int add = s.charAt(i) - 'a'; if (++f[add] == 2) dup++; if (i >= 3) { int rem = s.charAt(i - 3) - 'a'; if (f[rem]-- == 2) dup--; } if (i >= 2 && dup == 0) ans++; } return ans; } }")]),

    p("Steps to Reduce Binary String to One", "MEDIUM", "int numSteps(String s)",
      ["String", "Bit Manipulation", "Simulation"],
      "For a positive binary string, repeatedly divide by two if even or add one if odd until it becomes 1.",
      [t("s = \"1101\"", "6"), t("s = \"10\"", "1"), t("s = \"1\"", "0"), t("s = \"111\"", "4"), t("s = \"1011\"", "6")],
      [s("Carry Scan", "class Solution { public int numSteps(String s) { int steps = 0, carry = 0; for (int i = s.length() - 1; i > 0; i--) { int bit = s.charAt(i) - '0' + carry; if (bit % 2 == 0) steps += 1; else { steps += 2; carry = 1; } } return steps + carry; } }", "O(n)", "O(1)"),
       s("BigInteger Simulation", "class Solution { public int numSteps(String s) { java.math.BigInteger v = new java.math.BigInteger(s, 2), one = java.math.BigInteger.ONE, two = java.math.BigInteger.TWO; int steps = 0; while (!v.equals(one)) { v = v.testBit(0) ? v.add(one) : v.divide(two); steps++; } return steps; } }", "O(n^2)", "O(n)")]),

    p("Minimum Operations to Make Array Alternating", "MEDIUM", "int minimumOperations(int[] nums)",
      ["Array", "Hash Table", "Greedy"],
      "Change the fewest values so adjacent positions differ and all even positions share one value while all odd positions share another.",
      [t("nums = [3,1,3,2,4,3]", "3"), t("nums = [1,2,2,2,2]", "2"), t("nums = [1]", "0"),
       t("nums = [1,1,1,1]", "2"), t("nums = [1,2,1,2,1]", "0")],
      [s("Top Frequencies by Parity", "class Solution { public int minimumOperations(int[] nums) { if (nums.length == 1) return 0; int[][] e = top(nums, 0), o = top(nums, 1); int best = nums.length; for (int[] a : e) for (int[] b : o) if (a[0] != b[0]) best = Math.min(best, nums.length - a[1] - b[1]); return best; } int[][] top(int[] nums, int start) { Map<Integer,Integer> m = new HashMap<>(); for (int i = start; i < nums.length; i += 2) m.put(nums[i], m.getOrDefault(nums[i], 0) + 1); int[][] top = {{-1,0},{-2,0}}; for (var e : m.entrySet()) if (e.getValue() > top[0][1]) { top[1] = top[0]; top[0] = new int[]{e.getKey(), e.getValue()}; } else if (e.getValue() > top[1][1]) top[1] = new int[]{e.getKey(), e.getValue()}; return top; } }", "O(n)", "O(n)"),
       s("Try Candidate Values", "class Solution { public int minimumOperations(int[] nums) { Set<Integer> values = new HashSet<>(); for (int x : nums) values.add(x); values.add(-1); int best = nums.length; for (int a : values) for (int b : values) if (a != b) { int change = 0; for (int i = 0; i < nums.length; i++) if (nums[i] != (i % 2 == 0 ? a : b)) change++; best = Math.min(best, change); } return best; } }", "O(nu^2)", "O(u)")]),

    p("Destroying Asteroids In Order", "MEDIUM", "boolean asteroidsDestroyed(int mass, int[] asteroids)",
      ["Array", "Greedy", "Sorting"],
      "After sorting asteroids by mass, determine whether the planet can absorb every asteroid.",
      [t("mass = 10, asteroids = [4,9,23,4]", "true"), t("mass = 3, asteroids = [5,4,9]", "false"),
       t("mass = 5, asteroids = [1,2,3]", "true"), t("mass = 1, asteroids = [2]", "false"), t("mass = 100, asteroids = []", "true")],
      [s("Sort and Absorb", "class Solution { public boolean asteroidsDestroyed(int mass, int[] asteroids) { Arrays.sort(asteroids); long cur = mass; for (int a : asteroids) { if (cur < a) return false; cur += a; } return true; } }", "O(n log n)", "O(1)"),
       s("Priority Queue Absorb", "class Solution { public boolean asteroidsDestroyed(int mass, int[] asteroids) { PriorityQueue<Integer> pq = new PriorityQueue<>(); for (int a : asteroids) pq.add(a); long cur = mass; while (!pq.isEmpty()) { int a = pq.poll(); if (cur < a) return false; cur += a; } return true; } }", "O(n log n)", "O(n)")]),

    p("Original Array From Doubled Values", "MEDIUM", "int[] findOriginalArray(int[] changed)",
      ["Array", "Hash Table", "Greedy", "Sorting"],
      "The input was formed by taking an original array and appending each value doubled. Reconstruct the original array or return empty.",
      [t("changed = [1,3,4,2,6,8]", "[1,3,4]"), t("changed = [6,3,0,1]", "[]"),
       t("changed = [0,0,0,0]", "[0,0]"), t("changed = [2,1,2,4,2,4]", "[1,2,2]"), t("changed = []", "[]")],
      [s("Count Sorted Values", "class Solution { public int[] findOriginalArray(int[] changed) { if (changed.length % 2 == 1) return new int[0]; Arrays.sort(changed); Map<Integer,Integer> count = new HashMap<>(); for (int x : changed) count.put(x, count.getOrDefault(x, 0) + 1); int[] ans = new int[changed.length / 2]; int idx = 0; for (int x : changed) { if (count.get(x) == 0) continue; count.put(x, count.get(x) - 1); int d = x * 2; if (count.getOrDefault(d, 0) == 0) return new int[0]; count.put(d, count.get(d) - 1); ans[idx++] = x; } return ans; } }", "O(n log n)", "O(n)"),
       s("Queue by Value", "class Solution { public int[] findOriginalArray(int[] changed) { if (changed.length % 2 != 0) return new int[0]; Arrays.sort(changed); Queue<Integer> wait = new ArrayDeque<>(); List<Integer> out = new ArrayList<>(); for (int x : changed) { if (!wait.isEmpty() && wait.peek() == x) wait.poll(); else { out.add(x); wait.add(x * 2); } } if (!wait.isEmpty()) return new int[0]; int[] ans = new int[out.size()]; for (int i = 0; i < out.size(); i++) ans[i] = out.get(i); return ans; } }", "O(n log n)", "O(n)")]),

    p("Count Pairs Below Target", "EASY", "int countPairs(List<Integer> nums, int target)",
      ["Array", "Two Pointers", "Sorting"],
      "Count index pairs whose sum is strictly less than target.",
      [t("nums = [-1,1,2,3,1], target = 2", "3"), t("nums = [-6,2,5,-2,-7,-1,3], target = -2", "10"),
       t("nums = [1,2,3], target = 0", "0"), t("nums = [0,0,0], target = 1", "3"), t("nums = [], target = 5", "0")],
      [s("Sorted Two Pointers", "class Solution { public int countPairs(List<Integer> nums, int target) { Collections.sort(nums); int l = 0, r = nums.size() - 1, ans = 0; while (l < r) { if (nums.get(l) + nums.get(r) < target) { ans += r - l; l++; } else r--; } return ans; } }", "O(n log n)", "O(1)"),
       s("Brute Force Pairs", "class Solution { public int countPairs(List<Integer> nums, int target) { int ans = 0; for (int i = 0; i < nums.size(); i++) for (int j = i + 1; j < nums.size(); j++) if (nums.get(i) + nums.get(j) < target) ans++; return ans; } }", "O(n^2)", "O(1)")]),

    p("Minimum Deletions to Make String Balanced", "MEDIUM", "int minimumDeletions(String s)",
      ["String", "Dynamic Programming", "Stack"],
      "Delete the fewest characters so no b appears before an a.",
      [t("s = \"aababbab\"", "2"), t("s = \"bbaaaaabb\"", "2"), t("s = \"aaaa\"", "0"), t("s = \"bbbb\"", "0"), t("s = \"ba\"", "1")],
      [s("One Pass DP", "class Solution { public int minimumDeletions(String s) { int b = 0, del = 0; for (char ch : s.toCharArray()) { if (ch == 'b') b++; else del = Math.min(del + 1, b); } return del; } }"),
       s("Prefix Suffix Counts", "class Solution { public int minimumDeletions(String s) { int n = s.length(); int[] leftB = new int[n + 1], rightA = new int[n + 1]; for (int i = 0; i < n; i++) leftB[i + 1] = leftB[i] + (s.charAt(i) == 'b' ? 1 : 0); for (int i = n - 1; i >= 0; i--) rightA[i] = rightA[i + 1] + (s.charAt(i) == 'a' ? 1 : 0); int best = n; for (int cut = 0; cut <= n; cut++) best = Math.min(best, leftB[cut] + rightA[cut]); return best; } }", "O(n)", "O(n)")]),

    p("Append Characters to Make Subsequence", "MEDIUM", "int appendCharacters(String s, String t)",
      ["String", "Two Pointers"],
      "Return how many characters must be appended to s so that t becomes a subsequence.",
      [t("s = \"coaching\", t = \"coding\"", "4"), t("s = \"abcde\", t = \"a\"", "0"), t("s = \"z\", t = \"abcde\"", "5"),
       t("s = \"abc\", t = \"abc\"", "0"), t("s = \"abc\", t = \"abcd\"", "1")],
      [s("Subsequence Scan", "class Solution { public int appendCharacters(String s, String t) { int j = 0; for (int i = 0; i < s.length() && j < t.length(); i++) if (s.charAt(i) == t.charAt(j)) j++; return t.length() - j; } }"),
       s("IndexOf Search", "class Solution { public int appendCharacters(String s, String t) { int pos = -1, matched = 0; for (int i = 0; i < t.length(); i++) { pos = s.indexOf(t.charAt(i), pos + 1); if (pos == -1) break; matched++; } return t.length() - matched; } }")]),

    p("Partition Array by Pivot", "MEDIUM", "int[] pivotArray(int[] nums, int pivot)",
      ["Array", "Two Pointers", "Simulation"],
      "Return the stable partition of nums: values less than pivot, then equal values, then greater values.",
      [t("nums = [9,12,5,10,14,3,10], pivot = 10", "[9,5,3,10,10,12,14]"), t("nums = [-3,4,3,2], pivot = 2", "[-3,2,4,3]"),
       t("nums = [1,1,1], pivot = 1", "[1,1,1]"), t("nums = [2,1], pivot = 2", "[1,2]"), t("nums = [], pivot = 0", "[]")],
      [s("Three Pass Stable Fill", "class Solution { public int[] pivotArray(int[] nums, int pivot) { int[] ans = new int[nums.length]; int i = 0; for (int x : nums) if (x < pivot) ans[i++] = x; for (int x : nums) if (x == pivot) ans[i++] = x; for (int x : nums) if (x > pivot) ans[i++] = x; return ans; } }", "O(n)", "O(n)"),
       s("Three Lists", "class Solution { public int[] pivotArray(int[] nums, int pivot) { List<Integer> a = new ArrayList<>(), b = new ArrayList<>(), c = new ArrayList<>(); for (int x : nums) { if (x < pivot) a.add(x); else if (x == pivot) b.add(x); else c.add(x); } a.addAll(b); a.addAll(c); int[] ans = new int[nums.length]; for (int i = 0; i < nums.length; i++) ans[i] = a.get(i); return ans; } }", "O(n)", "O(n)")]),

    p("Longest Unequal Adjacent Groups Subsequence", "EASY", "int longestAlternatingGroups(int[] groups)",
      ["Array", "Greedy"],
      "Choose a subsequence whose neighboring chosen group values are different. Return the maximum length.",
      [t("groups = [0,1,1,1]", "2"), t("groups = [0,1,0,1]", "4"), t("groups = [1,1,1]", "1"),
       t("groups = []", "0"), t("groups = [1,0,0,1,0]", "4")],
      [s("Greedy Changes", "class Solution { public int longestAlternatingGroups(int[] groups) { if (groups.length == 0) return 0; int ans = 1, last = groups[0]; for (int i = 1; i < groups.length; i++) if (groups[i] != last) { ans++; last = groups[i]; } return ans; } }"),
       s("Dynamic Programming", "class Solution { public int longestAlternatingGroups(int[] groups) { int zero = 0, one = 0; for (int g : groups) { if (g == 0) zero = Math.max(zero, one + 1); else one = Math.max(one, zero + 1); } return Math.max(zero, one); } }")]),

    p("Find the Array Concatenation Value", "EASY", "long findTheArrayConcVal(int[] nums)",
      ["Array", "Two Pointers", "Simulation"],
      "Repeatedly concatenate the first and last values and add the result. A lone middle value is added directly.",
      [t("nums = [7,52,2,4]", "596"), t("nums = [5,14,13,8,12]", "673"), t("nums = [1]", "1"),
       t("nums = [10,2]", "102"), t("nums = []", "0")],
      [s("Two Pointers", "class Solution { public long findTheArrayConcVal(int[] nums) { long ans = 0; int l = 0, r = nums.length - 1; while (l < r) ans += Long.parseLong(String.valueOf(nums[l++]) + nums[r--]); if (l == r) ans += nums[l]; return ans; } }"),
       s("Digit Multiplier", "class Solution { public long findTheArrayConcVal(int[] nums) { long ans = 0; for (int l = 0, r = nums.length - 1; l <= r; l++, r--) { if (l == r) ans += nums[l]; else { int mult = 10, x = nums[r]; while (x >= 10) { mult *= 10; x /= 10; } ans += (long)nums[l] * mult + nums[r]; } } return ans; } }")]),

    p("Minimum Common Value in Two Sorted Arrays", "EASY", "int getCommon(int[] nums1, int[] nums2)",
      ["Array", "Two Pointers", "Binary Search"],
      "Both arrays are sorted. Return the smallest value appearing in both arrays, or -1.",
      [t("nums1 = [1,2,3], nums2 = [2,4]", "2"), t("nums1 = [1,2,3,6], nums2 = [2,3,4,5]", "2"),
       t("nums1 = [1], nums2 = [1]", "1"), t("nums1 = [1,2], nums2 = [3,4]", "-1"), t("nums1 = [], nums2 = [1]", "-1")],
      [s("Two Pointers", "class Solution { public int getCommon(int[] nums1, int[] nums2) { int i = 0, j = 0; while (i < nums1.length && j < nums2.length) { if (nums1[i] == nums2[j]) return nums1[i]; if (nums1[i] < nums2[j]) i++; else j++; } return -1; } }"),
       s("Hash Set Lookup", "class Solution { public int getCommon(int[] nums1, int[] nums2) { Set<Integer> set = new HashSet<>(); for (int x : nums1) set.add(x); for (int y : nums2) if (set.contains(y)) return y; return -1; } }", "O(n+m)", "O(n)")]),

    p("Maximum Number of Pairs in Array", "EASY", "int[] numberOfPairs(int[] nums)",
      ["Array", "Hash Table", "Counting"],
      "Return [number of pairs, leftover values] after forming equal-value pairs.",
      [t("nums = [1,3,2,1,3,2,2]", "[3,1]"), t("nums = [1,1]", "[1,0]"), t("nums = [0]", "[0,1]"),
       t("nums = []", "[0,0]"), t("nums = [1,2,3,4]", "[0,4]")],
      [s("Frequency Counts", "class Solution { public int[] numberOfPairs(int[] nums) { Map<Integer,Integer> count = new HashMap<>(); for (int x : nums) count.put(x, count.getOrDefault(x, 0) + 1); int pairs = 0, left = 0; for (int c : count.values()) { pairs += c / 2; left += c % 2; } return new int[]{pairs, left}; } }", "O(n)", "O(n)"),
       s("Toggle Set", "class Solution { public int[] numberOfPairs(int[] nums) { Set<Integer> open = new HashSet<>(); int pairs = 0; for (int x : nums) { if (!open.add(x)) { open.remove(x); pairs++; } } return new int[]{pairs, open.size()}; } }", "O(n)", "O(n)")]),

    p("Circular Sentence Check", "EASY", "boolean isCircularSentence(String sentence)",
      ["String"],
      "A sentence is circular if each word's last character equals the next word's first character, including the last to first.",
      [t("sentence = \"leetcode exercises sound delightful\"", "true"), t("sentence = \"eetcode\"", "true"),
       t("sentence = \"Leetcode is cool\"", "false"), t("sentence = \"a b a\"", "false"), t("sentence = \"ab ba\"", "true")],
      [s("Word Boundary Check", "class Solution { public boolean isCircularSentence(String sentence) { String[] words = sentence.split(\" \"); for (int i = 0; i < words.length; i++) { String a = words[i], b = words[(i + 1) % words.length]; if (a.charAt(a.length() - 1) != b.charAt(0)) return false; } return true; } }"),
       s("Character Scan", "class Solution { public boolean isCircularSentence(String sentence) { for (int i = 0; i < sentence.length(); i++) if (sentence.charAt(i) == ' ' && sentence.charAt(i - 1) != sentence.charAt(i + 1)) return false; return sentence.charAt(0) == sentence.charAt(sentence.length() - 1); } }")]),
])


def main():
    if len(PROBLEMS) != len(REPLACE_NUMBERS):
        raise SystemExit(f"replacement count mismatch: {len(PROBLEMS)} problems for {len(REPLACE_NUMBERS)} files")

    seen = set()
    duplicates = set()
    for problem in PROBLEMS:
        title = problem["title"]
        if title in seen:
            duplicates.add(title)
        seen.add(title)
    if duplicates:
        raise SystemExit(f"replacement list repeats titles: {sorted(duplicates)}")

    existing_titles = {}
    for path in BASE.glob("p*.json"):
        number = int(path.stem[1:])
        if number in REPLACE_NUMBERS:
            continue
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        existing_titles[data["title"]] = path.name

    conflicts = sorted(problem["title"] for problem in PROBLEMS if problem["title"] in existing_titles)
    if conflicts:
        details = ", ".join(f"{title} ({existing_titles[title]})" for title in conflicts)
        raise SystemExit(f"replacement titles already exist in retained official bank: {details}")

    for number, problem in zip(REPLACE_NUMBERS, PROBLEMS):
        data = {"id": str(number), **problem}
        path = BASE / f"p{number}.json"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {path.name}: {problem['title']}")

    print(f"replaced {len(PROBLEMS)} duplicate problem slots")


if __name__ == "__main__":
    main()
