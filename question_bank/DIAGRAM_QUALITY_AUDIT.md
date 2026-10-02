# AlgoCraft Diagram Quality Audit

This audit tracks visual quality, not just whether a problem has a diagram.
The earlier diagram tracker answered "does a diagram help the prompt"; this
file answers whether the actual image is readable, prompt-only, and visually
consistent with AlgoCraft.

## Visual Standard

- Prompt-only: diagrams may show the input, constraints, or domain structure,
  but must not highlight the answer, final result, or the solving algorithm.
- Readable in-game: use a fixed 768x432 canvas, large labels, thick edges, and
  stable spacing. Avoid tiny Mermaid text and oversized empty canvases.
- Text-fit hard gate: every generated text box must be recorded in
  `scripts/diagrams/minecraft_style_layout.json`; `OfficialDiagramLayoutTest`
  checks that text stays inside its drawn box, the box stays inside the PNG
  canvas, and Latin labels keep readable padding instead of touching borders.
  A single word overflowing a rectangle is a test failure, not a visual nit.
- Minecraft-compatible tone: low-saturation block colors, square nodes/cells,
  stone/dirt/water/redstone/lapis/gold accents, and no glossy AI-art style.
- Metadata coverage: prefer the normalized `diagrams` field so
  `QuestionBankQualityTest` can check file names, existence, and prompt-only
  captions. Legacy `images` references may remain temporarily for backward
  compatibility, but they should not be the only tracked reference.
- Reproducible assets: diagrams generated in this pass come from
  `scripts/diagrams/minecraft_style_diagrams.py`, not from one-off AI images.

## Batch 1: p1-p100

Status key:

- `NO_IMAGE_NEEDED`: reviewed in this batch; no picture is needed for the prompt.
- `REDRAW_DONE`: existing picture was replaced with the block-style renderer.
- `ADD_DONE`: a missing but valuable prompt diagram was added.
- `OPTIONAL_LATER`: no current picture; a future teaching diagram may help, but
  it is not required to fix an existing bad image.

