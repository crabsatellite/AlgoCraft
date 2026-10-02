# AlgoCraft Solution Review Audit

This file records whether official solutions teach distinct algorithmic ideas and
tradeoffs. It is intentionally a human review ledger, not a generated completion
table.

Review standard:

- A solution variant must teach a distinct algorithmic idea, data model, or
  tradeoff.
- Naming-only rewrites, loop-direction-only rewrites, and cosmetic refactors do
  not count as new educational coverage.
- A slow baseline is useful only when it is clearly framed as a baseline and is
  still reasonable for the included tests; preferred official reference
  solutions should respect the stated constraints.
- Classic problems should usually expose the canonical optimal approach plus at
  least one meaningful baseline or alternative paradigm.

Legend:

- OK: current coverage is educationally solid.
- ADD: add a distinct missing solution.
- REWORK: existing solutions are too similar or one solution should be replaced.
- DEFER: no immediate code edit; revisit if the track needs a deeper lesson.

## Batch 1: Problems 1-20

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p1 | Two Sum | one-pass hash map; brute force; sorting with original indexes | OK | No edit. Covers optimal, baseline, and sort/two-pointer tradeoff. |
| p2 | Contains Duplicate | hash set; sorting; brute force | OK | Expanded lesson text for membership state, sorted adjacency, and brute-force correctness baseline. |
| p3 | Valid Anagram | fixed array; sorting; hash map | OK | Expanded lesson text for alphabet-bound counting, sorting canonical form, and map-based generalization. |
| p4 | Group Anagrams | sorted-key grouping; character-count string key; custom frequency key object | OK | Added custom frequency key object. This is not another cosmetic hash-map version: it teaches Java structural key modeling, `equals`, `hashCode`, and why raw arrays are unsafe as map keys. |
| p5 | Top K Frequent Elements | bucket sort; min heap; quickselect | OK | Expanded lesson text for bucket frequency ordering, heap size-k maintenance, and QuickSelect tradeoffs. |
| p6 | Product of Array Except Self | output-array prefix/suffix; explicit prefix and suffix arrays | OK | Kept two official solutions intentionally. Division is disallowed by prompt and would teach the wrong constraint model; strengthened explanations and added zero/negative edge tests instead. |
| p7 | Valid Sudoku | per-unit sets; encoded set; bit masks | OK | Expanded explanation ladder and added an explicit row-duplicate test. |
| p8 | Encode and Decode Strings | length prefix; escaping; chunked framing | OK | Expanded practical serialization framing: length payloads, escaping reserved characters, and fixed-width headers. |
| p9 | Longest Consecutive Sequence | hash-set starts; sorting; union find | OK | Expanded boundary-start invariant, duplicate handling after sorting, and graph/component interpretation. |
| p10 | Subarray Sum Equals K | prefix sum + map; prefix sum brute force; brute force | OK | Expanded prefix-count invariant and clarified why negative numbers rule out ordinary sliding windows. |
| p11 | First Missing Positive | cyclic placement; negative marking; hash set | OK | Expanded in-place hashing explanations and added duplicate/full-prefix tests. |
| p12 | Majority Element | Boyer-Moore; hash map; sorting | OK | Expanded cancellation proof, counting baseline, and sorted-middle proof. |
| p13 | Move Zeroes | fast/slow write pointer; two-pass fill; snowball count | OK | Expanded stable compaction, two-pass fill, and zero-gap interpretation. |
| p14 | Find All Numbers Disappeared in an Array | negative marking; cyclic sort; hash set | OK | Expanded in-place marking, duplicate-safe cyclic placement, and membership baseline. |
| p15 | Intersection of Two Arrays II | hash counts; sorting two pointers; bounded counting array | OK | Expanded count preservation, sorted stream merge, and bounded-value tradeoff. |
| p16 | 3Sum | sort + two pointers; per-anchor hash set; frequency map over unique values | OK | Expanded duplicate handling and 2Sum relationship; added duplicate-zero and all-positive tests. |
| p17 | 4Sum | sort + two pointers; generic kSum recursion; pair-sum hash map | OK | Expanded overflow handling, kSum recursion as a family pattern, and pair-sum time/space tradeoff. |
| p18 | Next Permutation | pivot/successor/reverse suffix; binary search in descending suffix; selection-sort suffix baseline | OK | Replaced compact same-family implementation. New set teaches the canonical invariant, the suffix monotonicity refinement, and a slower baseline that explains why reverse is sufficient. |
| p19 | Pascal's Triangle | row DP; combinatorial formula; iterative row building | OK | Expanded DP dependency, binomial-coefficient construction, and right-to-left row compression invariant. |
| p20 | Set Matrix Zeroes | first-row/column markers; row/column boolean arrays; hash set | OK | Expanded marker-state invariant and the progression from explicit row/column state to O(1) space. |

## Batch 2: Problems 21-40

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p21 | Valid Palindrome | in-place two pointers; cleaned string; reverse compare | OK | Expanded normalization invariant, cleaned-string baseline, and reverse-compare tradeoff. |
| p22 | Two Sum II - Input Array Is Sorted | two pointers; binary search per anchor | OK | Kept two solutions intentionally and expanded monotonic pointer proof plus binary-search baseline. |
| p23 | Count Triplets With Smaller Sum | sorted two pointers; brute force | OK | Expanded batch-counting proof and brute-force baseline; added no-triplet test. |
| p24 | Container With Most Water | two pointers; skip optimization; brute force | OK | Expanded elimination proof, dominated-skip refinement, and brute-force baseline. |
| p25 | Trapping Rain Water | two pointers; prefix/suffix DP; monotonic stack | OK | Expanded boundary proof, position formula, and stack basin interpretation. |
| p26 | Remove Duplicates from Sorted Array | write pointer; run-length compression; binary-search boundary; quadratic shift baseline | FIX | Replaced the duplicate fast/slow variant with three real teaching contrasts and added prefix-mutation judge coverage. |
| p27 | Remove Element | stable write pointer; tail swap; two-end partition; bounded counting | FIX | Added prefix-multiset judge coverage and replaced the sentinel-only comparison with real partition/counting teaching routes. |
| p28 | Count Equal Value Pairs | online frequency counting; frequency-table combinations; sort-and-count runs; nested pair scan baseline | FIX | Replaced the two-route lesson with four real teaching contrasts covering incremental counting, grouped combinations, sorted runs, and the definition-level baseline. |
| p29 | Rotate Array | reverse-three-times; extra array destination mapping; cyclic replacements; block swap | FIXED | Added a distinct in-place block-swap route, strengthened cycle explanation, and kept the extra-array mapping as the reference model. |
| p30 | Squares of a Sorted Array | two pointers from both ends; split-and-merge; square-then-sort baseline; absolute-value counting buckets | FIXED | Added a distinct counting-bucket route and tightened all four solution explanations around absolute-value ordering. |
| p31 | Best Time to Buy and Sell Stock | running minimum; suffix future maximum; Kadane on daily gains; brute-force pair baseline | FIXED | Replaced the duplicate compact one-pass variant with distinct suffix-maximum and brute-force teaching routes. |
| p32 | Longest Substring Without Repeating Characters | HashMap last-index window; HashSet shrink-window; ASCII last-index array; brute-force unique-substring baseline | FIXED | Added the explicit brute-force baseline and strengthened left-boundary/substring route separation. |
| p33 | Longest Repeating Character Replacement | exact max-count sliding window; stale max-count sliding window; target-character enumeration window; binary search feasibility | FIXED | Added the target-character enumeration route as a real fourth teaching solution and hardened the route-specific local/remote gates. |
| p34 | Permutation in String | frequency array window; match-count window; deficit-count window; sort-each-window baseline | FIXED | Added the direct sorting baseline as a real fourth teaching route and hardened the route-specific local/remote gates. |
| p35 | Minimum Window Substring | hashmap formed-count window; deficit-array window; filtered sliding window; binary-search feasibility | FIXED | Expanded to four textbook routes and 13 edge tests covering duplicates, case sensitivity, late shorter windows, and impossible targets. |
| p36 | Sliding Window Maximum | monotonic deque; max heap with lazy removal; block decomposition; TreeMap multiset | FIXED | Expanded to four textbook routes and 13 edge tests covering repeated maxima, maxima leaving the window, monotonic arrays, k=1, k=n, and negative arrays. |
| p37 | Find All Anagrams in a String | frequency-array fixed window; incremental match-count window; deficit-count fixed window; sort-each-candidate baseline | FIXED | Replaced the old two-route judgment with four distinct teaching routes: direct frequency comparison, O(n) incremental match accounting, deficit/missing accounting, and a sorting baseline for correctness intuition. |
| p38 | Minimum Size Subarray Sum | positive-number sliding window; prefix-sum lower-bound binary search; answer-length binary search; quadratic running-sum baseline | FIXED | Replaced the old two-route judgment with four textbook routes: optimal positive sliding window, prefix lower-bound search, monotonic feasibility search on answer length, and a quadratic baseline for correctness intuition. |
| p39 | Longest Substring with At Most K Distinct Characters | hashmap sliding window; ASCII array sliding window; last-seen ordered eviction; quadratic distinct-set baseline | FIXED | Added a definition-first quadratic baseline and strengthened the route contrast between count-based windows and jump eviction. |
| p40 | Fruit Into Baskets | hashmap window; bounded count array; last-seen eviction; last-run constant space; quadratic baseline | FIXED | Reworked into five distinct teaching routes: general count-window invariant, value-range array tradeoff, jump eviction by last seen index, the classic last-run O(1) state model, and a definition-level baseline. |

