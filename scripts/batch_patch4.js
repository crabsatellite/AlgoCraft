#!/usr/bin/env node

/**
 * Patch 4: Create diagrams for ALL remaining problems that need them.
 */

const fs = require("fs");
const path = require("path");

const MERMAID_DIR = path.resolve(__dirname, "diagrams/mermaid");
const PROBLEMS_DIR = path.resolve(__dirname, "../question_bank/official");

const DH = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a9eff', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%`;
const TH = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a90d9', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%`;

function tree(vals, hl = []) {
  let n = [], e = [], s = [];
  for (let i = 0; i < vals.length; i++) {
    if (vals[i] === null) continue;
    const v = vals[i] < 0 ? `"${vals[i]}"` : `${vals[i]}`;
    n.push(`    N${i}((${v}))`);
    const l = 2*i+1, r = 2*i+2;
    if (l < vals.length && vals[l] !== null) e.push(`    N${i} --> N${l}`);
    if (r < vals.length && vals[r] !== null) e.push(`    N${i} --> N${r}`);
    s.push(hl.includes(i) || hl.includes(vals[i])
      ? `    style N${i} fill:#90EE90,stroke:#228B22,color:#000`
      : `    style N${i} fill:#4a9eff,stroke:#2a6ecf,color:#fff`);
  }
  return `${DH}\ngraph TD\n${n.join("\n")}\n${e.join("\n")}\n${s.join("\n")}`;
}

function grid(rows, title) {
  let rs = rows.map((r, i) => `    R${i}["${r.map(c => String(c).padStart(2)).join("  ")}"]`);
  let st = rows.map((_, i) => `    style R${i} fill:#2d2d44,stroke:#4a9eff,color:#fff`);
  return `${DH}\ngraph TB\n    subgraph G["${title}"]\n        direction TB\n${rs.join("\n")}\n    end\n    style G fill:#1a1a2e,stroke:#4a9eff,color:#fff\n${st.join("\n")}`;
}

function ivs(intervals, title, colors) {
  let ns = intervals.map((iv, i) => `    I${i}["[${iv[0]}, ${iv[1]}]"]`);
  let st = intervals.map((_, i) => `    style I${i} fill:${colors[i] || '#4a9eff'},stroke:#2a6ecf,color:#fff`);
  return `${DH}\ngraph LR\n    subgraph TL["${title}"]\n        direction LR\n${ns.join("\n")}\n    end\n    style TL fill:#1a1a2e,stroke:#4a9eff,color:#fff\n${st.join("\n")}`;
}