| ID | Title | Status | Action |
| --- | --- | --- | --- |
| p1 | Two Sum | NO_IMAGE_NEEDED | Text and examples are enough. |
| p2 | Contains Duplicate | NO_IMAGE_NEEDED | No diagram needed. |
| p3 | Valid Anagram | NO_IMAGE_NEEDED | Character counts are clearer in text/code. |
| p4 | Group Anagrams | NO_IMAGE_NEEDED | No current image to fix. |
| p5 | Top K Frequent Elements | NO_IMAGE_NEEDED | Heap/bucket visuals would be solution hints. |
| p6 | Product of Array Except Self | NO_IMAGE_NEEDED | Prefix/suffix diagram would be algorithmic. |
| p7 | Valid Sudoku | REDRAW_DONE | Rebuilt board image; caption no longer says the board is valid. |
| p8 | Encode and Decode Strings | NO_IMAGE_NEEDED | Format is described by examples. |
| p9 | Longest Consecutive Sequence | NO_IMAGE_NEEDED | No prompt image needed. |
| p10 | Subarray Sum Equals K | NO_IMAGE_NEEDED | Prefix-sum visuals would hint the solution. |
| p11 | First Missing Positive | NO_IMAGE_NEEDED | No current image to fix. |
| p12 | Majority Element | NO_IMAGE_NEEDED | No diagram needed. |
| p13 | Move Zeroes | OPTIONAL_LATER | Could use an array prompt image, but no bad image exists. |
| p14 | Find All Numbers Disappeared in an Array | OPTIONAL_LATER | Could use array cells later; no current image exists. |
| p15 | Intersection of Two Arrays II | NO_IMAGE_NEEDED | Examples are enough. |
| p16 | 3Sum | NO_IMAGE_NEEDED | Pointer diagrams would be solution hints. |
| p17 | 4Sum | NO_IMAGE_NEEDED | Same as p16. |
| p18 | Next Permutation | OPTIONAL_LATER | A permutation-order prompt diagram may help later. |
| p19 | Pascal's Triangle | REDRAW_DONE | Rebuilt as a compact block triangle. |
| p20 | Set Matrix Zeroes | OPTIONAL_LATER | Prior result-style image is disallowed; a prompt-only matrix can be added later. |
| p21 | Valid Palindrome | NO_IMAGE_NEEDED | No diagram needed. |
| p22 | Two Sum II - Input Array Is Sorted | NO_IMAGE_NEEDED | Pointer visuals would hint the solution. |
| p23 | Count Triplets With Smaller Sum | NO_IMAGE_NEEDED | No current image to fix. |
| p24 | Container With Most Water | REDRAW_DONE | Removed max-area highlight; added normalized diagram metadata. |
| p25 | Trapping Rain Water | REDRAW_DONE | Rebuilt elevation map in block style; no numeric answer label. |
| p26 | Remove Duplicates from Sorted Array | NO_IMAGE_NEEDED | No prompt image needed. |
| p27 | Remove Element | NO_IMAGE_NEEDED | No current image to fix. |
| p28 | Count Equal Value Pairs | NO_IMAGE_NEEDED | No diagram needed. |
| p29 | Rotate Array | OPTIONAL_LATER | Could use an array prompt image later. |
| p30 | Squares of a Sorted Array | NO_IMAGE_NEEDED | Examples are enough. |
| p31 | Best Time to Buy and Sell Stock | OPTIONAL_LATER | A price chart could help later, but no bad image exists. |
| p32 | Longest Substring Without Repeating Characters | NO_IMAGE_NEEDED | Window diagram would be solution-oriented. |
| p33 | Longest Repeating Character Replacement | NO_IMAGE_NEEDED | No current image to fix. |
| p34 | Permutation in String | NO_IMAGE_NEEDED | Frequency/window visuals would hint the solution. |
| p35 | Minimum Window Substring | NO_IMAGE_NEEDED | Same concern as p34. |
| p36 | Sliding Window Maximum | NO_IMAGE_NEEDED | Deque/window diagram would be solution-oriented. |
| p37 | Find All Anagrams in a String | NO_IMAGE_NEEDED | No image needed. |
| p38 | Minimum Size Subarray Sum | NO_IMAGE_NEEDED | No current image to fix. |
| p39 | Longest Substring with At Most K Distinct Characters | NO_IMAGE_NEEDED | No image needed. |
| p40 | Fruit Into Baskets | NO_IMAGE_NEEDED | No current image to fix. |
| p41 | Valid Parentheses | REDRAW_DONE | Rebuilt as bracket-token input, not stack mechanics. |
| p42 | Min Stack | NO_IMAGE_NEEDED | Stack-state diagrams would be implementation hints. |
| p43 | Evaluate Reverse Polish Notation | NO_IMAGE_NEEDED | Stack diagram would be a solution hint. |
| p44 | Generate Parentheses | NO_IMAGE_NEEDED | Tree/backtracking visuals would be algorithmic. |
| p45 | Daily Temperatures | NO_IMAGE_NEEDED | Monotonic-stack visual would be a solution hint. |
| p46 | Car Fleet | OPTIONAL_LATER | Road prompt image may help later; no bad image exists. |
| p47 | Largest Rectangle in Histogram | REDRAW_DONE | Removed largest-rectangle highlight; added normalized diagram metadata. |
| p48 | Basic Calculator II | NO_IMAGE_NEEDED | Parser/stack visuals would be solution hints. |
| p49 | Decode String | REDRAW_DONE | Rebuilt nested input image; caption no longer includes decoded output. |
| p50 | Asteroid Collision | OPTIONAL_LATER | Could use input-stream blocks later. |
| p51 | Binary Search | NO_IMAGE_NEEDED | Search-step visuals would be algorithmic. |
| p52 | Search a 2D Matrix | REDRAW_DONE | Rebuilt sorted matrix and refreshed legacy image. |
| p53 | Koko Eating Bananas | NO_IMAGE_NEEDED | No image needed. |
| p54 | Find Minimum in Rotated Sorted Array | OPTIONAL_LATER | Could use array shape later; no bad image exists. |
| p55 | Search in Rotated Sorted Array | OPTIONAL_LATER | Same as p54. |
| p56 | Time Based Key-Value Store | NO_IMAGE_NEEDED | Design-state diagram would be implementation detail. |
| p57 | Median of Two Sorted Arrays | NO_IMAGE_NEEDED | Merge/partition visuals would hint solution. |
| p58 | First Bad Version | NO_IMAGE_NEEDED | Binary-search visual would be algorithmic. |
| p59 | Search Insert Position | NO_IMAGE_NEEDED | No diagram needed. |
| p60 | Find First and Last Position of Element in Sorted Array | REDRAW_DONE | Rebuilt array/target image; added normalized diagram metadata. |
| p61 | Peak Index in a Mountain Array | REDRAW_DONE | Rebuilt as mountain-shape bars; no peak marker. |
| p62 | Find Peak Element | NO_IMAGE_NEEDED | No current image to fix. |
| p63 | Capacity To Ship Packages Within D Days | NO_IMAGE_NEEDED | Feasibility visuals would hint solution. |
| p64 | Split Array Largest Sum | NO_IMAGE_NEEDED | Partition visuals would hint solution. |
| p65 | Reverse Linked List | REDRAW_DONE | Rebuilt input-only linked list. |
| p66 | Merge Two Sorted Lists | REDRAW_DONE | Rebuilt two input lists. |
| p67 | Reorder List | REDRAW_DONE | Rebuilt input-only linked list. |
| p68 | Remove Nth Node From End of List | REDRAW_DONE | Rebuilt input list plus n value; refreshed legacy image. |
| p69 | Copy List with Random Pointer | REDRAW_DONE | Rebuilt next/random pointer input graph. |
| p70 | Add Two Numbers | ADD_DONE | Added reverse-digit input list diagram. |
| p71 | Linked List Cycle | REDRAW_DONE | Rebuilt cycle input. |
| p72 | Find the Duplicate Number | REDRAW_DONE | Rebuilt array prompt and added normalized diagram metadata. |
| p73 | LRU Cache | REDRAW_DONE | Rebuilt capacity/order image; removed HashMap/DLL caption. |
| p74 | Merge k Sorted Lists | REDRAW_DONE | Rebuilt multi-list input. |
| p75 | Reverse Nodes in k-Group | ADD_DONE | Added input list plus k value diagram. |
| p76 | Palindrome Linked List | REDRAW_DONE | Rebuilt input-only list. |
| p77 | Intersection of Two Linked Lists | REDRAW_DONE | Rebuilt shared-tail input. |
| p78 | Sort List | REDRAW_DONE | Rebuilt unsorted input list. |
| p79 | Partition List | REDRAW_DONE | Rebuilt input list plus x value; removed stale color-caption claim. |
| p80 | Rotate List | REDRAW_DONE | Rebuilt input list plus k value. |
| p81 | Invert Binary Tree | REDRAW_DONE | Rebuilt input tree only. |
| p82 | Maximum Depth of Binary Tree | REDRAW_DONE | Rebuilt input tree only. |
| p83 | Diameter of Binary Tree | REDRAW_DONE | Rebuilt input tree without highlighting a diameter. |
| p84 | Balanced Binary Tree | REDRAW_DONE | Rebuilt both input trees; captions no longer state outputs. |
| p85 | Same Tree | REDRAW_DONE | Rebuilt p/q comparison images for both examples. |
| p86 | Subtree of Another Tree | REDRAW_DONE | Rebuilt main-tree and candidate-subtree prompt images. |
| p87 | Lowest Common Ancestor of a Binary Search Tree | REDRAW_DONE | Rebuilt BST with queried nodes only. |
| p88 | Binary Tree Level Order Traversal | REDRAW_DONE | Rebuilt input tree without traversal output. |
| p89 | Binary Tree Right Side View | REDRAW_DONE | Rebuilt input tree without right-view highlight. |
| p90 | Count Good Nodes in Binary Tree | REDRAW_DONE | Rebuilt input tree without marking counted nodes. |
| p91 | Validate Binary Search Tree | REDRAW_DONE | Rebuilt example input trees. |
| p92 | Kth Smallest Element in a BST | REDRAW_DONE | Rebuilt BST input only. |
| p93 | Construct Binary Tree from Preorder and Inorder Traversal | REDRAW_DONE | Rebuilt traversal-array prompt; caption no longer shows constructed output. |
| p94 | Binary Tree Maximum Path Sum | ADD_DONE | Added input tree/path-rule diagram with no optimal path highlight. |
| p95 | Serialize and Deserialize Binary Tree | REDRAW_DONE | Rebuilt input tree. |
| p96 | Symmetric Tree | REDRAW_DONE | Rebuilt mirror-shaped input tree. |
| p97 | Path Sum | REDRAW_DONE | Rebuilt input tree without marking a path. |
| p98 | Path Sum II | REDRAW_DONE | Rebuilt input tree without marking paths. |
| p99 | Path Sum III | REDRAW_DONE | Rebuilt input tree without marking counted paths. |
| p100 | Flatten Binary Tree to Linked List | REDRAW_DONE | Rebuilt input tree before flattening. |

## Remaining Batches

- p101-p200: accepted in Batch 2 below.
- p201-p300: accepted in Batch 3 below.
- p301-p400: accepted in Batch 4 below.
- p401-p500: accepted in Batch 5 below.

## Batch 2: p101-p200