## Batch 3: Problems 41-60

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p41 | Valid Parentheses | bracket-map stack; expected-closer stack; manual array stack; recursive grammar parser; string-reduction baseline | FIXED | Reworked as a textbook entry with five distinct teaching routes, including recursion and reduction baseline instead of only stack rewrites. |
| p42 | Min Stack | duplicate-min two stacks; counted minimum stack; value/min pair stack; encoded difference stack; previous-min sentinel stack | FIXED | Reworked as a design-topic textbook entry with five O(1) routes, including duplicate-min compression and one-stack sentinel restoration. |
| p43 | Evaluate Reverse Polish Notation | standard operand stack; switch-expression stack; manual array stack; recursive right scan | FIXED | Reworked as a textbook entry with four distinct routes, adding a true manual-array-stack implementation and stronger operand-order teaching. |
| p44 | Generate Parentheses | mutable backtracking; immutable-string backtracking; iterative state stack; Catalan split DP; bitmask filter baseline | FIXED | Expanded to five real teaching routes and complete n=1..5 Catalan test coverage. |
| p45 | Daily Temperatures | monotonic stack; right-to-left jumping; bounded temperature next-index lookup; brute-force baseline | FIXED | Upgraded to four distinct teaching routes with strict-equality and boundary-case coverage. |
| p46 | Car Fleet | sorted frontier-time count; explicit arrival-time stack; coordinate bucket scan; quadratic ahead-time baseline | FIXED | Expanded to four teaching routes and added hard gates so remote updates cannot drop route diversity. |
| p47 | Largest Rectangle in Histogram | monotonic stack flush; precomputed smaller boundaries; segment-tree divide-and-conquer; quadratic minimum-scan baseline | FIXED | Expanded to four teaching routes and added hard gates so remote updates cannot drop the histogram route diversity. |
| p48 | Basic Calculator II | signed-term stack; O(1) last-term tracking; explicit two-stack precedence parser; recursive descent by precedence | FIXED | Expanded from two/three routes to four textbook routes; added route gates for stack-top absorption, lastTerm compression, general two-stack precedence, and grammar-level parsing. |
| p49 | Decode String | count/builder stacks; context Frame stack; recursive descent; single token-stack expansion | FIXED | Expanded to four routes and added static/remote route gates so stack-context parsing, frame grouping, grammar recursion, and token expansion cannot collapse into duplicate solutions. |
| p50 | Asteroid Collision | deque stack simulation; input-array in-place stack; ArrayList result stack; quadratic adjacent-pass baseline | FIXED | Expanded to four routes and added static/remote gates so optimized stack simulation, O(1) storage compression, output-order list stack, and direct physical baseline stay distinct. |
| p51 | Binary Search | closed interval; half-open interval; recursive | FIXED | Restored English names/descriptions and clarified interval contracts. |
| p52 | Search a 2D Matrix | flattened binary search; row-then-column binary search; top-right scan | FIXED | Restored English names/descriptions/comments and clarified why flattened search is valid. |
| p53 | Koko Eating Bananas | integer-ceiling answer binary search; Math.ceil variant | FIXED | Restored English explanations; kept two variants because non-binary-search alternatives miss the required constraint model. |
| p54 | Find Minimum in Rotated Sorted Array | compare with right; rotation-point framing | FIXED | Restored English explanations/comments and clarified unrotated handling. |
| p55 | Search in Rotated Sorted Array | one-pass rotated binary search; find pivot then binary search | FIXED | Restored English explanations/comments and separated the two teaching models. |
| p56 | Time Based Key-Value Store | timestamp list + binary search; TreeMap floorEntry | FIXED | Restored English explanations and emphasized timestamp ordering. |
| p57 | Median of Two Sorted Arrays | partition binary search; full merge; two pointers to median | FIXED | Restored English explanations/comments and kept linear baselines as intentional teaching support. |
| p58 | First Bad Version | first-true binary search; closed-interval boundary search | FIXED | Restored English explanations/comments; extra routes would be cosmetic for this API problem. |
| p59 | Search Insert Position | lower-bound search; closed-interval insertion point | FIXED | Restored English explanations and clarified why a failed search still returns useful pointer state. |
| p60 | Find First and Last Position of Element in Sorted Array | two boundary binary searches; two lower_bound calls; Arrays.binarySearch plus boundaries | FIXED | Restored English explanations/comments and clarified manual vs library boundary handling. |

## Batch 4: Problems 61-80

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p61 | Peak Index in a Mountain Array | binary search; linear scan | FIXED | Expanded slope-based binary-search proof and O(n) baseline explanation. |
| p62 | Find Peak Element | binary search; linear scan | OK | Existing explanations are long enough; fixed missing test descriptions. |
| p63 | Capacity To Ship Packages Within D Days | answer binary search; DP with prefix sums | OK | Existing explanations are solid; fixed missing test descriptions. |
| p64 | Split Array Largest Sum | answer binary search; DP | OK | Existing explanations are solid; fixed missing test descriptions. |
| p65 | Reverse Linked List | iterative pointer reversal; recursion | FIXED | Expanded iterative pointer invariant; recursion remains sufficient. |
| p66 | Merge Two Sorted Lists | dummy-node iteration; recursion | FIXED | Expanded recursive merge base case and head-selection logic. |
| p67 | Reorder List | split/reverse/merge; deque | FIXED | Expanded deque extra-space contrast with in-place route. |
| p68 | Remove Nth Node From End of List | one-pass two pointers; two-pass length count | FIXED | Expanded two-pass length-to-index conversion. |
| p69 | Copy List with Random Pointer | hashmap copy; interleaving O(1) extra space | OK | Strong coverage; fixed missing test descriptions. |
| p70 | Add Two Numbers | iterative carry; recursive carry | FIXED | Expanded recursive carry propagation and termination condition. |
| p71 | Linked List Cycle | Floyd; hash set | FIXED | Expanded both cycle-detection proofs. |
| p72 | Find the Duplicate Number | Floyd on value graph; binary search on value range | OK | Fixed missing test descriptions; sorting remains intentionally omitted because the classic constraint forbids modifying input. |
| p73 | LRU Cache | manual hashmap + doubly linked list; LinkedHashMap | FIXED | Expanded LinkedHashMap access-order explanation. |
| p74 | Merge k Sorted Lists | min heap; divide and conquer pair merging; sequential pairwise merge | FIXED | Expanded divide-and-conquer explanation; sequential baseline already present. |
| p75 | Reverse Nodes in k-Group | iterative group reversal; recursion | OK | Good coverage; fixed missing test descriptions. |
| p76 | Palindrome Linked List | reverse second half; stack first half | FIXED | Expanded O(1)-space reverse-half and O(n)-space stack tradeoff. |
| p77 | Intersection of Two Linked Lists | pointer switching; hash set | FIXED | Expanded HashSet identity-vs-value explanation. |
| p78 | Sort List | top-down merge sort; bottom-up merge sort | FIXED | Expanded linked-list merge-sort reasoning for both recursive and iterative routes. |
| p79 | Partition List | two dummy lists; in-place insertion | OK | Good coverage; fixed missing test descriptions. |
| p80 | Rotate List | circle then break; length plus two pointers | OK | Focused two-route coverage is acceptable; fixed missing test descriptions. |

## Batch 5: Problems 81-100

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p81 | Invert Binary Tree | recursive DFS; iterative BFS | OK | Expanded both explanations; two traversal routes are sufficient for this simple mirror operation. |
| p82 | Maximum Depth of Binary Tree | recursive DFS; iterative BFS | OK | Expanded recurrence and level-count explanations. |
| p83 | Diameter of Binary Tree | global max DFS; return-pair DFS | OK | Expanded both state-management models. |
| p84 | Balanced Binary Tree | bottom-up DFS; top-down height recomputation | OK | Expanded optimal sentinel route and recomputation baseline. |
| p85 | Same Tree | recursive DFS; iterative BFS | OK | Expanded structural equality and parallel traversal explanations. |
| p86 | Subtree of Another Tree | structural recursion; string serialization | OK | Expanded candidate-root matching and null-marker serialization reasoning. |
| p87 | Lowest Common Ancestor of a BST | iterative BST walk; recursive BST walk | OK | Expanded the split-point invariant for both forms; no cosmetic third route added. |
| p88 | Binary Tree Level Order Traversal | BFS queue; DFS by level | OK | Expanded queue level-size and DFS level-index explanations. |
| p89 | Binary Tree Right Side View | BFS last-per-level; right-first DFS | OK | Expanded the visible-node invariant in both traversal directions. |
| p90 | Count Good Nodes in Binary Tree | DFS with path max; BFS with path max | OK | Expanded path-maximum state in recursive and iterative forms. |
| p91 | Validate Binary Search Tree | range-bound DFS; inorder traversal; Morris inorder traversal | OK | Morris validation is now present; expanded all three descriptions and fixed test intent text. |
| p92 | Kth Smallest Element in a BST | iterative inorder; recursive inorder; Morris inorder traversal | OK | Morris kth traversal is now present; expanded all three descriptions and fixed test intent text. |
| p93 | Construct Binary Tree from Preorder and Inorder Traversal | recursive hashmap split; recursive boundary split; iterative stack reconstruction | FIXED | Added a real third model: preorder-driven node creation with a stack and inorder pointer. |
| p94 | Binary Tree Maximum Path Sum | global max DFS; return-pair DFS | OK | Expanded complete-path vs extendable-gain state separation. |
| p95 | Serialize and Deserialize Binary Tree | preorder DFS markers; BFS level order | OK | Expanded both codec models; two canonical representations are sufficient. |
| p96 | Symmetric Tree | recursive mirror check; iterative BFS | OK | Expanded mirror-pair invariant in recursive and iterative forms. |
| p97 | Path Sum | recursive DFS; iterative stack traversal | OK | Expanded leaf-only sum checking and iterative accumulated-sum state. |
| p98 | Path Sum II | DFS backtracking; iterative BFS with paths | OK | Expanded backtracking and copied-path tradeoff. |
| p99 | Path Sum III | prefix-sum DFS; brute-force DFS from each node | OK | Expanded prefix-sum counting and baseline start-node enumeration. |
| p100 | Flatten Binary Tree to Linked List | Morris rewiring; reverse postorder recursion; explicit preorder stack | OK | Explicit preorder stack is already present; expanded all three explanations and fixed test intent text. |

## Batch 6: Problems 101-120

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p101 | Implement Trie | array children; hashmap children | OK | Expanded fixed-alphabet vs sparse-child tradeoff. |
| p102 | Add and Search Word | trie DFS wildcard search; words grouped by length | OK | Expanded wildcard branching and length-bucket baseline. |
| p103 | Word Search II | trie + board backtracking; per-word hashset backtracking | OK | Expanded trie pruning and per-word baseline. |
| p104 | Replace Words | trie lookup; hashset prefix matching | OK | Expanded shortest-root invariant in both routes. |
| p105 | Map Sum Pairs | trie prefix sums; hashmap brute force | OK | Expanded delta update and scan baseline. |
| p106 | Kth Largest Element in a Stream | fixed-size min heap; sorted array list | OK | Expanded streaming kth-largest invariant and sorted-list baseline. |
| p107 | Last Stone Weight | max heap; repeated sorting | OK | Expanded heap simulation and sorting baseline. |
| p108 | K Closest Points to Origin | max heap of size k; full sort; Quickselect partition | OK | Quickselect is present; expanded all three explanations and fixed test intent text. |
| p109 | Kth Largest Element in an Array | min heap of size k; quickselect; full sort | OK | Full sort baseline is present; expanded all three explanations and fixed test intent text. |
| p110 | Task Scheduler | greedy formula; heap simulation | OK | Expanded frame formula and operational simulation. |
| p111 | Design Twitter | hashmap + min heap; linked-list k-way merge | OK | Expanded bounded feed heap and k-way merge model. |
| p112 | Find Median from Data Stream | two heaps; sorted list | OK | Expanded balancing invariant and sorted-list baseline. |
| p113 | Smallest Range Covering K Lists | heap with current max; merged-list sliding window | OK | Expanded one-value-per-list heap state and merged-window coverage. |
| p114 | Top K Frequent Words | hashmap sort; min heap; bucket by frequency | OK | Bucket route is present; expanded sort, heap, and bucket explanations. |
| p115 | Find K Pairs with Smallest Sums | heap frontier; brute force sort | OK | Expanded sorted-pair-grid frontier and brute-force baseline. |
| p116 | Subsets | backtracking; bitmask enumeration; iterative cascading | OK | Iterative cascading is present; expanded all three explanations and replaced answer-like image. |
| p117 | Combination Sum | backtracking; bottom-up DP | OK | Expanded unlimited-use backtracking and DP contrast. |
| p118 | Permutations | swap backtracking; used-array backtracking; iterative insertion | OK | Iterative insertion is present; expanded all three explanations. |
| p119 | Subsets II | duplicate-skipping backtracking; iterative build-up | OK | Expanded duplicate-skip and iterative duplicate-window invariants. |
| p120 | Combination Sum II | duplicate-skipping backtracking; counting-based backtracking | OK | Expanded single-use duplicate skipping and count-compressed route. |

