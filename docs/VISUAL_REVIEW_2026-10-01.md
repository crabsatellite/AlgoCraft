# AlgoCraft 模型与界面视觉审核（2026-10-01）

本轮将电脑、奖杯和游戏内／Web IDE 统一成深色工作站风格，改善辨识度、排版和窄屏操作。题库内容、判题、奖励和解题流程沿用此前审核结果。

本页保留视觉审核的历史封存结果。当前交付版本包含这些资产与界面改版，并完成服务器题库改造；请使用[服务器题库交付记录](SERVER_BANKS_2026-10-01.md)中的最新 JAR 与收据。

## 改动

- **电脑**：宽屏显示器、细支架、独立键帽、鼠标与桌垫、机箱通风口和绿色指示灯；屏幕显示文件树、代码行与通过状态。模型仍使用原版材质，包含 54 个互不穿插的部件。
- **奖杯与底座**：5 个等级和 23 个成就变体改为杯体、双侧把手与黑石底座；白银底座铆钉、黄金顶盖、钻石宝石、下界合金冠饰提供逐级区别。成就标识、物品 ID 与获奖数据保留。
- **游戏内 IDE**：统一按钮、面板、选中状态、难度标识和语法颜色；提交操作用绿色突出；工具栏随可用宽度换行；代码横向滚动和行号分别裁切；题目正文按标题与段落排版；导入与历史界面采用相同风格。
- **Web IDE**：统一品牌、配色、题目列表、图片说明和 Monaco 编辑器主题；正文用安全的文本节点渲染标题、列表、代码与强调；手机界面为题目、编辑器和控制台提供独立滚动区域。

此项目没有独立注册的讲台／领奖台方块，本轮对应改动为电脑的支架、桌垫和奖杯展示底座。没有新增方块或玩法。电脑碰撞／选中包围盒随新模型调整，属于可感知的几何变化；交互回调、配方、奖励和判题逻辑未修改。

资产可用 `python scripts/generate_computer_model.py` 与 `python scripts/generate_trophy_models.py` 重建；成就标识输入保存在 `scripts/trophy_variant_marks.json`。

## 验证

<!-- visual-verification:start -->
状态：**ready-for-human-test**，正式构建与全部规定门禁通过。

| 检查 | 封存结果 |
| --- | --- |
| 正式构建 | `mod-build.ps1 build` 成功；稳定发布与所有受管文件校验通过，框架 0.1.14 |
| 常规 JVM 回归 | 983 项通过，失败、错误、跳过均为 0 |
| 官方库契约与质量 | 67 项仓库契约、6 项质量检查通过 |
| 全量参考解答 | 500 题、2,171 个参考解答，生产 Judge 与适配器通道各全部通过 |
| GameTest 服务器 | 22／22 项通过；单次服务器批次 |
| 隐藏客户端验收 | 4,711 项检查，中英文共 1,000 个页面；4 张 IDE 截图，另有 2 张原生模型截图 |
| 隐藏客户端压力测试 | 10 轮、6,107 项检查；p1／p7／p73／p95／p500 实际运行、提交与草稿交互；8 张 IDE 截图，另有 2 张模型截图 |
| 桌面隔离 | 验收与压力会话分别连续验证 1,237／1,291 ticks，违例均为 0；每会话 1 进程、1 世界、1 正常退出；场景与清理失败均为 0 |
| 无头 Web | 48 项检查、1,000 个题面渲染、24 张截图；320／360／640／900／1024／1440 像素宽度无页面或按钮溢出，无 JS 错误 |
| 产物绑定 | 125 个源文件无漂移；158 个类、50 个构建资源与 JAR 字节一致；1,191 个题库文件大小与 SHA-256 核对通过 |
| 配置恢复 | 原有服务器与两套客户端的 options、FML、模组配置均恢复验证通过 |

JVM 与题库门禁合计 5,398 项，失败、错误、跳过均为 0。窄屏与高缩放下，正文和图片通过面板滚动访问；这次没有改变游戏的判题和发奖规则。

视觉审核历史 JAR：[algocraft-1.0.0.jar](../build/review/visual-refresh-2026-10-01/algocraft-1.0.0.jar)。SHA-256：

```text
e1f414cfb688e9c31599d370c82b619bf185dd9bee968b187058b3d6b4cac774
```

证据：[最终收据](../build/review/visual-refresh-2026-10-01/final-receipt.json)、[正式构建日志](../build/review/visual-refresh-2026-10-01/build-final.log)、[客户端验收日志](../build/review/visual-refresh-2026-10-01/ideAcceptanceTest.log)、[压力测试日志](../build/review/visual-refresh-2026-10-01/ideStressTest.log)、[客户端收据](../build/review/visual-refresh-2026-10-01/client/result.json)、[压力收据](../build/review/visual-refresh-2026-10-01/stress/result.json)、[Web 收据](../build/review/visual-refresh-2026-10-01/web/result.json)。封存目录中的早期 `build.log` 是被中断的诊断记录，不作为通过证据。
<!-- visual-verification:end -->

所有自动化测试在后台进行。浏览器使用 headless 模式；Minecraft 在创建窗口前关闭早期启动窗，使用隐藏、无焦点、静音、输入与剪贴板隔离的适配器。客户端每次仅启动一个进程、加载一个临时世界，结束后恢复原有配置；桌面隔离收据随结果保留。

## 实际截图

以下为实际 Minecraft 与浏览器渲染，模型预览脚本的近似图不作为真实游戏验收证据。

![游戏内电脑](../build/review/visual-refresh-2026-10-01/client/model-computer-world.png)

![物品栏模型，首行为等级基础款，次行为代表性成就变体](../build/review/visual-refresh-2026-10-01/client/model-items-gui.png)

![游戏内 IDE](../build/review/visual-refresh-2026-10-01/client/ide-wide.png)

![640×480、GUI scale 2](../build/review/visual-refresh-2026-10-01/client/ide-scaled.png)

![Web IDE](../build/review/visual-refresh-2026-10-01/web/web-1440.png)

![手机 Web IDE](../build/review/visual-refresh-2026-10-01/web/web-360.png)

## 交付范围

可以交给团队进行实际整合包手测。自动化已覆盖开发环境中的模型渲染、题目页面、运行／提交、草稿、导入和历史记录；最终 JAR 的类和资源已与受测构建逐字节核对。

仍需人在实际整合包检查资源包／字体兼容、个人缩放偏好、两客户端联机领奖与重连，以及长期奖励平衡和趣味性。此前的[题目首读审核](../question_bank/FIRST_READER_REVIEW_2026-09-30.md)与[生存玩法审核](GAMEPLAY_REVIEW_2026-10-01.md)继续保留各自的结论和历史产物。此轮没有提交、推送或发布。