| ID | Title | Status | Action |
| --- | --- | --- | --- |
| p101 | Implement Trie (Prefix Tree) | REDRAW_DONE | Rebuilt trie in block style; legacy image retained and normalized metadata already present. |
| p102 | Design Add and Search Words Data Structure | REDRAW_DONE | Rebuilt trie with wildcard-search context only. |
| p103 | Word Search II | REDRAW_DONE | Rebuilt character board and word list. |
| p104 | Replace Words | REDRAW_DONE | Rebuilt dictionary-root trie. |
| p105 | Map Sum Pairs | REDRAW_DONE | Rebuilt prefix tree with key values, not an implementation trace. |
| p106 | Kth Largest Element in a Stream | REDRAW_DONE | Rebuilt stream input and k value; no heap state shown. |
| p107 | Last Stone Weight | NO_IMAGE_NEEDED | No current image to fix. |
| p108 | K Closest Points to Origin | OPTIONAL_LATER | A coordinate-plane prompt image may help later. |
| p109 | Kth Largest Element in an Array | NO_IMAGE_NEEDED | Heap/partition visuals would hint the solution. |
| p110 | Task Scheduler | NO_IMAGE_NEEDED | Timeline visuals tend to show scheduling strategy. |
| p111 | Design Twitter | REDRAW_DONE | Rebuilt user/tweet/follow prompt structure. |
| p112 | Find Median from Data Stream | REDRAW_DONE | Rebuilt stream-prefix prompt and added normalized diagrams metadata. |
| p113 | Smallest Range Covering Elements from K Lists | NO_IMAGE_NEEDED | No current image to fix. |
| p114 | Top K Frequent Words | NO_IMAGE_NEEDED | No prompt image needed. |
| p115 | Find K Pairs with Smallest Sums | NO_IMAGE_NEEDED | Pair-generation visuals would be solution-oriented. |
| p116 | Subsets | REDRAW_DONE | Rebuilt input-set prompt and added normalized diagrams metadata. |
| p117 | Combination Sum | NO_IMAGE_NEEDED | Search-tree visuals would be solution hints. |
| p118 | Permutations | NO_IMAGE_NEEDED | Permutation tree would be algorithmic. |
| p119 | Subsets II | NO_IMAGE_NEEDED | Same as p116; no current bad image. |
| p120 | Combination Sum II | NO_IMAGE_NEEDED | No current image to fix. |
| p121 | Word Search | OPTIONAL_LATER | A prompt board image may help later. |
| p122 | Palindrome Partitioning | NO_IMAGE_NEEDED | Partition tree would be solution-oriented. |
| p123 | Letter Combinations of a Phone Number | OPTIONAL_LATER | Phone-keypad prompt image may help later. |
| p124 | N-Queens | REDRAW_DONE | Rebuilt empty n=4 board instead of showing a queen placement result. |
| p125 | Sudoku Solver | REDRAW_DONE | Rebuilt partial Sudoku board in the same style as p7. |
| p126 | Restore IP Addresses | NO_IMAGE_NEEDED | Split-position visuals would hint search strategy. |
| p127 | Partition to K Equal Sum Subsets | NO_IMAGE_NEEDED | No current image to fix. |
| p128 | Number of Islands | OPTIONAL_LATER | Prior result-style island image is disallowed; prompt-only grid can be added later. |
| p129 | Max Area of Island | OPTIONAL_LATER | Prior result-style image is disallowed; prompt-only grid can be added later. |
| p130 | Clone Graph | REDRAW_DONE | Rebuilt neutral input graph. |
| p131 | Walls and Gates | REDRAW_DONE | Rebuilt rooms/gates/walls input grid. |
| p132 | Rotting Oranges | REDRAW_DONE | Rebuilt orange-state input grid without minute propagation. |
| p133 | Pacific Atlantic Water Flow | REDRAW_DONE | Rebuilt heights grid with ocean labels only. |
| p134 | Surrounded Regions | REDRAW_DONE | Rebuilt X/O board; no flipped result shown. |
| p135 | Course Schedule | REDRAW_DONE | Rebuilt prerequisite graph. |
| p136 | Course Schedule II | REDRAW_DONE | Rebuilt prerequisite graph; no ordering output. |
| p137 | Graph Valid Tree | REDRAW_DONE | Rebuilt undirected input graph. |
| p138 | Number of Connected Components in an Undirected Graph | REDRAW_DONE | Rebuilt neutral component graph. |
| p139 | Redundant Connection | NO_IMAGE_NEEDED | Known result file remains disallowed; no prompt image needed now. |
| p140 | Word Ladder | OPTIONAL_LATER | Word graph prompt may help later, but no current image is referenced. |
| p141 | Word Ladder II | OPTIONAL_LATER | Same as p140. |
| p142 | Network Delay Time | REDRAW_DONE | Rebuilt directed weighted graph input. |
| p143 | Cheapest Flights Within K Stops | REDRAW_DONE | Rebuilt flight graph and k value. |
| p144 | Reconstruct Itinerary | REDRAW_DONE | Rebuilt ticket graph only. |
| p145 | Min Cost to Connect All Points | NO_IMAGE_NEEDED | MST diagram would be a solution hint. |
| p146 | Alien Dictionary | REDRAW_DONE | Rebuilt character-order graph; refreshed both normalized and legacy files. |
| p147 | Swim in Rising Water | REDRAW_DONE | Rebuilt elevation grid. |
| p148 | Shortest Path in Binary Matrix | REDRAW_DONE | Rebuilt binary grid with open cells only. |
| p149 | Path With Minimum Effort | REDRAW_DONE | Rebuilt heights grid without path highlight. |
| p150 | Is Graph Bipartite? | REDRAW_DONE | Rebuilt uncolored input graph. |
| p151 | Climbing Stairs | REDRAW_DONE | Rebuilt staircase prompt and added normalized diagrams metadata. |
| p152 | Min Cost Climbing Stairs | NO_IMAGE_NEEDED | Similar to p151; no current image to fix. |
| p153 | House Robber | NO_IMAGE_NEEDED | State/choice diagrams would be solution hints. |
| p154 | House Robber II | NO_IMAGE_NEEDED | Same as p153. |
| p155 | Longest Palindromic Substring | NO_IMAGE_NEEDED | Expansion visuals would be algorithmic. |
| p156 | Palindromic Substrings | NO_IMAGE_NEEDED | Same as p155. |
| p157 | Decode Ways | NO_IMAGE_NEEDED | DP split visuals would be solution-oriented. |
| p158 | Coin Change | REDRAW_DONE | Rebuilt coins/amount prompt and added normalized diagrams metadata. |
| p159 | Maximum Product Subarray | NO_IMAGE_NEEDED | No current image to fix. |
| p160 | Word Break | NO_IMAGE_NEEDED | Segmentation visuals would hint solution. |
| p161 | Longest Increasing Subsequence | REDRAW_DONE | Rebuilt input array without subsequence highlight. |
| p162 | Partition Equal Subset Sum | REDRAW_DONE | Rebuilt input array and total sum. |
| p163 | Unique Paths | REDRAW_DONE | Rebuilt grid with start/end only. |
| p164 | Longest Common Subsequence | REDRAW_DONE | Rebuilt two input strings without match lines. |
| p165 | Best Time to Buy and Sell Stock with Cooldown | REDRAW_DONE | Rebuilt price chart without buy/sell markers. |
| p166 | Coin Change II | REDRAW_DONE | Rebuilt coins/amount prompt. |
| p167 | Target Sum | REDRAW_DONE | Rebuilt input numbers and target. |
| p168 | Interleaving String | REDRAW_DONE | Rebuilt three input strings without mapping arrows. |
| p169 | Longest Increasing Path in a Matrix | REDRAW_DONE | Rebuilt input matrix; caption no longer implies a path result. |
| p170 | Distinct Subsequences | REDRAW_DONE | Rebuilt input strings s and t. |
| p171 | Edit Distance | REDRAW_DONE | Rebuilt input words only. |
| p172 | Burst Balloons | REDRAW_DONE | Rebuilt input balloon values without burst order. |
| p173 | Regular Expression Matching | REDRAW_DONE | Rebuilt string and pattern tokens. |
| p174 | Maximum Subarray | REDRAW_DONE | Rebuilt input array without maximum segment highlight. |
| p175 | Jump Game | REDRAW_DONE | Rebuilt jump-length array without reachability arrows. |
| p176 | Jump Game II | REDRAW_DONE | Rebuilt jump-length array; no jump path shown. |
| p177 | Gas Station | REDRAW_DONE | Rebuilt station gas/cost input without route result. |
| p178 | Hand of Straights | REDRAW_DONE | Rebuilt hand cards and group size without grouping answer. |
| p179 | Merge Triplets to Form Target Triplet | REDRAW_DONE | Rebuilt input triplets and target. |
| p180 | Partition Labels | REDRAW_DONE | Rebuilt input string without partition cuts. |
| p181 | Valid Parenthesis String | REDRAW_DONE | Rebuilt token string and wildcard note. |
| p182 | Insert Interval | REDRAW_DONE | Rebuilt intervals/newInterval prompt; caption no longer says what merges. |
| p183 | Merge Intervals | REDRAW_DONE | Rebuilt input intervals; caption no longer states overlap result. |
| p184 | Non-overlapping Intervals | REDRAW_DONE | Rebuilt input intervals; caption no longer names a removed interval. |
| p185 | Meeting Rooms | REDRAW_DONE | Rebuilt meeting intervals; caption no longer marks conflicts. |
| p186 | Meeting Rooms II | REDRAW_DONE | Rebuilt meeting intervals with normalized prompt caption. |
| p187 | Minimum Number of Arrows to Burst Balloons | NO_IMAGE_NEEDED | Interval/arrow visual would risk showing greedy grouping. |
| p188 | Rotate Image | REDRAW_DONE | Rebuilt input matrix only. |
| p189 | Spiral Matrix | REDRAW_DONE | Rebuilt input matrix without spiral path. |
| p190 | Minimum Right Rotations to Sort | NO_IMAGE_NEEDED | No current image to fix. |
| p191 | Happy Number | REDRAW_DONE | Rebuilt digit-square prompt. |
| p192 | Plus One | REDRAW_DONE | Rebuilt digits prompt without result. |
| p193 | Pow(x, n) | REDRAW_DONE | Rebuilt x/n prompt. |
| p194 | Multiply Strings | REDRAW_DONE | Rebuilt numeric strings prompt without product. |
| p195 | Detect Squares | REDRAW_DONE | Rebuilt points and query point. |
| p196 | Single Number | NO_IMAGE_NEEDED | Bit/count visuals would be solution hints. |
| p197 | Number of 1 Bits | NO_IMAGE_NEEDED | No prompt image needed. |
| p198 | Counting Bits | NO_IMAGE_NEEDED | DP/bit table would be solution-oriented. |
| p199 | Reverse Bits | NO_IMAGE_NEEDED | No current image to fix. |
| p200 | Missing Number | NO_IMAGE_NEEDED | No prompt image needed. |