## Batch 7: Problems 121-150

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p121 | Word Search | DFS backtracking; frequency-pruned DFS | OK | Expanded visited-cell backtracking and pre-search frequency pruning. |
| p122 | Palindrome Partitioning | backtracking; DP-assisted palindrome lookup | OK | Expanded substring-choice recursion and DP palindrome table. |
| p123 | Letter Combinations of a Phone Number | backtracking; iterative BFS-style expansion | OK | Expanded recursive product generation and iterative layer expansion. |
| p124 | N-Queens | set constraints; boolean-array constraints; bitmask backtracking | OK | Bitmask route is present; expanded all three explanations and fixed test intent text. |
| p125 | Sudoku Solver | backtracking; bitmask constraints | OK | Expanded plain validity checks and bitmask candidate tracking. |
| p126 | Restore IP Addresses | backtracking; iterative split loops | OK | Expanded segment validation and fixed-cut enumeration. |
| p127 | Partition to K Equal Sum Subsets | sorted backtracking; bitmask DP | OK | Expanded bucket search pruning and mask-state DP. |
| p128 | Number of Islands | DFS; BFS; Union-Find | OK | Union-Find is present; expanded all three component-counting routes. |
| p129 | Max Area of Island | DFS; BFS | OK | Expanded both area-count traversals; Union-Find remains omitted as lower teaching value here. |
| p130 | Clone Graph | DFS clone map; BFS clone map | OK | Expanded original-to-copy map invariant in both traversals. |
| p131 | Walls and Gates | multi-source BFS; DFS from gates | OK | Expanded shortest-distance BFS and DFS contrast. |
| p132 | Rotting Oranges | multi-source BFS; timestamp BFS | OK | Expanded minute-layer BFS and in-place timestamping. |
| p133 | Pacific Atlantic Water Flow | reverse-border DFS; reverse-border BFS | OK | Expanded reverse-flow invariant from ocean borders. |
| p134 | Surrounded Regions | border DFS; border BFS; Union-Find sentinel | OK | Union-Find route is present; expanded all three border-safety models. |
| p135 | Course Schedule | Kahn BFS topological sort; DFS cycle detection | OK | Expanded indegree processing and three-state DFS cycle detection. |
| p136 | Course Schedule II | Kahn BFS order; DFS postorder topological sort | OK | Expanded queue order construction and DFS postorder route. |
| p137 | Graph Valid Tree | Union-Find; connectivity traversal | OK | Expanded edge-count, cycle, and connectivity requirements. |
| p138 | Connected Components | Union-Find; DFS | OK | Expanded component-count invariants; BFS remains a traversal substitution. |
| p139 | Redundant Connection | Union-Find; DFS cycle detection | OK | Expanded first cycle-closing edge logic. |
| p140 | Word Ladder | BFS; bidirectional BFS | OK | Expanded shortest-layer BFS and meet-in-the-middle search. |
| p141 | Word Ladder II | BFS parent DAG; layered distance DAG; bidirectional BFS DAG | OK | Added bidirectional BFS reconstruction and expanded all three shortest-path explanations. |
| p142 | Network Delay Time | Dijkstra; Bellman-Ford; Floyd-Warshall | OK | Floyd-Warshall is present; expanded dense all-pairs contrast. |
| p143 | Cheapest Flights Within K Stops | Bellman-Ford by edge count; BFS with pruning; Dijkstra by state | OK | Stateful Dijkstra is present; expanded stop-budget invariants. |
| p144 | Reconstruct Itinerary | recursive Hierholzer; iterative Hierholzer | OK | Expanded Euler-path postorder and stack simulation. |
| p145 | Min Cost to Connect All Points | Prim; Kruskal | OK | Expanded MST cut-growth and sorted-edge lessons. |
| p146 | Alien Dictionary | BFS topological sort; DFS topological sort | OK | Expanded graph-building, prefix rejection, and cycle invariants. |
| p147 | Swim in Rising Water | min-heap Dijkstra; binary search + DFS; Union-Find activation | OK | Union-Find by rising water is present; expanded all three models. |
| p148 | Shortest Path in Binary Matrix | BFS; A* | OK | Expanded equal-edge BFS and admissible Chebyshev heuristic. |
| p149 | Path With Minimum Effort | Dijkstra; binary search + BFS; Union-Find sorted edges | OK | Union-Find route is present; expanded bottleneck-path models. |
| p150 | Is Graph Bipartite? | BFS coloring; DFS coloring; Union-Find neighbor partition | OK | Union-Find partition check is present; expanded all three bipartite proofs. |

## Batch 8: Problems 151-200

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p151 | Climbing Stairs | rolling Fibonacci DP; top-down memoization | OK | Duplicate route already replaced; expanded recurrence contrast. |
| p152 | Min Cost Climbing Stairs | rolling DP; top-down memoization | OK | Duplicate route already replaced; expanded step-cost recurrence. |
| p153 | House Robber | rolling DP; top-down memoization | OK | Duplicate route already replaced; expanded take-or-skip model. |
| p154 | House Robber II | two linear passes; memoized valid ranges | OK | Duplicate route already replaced; expanded circular reduction. |
| p155 | Longest Palindromic Substring | center expansion; DP table; Manacher's algorithm | OK | Manacher route is present; expanded all three palindrome models. |
| p156 | Palindromic Substrings | center expansion; DP table; Manacher radius counting | OK | Manacher counting route is present; expanded occurrence-count logic. |
| p157 | Decode Ways | bottom-up DP; O(1) DP; top-down memoization | OK | Top-down route is present; expanded zero-handling recurrence. |
| p158 | Coin Change | bottom-up DP; BFS shortest path; top-down memoization | OK | Top-down route is present; expanded unbounded-choice recurrence. |
| p159 | Maximum Product Subarray | min/max-product DP; prefix/suffix scan | OK | Duplicate route already replaced; expanded zero-separated segment model. |
| p160 | Word Break | bottom-up DP; BFS cut positions; trie-accelerated DP | OK | Trie route is present; expanded prefix-search DP. |
| p161 | Longest Increasing Subsequence | O(n^2) DP; patience sorting | OK | Expanded direct recurrence and tail-array invariant. |
| p162 | Partition Equal Subset Sum | 0/1 knapsack DP; bitset optimization | OK | Expanded target-sum reduction and bit-parallel transition. |
| p163 | Unique Paths | one-dimensional grid DP; combinatorics | OK | Expanded recurrence and binomial-count contrast. |
| p164 | Longest Common Subsequence | 2D DP; space-optimized DP; top-down memoization | OK | Top-down route is present; expanded all three LCS models. |
| p165 | Best Time to Buy and Sell Stock with Cooldown | state-machine DP; buy/sell memoization | OK | Memoized route is present; expanded cooldown state transitions. |
| p166 | Coin Change II | unbounded knapsack DP; top-down memoization | OK | Expanded combination-counting order and index-state recursion. |
| p167 | Target Sum | subset-sum transform; DFS memoization | OK | Expanded algebraic transform and sign-choice state. |
| p168 | Interleaving String | 2D DP; space-optimized DP; top-down memoization | OK | Top-down route is present; expanded all three interleaving models. |
| p169 | Longest Increasing Path in a Matrix | DFS memoization; topological BFS | OK | Expanded DAG interpretation and layer count. |
| p170 | Distinct Subsequences | 2D DP; space-optimized DP; top-down take/skip | OK | Top-down route is present; expanded take/skip counting. |
| p171 | Edit Distance | bottom-up DP; top-down memoization | OK | Expanded edit-operation recurrence in both directions. |
| p172 | Burst Balloons | bottom-up interval DP; top-down memoization | OK | Expanded last-burst interval recurrence. |
| p173 | Regular Expression Matching | bottom-up DP; recursive memoization | OK | Expanded dot/star transitions and memoized branching. |
| p174 | Maximum Subarray | Kadane; divide and conquer | OK | Expanded ending-sum and crossing-subarray contrast. |
| p175 | Jump Game | left-to-right greedy; right-to-left greedy | OK | Expanded both greedy correctness views. |
| p176 | Jump Game II | greedy BFS levels; bottom-up DP | OK | Expanded optimal layer scan and quadratic baseline. |
| p177 | Gas Station | greedy single pass; brute force with early exit | OK | Expanded failed-segment skip proof and simulation baseline. |
| p178 | Hand of Straights | TreeMap greedy; sort + hashmap | OK | Expanded smallest-card greedy proof. |
| p179 | Merge Triplets to Form Target Triplet | greedy coverage; set-based filtering | OK | Expanded safe-triplet coordinate coverage. |
| p180 | Partition Labels | last occurrence greedy; merge intervals | OK | Expanded partition-end invariant and interval model. |
| p181 | Valid Parenthesis String | min/max greedy; two stacks; top-down balance DP | OK | Added DP state model and expanded wildcard invariants. |
| p182 | Insert Interval | linear scan; binary search + merge | OK | Expanded sorted disjoint interval insertion phases. |
| p183 | Merge Intervals | sort and merge; graph components | OK | Expanded canonical merge and transitive component contrast. |
| p184 | Non-overlapping Intervals | sort by end; sort by start | OK | Expanded two greedy proofs for removal minimization. |
| p185 | Meeting Rooms | sorting check; brute force | OK | Expanded conflict predicate and sorted adjacency proof. |
| p186 | Meeting Rooms II | min heap; chronological sweep | OK | Expanded active-room heap and start/end sweep models. |
| p187 | Minimum Number of Arrows to Burst Balloons | sort by end; sort by start | OK | Expanded earliest-end arrow placement and overlap-window view. |
| p188 | Rotate Image | transpose + reverse; four-way swap; reverse rows + transpose | OK | Added second reflection decomposition and expanded in-place rotation models. |
| p189 | Spiral Matrix | layer simulation; direction vectors | OK | Expanded boundary shrinking and visited-direction simulation. |
| p190 | Minimum Right Rotations to Sort | count descents; try every rotation | OK | Expanded circular descent invariant and rotation baseline. |
| p191 | Happy Number | Floyd cycle detection; hashset cycle detection | OK | Expanded implicit functional graph cycle reasoning. |
| p192 | Plus One | right-to-left carry; explicit carry propagation | OK | Expanded carry stopping and full-carry allocation. |
| p193 | Pow(x, n) | iterative fast power; recursive fast power | OK | Expanded min-int handling and exponentiation-by-squaring recurrence. |
| p194 | Multiply Strings | grade-school multiplication; add-and-multiply | OK | Expanded digit-array carries and partial-product addition. |
| p195 | Detect Squares | hashmap counts; 2D count table | OK | Expanded duplicate-count multiplication and coordinate-table tradeoff. |
| p196 | Single Number | XOR; hashset; math sum formula | OK | Added algebraic distinct-sum baseline and expanded cancellation proof. |
| p197 | Number of 1 Bits | Brian Kernighan; bit shift; byte lookup table | OK | Added byte-table route and corrected Java int statement wording. |
| p198 | Counting Bits | DP by half; DP by last set bit | OK | Expanded both recurrence derivations. |
| p199 | Reverse Bits | bit-by-bit; divide and conquer masks | OK | Corrected signed-int statement wording and expanded 32-step bit reversal. |
| p200 | Missing Number | XOR; Gauss sum; sorting baseline | OK | Added sorting baseline and expanded range-cancellation models. |

