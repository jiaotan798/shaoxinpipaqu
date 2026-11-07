# 野生狗奶 AI 抠图记录

- 处理日期：2026-11-01。
- 工具：内置 image_gen 图片编辑，透明背景模式。
- [成品 PNG](wild-dog-milk-cutout-ai.png)：1312×1199，32 位 ARGB，已检查透明通道并目视检查主体。
- 输入：用户本次提供的海报，与本目录 [wild-dog-milk-reference-01.png](wild-dog-milk-reference-01.png) 为同一画面。
- 处理：提取完整盒装主体，去除海报背景、人物及盒外文字；保留蓝白包装、三分之四透视与主要文字；侧面马赛克区域由 AI 重绘为示意条码，不是对被遮内容的真实还原，条码不保证有效。
- AI 输出可能重绘局部纹理和小字，不作为源包装的逐像素复刻。未制作游戏内贴图或模型，原参考图保留。原素材来源与授权状态继续见本目录 README。

## 实际编辑提示词

```text
Use case: background-extraction / precise-object-edit. Edit target: provided poster image. Extract ONLY the blue-and-white 3D milk carton at bottom right as one clean isolated product cutout with TRUE transparent background. Remove the entire poster background, cartoon person, headings, and footer. Keep the carton complete, same three-quarter view showing front, right side and folded top, same proportions, slanted perspective, original blue-white packaging, dog standing on field, milk splash, and readable original Chinese text including 粤西特供, 野生狗奶, 100%野生狗奶, 保质期：永久, 净含量：200毫升. Do not redesign or straighten the carton. Repair ONLY the gray mosaic rectangle at bottom of the right-side panel: replace it with a plausible clean generic black barcode printed on white packaging, matching panel perspective, shading and print style; this is new illustrative detail, not a claimed recovery of obscured content. No gray blur, mosaic, censorship patch, extra objects, poster text, floor, or drop shadow outside object. Crisp natural edges without green fringe. Center carton, fill most of frame with small transparent margin. PNG RGBA transparent asset for game art.
```