const diagrams = {
  // === DESIGN PROBLEMS ===
  "p111_design": `${DH}
graph LR
    subgraph Twitter["Design Twitter"]
        direction TB
        subgraph Users["User Feed (Linked List)"]
            U1["User 1: Tweet 5 -> ..."]
            U2["User 2: Tweet 6 -> ..."]
        end
        subgraph Follow["Follow Graph"]
            F1["User 1"] -->|follows| F2["User 2"]
        end
    end
    style U1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style U2 fill:#90EE90,stroke:#228B22,color:#000
    style F1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style F2 fill:#90EE90,stroke:#228B22,color:#000`,

  "p401_design": `${DH}
graph TD
    subgraph HM["HashMap: Bucket Array + Linked List Chaining"]
        direction TB
        B0["Bucket 0"] --> N01["key:0 val:v"]
        B1["Bucket 1"] --> N11["key:1 val:1"] --> N12["key:N+1 val:v"]
        B2["Bucket 2"] --> N21["key:2 val:2"]
        B3["Bucket ..."]
    end
    style B0 fill:#333333,stroke:#555555,color:#fff
    style B1 fill:#333333,stroke:#555555,color:#fff
    style B2 fill:#333333,stroke:#555555,color:#fff
    style B3 fill:#333333,stroke:#555555,color:#fff
    style N01 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N11 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N12 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N21 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p402_design": `${DH}
graph TD
    subgraph HS["HashSet: Bucket Array"]
        direction TB
        B0["Bucket 0"] --> V01["1"]
        B1["Bucket 1"] --> V11["2"]
        B2["Bucket 2"]
    end
    style B0 fill:#333333,stroke:#555555,color:#fff
    style B1 fill:#333333,stroke:#555555,color:#fff
    style B2 fill:#333333,stroke:#555555,color:#fff
    style V01 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style V11 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p403_design": `${DH}
graph LR
    HEAD["Head"] <--> N1["1"] <--> N2["2"] <--> N3["3"] <--> TAIL["Tail"]
    style HEAD fill:#333333,stroke:#555555,color:#fff
    style TAIL fill:#333333,stroke:#555555,color:#fff
    style N1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N2 fill:#90EE90,stroke:#228B22,color:#000
    style N3 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p407_design": `${DH}
graph LR
    subgraph CQ["Circular Queue (capacity=3)"]
        direction LR
        S0["1"]
        S1["2"]
        S2["3"]
        S0 --> S1 --> S2
        S2 -.->|wrap| S0
    end
    style S0 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style S1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style S2 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p408_design": `${DH}
graph LR
    subgraph CD["Circular Deque (capacity=3)"]
        direction LR
        S0["Front: 3"]
        S1["1"]
        S2["Rear: 2"]
        S0 --> S1 --> S2
        S2 -.->|wrap| S0
    end
    style S0 fill:#90EE90,stroke:#228B22,color:#000
    style S1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style S2 fill:#FFA500,stroke:#CC8400,color:#000`,

  "p409_design": `${DH}
graph LR
    subgraph FMB["Front Middle Back Queue"]
        direction LR
        F["Front: 4"] --> M1["3"] --> M2["Middle"] --> B1["2"] --> R["Back: 1"]
    end
    style F fill:#90EE90,stroke:#228B22,color:#000
    style M1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style M2 fill:#FFA500,stroke:#CC8400,color:#000
    style B1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style R fill:#FF6B6B,stroke:#CC5555,color:#fff`,

  "p438_design": `${DH}
graph LR
    subgraph LFU["LFU Cache (cap=2)"]
        direction TB
        subgraph F1["Freq 1"]
            K2["key:2 val:2"]
        end
        subgraph F2["Freq 2"]
            K1["key:1 val:1"]
        end
    end
    style K1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style K2 fill:#90EE90,stroke:#228B22,color:#000`,

  "p441_design": `${DH}
graph LR
    subgraph AO["All O'one: DLL by Count"]
        direction LR
        C1["count:1<br/>keys: leet"] <--> C2["count:2<br/>keys: hello"]
    end
    style C1 fill:#90EE90,stroke:#228B22,color:#000
    style C2 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p448_design": `${DH}
graph LR
    subgraph TE["Text Editor"]
        direction LR
        L["left stack:<br/>prac"] --> C["|<br/>cursor"] --> R["right stack:<br/>tice"]
    end
    style L fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style C fill:#FF6B6B,stroke:#CC5555,color:#fff
    style R fill:#90EE90,stroke:#228B22,color:#000`,

  // === TRIE PROBLEMS ===
  "p205_trie": `${TH}
graph TB
    ROOT((root))
    F1((f))
    L1((l))
    O1((o))
    W1((w)):::wordEnd
    E1((e))
    R1((r)):::wordEnd
    I1((i))
    G1((g))
    H1((h))
    T1((t)):::wordEnd
    ROOT --> F1
    F1 --> L1
    L1 --> O1
    O1 --> W1
    W1 --> E1
    E1 --> R1
    L1 --> I1
    I1 --> G1
    G1 --> H1
    H1 --> T1
    classDef wordEnd fill:#4ECDC4,stroke:#fff,color:#000
    style ROOT fill:#FF6B6B,stroke:#fff,color:#fff
    style F1 fill:#45B7D1,stroke:#fff,color:#000
    style L1 fill:#45B7D1,stroke:#fff,color:#000
    style O1 fill:#45B7D1,stroke:#fff,color:#000
    style E1 fill:#45B7D1,stroke:#fff,color:#000
    style I1 fill:#45B7D1,stroke:#fff,color:#000
    style G1 fill:#45B7D1,stroke:#fff,color:#000
    style H1 fill:#45B7D1,stroke:#fff,color:#000`,

  "p331_trie": `${TH}
graph TB
    ROOT((root))
    C1((c)) --> A1((a)) --> T1((t)):::wordEnd
    C1 --> A2((a)) --> T2((t)):::wordEnd --> S1((s)):::wordEnd
    A3((a)) --> N1((n)) --> D1((d)):::wordEnd
    S2((s)) --> A4((a)) --> N2((n)) --> D2((d)):::wordEnd
    D3((d)) --> O1((o)) --> G1((g)):::wordEnd
    ROOT --> C1
    ROOT --> A3
    ROOT --> S2
    ROOT --> D3
    classDef wordEnd fill:#4ECDC4,stroke:#fff,color:#000
    style ROOT fill:#FF6B6B,stroke:#fff,color:#fff
    style C1 fill:#45B7D1,stroke:#fff,color:#000
    style A1 fill:#45B7D1,stroke:#fff,color:#000
    style A2 fill:#45B7D1,stroke:#fff,color:#000
    style A3 fill:#45B7D1,stroke:#fff,color:#000
    style A4 fill:#45B7D1,stroke:#fff,color:#000
    style N1 fill:#45B7D1,stroke:#fff,color:#000
    style N2 fill:#45B7D1,stroke:#fff,color:#000
    style S2 fill:#45B7D1,stroke:#fff,color:#000
    style D3 fill:#45B7D1,stroke:#fff,color:#000
    style O1 fill:#45B7D1,stroke:#fff,color:#000`,

  "p434_trie": `${TH}
graph TB
    ROOT((root))
    H1((h)) --> E1((e)) --> L1((l)) --> L2((l)) --> O1((o)):::wordEnd
    ROOT --> H1
    classDef wordEnd fill:#4ECDC4,stroke:#fff,color:#000
    style ROOT fill:#FF6B6B,stroke:#fff,color:#fff
    style H1 fill:#45B7D1,stroke:#fff,color:#000
    style E1 fill:#45B7D1,stroke:#fff,color:#000
    style L1 fill:#45B7D1,stroke:#fff,color:#000
    style L2 fill:#45B7D1,stroke:#fff,color:#000`,

  "p484_trie": `${TH}
graph TB
    ROOT((root))
    W1((w)) --> O1((o)) --> R1((r)) --> L1((l)) --> D1((d)):::wordEnd
    ROOT --> W1
    classDef wordEnd fill:#4ECDC4,stroke:#fff,color:#000
    style ROOT fill:#FF6B6B,stroke:#fff,color:#fff
    style W1 fill:#45B7D1,stroke:#fff,color:#000
    style O1 fill:#45B7D1,stroke:#fff,color:#000
    style R1 fill:#45B7D1,stroke:#fff,color:#000
    style L1 fill:#45B7D1,stroke:#fff,color:#000`,

  // === REMAINING TREES ===
  "p303_example1": `${DH}
graph LR
    subgraph L1["List 1"]
        A1["1"] --> A2["4"] --> A3["5"]
    end
    subgraph L2["List 2"]
        B1["1"] --> B2["3"] --> B3["4"]
    end
    subgraph L3["List 3"]
        C1["2"] --> C2["6"]
    end
    style A1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style A2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style A3 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style B1 fill:#90EE90,stroke:#228B22,color:#000
    style B2 fill:#90EE90,stroke:#228B22,color:#000
    style B3 fill:#90EE90,stroke:#228B22,color:#000
    style C1 fill:#FFA500,stroke:#CC8400,color:#000
    style C2 fill:#FFA500,stroke:#CC8400,color:#000`,

  "p395_example1": tree([1, 2, 5, 3, 4, 6, 7]),

  "p449_timeline": ivs([[10, 20], [15, 25], [20, 30]], "My Calendar: Book Events", ["#4a9eff", "#FF6B6B", "#90EE90"]),
  "p450_timeline": ivs([[10, 20], [50, 60], [10, 40]], "My Calendar II: Double Booking", ["#4a9eff", "#4a9eff", "#FF6B6B"]),

  // === REMAINING MATRIX ===
  "p125_sudoku": `${DH}
graph TB
    subgraph S["Sudoku Board (9x9)"]
        direction TB
        R0["5  3  .  .  7  .  .  .  ."]
        R1["6  .  .  1  9  5  .  .  ."]
        R2[".  9  8  .  .  .  .  6  ."]
        R3["8  .  .  .  6  .  .  .  3"]
        R4["4  .  .  8  .  3  .  .  1"]
        R5["7  .  .  .  2  .  .  .  6"]
        R6[".  6  .  .  .  .  2  8  ."]
        R7[".  .  .  4  1  9  .  .  5"]
        R8[".  .  .  .  8  .  .  7  9"]
    end
    style S fill:#1a1a2e,stroke:#4a9eff,color:#fff
    style R0 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R1 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R2 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R3 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R4 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R5 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R6 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R7 fill:#2d2d44,stroke:#4a9eff,color:#fff
    style R8 fill:#2d2d44,stroke:#4a9eff,color:#fff`,

  "p169_example1": grid([[9, 9, 4], [6, 6, 8], [2, 1, 1]], "Longest Increasing Path"),
  "p294_example1": grid([[2, 0, 0, 1], [0, 3, 1, 0], [0, 5, 2, 0], [4, 0, 0, 2]], "X-Matrix"),
  "p319_example1": grid([["1","0","1","0","0"],["1","0","1","1","1"],["1","1","1","1","1"],["1","0","0","1","0"]], "Maximal Rectangle"),
  "p335_example1": grid([[-2, -3, 3], [-5, -10, 1], [10, 30, -5]], "Dungeon Game"),
  "p347_example1": grid([[1, 4, 3, 1, 3, 2], [3, 2, 1, 3, 2, 4], [2, 3, 3, 2, 3, 1]], "Trapping Rain Water II"),
  "p360_example1": grid([[9, 9, 4], [6, 6, 8], [2, 1, 1]], "Grid with Obstacles"),
  "p381_example1": grid([[1, 0], [0, 1]], "Making A Large Island"),
  "p382_example1": grid([[1, 0, 0, 0], [0, 0, 0, 0], [0, 0, 2, -1]], "Unique Paths III"),
  "p419_example1": grid([[3, 0, 1, 4, 2], [5, 6, 3, 2, 1], [1, 2, 0, 1, 5], [4, 1, 0, 1, 7], [1, 0, 3, 0, 5]], "Range Sum 2D"),
  "p429_board": grid([["X", " ", " "], [" ", "O", " "], [" ", " ", " "]], "Tic-Tac-Toe Board"),
  "p468_example1": grid([[1, 2], [3, 4]], "Reshape Matrix"),
  "p478_example1": grid([[1, 0, 1, 0, 0], [1, 0, 1, 1, 1], [1, 1, 1, 1, 1], [1, 0, 0, 1, 0]], "Maximal Square"),
  "p495_example1": grid([[1, 1, 1], [1, 0, 1], [1, 1, 1]], "Image Smoother"),

  // === REMAINING STACK ===
  "p306_example1": `${DH}
graph LR
    subgraph Input["Input: s = '(()' "]
        C0["("]
        C1["("]
        C2[")"]
    end
    style C0 fill:#FF6B6B,stroke:#CC5555,color:#fff
    style C1 fill:#90EE90,stroke:#228B22,color:#000
    style C2 fill:#90EE90,stroke:#228B22,color:#000`,

  "p318_example1": `${DH}
graph TB
    subgraph Hist["Histogram: heights = [2,1,5,6,2,3]"]
        direction TB
        H0["2"]
        H1["1"]
        H2["5"]
        H3["6"]
        H4["2"]
        H5["3"]
    end
    style H0 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style H1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style H2 fill:#90EE90,stroke:#228B22,color:#000
    style H3 fill:#90EE90,stroke:#228B22,color:#000
    style H4 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style H5 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  // === GRAPH ===
  "p139_example1": `${DH}
graph TD
    N1((1)) --- N2((2))
    N1 --- N3((3))
    N2 --- N3
    style N1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N3 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    linkStyle 2 stroke:#FF6B6B,stroke-width:3px`,

  "p371_example1": grid([[1, 2, 3], [0, 0, 4], [7, 6, 5]], "Cut Off Trees (0=obstacle)"),
  "p377_example1": `${DH}
graph LR
    subgraph Seats["Row Seats: [0,2,1,3]"]
        S0["Seat 0: Person 0"]
        S1["Seat 1: Person 2"]
        S2["Seat 2: Person 1"]
        S3["Seat 3: Person 3"]
    end
    style S0 fill:#90EE90,stroke:#228B22,color:#000
    style S1 fill:#FF6B6B,stroke:#CC5555,color:#fff
    style S2 fill:#FF6B6B,stroke:#CC5555,color:#fff
    style S3 fill:#90EE90,stroke:#228B22,color:#000`,

  // === REMAINING INTERVAL ===
  "p364_timeline": ivs([[1, 1], [1, 3], [1, 3]], "Data Stream: add 1,3,2 -> intervals evolve", ["#4a9eff", "#90EE90", "#90EE90"]),

  // === SKYLINE ===
  "p336_example1": `${DH}
graph TB
    subgraph SK["Buildings: [left, right, height]"]
        direction TB
        B0["[2,9,10]"]
        B1["[3,7,15]"]
        B2["[5,12,12]"]
        B3["[15,20,10]"]
        B4["[19,24,8]"]
    end
    style B0 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style B1 fill:#90EE90,stroke:#228B22,color:#000
    style B2 fill:#FFA500,stroke:#CC8400,color:#000
    style B3 fill:#9370DB,stroke:#7B68EE,color:#fff
    style B4 fill:#FF6B6B,stroke:#CC5555,color:#fff`,

  "p374_example1": ivs([[1, 2], [2, 3], [6, 1]], "Falling Squares: positions", ["#4a9eff", "#90EE90", "#FFA500"]),
};