## Batch 9: Problems 201-250

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p201 | Sum of Two Integers | iterative bit carry; recursive bit carry | OK | Expanded fixed-width carry invariant. |
| p202 | Reverse Integer | int arithmetic overflow check; bounded string reconstruction | FIX | Replaced long-based string solution to respect no-64-bit statement. |
| p203 | Roman to Integer | left-to-right lookahead; right-to-left max symbol | OK | Expanded subtractive-pair reasoning. |
| p204 | Integer to Roman | greedy value-symbol table; digit lookup tables | OK | Expanded canonical Roman decomposition. |
| p205 | Longest Common Prefix | vertical scan; sort first/last; horizontal scan | FIX | Added horizontal scanning and removed trie solution-hint diagram. |
| p206 | Maximum Width Ramp | monotonic stack; brute force; sorted-index sweep | OK | Existing third route retained; tests expanded. |
| p207 | Remove Duplicates from Sorted List | iterative skip; recursive skip | OK | Expanded sorted-adjacent duplicate invariant. |
| p208 | Remove Duplicates from Sorted List II | dummy-head group skip; recursive group deletion | OK | Expanded duplicate-group deletion invariant. |
| p209 | Implement strStr() | KMP; brute-force window | OK | Expanded fallback table and baseline comparison. |
| p210 | Count and Say | iterative run-length simulation; recursive simulation | OK | Expanded sequence-generation model. |
| p211 | Length of Last Word | reverse scan; trim plus last-space | OK | Renamed baseline and expanded trailing-space handling. |
| p212 | Add Binary | carry simulation; front-insertion carry simulation | OK | Expanded binary addition recurrence. |
| p213 | Sqrt(x) | binary search; Newton method | OK | Expanded floor-root invariant and overflow-safe comparison. |
| p214 | Pascal's Triangle II | in-place DP; combinatorial formula | OK | Expanded reverse-update and binomial recurrence. |
| p215 | Best Time to Buy and Sell Stock II | greedy adjacent gains; state-machine DP | OK | Expanded unlimited-transaction equivalence. |
| p216 | Excel Sheet Column Title | one-offset base-26; recursive conversion | OK | Expanded no-zero digit model. |
| p217 | Excel Sheet Column Number | left-to-right base-26; right-to-left powers | OK | Expanded inverse title conversion. |
| p218 | Factorial Trailing Zeroes | count powers of five; recursive factor count | OK | Expanded repeated factor-of-five counting. |
| p219 | Reverse String | two pointers; half-index iteration | FIX | Replaced recursive O(n)-stack solution with O(1) route. |
| p220 | Reverse Vowels of a String | two pointers; collect and reverse vowels | OK | Expanded vowel-position invariants. |
| p221 | Intersection of Two Arrays | hash set; sort/two pointers; binary search | FIX | Added binary-search membership route and expanded uniqueness reasoning. |
| p222 | First Unique Character in a String | frequency array; hash map; candidate queue | FIX | Added queue-based candidate route and expanded first-index invariants. |
| p223 | Fizz Buzz | modulo; composition; countdown counters | FIX | Added periodic countdown route and corrected Chinese title. |
| p224 | Third Maximum Number | three variables; TreeSet; sort-scan | FIX | Added baseline sort-scan and covered Integer.MIN_VALUE. |
| p225 | Add Strings | two-pointer carry; fixed buffer carry | FIX | Replaced recursion-risk route with iterative fixed-buffer addition. |
| p226 | Number of Segments in a String | segment-start scan; trim/split | EXPLAIN | Expanded segment definition, boundaries, and allocation tradeoff. |
| p227 | Arranging Coins | binary search; quadratic formula | EXPLAIN | Expanded overflow-safe triangular-number reasoning. |
| p228 | Find All Duplicates in an Array | negate marking; cyclic sort | EXPLAIN | Expanded constant-space index-as-bucket invariants. |
| p229 | String Compression | read/write groups; manual count digits | FIX | Added prefix mutation assertion to tests and expanded in-place invariants. |
| p230 | Can Place Flowers | greedy planting; zero-run count | EXPLAIN | Expanded boundary and zero-run reasoning. |
| p231 | Maximum Product of Three Numbers | extreme scan; sort; fixed heaps | FIX | Added third extreme-maintenance route and stronger negative/zero tests. |
| p232 | Baseball Game | list stack; array stack | EXPLAIN | Expanded stack invariant and derived-score cases. |
| p233 | Binary Number with Alternating Bits | xor trick; iterative bit check | EXPLAIN | Expanded all-ones transform and large pattern tests. |
| p234 | To Lower Case | ASCII offset; guarded bitwise OR | EXPLAIN | Expanded printable ASCII boundaries. |
| p235 | Robot Return to Origin | coordinate count; direction count | EXPLAIN | Expanded axis-cancellation invariant. |
| p236 | Sort Array By Parity | two pointers; write-pointer partition | EXPLAIN | Expanded any-valid-order partition reasoning. |
| p237 | Unique Email Addresses | split normalization; manual parsing | EXPLAIN | Expanded local/domain rule boundary. |
| p238 | Range Sum of BST | recursive DFS; iterative BFS; inorder scan | FIX | Added inorder range traversal using BST sorted order. |
| p239 | Verifying an Alien Dictionary | order map; character remapping | EXPLAIN | Expanded prefix and custom-order comparison. |
| p240 | N-Repeated Element in Size 2N Array | HashSet; proximity check | EXPLAIN | Expanded pigeonhole proximity argument. |
| p241 | Count Negative Cells in Sorted Grid | staircase scan; binary search per row; full scan baseline | FIX | Added definition baseline and expanded sorted-grid exclusion reasoning. |
| p242 | Matrix Cells in Distance Order | sort by distance; BFS from center; distance buckets | FIX | Added bucket route and strict property-level validation for distance order and coordinate coverage. |
| p243 | Height Checker | counting sort; clone and sort; selection baseline | FIX | Added no-library baseline and expanded value-range reasoning. |
| p244 | Defanging an IP Address | replace; StringBuilder; split/join | FIX | Added delimiter-based route and corrected transform wording. |
| p245 | Jewels and Stones | hashset; boolean array; nested scan | FIX | Added small-constraint baseline and expanded case-sensitive lookup reasoning. |
| p246 | How Many Numbers Are Smaller Than the Current Number | counting array; sort/index map; brute force | FIX | Added definition baseline and expanded strict-smaller prefix count. |
| p247 | Number of Steps to Reduce a Number to Zero | simulation; bit counting; recursion | FIX | Added recursive rule view and expanded binary contribution formula. |
| p248 | Shuffle the Array | new array interleave; in-place bit packing; queue merge | FIX | Added two-stream merge view and clarified packing bound. |
| p249 | Running Sum of 1d Array | in-place prefix; new-array prefix | EXPLAIN | Expanded negative and cancellation tests; two core prefix variants remain sufficient. |
| p250 | Kids With the Greatest Number of Candies | max scan; sorting baseline | FIX | Replaced overlapping two-pass solution with a genuinely different baseline. |

