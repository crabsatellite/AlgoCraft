import json
from pathlib import Path


BASE = Path(__file__).resolve().parents[1] / "question_bank" / "official"


STATEMENTS = {
    23: {
        "body": "Given an integer array `nums` and an integer `target`, count the number of index triplets `(i, j, k)` such that `i < j < k` and `nums[i] + nums[j] + nums[k] < target`.",
        "constraints": [
            "`0 <= nums.length <= 1000`",
            "`-10^5 <= nums[i], target <= 10^5`",
            "The answer fits in a 32-bit signed integer.",
        ],
    },
    28: {
        "body": "Given an integer array `nums`, count pairs of indices `(i, j)` such that `i < j` and `nums[i] == nums[j]`.",
        "constraints": [
            "`0 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
            "The answer fits in a 32-bit signed integer.",
        ],
    },
    190: {
        "body": "Given an integer array `nums`, return the fewest right rotations needed to make the array nondecreasing. A right rotation moves the last element to the front. Return `-1` if no number of right rotations can make the array nondecreasing.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
            "A nondecreasing array may contain equal adjacent values.",
        ],
    },
    206: {
        "body": "Given an integer array `nums`, a width ramp is a pair `(i, j)` with `i < j` and `nums[i] <= nums[j]`. Return the maximum value of `j - i` over all width ramps. If no ramp exists, return `0`.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
        ],
    },
    241: {
        "body": "Given an `m x n` matrix `grid` whose rows and columns are each sorted in non-increasing order, return the number of negative values in the matrix.",
        "constraints": [
            "`1 <= m, n <= 100`",
            "`grid.length == m` and `grid[i].length == n`",
            "`-10^5 <= grid[i][j] <= 10^5`",
            "Every row and every column is sorted from largest to smallest.",
        ],
    },
    278: {
        "body": "You are given two permutations `A` and `B` of the numbers `1` through `n`. For each index `i`, let the prefix of each array be positions `0..i`. Return an array where `answer[i]` is the count of values that appear in both prefixes.",
        "constraints": [
            "`1 <= A.length == B.length <= 10^5`",
            "`A` and `B` are permutations of the integers from `1` to `n`.",
        ],
    },
    279: {
        "body": "You are given a permutation `nums` of the integers `1` through `n`. In one move, you may swap adjacent elements. Return the minimum number of moves needed to place value `1` at the first position and value `n` at the last position.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`nums` is a permutation of the integers from `1` to `n`.",
        ],
    },
    283: {
        "body": "Given an integer array `nums`, find the longest contiguous subarray of length at least two whose adjacent differences are `+1, -1, +1, -1, ...` starting with `+1`. Return the length of that subarray, or `-1` if no such subarray exists.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
        ],
    },
    286: {
        "body": "Given two integers `low` and `high`, count integers in the inclusive range `[low, high]` that have an even number of decimal digits and whose first-half digit sum equals their second-half digit sum.",
        "constraints": [
            "`1 <= low <= high <= 10^5`",
        ],
    },
    301: {
        "body": "You are given `bloomDay`, where `bloomDay[i]` is the day the `i`-th flower blooms. You need to make `m` bouquets, and each bouquet requires `k` adjacent bloomed flowers. Return the earliest day when this is possible, or `-1` if it can never be done.",
        "constraints": [
            "`1 <= bloomDay.length <= 10^5`",
            "`1 <= bloomDay[i] <= 10^9`",
            "`1 <= m, k <= 10^6`",
        ],
    },
    302: {
        "body": "For each index `i` in `nums`, compute the number of distinct values in the prefix `nums[0..i]` minus the number of distinct values in the suffix `nums[i+1..n-1]`. Return the resulting array.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
        ],
    },
    303: {
        "body": "Given a binary matrix `mat`, count rectangular submatrices whose cells are all `1`. A submatrix must use contiguous rows and contiguous columns.",
        "constraints": [
            "`1 <= mat.length, mat[i].length <= 150`",
            "`mat[i][j]` is either `0` or `1`.",
            "The answer fits in a 32-bit signed integer.",
        ],
    },
    307: {
        "body": "Let `f(x)` be the number of trailing zeroes in `x!`. Given an integer `k`, return how many non-negative integers `x` satisfy `f(x) == k`.",
        "constraints": [
            "`0 <= k <= 10^9`",
        ],
    },
    308: {
        "body": "Given an integer array `nums` that may contain negative numbers and an integer `k`, return the length of the shortest non-empty contiguous subarray whose sum is at least `k`. Return `-1` if no such subarray exists.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^5 <= nums[i] <= 10^5`",
            "`-10^9 <= k <= 10^9`",
        ],
    },
    309: {
        "body": "Given an integer matrix and an integer `k`, find the maximum sum of any rectangular submatrix whose sum is no more than `k`.",
        "constraints": [
            "`1 <= matrix.length, matrix[i].length <= 100`",
            "`-10^5 <= matrix[i][j] <= 10^5`",
            "`-10^9 <= k <= 10^9`",
            "At least one submatrix sum is no more than `k` in the official tests.",
        ],
    },
    311: {
        "body": "A good binary string is built by repeatedly appending either `zero` zero characters or `one` one characters. Count how many good strings have length between `low` and `high`, inclusive. Return the count modulo `1_000_000_007`.",
        "constraints": [
            "`1 <= low <= high <= 10^5`",
            "`1 <= zero, one <= high`",
        ],
    },
    316: {
        "body": "Given an array of positive integers, find the longest chain of values where every next value is the square of the previous value. Each chain value must appear in `nums`. Return the maximum chain length, or `-1` if every valid chain has length less than two.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`1 <= nums[i] <= 10^9`",
        ],
    },
    317: {
        "body": "Given an integer array `nums` and two integers `minK` and `maxK`, count subarrays whose minimum value is exactly `minK` and whose maximum value is exactly `maxK`.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i], minK, maxK <= 10^9`",
            "`minK <= maxK`",
            "The answer fits in a 64-bit signed integer.",
        ],
    },
    318: {
        "body": "You are given arrays `nums` and `multipliers`. Starting from the first multiplier, repeatedly choose either the leftmost or rightmost remaining number from `nums`, multiply it by the current multiplier, and add the product to your score. Return the maximum score after using every multiplier.",
        "constraints": [
            "`1 <= multipliers.length <= nums.length <= 10^5`",
            "`multipliers.length <= 1000`",
            "`-1000 <= nums[i], multipliers[i] <= 1000`",
        ],
    },
    321: {
        "body": "A bank is represented by binary strings, where `1` means a security device and `0` means an empty cell. A beam is formed between every device in one non-empty row and every device in the next non-empty row below it; empty rows between them are ignored. Return the total number of beams.",
        "constraints": [
            "`1 <= bank.length <= 500`",
            "`1 <= bank[i].length <= 500`",
            "Every row has the same length and contains only `0` and `1`.",
        ],
    },
    323: {
        "body": "You are given task difficulties. In one round, you may complete exactly two or exactly three tasks, and all tasks completed in the same round must have the same difficulty. Return the minimum number of rounds needed to complete all tasks, or `-1` if impossible.",
        "constraints": [
            "`1 <= tasks.length <= 10^5`",
            "`1 <= tasks[i] <= 10^9`",
        ],
    },
    326: {
        "body": "Given a pattern string and a space-separated sentence `s`, determine whether there is a one-to-one mapping between pattern characters and words. Each pattern character must always map to the same word, and no two different characters may map to the same word.",
        "constraints": [
            "`1 <= pattern.length <= 300`",
            "`s` contains one or more words separated by single spaces.",
            "Words and pattern characters are compared case-sensitively.",
        ],
    },
    327: {
        "body": "Given a positive integer `n`, compute the alternating digit sum from most significant digit to least significant digit: add the first digit, subtract the second, add the third, and so on.",
        "constraints": [
            "`1 <= n <= 10^9`",
        ],
    },
    328: {
        "body": "Given an integer array `nums` and an integer `limit`, return the length of the longest contiguous subarray such that the difference between its maximum and minimum values is at most `limit`.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i] <= 10^9`",
            "`0 <= limit <= 10^9`",
        ],
    },
    339: {
        "body": "You start at position `0` on a number line. In one move, you may jump forward by `a` or backward by `b`. Positions in `forbidden` may not be visited, and two backward jumps may not be made consecutively. Return the minimum number of jumps needed to reach `x`, or `-1` if it is impossible.",
        "constraints": [
            "`1 <= forbidden.length <= 1000`",
            "`1 <= a, b, x <= 2000`",
            "All forbidden positions are distinct and non-negative.",
        ],
    },
    340: {
        "body": "You are given an array of two-letter words. Count index pairs `(i, j)` with `i < j` such that `words[i]` is the reverse of `words[j]`.",
        "constraints": [
            "`0 <= words.length <= 10^5`",
            "`words[i].length == 2`",
            "`words[i]` contains lowercase English letters.",
            "Identical two-letter words can form a pair only when reversing one equals the other.",
        ],
    },
    342: {
        "body": "Given an even-length array `nums`, repeatedly remove the two smallest remaining values. For each pair, append the second-smallest value to the answer, then append the smallest value. Return the final answer array.",
        "constraints": [
            "`2 <= nums.length <= 100`",
            "`nums.length` is even.",
            "`-10^9 <= nums[i] <= 10^9`",
        ],
    },
    345: {
        "body": "You are given stick lengths. In one operation, choose two sticks, connect them into one stick, and pay a cost equal to their combined length. Return the minimum total cost needed to connect all sticks into one stick. If there is only one stick, the cost is `0`.",
        "constraints": [
            "`1 <= sticks.length <= 10^5`",
            "`1 <= sticks[i] <= 10^6`",
            "The answer fits in a 32-bit signed integer in the official tests.",
        ],
    },
    360: {
        "body": "You are given an even number of players, where `skill[i]` is one player's skill. Pair all players so every pair has the same total skill. The chemistry of a pair is the product of its two skills. Return the sum of all pair chemistry values, or `-1` if no valid pairing exists.",
        "constraints": [
            "`2 <= skill.length <= 10^5`",
            "`skill.length` is even.",
            "`1 <= skill[i] <= 10^6`",
            "The answer fits in a 64-bit signed integer.",
        ],
    },
    367: {
        "body": "Given a binary string `s` representing a positive integer, repeatedly apply these operations until the value becomes `1`: if the value is even, divide it by two; if it is odd, add one. Return the number of operations.",
        "constraints": [
            "`1 <= s.length <= 500`",
            "`s` contains only `0` and `1`.",
            "`s` represents a positive integer and has no leading zeroes unless `s == \"1\"`.",
        ],
    },
    370: {
        "body": "You may change any array value in one operation. Return the minimum operations needed so that all even indices contain one value, all odd indices contain a different value, and every adjacent pair has different values.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`1 <= nums[i] <= 10^5`",
        ],
    },
    421: {
        "body": "Given an array of non-negative integers, repeatedly take the first and last remaining values. Concatenate their decimal representations in that order and add the resulting number to the total. If one middle value remains, add it directly. Return the total.",
        "constraints": [
            "`1 <= nums.length <= 1000`",
            "`0 <= nums[i] <= 10^5`",
            "The answer fits in a 64-bit signed integer.",
        ],
    },
    422: {
        "body": "Given two integer arrays sorted in nondecreasing order, return the smallest value that appears in both arrays. If the arrays have no common value, return `-1`.",
        "constraints": [
            "`1 <= nums1.length, nums2.length <= 10^5`",
            "`-10^9 <= nums1[i], nums2[i] <= 10^9`",
            "Both arrays are sorted in nondecreasing order.",
        ],
    },
    423: {
        "body": "Given an integer array `nums`, repeatedly form pairs of equal values. Return an array `[pairs, leftovers]`, where `pairs` is the number of pairs formed and `leftovers` is the number of values not used in any pair.",
        "constraints": [
            "`1 <= nums.length <= 100`",
            "`0 <= nums[i] <= 100`",
        ],
    },
    424: {
        "body": "Given a binary array `groups`, choose a subsequence whose neighboring chosen values are different. Return the maximum possible length of such a subsequence.",
        "constraints": [
            "`1 <= groups.length <= 100`",
            "`groups[i]` is either `0` or `1`.",
        ],
    },
    425: {
        "body": "Given an integer array `nums` and an integer `pivot`, return a stable partition of `nums`: all values less than `pivot` first, then all values equal to `pivot`, then all values greater than `pivot`. The relative order within each of the three groups must be preserved.",
        "constraints": [
            "`1 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i], pivot <= 10^9`",
        ],
    },
    426: {
        "body": "There are people in a queue, and `tickets[i]` is how many tickets person `i` wants to buy. Each second, the person at the front buys one ticket and either leaves if finished or moves to the back. Return how many seconds pass until person `k` finishes buying all tickets.",
        "constraints": [
            "`1 <= tickets.length <= 100`",
            "`1 <= tickets[i] <= 100`",
            "`0 <= k < tickets.length`",
        ],
    },
    427: {
        "body": "A sentence is circular if the last character of each word is equal to the first character of the next word, including the last word followed by the first word. Given a sentence with words separated by single spaces, return whether it is circular.",
        "constraints": [
            "`1 <= sentence.length <= 500`",
            "`sentence` contains one or more words separated by single spaces.",
            "Each word contains English letters, and comparisons are case-sensitive.",
        ],
    },
    431: {
        "body": "Given an integer array `nums` and an integer `target`, count index pairs `(i, j)` such that `i < j` and `nums[i] + nums[j] < target`.",
        "constraints": [
            "`0 <= nums.length <= 10^5`",
            "`-10^9 <= nums[i], target <= 10^9`",
            "The answer fits in a 32-bit signed integer.",
        ],
    },
    432: {
        "body": "An original array was transformed by appending twice each original value and then shuffling. Given the transformed array `changed`, reconstruct any valid original array. Return an empty array if no valid original array exists.",
        "constraints": [
            "`0 <= changed.length <= 10^5`",
            "`changed.length` is even for any valid reconstruction.",
            "`0 <= changed[i] <= 10^5`",
        ],
    },
    433: {
        "body": "Given strings `s` and `t`, return the minimum number of characters that must be appended to the end of `s` so that `t` becomes a subsequence of the resulting string.",
        "constraints": [
            "`0 <= s.length, t.length <= 10^5`",
            "`s` and `t` contain lowercase English letters.",
        ],
    },
    434: {
        "body": "You are given a directed graph with red and blue edges. Return the shortest distance from node `0` to every node using a path whose edge colors strictly alternate. If a node cannot be reached by such a path, its distance is `-1`.",
        "constraints": [
            "`1 <= n <= 100`",
            "`0 <= redEdges.length, blueEdges.length <= 400`",
            "Every edge is a pair `[from, to]` with `0 <= from, to < n`.",
            "Parallel edges and self-loops may appear.",
        ],
    },
    435: {
        "body": "Given a string `s` containing only `a` and `b`, delete the fewest characters so that no `b` appears before an `a` in the remaining string. Return the minimum number of deletions.",
        "constraints": [
            "`1 <= s.length <= 10^5`",
            "`s[i]` is either `a` or `b`.",
        ],
    },
    436: {
        "body": "Given an array of words and a prefix `pref`, return how many words start with `pref`.",
        "constraints": [
            "`0 <= words.length <= 100`",
            "`1 <= words[i].length, pref.length <= 100`",
            "`words[i]` and `pref` contain lowercase English letters.",
        ],
    },
    437: {
        "body": "Given strings `s` and `t`, return the shortest substring of `s` that contains `t` as a subsequence. If there are multiple shortest substrings, return the earliest one. If no such substring exists, return the empty string.",
        "constraints": [
            "`0 <= s.length <= 2 * 10^4`",
            "`0 <= t.length <= 100`",
            "`s` and `t` contain ASCII characters supported by the official tests.",
        ],
    },
    453: {
        "body": "A good substring is a substring of length three whose characters are all distinct. Given a string `s`, return the number of good substrings.",
        "constraints": [
            "`1 <= s.length <= 10^5`",
            "`s` contains lowercase English letters.",
        ],
    },
    458: {
        "body": "A planet starts with mass `mass`. It can destroy an asteroid if its current mass is at least the asteroid's mass; after destroying it, the planet gains that asteroid's mass. Return whether the planet can destroy every asteroid in some order.",
        "constraints": [
            "`1 <= mass <= 10^5`",
            "`0 <= asteroids.length <= 10^5`",
            "`1 <= asteroids[i] <= 10^5`",
        ],
    },
    469: {
        "body": "Given an `n x n` integer grid, choose exactly one value from each row. You may not choose the same column in two adjacent rows. Return the minimum possible sum.",
        "constraints": [
            "`1 <= n <= 200`",
            "`grid.length == n` and `grid[i].length == n`",
            "`-10^9 <= grid[i][j] <= 10^9`",
            "The answer fits in a 32-bit signed integer in the official tests.",
        ],
    },
    474: {
        "body": "A circular binary array `original` produces `derived` where `derived[i] = original[i] XOR original[(i + 1) % n]`. Given `derived`, return whether at least one valid binary `original` array exists.",
        "constraints": [
            "`1 <= derived.length <= 10^5`",
            "`derived[i]` is either `0` or `1`.",
        ],
    },
    478: {
        "body": "A subarray is complete if it contains every distinct value that appears anywhere in the whole array. Given `nums`, return the number of complete subarrays.",
        "constraints": [
            "`1 <= nums.length <= 1000`",
            "`1 <= nums[i] <= 2000`",
        ],
    },
    483: {
        "body": "Given an even-length array `nums`, pair all numbers so that the maximum pair sum is as small as possible. Return that minimized maximum pair sum.",
        "constraints": [
            "`2 <= nums.length <= 10^5`",
            "`nums.length` is even.",
            "`1 <= nums[i] <= 10^9`",
        ],
    },
    484: {
        "body": "Given a string `s`, count the number of distinct palindromic subsequences of length three. Two subsequences are considered the same if their three-character strings are equal, even if they use different indices.",
        "constraints": [
            "`1 <= s.length <= 10^5`",
            "`s` contains lowercase English letters.",
        ],
    },
    492: {
        "body": "You are given a list `nums` and an integer `k`. Starting from the end of the list, scan elements until every value from `1` through `k` has been seen at least once. Return the number of scanned elements.",
        "constraints": [
            "`1 <= nums.size() <= 10^5`",
            "`1 <= k <= nums.size()`",
            "`1 <= nums[i] <= nums.size()`",
            "Every value from `1` through `k` appears at least once in `nums`.",
        ],
    },
}


def render_description(problem: dict, statement: dict) -> str:
    lines = [f"# {problem['title']}", "", statement["body"].strip(), ""]
    for index, example in enumerate(problem.get("examples", []), start=1):
        lines.extend(
            [
                f"## Example {index}:",
                "",
                f"**Input:** {example['input']}",
                f"**Output:** {example['output']}",
                "",
            ]
        )
    lines.extend(["## Constraints:", ""])
    for item in statement["constraints"]:
        lines.append(f"*   {item}")
    return "\n".join(lines)


def main() -> None:
    missing = []
    changed = []
    for number, statement in STATEMENTS.items():
        path = BASE / f"p{number}.json"
        if not path.exists():
            missing.append(str(path))
            continue
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        data["description"] = render_description(data, statement)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        changed.append(path.name)

    if missing:
        raise SystemExit("Missing problem files: " + ", ".join(missing))

    print(f"Updated {len(changed)} official problem statements")
    for name in changed:
        print(f"  {name}")


if __name__ == "__main__":
    main()
