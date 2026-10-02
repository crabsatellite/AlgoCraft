"""Keep reviewed diagram captions consistent with reproducible rule illustrations."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'question_bank/official'
# (Chinese, English); every entry was checked against its real example or labeled illustrative.
CAPTIONS = {
    69: ('示例 1：next 从左向右连接；表格按节点下标列出 random 的目标，null 表示没有目标', 'Example 1: next links run left to right; the table gives each random target by node index, with null for no target'),
    73: ('示例 1 中 put(1,1)、put(2,2)、get(1) 后的缓存：键 2 最久未使用，键 1 最近使用', 'Example 1 after put(1,1), put(2,2), get(1): key 2 is least recent and key 1 most recent'),
    75: ('示例 1：k = 2，框线标出两个完整组和一个不足 k 个节点的尾组', 'Example 1: k = 2; outlines mark two complete groups and a leftover single node'),
    87: ('示例 1 的完整二叉搜索树，黄色节点为 p = 2 和 q = 8，节点 4 的孩子是 3 和 5', 'Complete Example 1 BST; yellow nodes are p = 2 and q = 8, and node 4 has children 3 and 5'),
    101: ('示例 1 中插入 app 和 apple 后的前缀树；黄色表示完整单词的末尾', 'Example 1 trie after app and apple are inserted; yellow marks the end of a complete word'),
    102: ('bad、dad、mad 的前缀树；黄色表示单词末尾，.ad 可以匹配这三个单词', 'Trie for bad, dad and mad; yellow marks word ends, and .ad matches all three words'),
    104: ('字典中的 cat、bat、rat 展示为完整字符路径；黄色表示词根末尾', 'Dictionary strings cat, bat and rat in a prefix tree; yellow marks complete roots'),
    105: ('示例 1 中插入 apple:3 和 app:2 后的完整字符路径；黄色末尾节点分别保存键值', 'Complete character paths after inserting apple:3 and app:2 in Example 1; yellow terminal nodes store their respective values'),
    111: ('示例 1 中用户 1 关注用户 2、用户 2 发布推文 6 后：用户 1 的动态为 [6,5]，新推文在前', 'Example 1 after user 1 follows user 2 and user 2 posts tweet 6: user 1 sees [6,5], newest first'),
    112: ('示例 1：加入 1、2 后中位数为 1.5；再加入 3 后中位数为 2', 'Example 1: the median is 1.5 after adding 1 and 2, then 2 after adding 3'),
    124: ('规则示意：一个皇后攻击同一行、同一列和两条斜线；x 格不能再放皇后', 'Rule illustration: one queen attacks its row, column and diagonals; another queen cannot occupy an x square'),
    139: ('示例 1 的三条无向边 [[1,2],[1,3],[2,3]]，图中没有预先标出要删除的边', 'The three undirected edges [[1,2],[1,3],[2,3]] of Example 1, without selecting an edge to remove'),
    146: ('示例 1 的全部五个单词按已知顺序展示；比较单词使用未知字母顺序，完整前缀在前', 'All five Example 1 words in the given order; words use the unknown letter order, with complete prefixes first'),
    150: ('示例 1 的全部无向边；任务是把所有节点分成两组，使每条边连接不同组', 'All undirected edges of Example 1; partition all nodes into two groups so every edge crosses between groups'),
    151: ('n = 3 的规则示意：起点是第 0 层，终点就是第 3 层，每次走 1 或 2 层', 'Rule illustration for n = 3: start at level 0 and finish at level 3, moving one or two levels at a time'),
    152: ('示例 1：可选择下标 0 或 1 为起点并付费，每次前进 1 或 2 级，下标 3 是免费终点', 'Example 1: start and pay at index 0 or 1, move one or two steps, and reach the free top at index 3'),
    154: ('环形街道的规则示意：相邻房屋不能同时选择，第一间和最后一间也相邻', 'Rule illustration of a circular street: adjacent houses cannot both be chosen, including the first and last'),
    158: ('示例 1 的面额 1、2、5 可不限次数使用；目标总额为 11，题目要求最少硬币数', 'Example 1 denominations 1, 2 and 5 can be reused without limit; target amount is 11 and the task asks for the fewest coins'),
    159: ('连续范围的规则示意：[-4,0,7] 是合法子数组，乘积为 0；不能跳过中间的 0', 'Contiguous-range illustration: [-4,0,7] is a valid subarray with product 0; the interior zero cannot be skipped'),
    160: ('示例 1 中 leet 和 code 都在字典里，按顺序连接后覆盖整个 leetcode', 'Example 1: leet and code are dictionary words whose concatenation covers the entire string leetcode'),
    162: ('示例 1 的一种合法划分：[1,5,5] 和 [11]；每个输入元素使用一次，两组的和都是 11', 'One valid Example 1 partition: [1,5,5] and [11], using every input occurrence once with equal sums of 11'),
    163: ('示例 1 的 3 行 7 列网格：S 在左上角，E 在右下角，只能向右或向下移动一格', 'Example 1 grid with 3 rows and 7 columns: S at the top left, E at the bottom right, and one-cell right or down moves'),
    172: ('示例 1 的当前气球行：先戳破值 1 的气球可获 3*1*5 枚硬币，之后 3 和 5 成为相邻气球', 'Example 1 current row: bursting value 1 earns 3*1*5 coins, then values 3 and 5 become neighbors'),
    174: ('规则示意使用数组 [3,-2,5,-1,2]；[3,-2,5] 是合法连续候选，和为 6，未声称它是最优答案', 'Illustrative array [3,-2,5,-1,2]; [3,-2,5] is a valid contiguous candidate of sum 6, without claiming it is optimal'),
    195: ('示例 1 第一次 count 前：三个蓝色坐标各存一份，黄色 [11,10] 仅为查询点', 'Before the first count in Example 1: three blue stored points have one copy each; yellow [11,10] is only the query point'),
    331: ('示例 1 的完整字典与一种合法切分 cats and dog；字符串的所有字符按原顺序使用', 'Full Example 1 dictionary and one valid split, cats and dog, using every string character in order'),
    336: ('示例 1 的五座建筑，标出左右横坐标和高度；遮住的建筑仍然存在', 'Five Example 1 buildings labeled by horizontal endpoints and heights; occluded building portions still exist'),
    376: ('n=2、k=2 的规则示意：输入 01100 时四个连续两位窗口为 01、11、10、00，覆盖全部密码', 'Rule illustration for n=2, k=2: input 01100 yields consecutive windows 01, 11, 10 and 00, covering every password'),
    401: ('示例 1：put(1,1)、put(2,2) 后存有两个键，再 put(2,1) 只替换键 2 的值', 'Example 1: put(1,1) and put(2,2) store two keys; put(2,1) replaces only the value for key 2'),
    402: ('示例 1：add(1)、add(2) 后存有键 1、2，再 add(2) 不增加副本', 'Example 1: add(1) and add(2) store keys 1 and 2; another add(2) does not add a duplicate'),
    403: ('示例 1 中 addAtHead(1)、addAtTail(3)、addAtIndex(1,2) 后的节点；head 和 tail 标注真实首尾节点', 'Nodes after addAtHead(1), addAtTail(3), addAtIndex(1,2) in Example 1; head and tail label the actual endpoint nodes'),
    407: ('示例 1 最后一次成功入队后的逻辑顺序 [2,3,4]，队首为 2，队尾为 4', 'Logical order [2,3,4] after the final successful enqueue in Example 1; front is 2 and rear is 4'),
    408: ('示例 1 删除队尾 2、再将 4 插入队首后的逻辑顺序 [4,3,1]，队首为 4，队尾为 1', 'Logical order [4,3,1] after deleting rear 2 and inserting 4 at the front in Example 1; front is 4 and rear is 1'),
    412: ('示例 1 的两个时刻：初始 search(1) 返回商店 [1,0,2]；两次 rent 后 report 返回 [[0,1],[1,2]]', 'Two Example 1 moments: initial search(1) returns shops [1,0,2]; after two rent calls, report returns [[0,1],[1,2]]'),
    413: ('示例 1：fix(3)、fix(1) 后为 01010，再 flip 后为 10101，每一位都反转', 'Example 1: fix(3) and fix(1) produce 01010, then flip produces 10101, reversing every bit'),
    438: ('示例 1 中 put(1,1)、put(2,2)、get(1) 后，键 1 使用次数为 2，键 2 为 1，并列出最后使用操作', 'Example 1 after put(1,1), put(2,2), get(1): key 1 has frequency 2 and key 2 frequency 1, with their last-use operations shown'),
    495: ('示例 1 的邻域对比：中心像素计算 9 格，左上角像素只计算图像内的 4 格', 'Example 1 neighborhoods: the center uses 9 cells, while the top-left pixel uses only the 4 cells inside the image'),
    496: ('示例 1 最底层的位置是 [5,3,null,9]；宽度包含首尾节点和中间空位，共 4 格', 'Example 1 bottom-level positions are [5,3,null,9]; width includes both endpoints and the interior gap, totaling 4 positions'),
}

def apply():
    changed = []
    for n, pair in CAPTIONS.items():
        for language, caption in zip(('zh','en'),pair):
            path = BANK / (f'lang/zh_cn/p{n}.json' if language=='zh' else f'p{n}.json')
            p=json.loads(path.read_text(encoding='utf8'))
            diagrams=p['diagrams']
            assert len(diagrams)==1, n
            diagrams[0]['caption']=caption
            if 'alt' in diagrams[0]:
                diagrams[0]['alt']=caption
            path.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        changed.append(n)
    print(f'Updated bilingual captions for {len(changed)} reviewed diagrams')

if __name__=='__main__':
    apply()