## Batch 10: Problems 251-300

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p251 | Shuffle String | direct index mapping; sort by index; inverse permutation | FIX | Added inverse-permutation route. |
| p252 | Goal Parser Interpretation | replace; character iteration; startsWith parser | FIX | Added explicit grammar parser route. |
| p253 | Richest Customer Wealth | row max; separate sums | EXPLAIN | Expanded aggregation tests and explanations; two matrix-aggregation variants remain sufficient. |
| p254 | Decode XORed Array | XOR chain; running prefix XOR | EXPLAIN | Expanded XOR identity tests and explanations; one core invariant has two formulations. |
| p255 | Count Items Matching a Rule | linear scan; rule-key index lookup; switch selection | FIX | Added switch route and field-selection tests. |
| p256 | Sorting the Sentence | array placement; sort by suffix index | EXPLAIN | Expanded boundary tests and explanations; two canonical routes are sufficient. |
| p257 | Check if the Sentence Is Pangram | boolean array; hashset; bitmask | FIX | Added bitmask route. |
| p258 | Concatenation of Array | direct copy; System.arraycopy; modulo fill | FIX | Added modulo-index route. |
| p259 | Build Array from Permutation | direct build; in-place encoding | EXPLAIN | Expanded permutation boundary tests; two canonical routes are sufficient. |
| p260 | Final Value of Variable After Performing Operations | middle-character check; contains check; switch | FIX | Added exact-operation switch route. |
| p261 | Maximum Number of Words Found in Sentences | split tokens; manual space count | EXPLAIN | Expanded definitions and corrected the split/space-count naming mismatch. |
| p262 | Left and Right Sum Differences | prefix/suffix arrays; rolling total | EXPLAIN | Expanded left/right exclusion invariant and O(1) rolling derivation. |
| p263 | Convert the Temperature | direct formula; named variables | OK | Formula-trivial problem; two variants are sufficient when precision tests cover boundaries. |
| p264 | Smallest Even Multiple | parity check; gcd/lcm formula | FIX | Replaced superficial bit trick with a real number-theory route. |
| p265 | Divisible and Non-divisible Sums Difference | signed scan; arithmetic formula | EXPLAIN | Expanded num1-num2 invariant and series derivation. |
| p266 | Find Words Containing Character | indexOf scan; manual char scan | EXPLAIN | Expanded matching-once-per-word behavior. |
| p267 | Number of Employees Who Met Target | loop count; stream filter | EXPLAIN | Expanded inclusive threshold reasoning. |
| p268 | Count Pairs Below Target | brute force; sorted two pointers | EXPLAIN | Expanded strict inequality and batch-pair counting. |
| p269 | Faulty Keyboard | StringBuilder reverse; deque direction | FIX | Added the linear deque-direction solution for repeated reversals. |
| p270 | Split Balanced Strings | balance counter; separate counts | EXPLAIN | Expanded greedy cut proof. |
| p271 | Subtract Product and Sum of Digits | arithmetic digits; string digits | EXPLAIN | Expanded digit extraction and zero-digit behavior. |
| p272 | Decompress RLE List | dynamic list; preallocate fill; Arrays.fill runs | FIX | Added a true run-fill route and clarified allocation tradeoffs. |
| p273 | Create Target Array | ArrayList insertion; manual shift | EXPLAIN | Expanded insertion shift mechanics. |
| p274 | XOR Operation | iteration; build array; XOR prefix pattern | FIX | Added O(1) prefix-XOR bit route instead of only two linear variants. |
| p275 | Number of Good Pairs | brute force; online frequency; combination count | FIX | Added combinatorial counting route for stronger teaching coverage. |
| p276 | Design Parking System | array counter; individual fields | EXPLAIN | Expanded state transition rules and stub compile behavior. |
| p277 | Array Strings Equivalent | join compare; chunk two pointers | EXPLAIN | Expanded stream comparison without materializing strings. |
| p278 | Prefix Common Array | shared frequency; set baseline; Fenwick positions | FIX | Added scalable position-query route and fixed set-solution imports/stub. |
| p279 | Semi-Ordered Permutation | position formula; swap simulation | EXPLAIN | Expanded crossing correction and simulation baseline. |
| p280 | Count Consistent Strings | boolean set; bitmask subset | EXPLAIN | Expanded subset-mask reasoning and stub compile behavior. |
| p281 | Count of Matches in Tournament | elimination formula; round simulation | EXPLAIN | Expanded why every match eliminates one team and kept simulation as rule-level baseline. |
| p282 | Truncate Sentence | space-count cut; split/join prefix | EXPLAIN | Expanded k-th space invariant and allocation tradeoff. |
| p283 | Alternating Subarray Length | start each window; rolling expected difference | EXPLAIN | Replaced placeholder descriptions with the expected-difference invariant. |
| p284 | Count Number of Pairs With Absolute Difference K | online counting array; brute force pairs | EXPLAIN | Expanded duplicate pair multiplicity and one-pass counting. |
| p285 | Reverse Prefix of Word | StringBuilder prefix; two-pointer swap | EXPLAIN | Expanded first occurrence rule and prefix swap mechanics. |
| p286 | Count Symmetric Integers | string enumeration; numeric digit split | EXPLAIN | Replaced placeholder descriptions with digit-sum logic. |
| p287 | Rings and Rods | bitmask per rod; boolean color table | EXPLAIN | Expanded duplicate color handling and complete-rod mask. |
| p288 | Keep Multiplying Found Values by Two | hashset lookup; sorted scan | EXPLAIN | Expanded membership-chain behavior and duplicate handling. |
| p289 | Most Frequent Number Following Key In an Array | counting array; hashmap | EXPLAIN | Expanded target counting after key positions and last-key exclusion. |
| p290 | Cells in a Range on an Excel Sheet | nested loops; StringBuilder cells | EXPLAIN | Expanded column-major ordering requirement. |
| p291 | Percentage of Letter in String | count/divide; replace length difference | EXPLAIN | Expanded integer truncation and API alternative. |
| p292 | Strong Password Checker II | single pass; multi-pass validation | EXPLAIN | Expanded per-rule failure coverage. |
| p293 | Decode the Message | substitution array; hashmap | EXPLAIN | Expanded first-occurrence mapping and space preservation. |
| p294 | Check if Matrix Is X-Matrix | diagonal classification; single boolean relation | EXPLAIN | Expanded diagonal/off-diagonal invariants and odd-center case. |
| p295 | Arithmetic Triplets | hashset lookup; boolean array; triple enumeration | FIX | Added definition-level enumeration baseline for stronger teaching coverage. |
| p296 | Number of Common Factors | enumerate min; enumerate gcd factors | EXPLAIN | Expanded gcd factor-pair route. |
| p297 | Sort the People | index sort; TreeMap by height | EXPLAIN | Expanded duplicate-name and unique-height constraints. |
| p298 | Remove Letter To Equalize Frequency | try-removal; frequency-distribution cases | FIX | Replaced duplicate brute-force framing with case analysis and clearer tests. |
| p299 | Odd String Difference | compare difference arrays; key hashmap | EXPLAIN | Expanded common-pattern detection and odd-position cases. |
| p300 | Apply Operations to an Array | in-place simulate/compact; copy result | EXPLAIN | Expanded operation-before-compaction ordering. |

## Batch 11: Problems 301-350

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p301 | Minimum Bouquet Days | answer binary search; sorted candidate days | FIXED | Expanded monotonic feasibility explanation, corrected candidate complexity, and added adjacency/impossibility tests. |
| p302 | Distinct Difference Array | prefix/suffix sets; recompute baseline | FIXED | Replaced out-of-constraints empty-array test and expanded duplicate/last-occurrence coverage. |
| p303 | Count All-One Submatrices | height expansion; monotonic stack; prefix brute force | FIXED | Kept three real routes and added single-cell, column, full, and all-zero cases. |
| p304 | Swap Nodes in Pairs | iterative; recursive | FIXED | Added test descriptions and made the player stub compile with a safe return. |
| p305 | Substring with Concatenation of All Words | word-aligned sliding window; brute force frequency map | FIXED | Added duplicate-word and heavy-overlap cases; clarified offset-window invariant. |
| p306 | Longest Valid Parentheses | stack indices; DP; two-pass counter scan | FIXED | Kept three classic routes and added reset/nesting/unfinished-open tests. |
| p307 | Factorial Zeroes Preimage Size | boundary binary search; exact block search | FIXED | Added skipped-value and plateau tests with clearer preimage-size reasoning. |
| p308 | Shortest Subarray Sum At Least Target | prefix deque; quadratic prefix scan | FIXED | Added negative-number cases that break ordinary positive sliding windows. |
| p309 | Maximum Submatrix Sum No More Than K | column compression + TreeSet; brute force rectangles | FIXED | Added negative cap and larger mixed-sign matrix tests; clarified 2D-to-1D reduction. |
| p310 | Wildcard Matching | DP; greedy two-pointer; memoized recursion | FIXED | Added the recursive DP route and broad star/empty/complex false tests. |
| p311 | Count Good Binary Strings | length DP; memoized length DFS | FIXED | Added exact-length, unreachable, and mixed append-length tests. |
| p312 | N-Queens II | backtracking count; bitmask backtracking | FIXED | Added no-solution and larger-board counts for diagonal bookkeeping. |
| p313 | Permutation Sequence | factorial number system; iterative permutation generation | FIXED | Added first/last/middle factorial-block tests. |
| p314 | Valid Number | flag parser; DFA; grammar regex | FIXED | Added regex grammar route and broad decimal/exponent/sign tests. |
| p315 | Text Justification | greedy line packing; round-robin spaces | FIXED | Added single-word, exact-width, last-line, and narrow-width tests. |
| p316 | Longest Square Streak | hashset walk; sorted DP | FIXED | Corrected Chinese title and added duplicate/large-chain/tie tests. |
| p317 | Count Subarrays With Fixed Bounds | last-seen bounds; enumerate starts | FIXED | Added equal-bound, overlap, and last-seen formula tests. |
| p318 | Maximum Score From Multipliers | bottom-up DP; memoized choice | FIXED | Added mixed-sign and endpoint-choice tests that defeat greedy. |
| p319 | Maximal Rectangle | histogram stack; DP boundaries | FIXED | Added all-one, single-row, single-column, and all-zero tests. |
| p320 | Scramble String | 3D DP; recursive memoization | FIXED | Added anagram-false, duplicate-letter, and nested-swap tests. |
| p321 | Laser Beams Between Security Rows | previous nonempty row; collected counts | FIXED | Replaced invalid empty-bank test, expanded row-product cases, and corrected complexity. |
| p322 | Recover Binary Search Tree | inorder traversal; Morris inorder | FIXED | Added adjacent/non-adjacent swap tree tests and confirmed prompt image shows input only. |
| p323 | Minimum Rounds to Finish Tasks | frequency formula; greedy per count | FIXED | Replaced invalid empty-tasks test and added count 1/3/5/7 grouping cases. |
| p324 | Best Time to Buy and Sell Stock III | four-variable state machine; left-right scan DP; general two-transaction DP | FIXED | Added third DP framing plus single-day/two-day and separated-profit tests. |
| p325 | Best Time to Buy and Sell Stock IV | DP with k transactions; 2D DP table; memoized buy/sell DFS | FIXED | Added top-down state route and large-k/multi-transaction tests. |
| p326 | Word Pattern Bijection | two maps; encoded first positions | FIXED | Added length mismatch and repeated-group bijection tests. |
| p327 | Alternating Digit Sum | string digits; digit array | FIXED | Added zeros, repeated digits, long number, and most-significant-sign tests. |
| p328 | Longest Bounded Difference Window | monotonic deques; TreeMap multiset | FIXED | Added equal-window, large-limit, negative, and tight-limit tests. |
| p329 | Palindrome Partitioning II | center expansion DP; palindrome table + DP; palindrome-edge shortest path | FIXED | Added third graph view plus whole-palindrome and multi-cut tests. |
| p330 | Candy | two-pass greedy; slope counting; rating-order processing | FIXED | Added third rating-order route and slope/plateau tests. |
| p331 | Word Break II | memoized backtracking; DP feasibility + backtracking; trie-guided DFS | FIXED | Added trie-guided route and dense-overlap sentence tests; prompt image is prefix organization only. |
| p332 | Max Points on a Line | slope hashmap; cross-product brute force | FIXED | Added singleton, vertical, horizontal, tie, and extra-point tests. |
| p333 | Find Minimum in Rotated Sorted Array II | duplicate-aware binary search; linear scan | FIXED | Added singleton, all-duplicate, duplicate-surrounded-min, and rotated duplicate tests. |
| p334 | Maximum Gap | bucket sort; radix sort | FIXED | Added all-equal, two-far-values, upper-gap, and irregular bucket-boundary tests. |
| p335 | Dungeon Game | reverse DP; 1D reverse DP; top-down required-health DFS | FIXED | Added top-down route plus positive, negative, row, column, and mixed-grid tests; prompt image is input grid. |
| p336 | The Skyline Problem | line sweep; divide and conquer | FIXED | Added single-building, equal-height merge, reveal-lower-building, and same-span tests; prompt image is buildings only. |
| p337 | Basic Calculator | stack with sign; recursive descent; two stacks with unary minus | FIXED | Added two-stack route plus unary-minus and nested-parenthesis tests. |
| p338 | Shortest Palindrome | KMP; rolling hash; palindrome-prefix baseline | FIXED | Added brute-force baseline plus empty, single, and nontrivial-prefix tests. |
| p339 | Minimum Jumps to Target With Forbidden | BFS with direction state; level BFS | FIXED | Replaced invalid empty-forbidden test and added direction-state/forbidden-boundary cases. |
| p340 | Pair Words By Reversal | frequency reverse lookup; nested pair check | FIXED | Replaced empty-array emphasis with duplicate and palindromic-pair cases. |
| p341 | Remove Invalid Parentheses | BFS removals; DFS pruning; two-pass directional removal | FIXED | Added third classic route and stronger multi-answer boundary tests. |
| p342 | Minimum Number Game | sorted pairs; priority queue | FIXED | Replaced invalid empty test and clarified game-order simulation. |
| p343 | Count of Smaller Numbers After Self | merge sort; BIT; ordered insertion baseline | FIXED | Added baseline route and stricter duplicate/order tests. |
| p344 | Russian Doll Envelopes | sort + LIS binary search; O(n^2) LIS | FIXED | Added same-width and classic tie-order cases. |
| p345 | Minimum Cost to Connect Sticks | min-heap greedy; repeated sorting | FIXED | Added Huffman-style edge tests. |
| p346 | Frog Jump | hashmap jump sizes; 2D DP; DFS memo | FIXED | Added memoized DFS route and first-jump tests. |
| p347 | Trapping Rain Water II | min-heap border BFS; SPFA-like relaxation | FIXED | Added boundary-only, pit, and uneven-boundary tests. |
| p348 | Concatenated Words | word-break DP; DFS hashset; trie DFS | FIXED | Added trie route and missing-piece tests. |
| p349 | Largest Component Size by Common Factor | union find prime factors; sieve-like factor mapping | FIXED | Added coprime, prime-power, and disconnected-component tests. |
| p350 | Sliding Window Median | two TreeMaps; sorted list baseline; lazy dual heaps | FIXED | Added true lazy-deletion heap route and median edge tests. |

