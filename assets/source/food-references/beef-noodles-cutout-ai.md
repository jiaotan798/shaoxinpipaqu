# 牛肉面 AI 抠图记录

- 日期：2026-11-01；使用内置 image_gen 编辑，透明背景模式。
- 输入：[牛肉面原图](beef-noodles-reference.jpg)。
- 输出：[透明 PNG](beef-noodles-cutout-ai.png)，1448×1086，32 位 ARGB；已检查透明通道并目视检查。
- 保留碗和面、牛肉、香菜及酱汁，去除桌面和右上方其他餐具。AI 对源图边缘被裁切的碗形做少量补全，可能重绘食物细节，不是逐像素原图抠取。未制作游戏贴图。
- 原作者 avlxyz，原作 CC BY-SA 2.0；本衍生图片沿用 CC BY-SA 2.0，署名、原作及许可链接见[来源记录](README.md)。

## 实际提示词

```text
Use case: background-extraction. Edit target: the provided beef noodle bowl photograph. Extract ONLY the entire white ceramic bowl and ALL of its contents (noodles, beef, cilantro and sauce). Remove all table/background and the sliver of the other dish at upper right. Output a real transparent-background RGBA PNG, not a checkerboard painted behind the bowl. Preserve original photograph appearance, camera angle, food arrangement, colors, textures and white bowl. The source bowl touches the frame: keep all visible bowl and minimally complete any cropped outer rim or bottom so the cutout has a natural continuous complete bowl silhouette, with a small transparent margin on every side. Do not redesign the dish or add utensils, steam, ingredients, decorations, text, external shadows or watermarks. Clean natural antialiased edges, no table-color fringe, no detached speckles. Center the bowl and fill most of image.
```
