"""Apply reviewed prompt edits without touching executable contracts or solutions."""
import json
import re
import zipfile
from pathlib import Path
from readability_edits import (
    ADDITIONS, BODIES_EXTRA, DROP_PARAGRAPHS, LEXICAL_IDS, PALINDROME_IDS, PARAGRAPHS,
)

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'question_bank/official'
STATE = ROOT / 'build/review/readability-2026-09-30'

# Each tuple is (Chinese task statement, English task statement).
BODIES = {
    3: (
        '给定两个字符串 `s` 和 `t`，判断它们是否为字母异位词。是则返回 `true`，否则返回 `false`。\n\n两个字符串互为字母异位词，表示可以只改变字母顺序，把一个字符串变成另一个；每种字母的出现次数必须完全相同。例如，`"aab"` 与 `"aba"` 是字母异位词，`"aab"` 与 `"abb"` 不是。',
        'Given two strings `s` and `t`, return `true` if they are anagrams; otherwise return `false`.\n\nTwo strings are anagrams when rearranging the letters of one produces the other. Every letter must occur the same number of times in both strings. For example, `"aab"` and `"aba"` are anagrams, but `"aab"` and `"abb"` are not.'),
    4: (
        '给定字符串数组 `strs`，把互为字母异位词的字符串分在同一组，返回所有组。\n\n字母异位词使用相同的字母，而且每种字母的出现次数相同，只有顺序可以不同。例如，`"eat"`、`"tea"`、`"ate"` 属于同一组，`"tan"` 不属于这一组。\n\n每个输入字符串都必须在结果中出现一次，包括输入中的重复字符串。组与组之间、同一组内部都可以按任意顺序排列。',
        'Given the string array `strs`, group strings that are anagrams and return all groups.\n\nAnagrams contain the same letters with the same counts, in any order. For example, `"eat"`, `"tea"`, and `"ate"` belong together; `"tan"` does not belong to that group.\n\nInclude every input occurrence exactly once, including duplicate strings. Both the groups and the strings within each group may be returned in any order.'),
    8: (
        '实现两个方法，把字符串列表转换成一个字符串，再从这个字符串还原列表：\n\n```java\npublic String encode(List<String> strs)\npublic List<String> decode(String encoded)\n```\n\n还原结果必须与输入完全相同：字符串数量、顺序、每个字符串中的字符都不能改变。空列表 `[]` 和只包含一个空字符串的列表 `[""]` 是不同的输入，也必须能够区分。\n\n你可以自行选择编码格式。示例输出展示的是 `decode(encode(strs))` 的结果，不要求 `encode` 返回某个指定字符串。\n\n输入可以包含扩展 ASCII 字符，包括数字和标点。解码只能依赖编码后的字符串，不能依赖编码时保存在成员、静态变量或外部的原列表。请自行实现格式，不调用 JSON 或 Java 对象序列化等现成序列化库。',
        'Implement two methods that turn a list of strings into one string and reconstruct the list from it:\n\n```java\npublic String encode(List<String> strs)\npublic List<String> decode(String encoded)\n```\n\nReconstruction must preserve the number and order of strings and every character. Distinguish the empty list `[]` from a list containing one empty string `[""]`.\n\nChoose your own encoding format. Example outputs show `decode(encode(strs))`, not a prescribed result of `encode`.\n\nInputs may contain extended ASCII characters, including digits and punctuation. Decoding must rely only on the encoded string, without retaining the original list in instance, static, or external state. Implement the format yourself rather than calling JSON or Java object serialization libraries.'),
    17: (
        '给定整数数组 `nums` 和整数 `target`，返回所有和为 `target` 的不重复四元组。每个四元组中的四个数必须取自四个不同的下标；数值本身可以相同，只要数组中有足够多的对应元素。\n\n例如，数组中的四个不同位置都为 `2` 时，可以得到 `[2,2,2,2]`。但是同一组数值无论以什么顺序出现，都只应返回一次。\n\n四元组之间、每个四元组内部的数字都可以按任意顺序排列。',
        'Given an integer array `nums` and an integer `target`, return every distinct quadruplet whose sum is `target`. Choose the four values from four different indices. Values may be equal when the array has enough occurrences.\n\nFor example, four separate occurrences of `2` can form `[2,2,2,2]`. Return each multiset of four values only once, regardless of their order.\n\nThe quadruplets and the values within each quadruplet may appear in any order.'),
    18: (
        '给定整数数组 `nums`，把它原地改成下一个排列。排列使用原数组中的全部元素，每个元素恰好使用一次；重复值的数量也必须保持不变。\n\n这里按字典序比较两个排列：从左到右找到第一个不同的位置，该位置数值较小的排列排在前面。例如，`[1,2,3]` 排在 `[1,3,2]` 前面。\n\n“下一个”是所有排列按这个规则排序后紧跟当前排列的那个。如果当前排列已经最大，就改成最小的排列，也就是升序。\n\n只允许使用常数级额外空间。方法不返回新数组；示例输出是调用 `nextPermutation` 后 `nums` 的内容。',
        'Rearrange the integer array `nums` in place into its next permutation. Use every input occurrence exactly once, preserving the counts of duplicate values.\n\nCompare permutations lexicographically: at their first differing position, the smaller value comes first. For example, `[1,2,3]` comes before `[1,3,2]`.\n\nThe next permutation is the one immediately after the current permutation in that order. If the current one is largest, change it to the smallest, ascending order.\n\nUse only constant extra space. The method does not return a new array; example outputs show `nums` after `nextPermutation` returns.'),
    25: (
        '给定整数数组 `height`，它表示一排紧挨着的柱子：第 `i` 根柱子的高度为 `height[i]`，每根柱子的宽度都是 `1`。\n\n下雨后，雨水可以积在柱子之间的低处，但会从没有柱子挡住的左右边界流走。柱子本身占据的空间不能装水。返回最终留在这些柱子之间的雨水总量。\n\n每个宽为 `1`、高为 `1` 的小格算 `1` 单位水。例如，`[2,0,2]` 中间可以留下 `2` 单位水；`[0,1,2]` 留不住水。',
        'The integer array `height` describes adjacent bars. Bar `i` has height `height[i]` and width `1`.\n\nAfter rain, water can remain in low spaces between bars, but it escapes through an unblocked left or right boundary. Space occupied by a bar cannot hold water. Return the total water remaining between the bars.\n\nA square of width `1` and height `1` holds one unit of water. For example, `[2,0,2]` holds `2` units, while `[0,1,2]` holds none.'),
    64: (
        '给定非负整数数组 `nums` 和整数 `k`，把数组切成恰好 `k` 段非空连续子数组。所有元素必须按原来的顺序恰好属于其中一段，不能遗漏、重复或重新排序。\n\n每种切法都有一个分数：计算各段元素之和，取其中最大的和。请返回所有合法切法中最低的分数。\n\n例如，`[7,2,5,10,8]` 切成 `[7,2,5]` 和 `[10,8]`，两段的和是 `14` 和 `18`，这次切法的分数是 `18`。',
        'Split the nonnegative integer array `nums` into exactly `k` nonempty contiguous parts. Preserve the original order, and use every element exactly once.\n\nThe score of a split is the largest sum among its parts. Return the lowest possible score over all valid splits.\n\nFor example, splitting `[7,2,5,10,8]` into `[7,2,5]` and `[10,8]` gives sums `14` and `18`, so that split has score `18`.'),
    65: (
        '给定单链表的头节点 `head`，把节点顺序完全反转，并返回反转后的头节点。\n\n单链表的每个节点用 `val` 保存数值，用 `next` 指向下一个节点；最后一个节点的 `next` 为 `null`。例如，`1 -> 2 -> 3` 需要变成 `3 -> 2 -> 1`。\n\n必须复用原来的节点，只改变它们之间的连接。示例数组只是展示链表的节点顺序；你的方法接收和返回的都是 `ListNode` 节点。',
        'Reverse the node order of the singly linked list starting at `head` and return its new head.\n\nEach node stores a value in `val` and points to the next node through `next`. The last node points to `null`. For example, change `1 -> 2 -> 3` into `3 -> 2 -> 1`.\n\nReuse the existing nodes and change only their links. Example arrays display the node order; the method receives and returns `ListNode` objects.'),
    78: (
        '给定单链表的头节点 `head`，返回按节点值非递减排列的链表头节点。非递减表示相邻值可以相等，但后面的值不能更小。\n\n结果必须保留输入中每个值的出现次数，并且最后一个节点的 `next` 必须为 `null`，不能产生环。',
        'Given the head of a singly linked list, return a list whose node values are in nondecreasing order: equal adjacent values are allowed, but a later value cannot be smaller.\n\nPreserve the number of occurrences of every input value. The final node must point to `null`; do not create a cycle.'),
    93: (
        '给定两个数组 `preorder` 和 `inorder`，它们分别记录同一棵二叉树的前序遍历和中序遍历，所有节点值互不相同。请还原这棵树并返回根节点。\n\n“遍历”表示按规定顺序访问每个节点一次，并记下节点值：\n\n* 前序：先访问根节点，再访问整个左子树，最后访问整个右子树。\n* 中序：先访问整个左子树，再访问根节点，最后访问整个右子树。\n\n对每棵子树也使用相同的访问规则。例如，根为 `2`、左孩子为 `1`、右孩子为 `3` 的树，前序是 `[2,1,3]`，中序是 `[1,2,3]`。',
        'The arrays `preorder` and `inorder` record traversals of the same binary tree, whose node values are all distinct. Reconstruct the tree and return its root.\n\nA traversal visits every node once and records its value:\n\n* Preorder: visit the root, then the entire left subtree, then the entire right subtree.\n* Inorder: visit the entire left subtree, then the root, then the entire right subtree.\n\nApply the same rule within each subtree. For a root `2` with left child `1` and right child `3`, preorder is `[2,1,3]` and inorder is `[1,2,3]`.'),
}


