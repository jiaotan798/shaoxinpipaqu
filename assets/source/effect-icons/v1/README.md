# 食物状态效果图标设计初稿

日期：2026-11-03。内置imagegen分别生成两枚透明背景设计稿，用户批准后已在0.1.11接入游戏资源。

- forgetfulness-concept.png：忘情，暖金米白面碗、蒸汽与星光，表现安定。
- great-grandma-concept.png：我看见太奶了，冰蓝幽灵、白云与上升标记，表现飞行和整蛊含义。

提示词要求：Minecraft状态效果像素图标，18×18粗逻辑网格，深色描边、少色块、透明底、无文字、无背景边框、缩小可读。第一枚为金边面碗与蒸汽化成平静星光；第二枚为两个深蓝方眼的友好冰蓝幽灵从小白云升起，配短上升标记，不恐怖。工具参数transparent_background=true。

生成原图仍有细微渐变及不统一的像素步长，不是原生18×18成品。`scripts/prepare-resources.py`的`prepare_effect_icons()`现从原图以最近邻导出36×36 RGBA并二值化透明度，完整生成入口同步更新，不会再被旧菱形覆盖。18×18试导出会丢失幽灵双眼，因此最终使用36×36贴图；这不改变原版界面布局。`in-game-scale-preview.png`是最终贴图的放大预览，不是游戏截图；`nearest-preview.png`和`36px-preview.png`保留为导出比较。内部airborne标记不在此次设计范围。游戏内测试范围见[当前验证记录](../../../../VALIDATION.md)。
