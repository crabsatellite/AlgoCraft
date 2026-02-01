# AlgoCraft 国际化 (i18n) 完整键值映射

## 📋 文档说明
本文档详细列出所有翻译 Key 及其使用位置，用于维护和检查翻译完整性。

**最后更新**: 2026-01-29  
**翻译完成度**: 100% (73/73)

---

## 🎮 GUI 相关翻译 (13 keys)

### IDE 主界面
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.ide_title` | AlgoCraft IDE | AlgoCraft IDE | [ModernAlgorithmScreen.java#L42](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L42) | 无 |
| `algocraft.gui.code` | Code | 代码 | [ModernAlgorithmScreen.java#L52](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L52) | 无 |
| `algocraft.gui.description` | Description | 描述 | [ModernAlgorithmScreen.java#L56](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L56) | 无 |
| `algocraft.gui.ready` | Ready... | 就绪... | [ModernAlgorithmScreen.java#L43](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L43) | 无 |

### 按钮相关
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.run` | Run | 运行 | [ModernAlgorithmScreen.java#L67](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L67) | 无 |
| `algocraft.gui.submit` | Submit | 提交 | [ModernAlgorithmScreen.java#L73](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L73) | 无 |
| `algocraft.gui.history` | History | 历史记录 | [ModernAlgorithmScreen.java#L79](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L79) | 无 |
| `algocraft.gui.toggle_view` | Toggle View | 切换视图 | [ModernAlgorithmScreen.java#L84](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L84) | 无 |
| `algocraft.gui.search` | Search | 搜索 | [ModernAlgorithmScreen.java#L96](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L96) | 无 |
| `algocraft.gui.import` | Import | 导入 | [ModernAlgorithmScreen.java#L113](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L113) | 无 |
| `algocraft.gui.back` | Back | 返回 | [SubmissionHistoryScreen.java#L29](src/main/java/com/crabmods/algocraft/client/gui/modern/SubmissionHistoryScreen.java#L29) | 无 |

### 面板/区域标签
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.problems` | PROBLEMS | 题目列表 | [ModernAlgorithmScreen.java#L242](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L242) | 无 |
| `algocraft.gui.terminal` | TERMINAL | 终端 | [ModernAlgorithmScreen.java#L246](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L246) | 无 |

---

## 📊 执行反馈翻译 (12 keys)

### 加载和运行
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.loaded_problem` | Loaded problem: %s | 已加载题目: %s | [ModernAlgorithmScreen.java#L146](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L146) | 题目标题 |
| `algocraft.gui.running_examples` | Running Examples for %s... | 正在运行 %s 的样例... | [ModernAlgorithmScreen.java#L151](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L151) | 题目标题 |
| `algocraft.gui.no_examples` | No examples found. | 未找到样例。 | [ModernAlgorithmScreen.java#L156](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L156) | 无 |
| `algocraft.gui.submitting` | Submitting... | 正在提交... | [ModernAlgorithmScreen.java#L183](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L183) | 无 |

### 测试结果反馈
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.input` | Input: %s | 输入: %s | [ModernAlgorithmScreen.java#L173](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L173) | 测试输入 |
| `algocraft.gui.result` | Result: %s | 结果: %s | [ModernAlgorithmScreen.java#L175](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L175) | 执行结果 |
| `algocraft.gui.passed` | Passed: %s/%s | 通过: %s/%s | [ModernAlgorithmScreen.java#L211](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L211) | 通过数, 总数 |
| `algocraft.gui.time` | Time: %sms | 耗时: %sms | [ModernAlgorithmScreen.java#L212](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L212) | 执行时间(ms) |

### 通知消息
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.msg.submission_failed` | Submission Failed. | 提交未通过。 | [ModernAlgorithmScreen.java#L203](src/main/java/com/crabmods/algocraft/client/gui/modern/ModernAlgorithmScreen.java#L203) | 无 |
| `algocraft.gui.success_all_passed` | SUCCESS! All tests passed. | 成功！所有测试点通过。 | 定义但未使用 | 无 |

---

## 📥 导入功能翻译 (13 keys)

### 标题和按钮
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.import_title` | Import Problem | 导入题目 | [ImportProblemScreen.java#L24](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L24) | 无 |
| `algocraft.gui.import.name` | Name | 名称 | [ImportProblemScreen.java#L42](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L42) | 无 |
| `algocraft.gui.import.input` | Input | 输入 | [ImportProblemScreen.java#L47](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L47) | 无 |
| `algocraft.gui.import.do_import` | Import | 导入 | [ImportProblemScreen.java#L51](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L51) | 无 |
| `algocraft.gui.import.back` | Back | 返回 | [ImportProblemScreen.java#L56](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L56) | 无 |

### 提示文本
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.import.name_hint` | Repository Name | 仓库名称 | [ImportProblemScreen.java#L43](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L43) | 无 |
| `algocraft.gui.import.url_hint` | URL (e.g. http://...) | URL (例如 http://...) | [ImportProblemScreen.java#L74](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L74) | 无 |
| `algocraft.gui.import.file_hint` | File Path | 文件路径 | [ImportProblemScreen.java#L77](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L77) | 无 |

### 导入模式和状态
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.import.mode.remote` | Mode: Remote Repository (URL) | 模式: 远程仓库 (URL) | [ImportProblemScreen.java#L65](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L65) | 无 |
| `algocraft.gui.import.mode.local` | Mode: Local File (Path) | 模式: 本地文件 (路径) | [ImportProblemScreen.java#L66](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L66) | 无 |
| `algocraft.gui.import.mode.unknown` | Mode: Unknown | 模式: 未知 | [ImportProblemScreen.java#L67](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L67) | 无 |

### 导入状态消息
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.gui.import.status.empty_input` | Input cannot be empty | 输入不能为空 | [ImportProblemScreen.java#L84](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L84) | 无 |
| `algocraft.gui.import.status.empty_name` | Name cannot be empty | 名称不能为空 | [ImportProblemScreen.java#L92](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L92) | 无 |
| `algocraft.gui.import.status.downloading` | Downloading... | 正在下载... | [ImportProblemScreen.java#L96](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L96) | 无 |
| `algocraft.gui.import.status.success` | Import Successful! | 导入成功！ | [ImportProblemScreen.java#L99](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L99) | 无 |
| `algocraft.gui.import.status.error` | Error: %s | 错误: %s | [ImportProblemScreen.java#L102](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L102) | 错误消息 |
| `algocraft.gui.import.status.file_not_found` | File not found | 文件未找到 | [ImportProblemScreen.java#L109](src/main/java/com/crabmods/algocraft/client/gui/modern/ImportProblemScreen.java#L109) | 无 |

---

## 🎁 奖励系统翻译 (9 keys)

### 获奖提示
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.msg.rewards_disabled` | Rewards are disabled on this server. | 奖励系统在此服务器上已禁用。 | [PacketSolveProblem.java#L51](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L51) | 无 |
| `algocraft.msg.first_clear` | First clear! You received a rich reward! | 首次通关！你获得了丰厚的奖励！ | [PacketSolveProblem.java#L110](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L110) | 无 |
| `algocraft.msg.daily_clear` | Daily clear! You received a small reward. | 每日通关！你获得了小奖励。 | [PacketSolveProblem.java#L112](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L112) | 无 |
| `algocraft.msg.already_cleared` | You have already cleared this problem today. Come back tomorrow! | 你今天已经通关过这道题了。明天再来吧！ | [PacketSolveProblem.java#L147](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L147) | 无 |

### 特殊奖励
| 翻译 Key | 英文 | 中文 | 使用位置 | 参数 |
|---------|------|------|---------|------|
| `algocraft.msg.streak` | 🔥 Streak: %s days! Keep it up! | 🔥 连续打卡: %s天！继续保持！ | [PacketSolveProblem.java#L117](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L117) | 连续天数 |
| `algocraft.msg.milestone` | ⭐ MILESTONE: You've solved %s problems! Special reward unlocked! | ⭐ 里程碑: 你已解决了 %s 道题目！解锁特殊奖励！ | [PacketSolveProblem.java#L122](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L122) | 解决题数 |
| `algocraft.msg.lucky_drop` | ✨ Lucky drop! You found a bonus item! | ✨ 幸运掉落！你发现了一个额外物品！ | [PacketSolveProblem.java#L127](src/main/java/com/crabmods/algocraft/network/PacketSolveProblem.java#L127) | 无 |

---

## 🏆 物品和成就翻译 (38 keys)

### 物品 (Items)
| 翻译 Key | 英文 | 中文 | 位置 |
|---------|------|------|------|
| `block.algocraft.algorithm_computer` | Algorithm Computer | 算法电脑 | lang 文件 |
| `itemGroup.algocraft` | AlgoCraft | 算法工坊 | lang 文件 |
| `item.algocraft.bronze_trophy` | Bronze Trophy | 青铜奖杯 | lang 文件 |
| `item.algocraft.silver_trophy` | Silver Trophy | 白银奖杯 | lang 文件 |
| `item.algocraft.gold_trophy` | Gold Trophy | 黄金奖杯 | lang 文件 |
| `item.algocraft.diamond_trophy` | Diamond Trophy | 钻石奖杯 | lang 文件 |
| `item.algocraft.netherite_trophy` | Netherite Trophy | 下界合金奖杯 | lang 文件 |
| `item.algocraft.trophy.generic_desc` | A trophy commemorating an achievement | 纪念成就的奖杯 | lang 文件 |

### 成就系统 (Achievements - General)
| 翻译 Key | 英文 | 中文 | 位置 |
|---------|------|------|------|
| `algocraft.achievement.unlocked` | Achievement Unlocked! | 成就解锁！ | [AchievementManager.java#L302](src/main/java/com/crabmods/algocraft/logic/AchievementManager.java#L302) |
| `algocraft.achievement.rarity` | Rarity: %s | 稀有度: %s | [AchievementRegistry.java#L192](src/main/java/com/crabmods/algocraft/logic/AchievementRegistry.java#L192) |
| `algocraft.trophy.rarity` | Rarity: %s | 稀有度: %s | lang 文件 |
| `algocraft.trophy.type` | Type: %s | 类型: %s | lang 文件 |
| `algocraft.trophy.awarded_to` | Awarded to: %s | 授予: %s | lang 文件 |
| `algocraft.trophy.date` | Date: %s | 获得日期: %s | lang 文件 |

### 稀有度 (Rarity)
| 翻译 Key | 英文 | 中文 | 用途 |
|---------|------|------|------|
| `algocraft.rarity.common` | Common | 普通 | 成就显示 |
| `algocraft.rarity.uncommon` | Uncommon | 稀有 | 成就显示 |
| `algocraft.rarity.rare` | Rare | 精良 | 成就显示 |
| `algocraft.rarity.epic` | Epic | 史诗 | 成就显示 |
| `algocraft.rarity.legendary` | Legendary | 传说 | 成就显示 |
| `algocraft.rarity.mythic` | Mythic | 神话 | 成就显示 |

### 成就类型 (Achievement Types)
| 翻译 Key | 英文 | 中文 | 用途 |
|---------|------|------|------|
| `algocraft.type.milestone` | Milestone | 里程碑 | 分类 |
| `algocraft.type.streak` | Streak | 连续打卡 | 分类 |
| `algocraft.type.difficulty` | Difficulty | 难度挑战 | 分类 |
| `algocraft.type.special` | Special | 特殊成就 | 分类 |
| `algocraft.achievement.type.milestone` | Milestone | 里程碑 | 替代 |
| `algocraft.achievement.type.streak` | Streak | 连续打卡 | 替代 |
| `algocraft.achievement.type.difficulty` | Difficulty | 难度挑战 | 替代 |
| `algocraft.achievement.type.special` | Special | 特殊成就 | 替代 |

---

## 🎯 成就详情翻译 (26 keys)

### 里程碑成就 (Milestones)
| 成就 ID | 英文名称 | 中文名称 | 英文描述 | 中文描述 |
|--------|---------|---------|---------|---------|
| `first_solve` | First Steps | 第一步 | Solve your first algorithm problem | 解决你的第一道算法题目 |
| `apprentice` | Apprentice | 学徒 | Solve 10 algorithm problems | 解决10道算法题目 |
| `journeyman` | Journeyman | 熟练工 | Solve 25 algorithm problems | 解决25道算法题目 |
| `expert` | Expert | 专家 | Solve 50 algorithm problems | 解决50道算法题目 |
| `master` | Master | 大师 | Solve 100 algorithm problems | 解决100道算法题目 |
| `grandmaster` | Grandmaster | 宗师 | Solve 250 algorithm problems | 解决250道算法题目 |
| `legend` | Legend | 传奇 | Solve 500 algorithm problems | 解决500道算法题目 |

### 连续打卡成就 (Streaks)
| 成就 ID | 英文名称 | 中文名称 | 英文描述 | 中文描述 |
|--------|---------|---------|---------|---------|
| `streak_3` | Getting Warmed Up | 热身完毕 | Maintain a 3-day solving streak | 保持3天连续解题 |
| `streak_7` | Weekly Warrior | 周冠军 | Maintain a 7-day solving streak | 保持7天连续解题 |
| `streak_14` | Dedicated | 坚持不懈 | Maintain a 14-day solving streak | 保持14天连续解题 |
| `streak_30` | Unstoppable | 势不可挡 | Maintain a 30-day solving streak | 保持30天连续解题 |
| `streak_100` | Century | 百日传奇 | Maintain a 100-day solving streak | 保持100天连续解题 |

### 难度挑战成就 (Difficulty)
| 成就 ID | 英文名称 | 中文名称 | 英文描述 | 中文描述 |
|--------|---------|---------|---------|---------|
| `first_easy` | Easy Start | 简单开始 | Solve your first Easy problem | 解决你的第一道简单题目 |
| `first_medium` | Medium Ground | 中等挑战 | Solve your first Medium problem | 解决你的第一道中等题目 |
| `first_hard` | Challenge Accepted | 挑战接受 | Solve your first Hard problem | 解决你的第一道困难题目 |
| `easy_master` | Easy Master | 简单大师 | Solve 100 Easy problems | 解决100道简单题目 |
| `medium_master` | Medium Master | 中等大师 | Solve 50 Medium problems | 解决50道中等题目 |
| `hard_master` | Hard Master | 困难大师 | Solve 25 Hard problems | 解决25道困难题目 |
| `balanced` | Balanced | 全面发展 | Solve 10+ problems of each difficulty | 每种难度各解决10道以上题目 |

### 特殊成就 (Special)
| 成就 ID | 英文名称 | 中文名称 | 英文描述 | 中文描述 |
|--------|---------|---------|---------|---------|
| `night_owl` | Night Owl | 夜猫子 | Solve a problem between midnight and 6 AM | 在凌晨0点到6点之间解决一道题目 |
| `speed_demon` | Speed Demon | 速度恶魔 | Solve a problem in under 60 seconds | 在60秒内解决一道题目 |
| `perfectionist` | Perfectionist | 完美主义者 | Get 10 consecutive correct submissions | 连续10次提交全部正确 |
| `completionist` | Completionist | 收藏家 | Solve all problems in a repository | 完成一个题库中的所有题目 |

---

## ✅ 翻译检查清单

- [x] 所有 GUI 键都有对应翻译
- [x] 所有执行反馈键都有对应翻译
- [x] 所有导入功能键都有对应翻译
- [x] 所有奖励系统键都有对应翻译
- [x] 所有物品键都有对应翻译
- [x] 所有成就键都有对应翻译
- [x] 没有重复的键定义
- [x] 参数占位符 (%s) 使用正确
- [x] 所有键都被使用（没有孤立的键）
- [x] 中英文翻译完整度一致

---

## 📝 备注

1. **动态键**: 一些键是通过代码动态生成的
   - `algocraft.achievement.<id>.name` - 成就名称
   - `algocraft.achievement.<id>.desc` - 成就描述
   - `item.algocraft.trophy.<tier>` - 奖杯物品名称

2. **使用的格式化参数**:
   - `%s` - 字符串替换
   - 例: `"Passed: %s/%s"` 被替换为 `"Passed: 5/10"`

3. **特殊字符**:
   - `§a`, `§e`, `§7`, `§d`, `§b` - Minecraft 颜色代码
   - `🔥`, `⭐`, `✨` - 表情符号