## Batch 12: Problems 351-400

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p351 | Smallest Good Base | binary search by digit count; direct computation | FIXED | Repaired overflow-safe geometric sums and added exact small/base-2 tests. |
| p352 | Zuma Game | DFS pruning; BFS state compression | FIXED | Added additional known solvable cases. |
| p353 | Reverse Pairs | merge sort counting; BIT | FIXED | Added negative and integer-overflow tests. |
| p354 | Freedom Trail | ring-position DP; top-down memoization | FIXED | Added repeated-position and rotation-direction tests. |
| p355 | Super Washing Machines | greedy running balance; prefix-sum framing | FIXED | Added flow and impossible-total tests. |
| p356 | Remove Boxes | 3D memoized DP; bottom-up 3D DP | FIXED | Added delayed-merge and no-merge tests. |
| p357 | Student Attendance Record II | six-state DP; matrix exponentiation | FIXED | Added exact small and medium count tests. |
| p358 | K-th Smallest in Lexicographical Order | prefix counting; prefix-tree traversal | FIXED | Added larger prefix-navigation tests. |
| p359 | Find the Closest Palindrome | half-candidate generation; candidate check | FIXED | Added length-boundary tests and repaired prefix-length candidate handling. |
| p360 | Divide Players Into Teams | sort pair ends; frequency pairing | FIXED | Added all-equal, valid-end-pair, and invalid-pairing tests. |
| p361 | Patching Array | greedy coverage extension; explicit tracking | FIXED | Added large-bound and missing-one tests; retained two-route greedy invariant because efficient alternatives reduce to the same proof. |
| p362 | Self Crossing | three crossing conditions; state-machine approach | FIXED | Added endpoint-touch and fifth-segment tests. |
| p363 | Palindrome Pairs | reverse lookup hashmap; trie search | FIXED | Added split-palindrome tests and fixed duplicate pair output in the trie route. |
| p364 | Data Stream as Disjoint Intervals | TreeMap intervals; boolean array merge; TreeSet value scan | FIXED | Expanded from one design test and added a third baseline implementation. |
| p365 | Max Sum of Rectangle No Larger Than K | column compression + TreeSet; row compression baseline | FIXED | Added negative-bound and larger mixed-matrix tests. |
| p366 | Perfect Rectangle | corner counting + area; line sweep | FIXED | Added single, tiling, and duplicate-overlap tests. |
| p367 | Steps to Reduce Binary String to One | carry scan; BigInteger simulation | FIXED | Added all-ones and power-of-two tests with explicit descriptions. |
| p368 | Arithmetic Slices II - Subsequence | hashmap DP per index; array-of-maps variant | FIXED | Added short progression, equal-four, and minimum-length tests. |
| p369 | Poor Pigs | information-theory base; logarithmic calculation | FIXED | Added one-bucket, exact-state, and large-state tests. |
| p370 | Minimum Operations to Make Array Alternating | top frequencies by parity; try candidate values | FIXED | Added parity conflict test and corrected its expected value to 2. |
| p371 | Cut Off Trees for Golf Event | sort trees + BFS; sort trees + A* | FIXED | Added no-tree, blocked-row, and small-grid path tests. |
| p372 | 24 Game | backtracking all operations; array reduction | FIXED | Added fractional and all-one tests. |
| p373 | Stickers to Spell Word | bitmask DP; DFS memo by remaining string | FIXED | Added reusable-sticker, two-sticker, and larger memoization tests. |
| p374 | Falling Squares | interval simulation; coordinate compression + segment tree | FIXED | Added separated and chained-overlap tests. |
| p375 | Reach a Number | sum/parity math; binary search starting step | FIXED | Added negative-target and parity-boundary tests. |
| p376 | Cracking the Safe | De Bruijn Euler DFS; iterative De Bruijn construction | FIXED | Added deterministic k=1 base cases. |
| p377 | Couples Holding Hands | greedy swap; union-find cycle counting | FIXED | Added natural, one-cross-pair, and component-size tests. |
| p378 | Max Chunks To Make Sorted II | monotonic stack; prefix-sum comparison | FIXED | Added duplicate forced-merge tests. |
| p379 | Basic Calculator IV | polynomial recursive parser; stack evaluator | FIXED | Added like-term merge and zero-polynomial tests. |
| p380 | Race Car | dynamic programming; BFS states | FIXED | Added small target and larger DP split tests. |
| p381 | Making A Large Island | DFS component labeling; union-find sizes | FIXED | Added all-water, separated-corner, and duplicate-neighbor tests. |
| p382 | Unique Paths III | backtracking; bitmask DP | FIXED | Added adjacent-end, single-empty, and isolated-start tests. |
| p383 | Minimize Malware Spread | union-find component analysis; DFS labeling; simulation per removed initial node | FIXED | Added baseline simulation route and tie/unsorted-initial tests. |
| p384 | Three Equal Parts | count ones and match pattern; direct comparison | FIXED | Added all-zero and trailing-zero alignment tests. |
| p385 | Cat and Mouse | bottom-up minimax BFS; top-down DFS memoization | FIXED | Added direct mouse-win and forced cat-win tests. |
| p386 | Number of Music Playlists | length/unique DP; 1D optimization | FIXED | Added single-song, permutation, and repeat-gap tests. |
| p387 | Minimize Malware Spread II | BFS per removal; union-find after removal | FIXED | Added cases distinguishing full removal from p383 initial-list removal. |
| p388 | Least Operators to Express Number | DFS base-x representation; iterative digit DP | FIXED | Added exact-x and exact-square tests. |
| p389 | Binary Tree Cameras | greedy DFS states; tree DP states | FIXED | Added single-root, balanced-root-camera, and short-chain tests. |
| p390 | Equal Rational Numbers | expand repeating decimal; exact fraction comparison | FIXED | Added false and equivalent finite/repeating rational tests. |
| p391 | Tallest Billboard | DP on height difference; meet in the middle | FIXED | Added small balance, equal rods, and larger meet-in-the-middle tests. |
| p392 | Triples with Bitwise AND Equal To Zero | pairwise AND counting; SOS DP | FIXED | Added singleton, two-value, and zero-mixed tests. |
| p393 | Minimum Cost to Merge Stones | interval DP; top-down memoization | FIXED | Added one-pile, merge-all-once, and symmetric-cost tests. |
| p394 | Grid Illumination | line-count hashmaps; long key encoding; brute-force lamp scan baseline | FIXED | Added teaching baseline plus duplicate-lamp and shutoff-radius tests. |
| p395 | Recover a Tree From Preorder Traversal | iterative stack; recursive parser | FIXED | Added single-root and mixed multi-digit tree tests. |
| p396 | Stream of Characters | reverse trie; Aho-Corasick | FIXED | Expanded from one trace to four design traces. |
| p397 | Escape a Large Maze | limited BFS from both sides; limited DFS | FIXED | Added trapped-target and non-enclosing-block tests. |
| p398 | Parsing A Boolean Expression | stack evaluation; recursive descent | FIXED | Added single-negation, all-true AND, and nested-false tests. |
| p399 | Smallest Sufficient Team | bitmask DP; path reconstruction | FIXED | Added single-skill, one-person-all-skills, and forced-all-singletons tests. |
| p400 | Longest Chunked Palindrome Decomposition | greedy two pointers; rolling hash | FIXED | Added repeated-char, outer-chunk, and no-match tests. |