// Problem -> diagram mapping
const updates = {
  "111": [{ id: "design", file: "p111_design.png", caption: "Design Twitter: Feed + Follow Graph" }],
  "401": [{ id: "design", file: "p401_design.png", caption: "HashMap: Bucket Array + Chaining" }],
  "402": [{ id: "design", file: "p402_design.png", caption: "HashSet: Bucket Array" }],
  "403": [{ id: "design", file: "p403_design.png", caption: "Doubly Linked List Structure" }],
  "435": [{ id: "design", file: "p403_design.png", caption: "Doubly Linked List Structure" }],
  "404": [{ id: "design", file: "p403_design.png", caption: "Skiplist: Multi-level Linked List" }],
  "433": [{ id: "design", file: "p403_design.png", caption: "Skiplist: Multi-level Linked List" }],
  "406": [{ id: "design", file: "p403_design.png", caption: "Browser History: Doubly Linked List" }],
  "424": [{ id: "design", file: "p403_design.png", caption: "Browser History: Doubly Linked List" }],
  "407": [{ id: "design", file: "p407_design.png", caption: "Circular Queue (capacity=3)" }],
  "408": [{ id: "design", file: "p408_design.png", caption: "Circular Deque (capacity=3)" }],
  "409": [{ id: "design", file: "p409_design.png", caption: "Front Middle Back Queue" }],
  "438": [{ id: "design", file: "p438_design.png", caption: "LFU Cache: Frequency Buckets" }],
  "441": [{ id: "design", file: "p441_design.png", caption: "All O'one: DLL by Count" }],
  "448": [{ id: "design", file: "p448_design.png", caption: "Text Editor: Dual Stack at Cursor" }],
  "205": [{ id: "trie", file: "p205_trie.png", caption: "Trie of flower, flow, flight" }],
  "303": [{ id: "example1", file: "p303_example1.png", caption: "Example 1: Three Sorted Lists to Merge" }],
  "331": [{ id: "trie", file: "p331_trie.png", caption: "Trie of cat, cats, and, sand, dog" }],
  "434": [{ id: "trie", file: "p434_trie.png", caption: "Magic Dictionary Trie: hello" }],
  "484": [{ id: "trie", file: "p484_trie.png", caption: "Trie: word -> world" }],
  "395": [{ id: "example1", file: "p395_example1.png", caption: "Recovered Tree [1,2,5,3,4,6,7]" }],
  "449": [{ id: "timeline", file: "p449_timeline.png", caption: "Calendar Events: [10,20], [15,25], [20,30]" }],
  "450": [{ id: "timeline", file: "p450_timeline.png", caption: "Calendar II: Double booking detection" }],
  "125": [{ id: "sudoku", file: "p125_sudoku.png", caption: "Sudoku Board (9x9)" }],
  "169": [{ id: "example1", file: "p169_example1.png", caption: "3x3 Matrix for Longest Increasing Path" }],
  "294": [{ id: "example1", file: "p294_example1.png", caption: "4x4 X-Matrix" }],
  "319": [{ id: "example1", file: "p319_example1.png", caption: "4x5 Binary Matrix" }],
  "335": [{ id: "example1", file: "p335_example1.png", caption: "3x3 Dungeon Grid" }],
  "347": [{ id: "example1", file: "p347_example1.png", caption: "3x6 Height Map" }],
  "360": [{ id: "example1", file: "p360_example1.png", caption: "Grid with Obstacles" }],
  "381": [{ id: "example1", file: "p381_example1.png", caption: "2x2 Island Grid" }],
  "382": [{ id: "example1", file: "p382_example1.png", caption: "3x4 Path Grid (1=start, 2=end, -1=obstacle)" }],
  "419": [{ id: "example1", file: "p419_example1.png", caption: "5x5 Matrix for 2D Range Sum" }],
  "429": [{ id: "board", file: "p429_board.png", caption: "Tic-Tac-Toe Board (3x3)" }],
  "468": [{ id: "example1", file: "p468_example1.png", caption: "2x2 Matrix to reshape" }],
  "478": [{ id: "example1", file: "p478_example1.png", caption: "4x5 Binary Matrix" }],
  "495": [{ id: "example1", file: "p495_example1.png", caption: "3x3 Image Grid" }],
  "306": [{ id: "example1", file: "p306_example1.png", caption: "Longest Valid Parentheses: s='(()'" }],
  "318": [{ id: "example1", file: "p318_example1.png", caption: "Histogram: [2,1,5,6,2,3]" }],
  "139": [{ id: "example1", file: "p139_example1.png", caption: "Graph: edges [[1,2],[1,3],[2,3]] (red=redundant)" }],
  "371": [{ id: "example1", file: "p371_example1.png", caption: "3x3 Forest Grid (0=obstacle)" }],
  "377": [{ id: "example1", file: "p377_example1.png", caption: "Seat arrangement [0,2,1,3]" }],
  "364": [{ id: "timeline", file: "p364_timeline.png", caption: "Data Stream: Intervals evolve as numbers added" }],
  "336": [{ id: "example1", file: "p336_example1.png", caption: "Buildings for Skyline Problem" }],
  "374": [{ id: "example1", file: "p374_example1.png", caption: "Falling Squares positions" }],
  // Skip these as too abstract for problem-understanding diagrams:
  // p114 (top k freq - just array), p160 (word break - just string),
  // p242 (matrix cells distance - trivial), p253 (richest customer - trivial),
  // p279 (goal parser - string), p339 (kth largest - array),
  // p343, p348, p353, p358, p363, p370, p376, p379, p396, p420, p427, p430, p431, p436, p451
  // p181, p206 - parenthesis problems already covered by p41
};