def replace_intro(document, body):
    title = document['description'].splitlines()[0]
    rest = re.search(r'(?m)^## ', document['description'])
    if not rest:
        raise ValueError(f'Missing statement sections: {document["title"]}')
    document['description'] = title + '\n\n' + body.strip() + '\n\n' + document['description'][rest.start():]


def write(number, language, document):
    path = BANK / (f'lang/zh_cn/p{number}.json' if language == 'zh' else f'p{number}.json')
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def replace_section(document, headings, body):
    pattern = r'(?m)^## (?:' + '|'.join(map(re.escape, headings)) + r')[^\n]*\n[\s\S]*?(?=^## |\Z)'
    match = re.search(pattern, document['description'], re.M)
    if not match:
        raise ValueError(f'Missing section {headings}')
    document['description'] = document['description'][:match.start()] + body + document['description'][match.end():]


def append_intro(document, body):
    match = re.search(r'(?m)^## ', document['description'])
    assert match
    document['description'] = document['description'][:match.start()].rstrip() + '\n\n' + body + '\n\n' + document['description'][match.start():]


def edit(document, number, language, canonical):
    lang_index = 0 if language == 'zh' else 1
    first_section = re.search(r'(?m)^## ', document['description'])
    assert first_section
    parts = re.split(r'\n\s*\n', document['description'][:first_section.start()].strip())
    for index, pair in PARAGRAPHS.get(number, {}).items():
        assert isinstance(pair, tuple) and len(pair) == 2, (number, index)
        parts[index] = pair[lang_index]
    for index in sorted(DROP_PARAGRAPHS.get(number, []), reverse=True):
        del parts[index]
    if number in PARAGRAPHS or number in DROP_PARAGRAPHS:
        document['description'] = '\n\n'.join(parts) + '\n\n' + document['description'][first_section.start():]
    bodies = {**BODIES, **BODIES_EXTRA}
    if number in bodies:
        replace_intro(document, bodies[number][lang_index])
    if number in ADDITIONS:
        append_intro(document, ADDITIONS[number][lang_index])
    if number in PALINDROME_IDS:
        append_intro(document, ('回文串正着读和反着读相同，例如 `"aba"`、`"aa"`；单个字符也是回文串。', 'A palindrome reads the same forward and backward, such as `"aba"` or `"aa"`. A single character is also a palindrome.')[lang_index])
    if number in LEXICAL_IDS:
        append_intro(document, ('字典序从左到右比较第一处不同的字母，按 `a` 到 `z` 的顺序决定先后。若一个字符串是另一个的完整开头，较短的在前，例如 `"ab"` 在 `"abc"` 前。', 'Lexicographic order compares the first differing letter using `a` through `z`. If one string is the complete beginning of another, the shorter string comes first: `"ab"` precedes `"abc"`.')[lang_index])

    special = {
        72: ('## 约束条件\n\n- `1 <= n <= 100000`。\n- `nums.length == n + 1`。\n- `1 <= nums[i] <= n`。\n- 恰好一种数值出现两次或更多次；其他数值最多出现一次，也可以没有出现。\n\n', '## Constraints\n\n- `1 <= n <= 100000`.\n- `nums.length == n + 1`.\n- `1 <= nums[i] <= n`.\n- Exactly one value occurs two or more times; all other values occur at most once and may be absent.\n\n'),
        106: ('## 约束\n\n* `k >= 1`，初始化后固定不变。\n* 输入值是整数，允许负数、零和重复值。\n* 每次 `add` 之后，当前数据流至少有 `k` 个值。\n\n', '## Constraints\n\n* `k >= 1` and remains fixed after initialization.\n* Input values are integers and may be negative, zero, or repeated.\n* At least `k` values are available after every `add` call.\n\n'),
        107: ('## 约束\n\n至少有一块石头，重量都是正整数，允许重复重量。\n\n', '## Constraints\n\nThere is at least one stone. Weights are positive integers and may repeat.\n\n'),
        108: ('## 约束\n\n* `1 <= k <= points.length`。\n* 每个输入点包含两个整数坐标，坐标允许为负数或零，点允许重复。\n\n', '## Constraints\n\n* `1 <= k <= points.length`.\n* Each point has two integer coordinates, which may be negative or zero; points may repeat.\n\n'),
    }
    if number in special:
        replace_section(document, ['Constraints', '约束'], special[number][lang_index])
    if number == 217:
        old = ('*   `columnTitle` 在 `["A", "FXSHRXW"]` 范围内。', '*   `columnTitle` is in the range `["A", "FXSHRXW"]`.')[lang_index]
        new = ('*   `columnTitle` 对应的列号在 `[1, 2^31 - 1]` 范围内。', '*   The column number represented by `columnTitle` is in `[1, 2^31 - 1]`.')[lang_index]
        assert old in document['description']
        document['description'] = document['description'].replace(old, new)
    if number == 125 and language == 'zh':
        document['description'] = document['description'].replace('子宫格', '宫').replace('子宫', '宫')
    if number in (128, 129):
        replacement = ('从一块陆地开始，沿上下左右相邻的陆地可以走到的所有陆地格子，组成同一座岛屿。斜向相邻不算。一块孤立陆地也算岛屿。网格之外视为水。', 'All land cells reachable from one land cell through side-adjacent land form one island. Diagonal contact does not count. An isolated land cell is also an island; outside the grid is water.')[lang_index]
        if number == 129:
            replacement += (' 岛屿面积是它包含的陆地格子数；没有陆地返回 `0`。', ' Area counts its land cells; return `0` if there is no land.')[lang_index]
        intro = re.search(r'(?m)^## ', document['description'])
        paragraphs = re.split(r'\n\s*\n', document['description'][:intro.start()].strip())
        paragraphs[2] = replacement
        document['description'] = '\n\n'.join(paragraphs) + '\n\n' + document['description'][intro.start():]
    if number == 349:
        append_intro(document, ('最大公因数是同时整除两个数的最大正整数。一个连通组包含沿边可以互相走到的全部节点，大小按节点数计算。', 'The greatest common divisor is the largest positive integer dividing both values. A connected group consists of all nodes reachable from each other along edges, and its size counts nodes.')[lang_index])
    if number in (197, 199):
        append_intro(document, ('负数也有固定的 32 个二进制位。例如，`-1` 的 32 位全部为 `1`；`-3` 的位模式是 `11111111111111111111111111111101`。', 'Negative values also have exactly 32 bits. For example, `-1` has 32 one bits; `-3` has pattern `11111111111111111111111111111101`.')[lang_index])
    if number == 338:
        replace_section(document, ['Important Details', '重要细节'], ('## 重要细节\n\n如果 `s` 已经是回文串，返回原字符串。空字符串返回空字符串。原字符不能删除、重排或在中间插入字符。\n\n', '## Important Details\n\nReturn `s` unchanged if it is already a palindrome. An empty input returns an empty string. Do not remove or reorder original characters or insert characters within them.\n\n')[lang_index])
    if number == 339:
        replace_section(document, ['Important Details', '重要细节'], ('## 重要细节\n\n位置不能小于 `0`。可以跳过目标位置后再返回，但每次都不能落在禁止位置，且不能连续向后跳两次。\n\n', '## Important Details\n\nPositions below `0` are forbidden. You may jump beyond the target and come back, but never land on a forbidden position or make two backward jumps consecutively.\n\n')[lang_index])
    if number == 337:
        line = ('* 不要生成中间表达式字符串；应该直接解析输入。', '* Do not emit or store intermediate string expressions; parse the expression directly.')[lang_index]
        assert line in document['description']
        document['description'] = document['description'].replace(line + '\n', '')
    if number == 400:
        replace_section(document, ['Explanation', 'Note', '说明'], '')
    if number == 485:
        replace_section(document, ['Key Clarification', '关键澄清'], ('## 关键澄清\n\n没有被禁止的参议员，会在下一轮按原来的相对顺序再次行动。\n\n', '## Key Clarification\n\nSenators who remain active act again in the next round, keeping their original relative order.\n\n')[lang_index])
    if number == 486:
        replace_section(document, ['Key Clarification', '关键澄清'], ('## 关键澄清\n\n复制会替换上一次复制的内容。粘贴把整段复制内容加到现有文本末尾，不会消耗复制内容。\n\n', '## Key Clarification\n\nCopy replaces the previous copied content. Paste appends that whole content to the current text without consuming the copy.\n\n')[lang_index])
    if number == 438 and language == 'zh':
        document['description'] = document['description'].replace('最近最久未使用的那个', '距离上次使用时间最久的那个')
    if number in (361, 493):
        document['description'] = document['description'].replace('升序', '非递减').replace('ascending order', 'nondecreasing order')
    if number == 421:
        append_intro(document, ('每次用完首尾两个数后，将它们从剩余数组中移除，再处理新的首尾。', 'Remove both endpoint values after using them, then process the new endpoints.')[lang_index])
    if number == 461:
        append_intro(document, ('返回 `x` 和 `y` 对应二进制位不同的数量。', 'Return the number of differing binary positions between `x` and `y`.')[lang_index])
    if number == 231:
        append_intro(document, ('三个数必须来自三个不同的下标；允许数值相同，只要输入中有对应的多个元素。', 'Select three different indices; equal values are allowed when the input contains enough occurrences.')[lang_index])

    # Per-question format notes are selected from actual signatures and examples.
    if 'TreeNode' in canonical.get('initialCode', ''):
        append_intro(document, ('示例中的树用层序列表展示：先写根，再按已有节点的顺序依次列出它们的左、右孩子；`null` 表示缺少孩子，末尾的 `null` 可以省略。树节点对象使用 `val`、`left`、`right` 保存值和孩子；代码中的树参数或返回值是 `TreeNode` 对象。', 'Example trees use a level-order display: list the root, then each existing node\'s left and right children in node order. `null` means an absent child; trailing nulls may be omitted. A tree node stores `val`, `left`, and `right`; tree parameters or return values in code are `TreeNode` objects.')[lang_index])
    if 'ListNode' in canonical.get('initialCode', '') and number != 65:
        append_intro(document, ('示例数组按 `next` 的连接顺序展示节点值，`[]` 表示空链表（`null`）。节点用 `val` 保存数值、`next` 指向下一个节点；方法中的链表参数和返回值按初始代码使用节点对象。', 'Example arrays display values in `next` order; `[]` denotes an empty list (`null`). A node stores its value in `val` and its next node in `next`. Use node objects for list parameters and results as shown in the initial code.')[lang_index])
    examples = canonical.get('examples', [])
    first_input = examples[0].get('input', '') if examples else ''
    if re.match(r'^\s*\[\s*"[A-Za-z]', first_input) and re.search(r'class\s+(?!Solution\b)\w+', canonical.get('initialCode', '')):
        append_intro(document, ('操作示例中，第一组数组是操作名，第二组是对应参数，按同一下标配对并从左到右执行。第一项创建对象，之后都操作同一个对象。输出数组也逐项对应：构造和没有返回值的方法写作 `null`；`[]` 作为参数表示无参数调用。', 'In operation examples, the first array lists operations and the second lists their argument lists, paired by index and executed left to right. The first operation constructs the object; later operations use that same object. Outputs align by index too: constructors and void methods produce `null`; an empty argument list `[]` means no arguments.')[lang_index])


def apply():
    changed = []
    baseline = zipfile.ZipFile(STATE / 'question-bank-before.zip')
    for number in range(1, 501):
        canonical = json.loads(baseline.read(f'question_bank/official/p{number}.json'))
        for language in ('zh', 'en'):
            path = BANK / (f'lang/zh_cn/p{number}.json' if language == 'zh' else f'p{number}.json')
            document = json.loads(path.read_text(encoding='utf-8'))
            prior_description = document['description']
            original = baseline.read(path.relative_to(ROOT).as_posix())
            before = json.loads(original)
            document['description'] = before['description']
            edit(document, number, language, canonical)
            if document['description'] != before['description']:
                changed.append((number, language))
            if document['description'] != prior_description:
                write(number, language, document)
    print(f'Reviewed edits: {len(changed)} localized statements across {len(set(n for n, _ in changed))} questions')
    (STATE / 'text-edits.json').write_text(json.dumps(changed, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    apply()
