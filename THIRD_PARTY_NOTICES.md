# 第三方素材与依赖

此说明区分素材来源、项目声明与第三方许可。2026-11-07，发布委托方确认已取得此次随源码及成品再分发琵琶曲音频、狗奶包装图和表情包所需的授权。

## 忘情牛肉面物品图

- 原作：牛肉麵 Chinese Braised Beef Noodles - whole bowl visible。
- 作者：avlxyz。
- 原作链接：https://www.flickr.com/photos/10559879@N00/371505766/
- 文件与许可记录：https://commons.wikimedia.org/wiki/File:牛肉麵_Chinese_Braised_Beef_Noodles_-_whole_bowl_visible.jpg
- 许可：CC BY-SA 2.0，https://creativecommons.org/licenses/by-sa/2.0/
- 修改：AI去除照片背景、补全局部边缘并保留整碗食物，生成透明PNG；随后缩放至128×128作为游戏物品图。衍生图片沿用CC BY-SA 2.0，项目All Rights Reserved声明不适用于该图片的独立许可。
- 对应文件：`assets/source/food-references/beef-noodles-cutout-ai.png`与`mod/src/main/resources/assets/shaoxinpipaqu/textures/item/forgetful_beef_noodles.png`。

## 音频、狗奶包装图及表情包

- 琵琶曲音频来源为[抖音视频](https://v.douyin.com/Mul29QvZXkc/)，音频文件由发布委托方提供。原件 `assets/source/提取音频.m4a` 的 SHA256 为 `de7e938af75cb3d44820c8056d45387c00a95a14b8f4a02982a7979baccfee3a`。游戏 OGG 另作转换及循环淡化。
- 野生狗奶物品图取自委托方提供的海报画面，AI 提取盒装主体并补绘示意条码。见[来源与处理记录](assets/source/food-references/README.md)。
- 大狗、白菜与肥嘟嘟表情包原件和来源见[卡点图片记录](assets/source/meme-references/README.md)。游戏纹理由原件缩放导出为 RGBA。
- 两枚状态效果图标按本项目提示词生成，从[概念原图](assets/source/effect-icons/v1/README.md)导出 36×36 透明 PNG。

## 独立依赖

前置 friendswine 0.1.19 由玩家独立安装。YSM 2.6.5 和车万女仆 1.5.3 仅用于 NeoForge 1.21.1 的可选兼容。附属成品保留 Forge 1.20.1 构建声明中的 MixinExtras 0.4.1 依赖。 [MixinExtras 上游](https://github.com/LlamaLad7/MixinExtras)

Gradle Wrapper保留上游文件原字节及工具本身的许可证声明，不能因忽略成品JAR而漏掉构建所需Wrapper JAR。