## Batch 3: p201-p300

| ID | Title | Status | Action |
| --- | --- | --- | --- |
| p201 | Sum of Two Integers | NO_IMAGE_NEEDED | Bit visuals would be solution-oriented. |
| p202 | Reverse Integer | NO_IMAGE_NEEDED | Text examples are sufficient. |
| p203 | Roman to Integer | NO_IMAGE_NEEDED | Symbol table is clearer in text. |
| p204 | Integer to Roman | NO_IMAGE_NEEDED | No current image to fix. |
| p205 | Longest Common Prefix | REDRAW_DONE | Rebuilt trie in block style. |
| p206 | Maximum Width Ramp | NO_IMAGE_NEEDED | Index/ramp highlighting would reveal target structure. |
| p207 | Remove Duplicates from Sorted List | REDRAW_DONE | Rebuilt sorted input list and neutral caption. |
| p208 | Remove Duplicates from Sorted List II | REDRAW_DONE | Rebuilt sorted input list without color-result claims. |
| p209 | Implement strStr() | NO_IMAGE_NEEDED | Matching-window visuals would be solution-oriented. |
| p210 | Count and Say | NO_IMAGE_NEEDED | No prompt image needed. |
| p211 | Length of Last Word | NO_IMAGE_NEEDED | Text-only prompt is clear. |
| p212 | Add Binary | NO_IMAGE_NEEDED | Carry diagram would be algorithmic. |
| p213 | Sqrt(x) | NO_IMAGE_NEEDED | Binary-search visual would hint solution. |
| p214 | Pascal's Triangle II | NO_IMAGE_NEEDED | p19 already covers triangle visual style. |
| p215 | Best Time to Buy and Sell Stock II | OPTIONAL_LATER | A price chart could help later; no bad image exists. |
| p216 | Excel Sheet Column Title | NO_IMAGE_NEEDED | Conversion table is not needed. |
| p217 | Excel Sheet Column Number | NO_IMAGE_NEEDED | Same as p216. |
| p218 | Factorial Trailing Zeroes | NO_IMAGE_NEEDED | Factor-count diagram would be solution-oriented. |
| p219 | Reverse String | NO_IMAGE_NEEDED | No prompt image needed. |
| p220 | Reverse Vowels of a String | NO_IMAGE_NEEDED | No current image to fix. |
| p221 | Intersection of Two Arrays | NO_IMAGE_NEEDED | Examples are enough. |
| p222 | First Unique Character in a String | NO_IMAGE_NEEDED | Frequency visuals would hint solution. |
| p223 | Fizz Buzz | NO_IMAGE_NEEDED | No image needed. |
| p224 | Third Maximum Number | NO_IMAGE_NEEDED | No current image to fix. |
| p225 | Add Strings | NO_IMAGE_NEEDED | Carry diagram would be solution-oriented. |
| p226 | Number of Segments in a String | NO_IMAGE_NEEDED | Text prompt is clear. |
| p227 | Arranging Coins | OPTIONAL_LATER | Staircase picture may help later; no current image exists. |
| p228 | Find All Duplicates in an Array | NO_IMAGE_NEEDED | Marking-index visual would hint solution. |
| p229 | String Compression | NO_IMAGE_NEEDED | No current image to fix. |
| p230 | Can Place Flowers | OPTIONAL_LATER | Flowerbed prompt image may help later. |
| p231 | Maximum Product of Three Numbers | NO_IMAGE_NEEDED | No prompt image needed. |
| p232 | Baseball Game | NO_IMAGE_NEEDED | Stack-like scoreboard would be implementation detail. |
| p233 | Binary Number with Alternating Bits | NO_IMAGE_NEEDED | No image needed. |
| p234 | To Lower Case | NO_IMAGE_NEEDED | No image needed. |
| p235 | Robot Return to Origin | OPTIONAL_LATER | Path prompt image may help later. |
| p236 | Sort Array By Parity | NO_IMAGE_NEEDED | Sorting visual would be solution-oriented. |
| p237 | Unique Email Addresses | NO_IMAGE_NEEDED | Text rules are enough. |
| p238 | Range Sum of BST | REDRAW_DONE | Rebuilt BST with low/high values, no highlighted result nodes. |
| p239 | Verifying an Alien Dictionary | NO_IMAGE_NEEDED | Character-order visuals would duplicate p146. |
| p240 | N-Repeated Element in Size 2N Array | NO_IMAGE_NEEDED | No prompt image needed. |
| p241 | Count Negative Cells in Sorted Grid | OPTIONAL_LATER | Grid prompt image may help later. |
| p242 | Matrix Cells in Distance Order | NO_IMAGE_NEEDED | Distance arrows would hint sorting strategy. |
| p243 | Height Checker | NO_IMAGE_NEEDED | No current image to fix. |
| p244 | Defanging an IP Address | NO_IMAGE_NEEDED | No image needed. |
| p245 | Jewels and Stones | NO_IMAGE_NEEDED | Text prompt is clear. |
| p246 | How Many Numbers Are Smaller Than the Current Number | NO_IMAGE_NEEDED | No prompt image needed. |
| p247 | Number of Steps to Reduce a Number to Zero | NO_IMAGE_NEEDED | State trace would be algorithmic. |
| p248 | Shuffle the Array | OPTIONAL_LATER | Array layout image may help later. |
| p249 | Running Sum of 1d Array | NO_IMAGE_NEEDED | Prefix visual would hint solution. |
| p250 | Kids With the Greatest Number of Candies | NO_IMAGE_NEEDED | No image needed. |
| p251 | Shuffle String | NO_IMAGE_NEEDED | No current image to fix. |
| p252 | Goal Parser Interpretation | NO_IMAGE_NEEDED | Text rules are enough. |
| p253 | Richest Customer Wealth | NO_IMAGE_NEEDED | Matrix sum visual unnecessary. |
| p254 | Decode XORed Array | NO_IMAGE_NEEDED | XOR trace would be solution-oriented. |
| p255 | Count Items Matching a Rule | NO_IMAGE_NEEDED | No image needed. |
| p256 | Sorting the Sentence | NO_IMAGE_NEEDED | No current image to fix. |
| p257 | Check if the Sentence Is Pangram | NO_IMAGE_NEEDED | Alphabet table unnecessary. |
| p258 | Concatenation of Array | NO_IMAGE_NEEDED | No image needed. |
| p259 | Build Array from Permutation | NO_IMAGE_NEEDED | Index mapping would hint implementation. |
| p260 | Final Value of Variable After Performing Operations | NO_IMAGE_NEEDED | No image needed. |
| p261 | Maximum Number of Words Found in Sentences | NO_IMAGE_NEEDED | Text prompt is clear. |
| p262 | Left and Right Sum Differences | NO_IMAGE_NEEDED | Prefix/suffix visual would be algorithmic. |
| p263 | Convert the Temperature | NO_IMAGE_NEEDED | Formula text is enough. |
| p264 | Smallest Even Multiple | NO_IMAGE_NEEDED | No image needed. |
| p265 | Divisible and Non-divisible Sums Difference | NO_IMAGE_NEEDED | No prompt image needed. |
| p266 | Find Words Containing Character | NO_IMAGE_NEEDED | No image needed. |
| p267 | Number of Employees Who Met the Target | NO_IMAGE_NEEDED | No current image to fix. |
| p268 | Count Pairs Whose Sum is Less than Target | NO_IMAGE_NEEDED | Pairing visual would hint solution. |
| p269 | Faulty Keyboard | NO_IMAGE_NEEDED | Text rule is clear. |
| p270 | Split a String in Balanced Strings | NO_IMAGE_NEEDED | Counter trace would be solution-oriented. |
| p271 | Subtract the Product and Sum of Digits of an Integer | NO_IMAGE_NEEDED | No image needed. |
| p272 | Decompress Run-Length Encoded List | NO_IMAGE_NEEDED | Array expansion visual unnecessary. |
| p273 | Create Target Array in the Given Order | NO_IMAGE_NEEDED | Step-by-step visual would show process. |
| p274 | XOR Operation in an Array | NO_IMAGE_NEEDED | No prompt image needed. |
| p275 | Number of Good Pairs | NO_IMAGE_NEEDED | Pairing visual would be answer-like. |
| p276 | Design Parking System | OPTIONAL_LATER | Slot prompt image may help later. |
| p277 | Check if Two String Arrays are Equivalent | NO_IMAGE_NEEDED | No image needed. |
| p278 | Prefix Common Array of Two Permutations | NO_IMAGE_NEEDED | Prefix trace would be algorithmic. |
| p279 | Semi-Ordered Permutation Moves | NO_IMAGE_NEEDED | Move trace would reveal process. |
| p280 | Count the Number of Consistent Strings | NO_IMAGE_NEEDED | No current image to fix. |
| p281 | Count of Matches in Tournament | OPTIONAL_LATER | Bracket prompt may help later. |
| p282 | Truncate Sentence | NO_IMAGE_NEEDED | Text prompt is clear. |
| p283 | Alternating Subarray Length | NO_IMAGE_NEEDED | No image needed. |
| p284 | Count Number of Pairs With Absolute Difference K | NO_IMAGE_NEEDED | Pairing visual would be answer-like. |
| p285 | Reverse Prefix of Word | NO_IMAGE_NEEDED | No current image to fix. |
| p286 | Count Symmetric Integers | NO_IMAGE_NEEDED | No image needed. |
| p287 | Rings and Rods | OPTIONAL_LATER | Rod/ring prompt image may help later. |
| p288 | Keep Multiplying Found Values by Two | NO_IMAGE_NEEDED | Trace visual would be algorithmic. |
| p289 | Most Frequent Number Following Key In an Array | NO_IMAGE_NEEDED | No prompt image needed. |
| p290 | Cells in a Range on an Excel Sheet | OPTIONAL_LATER | Spreadsheet grid prompt may help later. |
| p291 | Percentage of Letter in String | NO_IMAGE_NEEDED | No image needed. |
| p292 | Strong Password Checker II | NO_IMAGE_NEEDED | Rule list is clearer in text. |
| p293 | Decode the Message | OPTIONAL_LATER | Substitution table may help later. |
| p294 | Check if Matrix Is X-Matrix | REDRAW_DONE | Rebuilt X-matrix condition diagram in block style. |
| p295 | Arithmetic Triplets | NO_IMAGE_NEEDED | No prompt image needed. |
| p296 | Number of Common Factors | NO_IMAGE_NEEDED | No image needed. |
| p297 | Sort the People | NO_IMAGE_NEEDED | No current image to fix. |
| p298 | Remove Letter To Equalize Frequency | NO_IMAGE_NEEDED | Frequency visual would hint solution. |
| p299 | Odd String Difference | NO_IMAGE_NEEDED | No image needed. |
| p300 | Apply Operations to an Array | NO_IMAGE_NEEDED | Step trace would be solution-oriented. |

