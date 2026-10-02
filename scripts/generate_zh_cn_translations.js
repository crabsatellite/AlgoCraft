const fs = require('fs');
const path = require('path');

const root = path.join('question_bank', 'official');
const outDir = path.join(root, 'lang', 'zh_cn');
const cachePath = path.join('build', 'zh-cn-translation-cache.json');
fs.mkdirSync(outDir, { recursive: true });
fs.mkdirSync(path.dirname(cachePath), { recursive: true });

let cache = {};
if (fs.existsSync(cachePath)) {
  cache = JSON.parse(fs.readFileSync(cachePath, 'utf8'));
}
let dirty = 0;

const titleOverrides = new Map(Object.entries({
  '1': '两数之和',
  '2': '存在重复元素',
  '3': '有效的字母异位词',
  '4': '字母异位词分组',
  '5': '前 K 个高频元素',
  '6': '除自身以外数组的乘积',
  '7': '有效的数独',
  '8': '字符串编码与解码',
  '9': '最长连续序列',
  '10': '和为 K 的子数组',
  '11': '缺失的第一个正数',
  '12': '多数元素',
  '13': '移动零',
  '14': '找到所有数组中消失的数字',
  '15': '两个数组的交集 II',
  '16': '三数之和',
  '17': '四数之和',
  '18': '下一个排列',
  '19': '杨辉三角',
  '20': '矩阵置零',
  '21': '有效回文串',
  '22': '两数之和 II - 输入数组已排序',
  '24': '盛最多水的容器',
  '25': '接雨水',
  '31': '买卖股票的最佳时机',
  '32': '无重复字符的最长子串',
  '35': '最小覆盖子串',
  '41': '有效的括号',
  '42': '最小栈',
  '47': '柱状图中最大的矩形',
  '51': '二分查找',
  '65': '反转链表',
  '66': '合并两个有序链表',
  '70': '两数相加',
  '73': 'LRU 缓存',
  '81': '翻转二叉树',
  '91': '验证二叉搜索树',
  '95': '二叉树的序列化与反序列化',
  '101': '实现 Trie（前缀树）',
  '128': '岛屿数量',
  '135': '课程表',
  '140': '单词接龙',
  '151': '爬楼梯',
  '155': '最长回文子串',
  '158': '零钱兑换',
  '164': '最长公共子序列',
  '171': '编辑距离',
  '173': '正则表达式匹配',
  '181': '有效的括号字符串',
  '188': '旋转图像',
  '189': '螺旋矩阵',
  '193': '快速幂 Pow(x, n)',
  '203': '罗马数字转整数',
  '204': '整数转罗马数字',
  '238': '二叉搜索树的范围和',
  '301': '制作花束的最少天数',
  '306': '最长有效括号',
  '312': 'N 皇后 II',
  '336': '天际线问题',
  '337': '基本计算器',
  '352': '祖玛游戏',
  '360': '将玩家分成技能值相等的队伍',
  '389': '二叉树中的摄像头',
  '401': '设计 HashMap',
  '402': '设计 HashSet',
  '403': '设计链表',
  '404': '设计跳表',
  '438': 'LFU 缓存',
  '441': '全 O(1) 的数据结构',
  '448': '设计文本编辑器',
  '449': '我的日程安排表 I',
  '450': '我的日程安排表 II',
  '451': '我的日程安排表 III',
  '485': 'Dota2 参议院',
  '486': '两个键的键盘',
  '488': '两数之和 IV - 输入是一棵 BST',
  '491': '乘法表中第 K 小的数',
  '498': '奇怪的打印机',
  '500': '修剪二叉搜索树'
}));

function saveCache(force = false) {
  if (force || dirty >= 25) {
    fs.writeFileSync(cachePath, JSON.stringify(cache, null, 2), 'utf8');
    dirty = 0;
  }
}

function normalizeChinese(text) {
  return text
    .replaceAll('您', '你')
    .replaceAll('， ，', '，')
    .replaceAll(' 。', '。')
    .replaceAll(' ：', '：')
    .replaceAll('LRU缓存', 'LRU 缓存')
    .replaceAll('LFU缓存', 'LFU 缓存')
    .replaceAll('字谜词', '字母异位词')
    .replaceAll('排列组合', '排列')
    .replaceAll('布尔', '布尔值')
    .replaceAll('二进制搜索树', '二叉搜索树')
    .replaceAll('优先级队列', '优先队列');
}