## Batch 13: Problems 401-450

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p401 | Design HashMap | direct-address array; linked-list bucket variant | FIXED | Corrected the first route from mislabeled chaining to direct addressing; added boundary and collision-style tests. |
| p402 | Design HashSet | boolean direct-address table; chained hash table | OK | No edit. Good contrast between constraint-specific and general hashing. |
| p403 | Design Linked List | singly linked sentinel; doubly linked sentinels | OK | No edit. Good design coverage. |
| p404 | Design Skiplist | multi-level list; node-object skiplist | OK | No edit. Strong coverage for probabilistic design. |
| p405 | Design Underground System | two hash maps; array payload maps | OK | No edit. Appropriate design alternatives. |
| p406 | Design Browser History | ArrayList pointer; doubly linked list | OK | No edit. Good coverage of append-truncation vs pointer navigation. |
| p407 | Design Circular Queue | circular array; linked-list circular queue | OK | No edit. Good data-structure contrast. |
| p408 | Design Circular Deque | circular array; doubly linked deque | OK | No edit. Good coverage. |
| p409 | Design Front Middle Back Queue | balanced deques; ArrayList baseline | OK | No edit. Strong enough for a medium design task. |
| p410 | Design Ordered Stream | array pointer; hash map pointer | OK | No edit. Simple but clear. |
| p411 | Design Authentication Manager | expiry hash map; linked hash map lazy cleanup | OK | No edit. Good coverage of lazy deletion. |
| p412 | Design Movie Rental System | TreeSet indexes; sort-on-query baseline | OK | No edit. Strong coverage of online ordered queries. |
| p413 | Design Bitset | two arrays with flip flag; XOR flag | OK | No edit. Good coverage of lazy flip. |
| p414 | Design Video Sharing Platform | priority queue ID reuse; TreeSet ID reuse | OK | No edit. Good design alternatives. |
| p415 | Design Memory Allocator | array simulation; interval list simulation | OK | No edit. Good baseline and interval model. |
| p416 | Design SQL | hash map of tables; row id tracking | OK | No edit. Sufficient for local design scope. |
| p417 | Design Graph With Shortest Path Calculator | Dijkstra adjacency list; Floyd-Warshall matrix | OK | No edit. Strong classic alternatives. |
| p418 | Range Sum Query - Immutable | prefix sums; sqrt decomposition | OK | No edit. Good query preprocessing contrast. |
| p419 | Range Sum Query 2D - Immutable | 2D prefix sum; row-wise prefix sum | OK | No edit. Good coverage. |
| p420 | Range Sum Query - Mutable | Fenwick tree with stored values; recursive segment tree; iterative segment tree; mutable block decomposition; direct scan baseline | FIXED | Expanded from the canonical pair into five distinct teaching routes and added route-specific quality gates. |
| p421 | Find the Array Concatenation Value | string two-pointers; arithmetic digit multiplier; deque simulation; recursive inward reduction; constraint-based power thresholds | FIXED | Expanded from three routes to five distinct teaching routes and added route-specific quality gates. |
| p422 | Minimum Common Value in Two Sorted Arrays | merge-style two pointers; duplicate-block skipping; hash set lookup; shorter-array binary search; alternating lower-bound jumps | FIXED | Expanded from three routes to five distinct sorted-array teaching routes and added route-specific quality gates. |
| p423 | Maximum Number of Pairs in Array | bounded frequency array; HashMap counts; toggle set; sort adjacent values; bounded parity table | TESTED | Expanded to five textbook routes with 8 edge tests and matching local/remote quality gates. |
| p424 | Longest Unequal Adjacent Groups Subsequence | run-boundary greedy; explicit run compression; two-state DP; quadratic subsequence DP; recursive memoized choice | TESTED | Clarified subsequence/run invariant, expanded to five textbook routes, and added local/remote quality gates. |
| p425 | Partition Array by Pivot | three-pass stable fill; three buckets; pre-counted stable index fill; opposite-end stable fill; category/original-index sort | TESTED | Expanded stable partition coverage to five textbook routes with 9 tests and matching local/remote quality gates. |
| p426 | Time Needed to Buy Tickets | single-pass capped contribution; split prefix/suffix caps; queue simulation; cyclic array simulation; round-layer counting | TESTED | Expanded final-round reasoning to five textbook routes with 9 edge tests and matching local/remote quality gates. |
| p427 | Circular Sentence Check | split word-boundary comparison; space-boundary scan; delimiter-jump scan; boundary-character arrays; recursive word-link checking | TESTED | Expanded to five textbook routes with 10 edge tests and matching local/remote quality gates. |
| p428 | Design a Stack With Increment Operation | lazy increment array; direct fixed-array increment; linked-list bottom iterator; Fenwick range-add ledger; segment-tree range-add ledger | TESTED | Expanded to five textbook data-structure routes with 7 serialized traces and matching local/remote quality gates. |
| p429 | Design a Leaderboard | TreeMap score counts; sort-on-query; min-heap Top-K; bounded score buckets; TreeSet player records | TESTED | Expanded to five textbook data-structure routes with 7 serialized traces and matching local/remote quality gates. |
| p430 | Design File System | full-path HashMap; object trie nodes; array-indexed trie; path-id registry; parent-children index | TESTED | Expanded to five textbook data-structure routes with 7 serialized traces and matching local/remote quality gates. |
| p431 | Count Pairs Below Target | sorted two pointers; sorted binary-search counting; online Fenwick compression; merge-sort pair counting; brute-force pairs | TESTED | Expanded to five textbook pair-counting routes with 9 boundary tests and matching local/remote quality gates. |
| p432 | Original Array From Doubled Values | sorted count map; bounded counting array; sorted demand queue; TreeMap frequency greedy; used-index baseline | TESTED | Expanded to five textbook doubled-multiset routes with 10 boundary tests and matching local/remote quality gates. |
| p433 | Append Characters to Make Subsequence | two-pointer prefix scan; IndexOf search; position-list binary search; character index queues; next-occurrence table | TESTED | Expanded to five textbook subsequence-matching routes with 10 boundary tests and matching local/remote quality gates. |
| p434 | Shortest Alternating Color Paths | last-color BFS; layered graph BFS; Dijkstra on color-state graph; Bellman-Ford state relaxation | TESTED | Expanded from two main routes to four teaching routes and added local/remote gates for the required solution set. |
| p435 | Minimum Deletions to Make String Balanced | one-pass deletion-choice DP; prefix/suffix split counts; unmatched-b greedy stack; longest kept balanced subsequence; two-state DP | TESTED | Expanded to five teaching routes and added local/remote gates for tests and solution coverage. |
| p436 | Count Words With Prefix | library scan; manual character check; Trie pass counting; sorted-range binary search; prefix-frequency table | TESTED | Expanded to five teaching routes and added local/remote gates for tests and solution coverage. |
| p437 | Minimum Window Subsequence | forward/backward scan; next occurrence table; position-list binary search; forward start-DP; backward end-DP | TESTED | Expanded to five textbook routes with 15 boundary tests and matching local/remote quality gates. |
| p438 | LFU Cache | LinkedHashSet frequency buckets; LinkedHashMap value buckets; intrusive node lists per frequency; frequency-bucket linked list; direct-address key table with frequency buckets | TESTED | Removed the O(log n) TreeSet answer, expanded to five O(1) teaching routes, added 9 design traces, and added matching local/remote gates. |
| p439 | Insert Delete GetRandom O(1) | ArrayList index map; dynamic int-array index map; dense-slot HashMaps; entry-object dense array; primitive open-addressing index map | TESTED | Expanded to five dense-index teaching routes, added 10 randomized design traces, and added matching local/remote quality gates. |
| p440 | Insert Delete GetRandom O(1) - Duplicates allowed | LinkedHashSet index buckets; dynamic int-array index sets; dense-slot HashMaps with index sets; occurrence-entry dense array; index bag with back-pointers | TESTED | Expanded from two routes to five textbook routes and added duplicate occurrence-index tests for the local and remote quality gates. |
| p441 | All O(1) Data Structure | key-to-bucket list; count-map bucket index; circular bucket list with key records; intrusive key-node buckets; bounded count-bucket array | TESTED | Expanded from three bucket-list variants to five textbook O(1) routes and added tie-safe bucket-transition tests to local and remote quality gates. |
| p442 | Snapshot Array | TreeMap floor-entry histories; ArrayList change logs with binary search; sparse HashMap histories; pending-write commits at snap; primitive dynamic arrays per index | TESTED | Expanded from two routes to five textbook routes and added persistent-version/sparse-history tests to local and remote quality gates. |
| p443 | Stock Price Fluctuation | TreeMap price counts; dual heaps with lazy deletion; ordered active-record TreeSet; dynamic segment tree over price values; HashMap counts with on-demand extremum rebuild | TESTED | Expanded from three routes to five textbook routes and added correction/extremum-invalidation tests to local and remote quality gates. |
| p444 | Simple Bank System | zero-indexed array simulation; one-indexed padded array ledger; HashMap account table; account-object ledger; List ledger with transaction guards | TESTED | Expanded from three routes to five textbook routes and added atomic-failure/boundary transaction tests to local and remote quality gates. |
| p445 | Design ATM Machine | fixed-array greedy plan; temporary inventory copy; in-place greedy rollback; recursive denomination planner; denomination-object ledger | TESTED | Expanded from three routes to five textbook routes and added greedy-priority/rollback tests to local and remote quality gates. |
| p446 | Design Food Rating System | ordered set per cuisine; lazy heap with current-rating map; rating buckets per cuisine; indexed binary heap per cuisine; square-root cuisine blocks | TESTED | Expanded from two useful routes to five textbook routes and added stale-snapshot, tie-breaking, bucket-cleanup, cuisine-isolation, and rating-boundary tests to local and remote quality gates. |
| p447 | Design Number Container System | TreeSet of indices per number; lazy min-heap per number; global ordered number-index records; indexed min-heap per number; dynamic segment tree per number | TESTED | Expanded from two useful routes to five textbook routes and added replacement-cleanup, stale-index, current-min-removal, multi-number-isolation, sparse-boundary, and missing-number tests to local and remote quality gates. |
| p448 | Design Text Editor | two StringBuilder halves; single StringBuilder cursor index; doubly linked-list cursor; gap buffer; implicit Treap rope | TESTED | Expanded from two useful routes to five textbook routes and added empty-delete, middle-insert, clamp, last-10-window, repeated-cursor-move, and long-text right-side preservation tests to local and remote quality gates. |
| p449 | My Calendar I | TreeMap neighbor check; brute-force interval scan; sorted arrays with binary-search insertion; sweep-line rollback; dynamic segment-tree occupancy | TESTED | Expanded from two useful routes to five textbook routes and added same-start, same-end, contained, enclosing, endpoint-touching, coordinate-bound, rollback, sorted-gap, unsorted-insertion, and one-point-overlap traces to local and remote quality gates. |
| p450 | My Calendar II | single/double booking lists; TreeMap sweep-line rollback; dynamic segment-tree max count; coordinate-compression rebuild; ordered disjoint segment counts | TESTED | Expanded from two useful routes to five textbook routes and added endpoint-touching, rollback, coordinate-bound, same-start, same-end, contained-triple, non-contiguous-double-region, and dense local triple traces to local and remote quality gates. |

## Batch 14: Problems 451-500