## Batch 4: p301-p400

| ID | Title | Status | Action |
| --- | --- | --- | --- |
| p301 | Minimum Bouquet Days | NO_IMAGE_NEEDED | Binary-search feasibility visual would be solution-oriented. |
| p302 | Distinct Difference Array | NO_IMAGE_NEEDED | Prefix/set trace would hint solution. |
| p303 | Count All-One Submatrices | OPTIONAL_LATER | Matrix prompt image may help later; no current image exists. |
| p304 | Swap Nodes in Pairs | REDRAW_DONE | Rebuilt input list before swaps; caption no longer mentions highlighted pairs. |
| p305 | Substring with Concatenation of All Words | NO_IMAGE_NEEDED | Window visual would be solution-oriented. |
| p306 | Longest Valid Parentheses | REDRAW_DONE | Rebuilt input string only. |
| p307 | Factorial Zeroes Preimage Size | NO_IMAGE_NEEDED | Math visualization would be unclear. |
| p308 | Shortest Subarray Sum At Least Target | NO_IMAGE_NEEDED | Prefix/deque visual would hint solution. |
| p309 | Maximum Submatrix Sum No More Than K | OPTIONAL_LATER | Matrix prompt could help later; no bad image exists. |
| p310 | Wildcard Matching | NO_IMAGE_NEEDED | DP table would be solution-oriented. |
| p311 | Count Good Binary Strings | NO_IMAGE_NEEDED | No prompt image needed. |
| p312 | N-Queens II | NO_IMAGE_NEEDED | p124 covers N-Queens board style. |
| p313 | Permutation Sequence | NO_IMAGE_NEEDED | Permutation tree would be algorithmic. |
| p314 | Valid Number | NO_IMAGE_NEEDED | Finite-state diagram would be implementation detail. |
| p315 | Text Justification | NO_IMAGE_NEEDED | Examples are clearer as text. |
| p316 | Longest Square Streak | NO_IMAGE_NEEDED | No current image to fix. |
| p317 | Count Subarrays With Fixed Bounds | NO_IMAGE_NEEDED | Window trace would hint solution. |
| p318 | Maximum Score From Multipliers | NO_IMAGE_NEEDED | DP choice diagram would be solution-oriented. |
| p319 | Maximal Rectangle | REDRAW_DONE | Rebuilt binary matrix without rectangle highlight. |
| p320 | Scramble String | NO_IMAGE_NEEDED | Recursion tree would be algorithmic. |
| p321 | Laser Beams Between Security Rows | OPTIONAL_LATER | Security-row grid may help later. |
| p322 | Recover Binary Search Tree | REDRAW_DONE | Rebuilt input tree and neutral caption. |
| p323 | Minimum Rounds to Finish Tasks | NO_IMAGE_NEEDED | Counting visual unnecessary. |
| p324 | Best Time to Buy and Sell Stock III | OPTIONAL_LATER | Price chart may help later; no current image exists. |
| p325 | Best Time to Buy and Sell Stock IV | OPTIONAL_LATER | Same as p324. |
| p326 | Word Pattern Bijection | NO_IMAGE_NEEDED | Mapping diagram could be solution-like. |
| p327 | Alternating Digit Sum | NO_IMAGE_NEEDED | No image needed. |
| p328 | Longest Bounded Difference Window | NO_IMAGE_NEEDED | Window visual would hint solution. |
| p329 | Palindrome Partitioning II | NO_IMAGE_NEEDED | Partition DP visual would be algorithmic. |
| p330 | Candy | NO_IMAGE_NEEDED | Two-pass visual would hint solution. |
| p331 | Word Break II | REDRAW_DONE | Rebuilt dictionary trie without sentence decomposition output. |
| p332 | Max Points on a Line | OPTIONAL_LATER | Coordinate prompt image may help later. |
| p333 | Find Minimum in Rotated Sorted Array II | OPTIONAL_LATER | Array shape may help later. |
| p334 | Maximum Gap | NO_IMAGE_NEEDED | Bucket visual would be solution-oriented. |
| p335 | Dungeon Game | REDRAW_DONE | Rebuilt dungeon grid input. |
| p336 | The Skyline Problem | REDRAW_DONE | Rebuilt building rectangles without skyline result. |
| p337 | Basic Calculator | NO_IMAGE_NEEDED | Parser/stack visual would be implementation detail. |
| p338 | Shortest Palindrome | NO_IMAGE_NEEDED | KMP/hash visuals would be solution hints. |
| p339 | Minimum Jumps to Target With Forbidden | OPTIONAL_LATER | Number-line prompt may help later. |
| p340 | Pair Words By Reversal | NO_IMAGE_NEEDED | No current image to fix. |
| p341 | Remove Invalid Parentheses | NO_IMAGE_NEEDED | Search tree would be algorithmic. |
| p342 | Minimum Number Game | NO_IMAGE_NEEDED | No prompt image needed. |
| p343 | Count of Smaller Numbers After Self | NO_IMAGE_NEEDED | Fenwick/merge visual would hint solution. |
| p344 | Russian Doll Envelopes | OPTIONAL_LATER | Envelope prompt image may help later. |
| p345 | Minimum Cost to Connect Sticks | NO_IMAGE_NEEDED | Heap combine visual would be solution-oriented. |
| p346 | Frog Jump | OPTIONAL_LATER | Stones prompt image may help later. |
| p347 | Trapping Rain Water II | REDRAW_DONE | Rebuilt height map without trapped-water result. |
| p348 | Concatenated Words | NO_IMAGE_NEEDED | Trie/DP visual would be solution-oriented. |
| p349 | Largest Component Size by Common Factor | NO_IMAGE_NEEDED | Factor graph would hint solution. |
| p350 | Sliding Window Median | NO_IMAGE_NEEDED | Heap/window visual would be algorithmic. |
| p351 | Smallest Good Base | NO_IMAGE_NEEDED | Math prompt is text-only. |
| p352 | Zuma Game | OPTIONAL_LATER | Board prompt may help later. |
| p353 | Reverse Pairs | NO_IMAGE_NEEDED | Merge-sort visual would be solution hint. |
| p354 | Freedom Trail | OPTIONAL_LATER | Ring/key prompt may help later. |
| p355 | Super Washing Machines | OPTIONAL_LATER | Machine row prompt may help later. |
| p356 | Remove Boxes | OPTIONAL_LATER | Box sequence prompt may help later. |
| p357 | Student Attendance Record II | NO_IMAGE_NEEDED | State-machine visual would be solution-oriented. |
| p358 | K-th Smallest in Lexicographical Order | NO_IMAGE_NEEDED | Trie traversal visual would hint solution. |
| p359 | Find the Closest Palindrome | NO_IMAGE_NEEDED | No image needed. |
| p360 | Divide Players Into Teams | NO_IMAGE_NEEDED | Pairing visual could be answer-like. |
| p361 | Patching Array | NO_IMAGE_NEEDED | Coverage interval visual would hint greedy proof. |
| p362 | Self Crossing | OPTIONAL_LATER | Geometry prompt could help later. |
| p363 | Palindrome Pairs | NO_IMAGE_NEEDED | Trie visual would be solution-oriented. |
| p364 | Data Stream as Disjoint Intervals | OPTIONAL_LATER | Timeline prompt may help later. |
| p365 | Max Sum of Rectangle No Larger Than K | REDRAW_DONE | Rebuilt matrix and k value without rectangle result. |
| p366 | Perfect Rectangle | OPTIONAL_LATER | Rectangle prompt could help later. |
| p367 | Steps to Reduce Binary String to One | NO_IMAGE_NEEDED | Step trace would be solution-oriented. |
| p368 | Arithmetic Slices II - Subsequence | NO_IMAGE_NEEDED | No current image to fix. |
| p369 | Poor Pigs | NO_IMAGE_NEEDED | Combinatorial diagram would be unclear. |
| p370 | Minimum Operations to Make Array Alternating | NO_IMAGE_NEEDED | No image needed. |
| p371 | Cut Off Trees for Golf Event | REDRAW_DONE | Rebuilt forest grid input. |
| p372 | 24 Game | NO_IMAGE_NEEDED | Expression tree would be solution-oriented. |
| p373 | Stickers to Spell Word | OPTIONAL_LATER | Sticker/target prompt may help later. |
| p374 | Falling Squares | REDRAW_DONE | Rebuilt square positions input. |
| p375 | Reach a Number | NO_IMAGE_NEEDED | Number-line trace would hint solution. |
| p376 | Cracking the Safe | NO_IMAGE_NEEDED | De Bruijn graph visual would be solution-level. |
| p377 | Couples Holding Hands | REDRAW_DONE | Rebuilt neutral seat row. |
| p378 | Max Chunks To Make Sorted II | NO_IMAGE_NEEDED | Chunk visualization would reveal solution structure. |
| p379 | Basic Calculator IV | NO_IMAGE_NEEDED | Expression tree would be implementation detail. |
| p380 | Race Car | OPTIONAL_LATER | Number-line prompt may help later. |
| p381 | Making A Large Island | REDRAW_DONE | Rebuilt binary island grid without flip result. |
| p382 | Unique Paths III | REDRAW_DONE | Rebuilt grid with start/end/obstacle only. |
| p383 | Minimize Malware Spread | REDRAW_DONE | Rebuilt graph with initial infected nodes only. |
| p384 | Three Equal Parts | NO_IMAGE_NEEDED | Bit partition visual would be solution-oriented. |
| p385 | Cat and Mouse | REDRAW_DONE | Rebuilt game graph with role labels. |
| p386 | Number of Music Playlists | NO_IMAGE_NEEDED | DP state visual would be solution-level. |
| p387 | Minimize Malware Spread II | REDRAW_DONE | Rebuilt directed graph with initial infected nodes only. |
| p388 | Least Operators to Express Number | NO_IMAGE_NEEDED | Math search visual would be unclear. |
| p389 | Binary Tree Cameras | REDRAW_DONE | Rebuilt input tree without camera placement. |
| p390 | Equal Rational Numbers | NO_IMAGE_NEEDED | String/rational rules are text-only. |
| p391 | Tallest Billboard | NO_IMAGE_NEEDED | DP balance visual would be solution-oriented. |
| p392 | Triples with Bitwise AND Equal To Zero | NO_IMAGE_NEEDED | No prompt image needed. |
| p393 | Minimum Cost to Merge Stones | NO_IMAGE_NEEDED | Merge DP visual would show process. |
| p394 | Grid Illumination | OPTIONAL_LATER | Grid prompt may help later. |
| p395 | Recover a Tree From Preorder Traversal | REDRAW_DONE | Rebuilt traversal string, no recovered tree result. |
| p396 | Stream of Characters | NO_IMAGE_NEEDED | Trie visual would be implementation detail. |
| p397 | Escape a Large Maze | OPTIONAL_LATER | Grid prompt may help later. |
| p398 | Parsing A Boolean Expression | NO_IMAGE_NEEDED | Parser tree would be solution-oriented. |
| p399 | Smallest Sufficient Team | NO_IMAGE_NEEDED | Set-cover visual would be solution-level. |
| p400 | Longest Chunked Palindrome Decomposition | NO_IMAGE_NEEDED | Chunk visual would reveal output structure. |