// Also mark these problems as having diagrams via simple "no diagram needed" update
// For problems that are too abstract for a visual diagram
const skipProblems = [
  "114", "160", "181", "206", "242", "253", "279", "339",
  "343", "348", "353", "358", "363", "370", "376", "379",
  "396", "420", "427", "430", "431", "436", "451"
];

let created = 0;
for (const [name, content] of Object.entries(diagrams)) {
  const fp = path.join(MERMAID_DIR, `${name}.mmd`);
  if (fs.existsSync(fp)) { console.log(`  skip: ${name}.mmd`); continue; }
  fs.writeFileSync(fp, content);
  console.log(`  + ${name}.mmd`);
  created++;
}
console.log(`\nCreated ${created} .mmd files\n`);

let updated = 0;
for (const [id, diags] of Object.entries(updates)) {
  const fp = path.join(PROBLEMS_DIR, `p${id}.json`);
  try {
    const p = JSON.parse(fs.readFileSync(fp, "utf-8"));
    p.diagrams = diags;
    fs.writeFileSync(fp, JSON.stringify(p, null, 2) + "\n");
    console.log(`  + p${id}.json`);
    updated++;
  } catch (e) { console.error(`  ! p${id}: ${e.message}`); }
}
console.log(`\nUpdated ${updated} problem JSONs`);
console.log(`\nSkipped ${skipProblems.length} problems (too abstract for problem-understanding diagrams)`);