| ID | Title | Existing coverage | Review verdict | Action |
| --- | --- | --- | --- | --- |
| p451 | My Calendar III | TreeMap sweep-line delta; dynamic segment tree; coordinate compression; ordered disjoint segment counts; start-endpoint scan baseline | FIXED | Expanded from two tests/three routes to 10 maximum-overlap tests and five teaching routes; added remote quality gates for weak tests and missing routes. |
| p452 | Subarray Product Less Than K | sliding-window product; reset-aware sliding window; prefix-log binary search; brute-force early break | FIXED | Replaced duplicate Prefix Common Array with a new problem, added 10 strict-product tests, and added remote quality gates for weak tests and missing routes. |
| p453 | Count Good Substrings of Length Three | fixed three-character comparison; frequency-window duplicate count; three-character bitmask; HashSet window baseline | FIXED | Expanded to 10 fixed-window distinctness tests and four teaching routes; added remote quality gates for weak tests and missing routes. |
| p454 | Sum of Digits of String After Convert | literal numeric-string simulation; direct first digit sum; recursive transform decomposition; bounded digital-root finish | FIXED | Expanded from three weak routes to four distinct teaching routes and added local/remote gates for repeated-transform edge coverage. |
| p455 | Find Missing Observations | quotient-remainder distribution; all-ones greedy top-up; lower-bound feasibility fill; upper-reserve fill | FIXED | Replaced the weak three-route review with four distinct constructive lessons and added local/remote gates for dice-sum feasibility coverage. |
| p456 | Walking Robot Simulation | encoded HashSet step simulation; direction-vector rotation simulation; TreeSet axis jumps; sorted-list binary-search jumps | FIXED | Expanded from three routes with only four tests to four textbook routes and 12 blocking/direction edge tests; added local and remote quality gates for weak tests and missing routes. |
| p457 | Circular Array Loop | fast/slow in-place marking; visited color traversal; path-index map detection; timestamped path arrays | FIXED | Expanded from three routes and six tests to four textbook routes and 12 direction/self-loop edge tests; added local and remote quality gates for weak tests and missing routes. |
| p458 | Destroying Asteroids | sort-and-absorb greedy; min-heap choose-smallest; counting-sort frequency sweep; TreeMap frequency sweep | FIXED | Renamed the problem, aligned tests/zh_cn, and added local plus remote gates requiring all four teaching routes. |
| p459 | Repeated Substring Pattern | doubled-string interior search; divisor length verification; KMP prefix function; Z-function period check | FIXED | Expanded from three routes to four structurally distinct teaching routes and added local plus remote gates requiring each route. |
| p460 | Island Perimeter | exposed-boundary scan; shared-edge formula; DFS boundary count; BFS boundary count | FIXED | Added BFS route, clarified the no-lake contract, expanded to 12 edge-shape tests, and added local plus remote gates requiring all four routes. |
| p461 | Hamming Distance | XOR library bit count; Kernighan bit clearing; fixed-width bit scan; nibble lookup table | FIXED | Expanded from three compact routes to four explicit teaching routes, added 12 bit-boundary tests, and added local plus remote gates requiring each route. |
| p462 | Minimum Moves to Equal Array Elements II | sort-to-median absolute distance; paired extremes median interval; randomized three-way quickselect median | FIXED | Expanded to 12 boundary cases and added local/remote gates requiring all three textbook routes. |
| p463 | Student Attendance Record I | single-pass counters; direct independent rule checks; finite-state automaton; regex rejection | FIXED | Expanded to 12 attendance-rule boundary cases and added local/remote gates plus a direct Judge regression for all four routes. |
| p464 | Reverse Words in a String III | hand-written tokenize and reverse; in-place word-boundary reverse; direct span copy; manual word buffer | TESTED | Expanded to 12 tests and added local/remote/Judge gates for all four teaching routes. |
| p465 | Maximum Depth of N-ary Tree | recursive child-max DFS; level-order BFS; iterative depth stack; iterative postorder height map | TESTED | Expanded to 12 tests and added local/remote/Judge gates for all four teaching routes. |
| p466 | Array Partition | library-sort adjacent greedy; bounded counting sweep; min-heap adjacent pairing; manual merge-sort pairing | TESTED | Expanded to 12 tests across samples, negatives, duplicates, zeros, extremes, mixed signs, and longer unsorted arrays. |
| p467 | Binary Tree Tilt | recursive postorder accumulator; recursive sum-and-tilt pair; two-stack iterative postorder; visited-flag iterative postorder | TESTED | Expanded to 12 tests across samples, empty/singleton trees, one-sided chains, negative values, balanced trees, and zero-tilt symmetry. |
| p468 | Reshape the Matrix | linear index mapping; source-scan target counter; array flatten and rebuild; queue flatten and fill | TESTED | Expanded to 12 tests across valid and invalid reshape, row/column conversions, negative values, same-shape identity, and rectangular conversions. |
| p469 | Minimum Falling Path Sum Without Same Column | full-table two-minimum DP; prefix/suffix minima DP; rolling two-minimum DP; in-place two-minimum DP | TESTED | Expanded to four accepted O(n^2) teaching routes and 12 required tests; removed TLE-prone O(n^3) reference routes from the executable answer set. |
| p470 | Distribute Candies | hash set distinct cap; sorted distinct count; direct-address boolean table | TESTED | Expanded to 12 tests covering capacity caps, exact-capacity cases, duplicates, negative values, and value-range endpoints. |
| p471 | Longest Harmonious Subsequence | hash-map frequency count; sorted sliding window; ordered frequency map | TESTED | Expanded to 12 tests covering singleton/all-equal zero answers, no-adjacent arrays, negative values, range endpoints, and competing adjacent-value pairs. |
| p472 | Range Addition II | single-pass minimum bounds; explicit rectangle intersection; sorted-bound extraction | TESTED | Expanded to 12 tests covering empty ops, repeated full-cover ops, mixed overlaps, max dimensions, and limiting row/column boundaries. |
| p473 | Minimum Index Sum of Two Lists | hash-map index lookup; index-sum layer search; brute-force index-sum baseline | TESTED | Expanded to 12 cases covering samples, singleton input, strings with spaces, all-tie outputs, late ties, reversed-list ties, and non-first common restaurants. |
| p474 | Neighboring Bitwise XOR Validity | XOR parity invariant; zero-start candidate construction; two-start rolling simulation | TESTED | Expanded to 12 parity and circular-closing cases; duplicate parity-only construction replaced with explicit array reconstruction. |
| p475 | Construct String from Binary Tree | recursive StringBuilder preorder; recursive returned-string grammar baseline; explicit stack token simulation | TESTED | Expanded to 12 cases covering singleton, left-only/right-only structures, missing-left ambiguity, full trees, negative values, and internal gaps. |
| p476 | Merge Two Binary Trees | recursive new-tree merge with cloning; recursive in-place DFS; iterative BFS in-place merge | TESTED | Expanded to 12 overlap, empty-tree, negative, asymmetric, full-tree, zero-cancellation, deep-chain, and null-placeholder boundary tests. |
| p477 | Maximum Distance in Arrays | update-order global endpoint scan; sorted endpoint candidates; prefix/suffix exclusion arrays | TESTED | Expanded to 12 different-array endpoint tests covering samples, equal singleton arrays, negatives, same-array global extremes, increasing endpoints, value bounds, interior arrays, and all-zero distances. |
| p478 | Count Complete Subarrays | HashMap sliding window; enumeration baseline; bounded frequency-array sliding window | TESTED | Expanded to 12 cases covering samples, empty/singleton arrays, all-same/all-distinct arrays, repeated two-value arrays, staggered repeats, grouped duplicates, value bounds, late new distinct values, and repeated runs. |
| p479 | Average of Levels in Binary Tree | BFS level-order queue; recursive DFS level accumulators; iterative DFS explicit depth stack | TESTED | Expanded to 12 level-average cases covering samples, singleton and negative trees, int-boundary overflow pressure, sparse trees, left/right skewed chains, fractional levels, and mixed large values. |
| p480 | Maximum Average Subarray I | sliding-window running sum; prefix-sum window queries; answer-space binary search | TESTED | Rewrote explanations and tests; all three reference routes are now enforced by quality gates and covered by a real Judge acceptance test. |
| p481 | Set Mismatch | in-place index marking; sum and square equations; XOR partitioning; sort and gap scan | TESTED | Rewrote explanations and tests; all four reference routes are now enforced by quality gates and covered by a real Judge acceptance test. |
| p482 | Maximum Length of Pair Chain | greedy by end; DP by start; binary-search tail ends | TESTED | Rewrote explanations and tests; all three reference routes are now enforced by quality gates and covered by a real Judge acceptance test. |
| p483 | Minimize Maximum Pair Sum in Array | sort opposite ends; answer-space binary search; TreeMap ordered multiset | TESTED | Removed duplicate deque formulation and invalid bucket-by-max route, then added value-bound and duplicate-pattern tests plus Judge and remote-refresh gates. |
| p484 | Count Length-Three Palindromic Subsequences | first/last window; prefix/suffix presence tables; streaming bitmasks | TESTED | Rewrote garbled Chinese, expanded distinct-string palindrome tests, and added local, remote-refresh, and Judge gates for all three reviewed routes. |
| p485 | Dota2 Senate | two-queue index greedy; single-queue pending bans; boolean-array round simulation; round-buffer simulation | TESTED | Rewrote the problem as a circular-order greedy lesson, expanded to 16 boundary tests, fixed the Chinese translation, and added local, remote-refresh, and Judge gates for all four routes. |
| p486 | 2 Keys Keyboard | prime factorization; largest-divisor recurrence; bottom-up divisor DP; top-down memoized divisor DP | TESTED | Rewrote the lesson around copy-paste growth phases, expanded to 17 factorization boundary tests, fixed the Chinese translation, and added local, remote-refresh, and Judge gates for all four routes. |
| p487 | Find Duplicate Subtrees | postorder string serialization; canonical integer IDs; structural-equality buckets | TESTED | Rewrote the lesson around duplicate-kind semantics, expanded to 12 structural boundary tests, fixed the Chinese translation, and added local, remote-refresh, and Judge gates for all three routes. |
| p488 | Two Sum IV - Input is a BST | DFS hash set; BFS hash set; inorder list with two pointers; dual BST iterators; direct BST complement search | TESTED | Expanded to five teaching routes and 12 edge tests, with Judge and remote-cache rejection coverage. |
| p489 | Maximum Binary Tree | recursive maximum split; monotonic-stack Cartesian tree; segment-tree RMQ; sparse-table RMQ | TESTED | Expanded to four teaching routes and 12 tree-shape tests, with Judge and remote-cache rejection coverage. |
| p490 | Print Binary Tree | DFS height/offset placement; BFS coordinate queues; column-interval recursion; coordinate collection then materialize | TESTED | Expanded to four teaching routes and 12 exact matrix-layout tests, fixed the Chinese mojibake translation, and added local, remote-refresh, and Judge gates. |
| p491 | Kth Smallest Number in Multiplication Table | smaller-dimension value binary search; early-stopping feasibility; staircase count; quotient-grouped count | TESTED | Expanded HARD-topic coverage to four counting routes and 12 rank-boundary tests, fixed the Chinese mojibake translation, and added local, remote-refresh, and Judge gates. |
| p492 | Minimum Operations to Collect Elements | reverse HashSet removal; boolean remaining count; BitSet reverse scan; rightmost-boundary scan | TESTED | Expanded to four suffix-collection routes and 12 boundary tests, fixed the Chinese mojibake translation, and added local, remote-refresh, and Judge gates. |
| p493 | Find K Closest Elements | left-bound binary search; two-pointer shrink window; insertion-point expansion; bounded max-heap ranking | TESTED | Expanded to four teaching routes and 12 closest-window boundary tests, fixed the Chinese mojibake translation, and added local, remote-refresh, and Judge gates. |
| p494 | Split Array into Consecutive Subsequences | two-map greedy; global active-subsequence heap; length-bucket greedy; tail-grouped shortest-length heaps | TESTED | Expanded to four teaching routes and 13 duplicate/gap/short-sequence tests, fixed the Chinese mojibake translation, and added local, remote-refresh, and Judge gates. |
| p495 | Image Smoother | neighbor enumeration; 2D prefix sums; rolling column sums; in-place bit encoding | TESTED | Expanded to four teaching routes and 13 matrix-boundary tests, fixed the Chinese mojibake translation and caption, and added local, remote-refresh, and Judge gates. |
| p496 | Maximum Width of Binary Tree | BFS normalized indices; recursive DFS first index per depth; iterative DFS stack ranges; level-boundary lists | TESTED | Expanded to four teaching routes and 13 complete-position width tests, rewrote the Chinese translation and caption, and added local, remote-refresh, and Judge gates. |
| p497 | Beautiful Arrangement II | prefix zigzag; increasing-prefix tail zigzag; descending-difference walk; global-extreme shrinking | TESTED | Expanded to four O(n) teaching routes and 14 property-judged boundary tests, rewrote the Chinese translation, and added local, remote-refresh, and Judge gates. |
| p498 | Strange Printer | bottom-up interval DP; top-down memoization; compressed bottom-up DP; compressed top-down DP | TESTED | Expanded to four DP teaching routes and 16 interval-merge tests, rewrote the Chinese translation, and added local, remote-refresh, and Judge gates. |
| p499 | Non-decreasing Array | greedy single-pass repair; local violation certificate; prefix/suffix sorted certificates; virtual first-violation edits | TESTED | Expanded to four one-edit boundary teaching routes and 16 edge tests, rewrote the Chinese translation, and added local, remote-refresh, and Judge gates. |
| p500 | Trim a Binary Search Tree | recursive range-pruning DFS; iterative root and side relinking; stack-based rewire traversal; non-mutating cloned trim | TESTED | Expanded to four BST-pruning teaching routes and 16 structural tests, rewrote the Chinese translation, and added local, remote-refresh, and Judge gates. |