## Batch 5: p401-p500

| ID | Title | Status | Action |
| --- | --- | --- | --- |
| p401 | Design HashMap | REDRAW_DONE | Rebuilt bucket/key-value prompt in block style. |
| p402 | Design HashSet | REDRAW_DONE | Rebuilt unique-key bucket prompt. |
| p403 | Design Linked List | REDRAW_DONE | Rebuilt linked-list node structure with head/tail labels. |
| p404 | Design Skiplist | REDRAW_DONE | Replaced incorrect p403 linked-list reuse with a real skiplist prompt. |
| p405 | Design Underground System | NO_IMAGE_NEEDED | Serialized trip data is clearer in text; station-flow art would be decorative. |
| p406 | Design Browser History | REDRAW_DONE | Replaced incorrect p403 linked-list reuse with browser-history state. |
| p407 | Design Circular Queue | REDRAW_DONE | Rebuilt capacity-3 ring buffer prompt. |
| p408 | Design Circular Deque | REDRAW_DONE | Rebuilt double-ended ring prompt. |
| p409 | Design Front Middle Back Queue | REDRAW_DONE | Rebuilt queue positions without operation trace. |
| p410 | Design Ordered Stream | NO_IMAGE_NEEDED | Index/value stream behavior is already explicit in examples. |
| p411 | Design Authentication Manager | NO_IMAGE_NEEDED | Token expiration timeline would be implementation/process oriented. |
| p412 | Design Movie Rental System | NO_IMAGE_NEEDED | Multi-table marketplace state would be too dense for a small diagram. |
| p413 | Design Bitset | NO_IMAGE_NEEDED | Bit operations are clearer as text examples. |
| p414 | Design Video Sharing Platform | NO_IMAGE_NEEDED | UI-like art would not clarify the data-structure API. |
| p415 | Design Memory Allocator | OPTIONAL_LATER | A memory-block prompt may help later; no current bad image exists. |
| p416 | Design SQL | NO_IMAGE_NEEDED | Table API details are better represented in examples. |
| p417 | Design Graph With Shortest Path Calculator | REDRAW_DONE | Rebuilt directed weighted graph without shortest-path highlight. |
| p418 | Range Sum Query - Immutable | NO_IMAGE_NEEDED | Prefix-sum visuals would be solution-oriented. |
| p419 | Range Sum Query 2D - Immutable | REDRAW_DONE | Rebuilt matrix and query rectangle in block style. |
| p420 | Range Sum Query - Mutable | REDRAW_DONE | Added an update/query current-array diagram without revealing Fenwick or segment-tree internals. |
| p421 | Find the Array Concatenation Value | NO_IMAGE_NEEDED | Pairing is simple enough from examples. |
| p422 | Minimum Common Value in Two Sorted Arrays | NO_IMAGE_NEEDED | Pointer visuals would hint the solution. |
| p423 | Maximum Number of Pairs in Array | NO_IMAGE_NEEDED | Pairing visualization would be answer-like. |
| p424 | Longest Unequal Adjacent Groups Subsequence | NO_IMAGE_NEEDED | Subsequence selection visuals would show output structure. |
| p425 | Partition Array by Pivot | NO_IMAGE_NEEDED | Partition diagram would reveal the output ordering. |
| p426 | Time Needed to Buy Tickets | NO_IMAGE_NEEDED | Queue trace would be process/answer oriented. |
| p427 | Circular Sentence Check | NO_IMAGE_NEEDED | Character boundary rule is clearer in text. |
| p428 | Design a Stack With Increment Operation | NO_IMAGE_NEEDED | Stack-state diagram would be implementation oriented. |
| p429 | Design a Leaderboard | REDRAW_DONE | Replaced unrelated Tic-Tac-Toe image with leaderboard score state. |
| p430 | Design File System | OPTIONAL_LATER | Path tree prompt may help later; no current bad image exists. |
| p431 | Count Pairs Below Target | NO_IMAGE_NEEDED | Pair selection visual would be answer-like. |
| p432 | Original Array From Doubled Values | NO_IMAGE_NEEDED | Matching pairs would hint the reconstruction. |
| p433 | Append Characters to Make Subsequence | NO_IMAGE_NEEDED | Subsequence pointers would be solution-oriented. |
| p434 | Shortest Alternating Color Paths | OPTIONAL_LATER | Colored graph prompt may help later; no current bad image exists. |
| p435 | Minimum Deletions to Make String Balanced | NO_IMAGE_NEEDED | Deletion marks would reveal output choices. |
| p436 | Count Words With Prefix | NO_IMAGE_NEEDED | Prefix check is clear from examples. |
| p437 | Minimum Window Subsequence | NO_IMAGE_NEEDED | Window/subsequence diagram would hint the solution. |
| p438 | LFU Cache | REDRAW_DONE | Rebuilt frequency-bucket state in the common visual style. |
| p439 | Insert Delete GetRandom O(1) | NO_IMAGE_NEEDED | Internal array/map diagram would be implementation-specific. |
| p440 | Insert Delete GetRandom O(1) - Duplicates allowed | NO_IMAGE_NEEDED | Same as p439. |
| p441 | All O(1) Data Structure | REDRAW_DONE | Rebuilt count-bucket state without old slide styling; refreshed title and layout hash. |
| p442 | Snapshot Array | NO_IMAGE_NEEDED | Version timeline would be implementation-oriented. |
| p443 | Stock Price Fluctuation | NO_IMAGE_NEEDED | Price-state table is clearer in examples. |
| p444 | Simple Bank System | NO_IMAGE_NEEDED | Account balances are text/table data. |
| p445 | Design ATM Machine | OPTIONAL_LATER | Banknote slot prompt may help later; no current bad image exists. |
| p446 | Design Food Rating System | NO_IMAGE_NEEDED | Ranking data would be too table-heavy for the diagram slot. |
| p447 | Design Number Container System | NO_IMAGE_NEEDED | Map/index state would be implementation-specific. |
| p448 | Design Text Editor | REDRAW_DONE | Rebuilt as document text around cursor instead of stack internals. |
| p449 | My Calendar I | NO_IMAGE_NEEDED | Known timeline result image remains disallowed. |
| p450 | My Calendar II | NO_IMAGE_NEEDED | Known timeline result image remains disallowed. |
| p451 | My Calendar III | NO_IMAGE_NEEDED | Booking overlap visualization risks showing the answer. |
| p452 | Find the Prefix Common Array of Two Arrays | NO_IMAGE_NEEDED | Prefix trace would be algorithmic. |
| p453 | Count Good Substrings of Length Three | NO_IMAGE_NEEDED | Text examples are enough. |
| p454 | Sum of Digits of String After Convert | NO_IMAGE_NEEDED | Conversion trace would be process-oriented. |
| p455 | Find Missing Observations | NO_IMAGE_NEEDED | Dice values are clear in text. |
| p456 | Walking Robot Simulation | OPTIONAL_LATER | Grid prompt may help later; no current bad image exists. |
| p457 | Circular Array Loop | OPTIONAL_LATER | Circular index prompt may help later, but arrows could reveal traversal. |
| p458 | Destroying Asteroids In Order | NO_IMAGE_NEEDED | Sorted/order trace would hint solution. |
| p459 | Repeated Substring Pattern | NO_IMAGE_NEEDED | Pattern marks would reveal the answer. |
| p460 | Island Perimeter | REDRAW_DONE | Rebuilt input-only island grid and removed stale yellow-stripe explanation. |
| p461 | Hamming Distance | NO_IMAGE_NEEDED | Bit comparison is clear from examples. |
| p462 | Minimum Moves to Equal Array Elements II | NO_IMAGE_NEEDED | Median/target visualization would hint solution. |
| p463 | Student Attendance Record I | NO_IMAGE_NEEDED | String rule is text-only. |
| p464 | Reverse Words in a String III | NO_IMAGE_NEEDED | Reversal result visualization would be answer-like. |
| p465 | Maximum Depth of N-ary Tree | REDRAW_DONE | Rebuilt input N-ary tree and removed depth answer from caption. |
| p466 | Array Partition | NO_IMAGE_NEEDED | Pairing visualization would reveal solution structure. |
| p467 | Binary Tree Tilt | REDRAW_DONE | Rebuilt input tree in block style. |
| p468 | Reshape the Matrix | REDRAW_DONE | Rebuilt input matrix with requested shape, no reshaped output. |
| p469 | Minimum Falling Path Sum Without Same Column | NO_IMAGE_NEEDED | Path highlight would reveal output structure. |
| p470 | Distribute Candies | NO_IMAGE_NEEDED | Count/set visual unnecessary. |
| p471 | Longest Harmonious Subsequence | NO_IMAGE_NEEDED | Subsequence selection would be solution-oriented. |
| p472 | Range Addition II | NO_IMAGE_NEEDED | Overlap visualization would hint the final count. |
| p473 | Minimum Index Sum of Two Lists | NO_IMAGE_NEEDED | Matching rows would be answer-like. |
| p474 | Neighboring Bitwise XOR Validity | NO_IMAGE_NEEDED | XOR trace would be algorithmic. |
| p475 | Construct String from Binary Tree | REDRAW_DONE | Rebuilt input tree only. |
| p476 | Merge Two Binary Trees | REDRAW_DONE | Rebuilt two input trees without merged-result tree. |
| p477 | Maximum Distance in Arrays | NO_IMAGE_NEEDED | Extremes visualization would hint solution. |
| p478 | Count Complete Subarrays | NO_IMAGE_NEEDED | Window trace would be algorithmic. |
| p479 | Average of Levels in Binary Tree | REDRAW_DONE | Rebuilt input tree only. |
| p480 | Maximum Average Subarray I | NO_IMAGE_NEEDED | Window highlight would be solution-oriented. |
| p481 | Set Mismatch | NO_IMAGE_NEEDED | Duplicate/missing marks would reveal output. |
| p482 | Maximum Length of Pair Chain | NO_IMAGE_NEEDED | Chain selection would reveal solution structure. |
| p483 | Minimize Maximum Pair Sum in Array | NO_IMAGE_NEEDED | Pairing visualization would be answer-like. |
| p484 | Count Length-Three Palindromic Subsequences | NO_IMAGE_NEEDED | Subsequence marks would reveal counted structures. |
| p485 | Dota2 Senate | NO_IMAGE_NEEDED | Round trace would be process-oriented. |
| p486 | 2 Keys Keyboard | NO_IMAGE_NEEDED | Operation trace would reveal dynamic programming states. |
| p487 | Find Duplicate Subtrees | REDRAW_DONE | Rebuilt input tree without duplicate-subtree highlighting. |
| p488 | Two Sum IV - Input is a BST | REDRAW_DONE | Rebuilt BST input with k value, no pair highlight. |
| p489 | Maximum Binary Tree | REDRAW_DONE | Replaced output-tree image with input-array prompt. |
| p490 | Print Binary Tree | REDRAW_DONE | Rebuilt input tree before formatted matrix placement. |
| p491 | Kth Smallest Number in Multiplication Table | NO_IMAGE_NEEDED | Table/rank shading would hint counting strategy. |
| p492 | Minimum Operations to Collect Elements | NO_IMAGE_NEEDED | Collection trace would be process-oriented. |
| p493 | Find K Closest Elements | NO_IMAGE_NEEDED | Distance marks would reveal output choices. |
| p494 | Split Array into Consecutive Subsequences | NO_IMAGE_NEEDED | Grouping visual would be answer-like. |
| p495 | Image Smoother | REDRAW_DONE | Rebuilt input grid plus local neighborhood prompt. |
| p496 | Maximum Width of Binary Tree | REDRAW_DONE | Rebuilt input tree without width highlight. |
| p497 | Beautiful Arrangement II | NO_IMAGE_NEEDED | Construction visual would show the answer pattern. |
| p498 | Strange Printer | NO_IMAGE_NEEDED | Interval DP visual would be solution-level. |
| p499 | Non-decreasing Array | NO_IMAGE_NEEDED | Edit mark would reveal output choice. |
| p500 | Trim a Binary Search Tree | ADD_DONE | Added prompt-only BST and bounds diagram using a non-disallowed file name. |
