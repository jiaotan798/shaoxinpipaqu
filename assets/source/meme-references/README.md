# 歌词卡点表情包候选

日期：2026-11-03。三个子代理的网络搜索候选保留如下；用户随后提供并指定了“大狗叫”“胆子肥嘟嘟”和“白菜对我笑”三张附件，原件已归档。0.1.14从原件另存游戏PNG并接入卡点，当前的联机画面覆盖见[验证记录](../../../VALIDATION.md)。

## 已选定的用户附件

| 用途 | 原件 | 内容及对应卡点 |
| --- | --- | --- |
| 胆子肥嘟嘟 | [chubby-courage-original.png](chubby-courage-original.png) | 黄色胖袋鼠，保留底部“胆子真是肥嘟嘟的”文字；对应第22句结尾对白 |
| 大狗叫 | [big-dog-bark-original.jpg](big-dog-bark-original.jpg) | 正面张大嘴露齿的狗；对应第14句大狗卡点 |
| 白菜对我笑 | [smiling-cabbage-original.jpg](smiling-cabbage-original.jpg) | 用户最新提供的无可见抖音号版本，白菜捧腹大笑；对应第17句 |

三张均来自用户本次会话附件，复制后与附件SHA256逐字节一致；文件大小、原文件名和摘要见[原件清单](selected-originals.json)。保留原背景、文字、比例与编码，未裁剪、抠图、缩放或重编码。白菜先前带抖音号的图片已被用户最新附件替代，未调用图像编辑生成处理版。后续若导出游戏纹理，另存派生资源，不覆盖这些原件。用户选用附件不等于已核实第三方再分发许可。

以下网络图片仅保留为历史搜索候选，不替代用户指定的三张图。

## 大狗叫

来源：[新浪“大狗大狗叫叫叫表情包”合集](https://www.sina.cn/news/detail/5292717351633106.html)，账号“抖音表情包”，页面标注2026-04-28。子代理已在浏览器实际查看前两图。

| 编号 | 候选 | 图片 |
| --- | --- | --- |
| D1 | 浅黄色拉布拉多正脸、爪子捂嘴，同梗形象但没有叫的动作 | [JPG](https://wx3.sinaimg.cn/middle/de80a5abgy1icmlap6rrej20dw0dzjrm.jpg) |
| D2 | 同一只拉布拉多张嘴露齿，更适合大狗叫的触发画面 | [JPG](https://wx3.sinaimg.cn/middle/de80a5abgy1icmlap6sv0j20bg0bgt8u.jpg) |

这是静态候选，不宣称已找到原歌词中确切的叫／嚼动作GIF。直链抓取曾返回403，来源页能显示，可能有防盗链。另有[B站《大狗嚼表情包出处找到了》](https://www.bilibili.com/video/BV1LnTF65EFc/)线索，仅搜索标题可核实，视频页412未观看，不作为已确认原出处。

## 白菜对我笑

来源：[3DM《白菜对我笑表情包图片大全》](https://shouyou.3dmgame.com/gl/611974.html)，页面标注2026-02-26。子代理通过图片搜索预览及来源页确认候选；部分CDN原图抓取返回Cache miss，未检查透明通道。

| 编号 | 候选 | 图片 |
| --- | --- | --- |
| C1 | 红底拟人白菜捧腹大笑，带“白菜对我笑”文字 | [PNG](https://syimg.3dmgame.com/uploadimg/upload/image/20260226/20260226135511_48843.png) |
| C2 | 实物白菜贴眯眼笑脸，主体简洁 | [PNG](https://syimg.3dmgame.com/uploadimg/upload/image/20260226/20260226135511_55080.png) |
| C3 | 菜田中多颗白菜带夸张笑脸 | [PNG](https://syimg.3dmgame.com/uploadimg/upload/image/20260226/20260226135511_96447.png) |

备用：[游侠同梗图集](https://m.ali213.net/news/gl2602/1748601.html)。

## 你的胆子真是肥嘟嘟的

来源：[肥嘟嘟图片集合](https://feidudu.org/)。子代理已目视确认两张；网站包含币圈链接，仅记录图片，不将该站当成原作者或授权方。

| 编号 | 候选 | 图片 |
| --- | --- | --- |
| F1 | 白底黄色胖袋鼠，站姿双手搭肚子，文字“胆子真是肥嘟嘟的”；512×512，缺少句首“你的”，常规梗形象匹配 | [PNG](https://feidudu.org/stickers/01_belly.png) |
| F2 | 黄色袋鼠坐椅子指向观众，完整文案“你的胆子真是肥嘟嘟的”；960×1280，有明显Binance／BTC元素，列为备选 | [JPG](https://feidudu.org/stickers/20_boss.jpg) |

[3DM相关梗介绍](https://shouyou.3dmgame.com/gl/633119.html)另有外卖对话来源说法，但不能据此确认上述图片的原作者。

## 素材状态

3DM 的来源记录未注明作者。此次发布依据见[第三方素材说明](../../../THIRD_PARTY_NOTICES.md)。网络候选未下载。选定原件按原字节保存，`scripts/prepare-resources.py` 导出 RGBA PNG 至游戏 `textures/meme/`，大狗为 396×388、白菜为 379×512、肥嘟嘟为 512×512。按比例限制最长边为 512，保留背景和文字。原件摘要继续校验。