function protectInlineCode(text) {
  const held = [];
  const protectedText = text.replace(/`[^`]*`/g, (match) => {
    const key = `@@CODE${held.length}@@`;
    held.push([key, match]);
    return key;
  });
  return { protectedText, held };
}

function restoreHeld(text, held) {
  let out = text;
  for (const [key, value] of held) {
    out = out.replaceAll(key, value);
  }
  return out;
}

async function translateRaw(text) {
  const key = 'raw:' + text;
  if (cache[key]) {
    return cache[key];
  }

  const url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q='
    + encodeURIComponent(text);
  let lastErr = null;
  for (let attempt = 0; attempt < 5; attempt++) {
    try {
      const res = await fetch(url, { headers: { 'User-Agent': 'AlgoCraftTranslationAudit/1.0' } });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const data = await res.json();
      const translated = normalizeChinese(data[0].map(part => part[0]).join(''));
      cache[key] = translated;
      dirty++;
      saveCache();
      return translated;
    } catch (err) {
      lastErr = err;
      await new Promise(resolve => setTimeout(resolve, 500 * Math.pow(2, attempt)));
    }
  }
  throw lastErr;
}

async function translateProtected(text) {
  const { protectedText, held } = protectInlineCode(text);
  let translated = await translateRaw(protectedText);
  translated = restoreHeld(translated, held);
  return normalizeChinese(translated);
}

function postprocessTitle(id, english, translated) {
  if (titleOverrides.has(String(id))) {
    return titleOverrides.get(String(id));
  }
  const title = normalizeChinese(translated)
    .replace(/^算法题：/, '')
    .replace(/^LeetCode 题名：/, '')
    .replace(/^设计 /, '设计')
    .replace('3总和', '三数之和')
    .replace('4总和', '四数之和')
    .replace('两和', '两数之和')
    .replace('字谜词', '字母异位词')
    .replace('k', 'K')
    .trim();
  if (!/[\u4e00-\u9fff]/.test(title)) {
    return english;
  }
  return title;
}

async function translateTitle(id, english) {
  const translated = await translateRaw(english);
  return postprocessTitle(id, english, translated);
}

async function translateLine(line) {
  if (line.trim() === '') {
    return line;
  }
  const trimmed = line.trim();

  let match = /^## Example\s*(\d*):?/.exec(trimmed);
  if (match) {
    return match[1] ? `## 示例 ${match[1]}：` : '## 示例：';
  }
  if (/^## Constraints:?/.test(trimmed)) {
    return '## 约束条件：';
  }
  if (/^## Follow-up:?/i.test(trimmed)) {
    return '## 进阶：';
  }

  match = /^\*\*Input:\*\*\s*(.*)$/.exec(trimmed);
  if (match) {
    return `**输入：** ${match[1]}`.trimEnd();
  }
  match = /^\*\*Output:\*\*\s*(.*)$/.exec(trimmed);
  if (match) {
    return `**输出：** ${match[1]}`.trimEnd();
  }
  match = /^\*\*Explanation:\*\*\s*(.*)$/.exec(trimmed);
  if (match) {
    const rest = match[1] ? await translateProtected(match[1]) : '';
    return `**解释：** ${rest}`.trimEnd();
  }

  if (/^\*\s+`[^`]+`\s*(<=|>=|<|>|=|==|!=)/.test(trimmed)
      || /^\*\s+[-0-9`\[\]a-zA-Z_., <=>&|^+*/%()]+$/.test(trimmed)) {
    return trimmed;
  }

  if (/^\*\s+\*\*Only one valid answer exists\.\*\*/.test(trimmed)) {
    return '*   **只存在一个有效答案。**';
  }

  return translateProtected(line);
}

async function translateDescription(problem, zhTitle) {
  const lines = problem.description.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
  const out = [];
  for (const line of lines) {
    if (/^#\s+/.test(line)) {
      out.push(`# ${zhTitle}`);
    } else {
      out.push(await translateLine(line));
    }
  }
  return normalizeChinese(out.join('\n'))
    .replace(/^## 限制：/gm, '## 约束条件：')
    .replace(/^## 示例 (\d+)：/gm, '## 示例 $1：')
    .replace(/\*\*输入：\*\*\s+/g, '**输入：** ')
    .replace(/\*\*输出：\*\*\s+/g, '**输出：** ')
    .replace(/\*\*解释：\*\*\s+/g, '**解释：** ');
}

(async () => {
  for (let id = 1; id <= 500; id++) {
    const problemPath = path.join(root, `p${id}.json`);
    const translationPath = path.join(outDir, `p${id}.json`);
    const problem = JSON.parse(fs.readFileSync(problemPath, 'utf8'));
    const existingTranslation = fs.existsSync(translationPath)
      ? JSON.parse(fs.readFileSync(translationPath, 'utf8'))
      : {};
    const zhTitle = await translateTitle(id, problem.title);
    const zhDescription = await translateDescription(problem, zhTitle);
    const payload = {
      title: zhTitle,
      description: zhDescription
    };
    if (Array.isArray(existingTranslation.solutions)) {
      payload.solutions = existingTranslation.solutions;
    }
    fs.writeFileSync(translationPath, JSON.stringify(payload, null, 2) + '\n', 'utf8');
    if (id % 25 === 0) {
      console.log(`translated ${id}/500`);
      saveCache(true);
    }
  }
  saveCache(true);
  console.log('translated 500/500');
})().catch(err => {
  saveCache(true);
  console.error(err);
  process.exit(1);
});
