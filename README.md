# 《烧心琵琶曲》

> 《烧心琵琶曲》是《朋友的酒》的附属模组，给玩偶加了一首能让附近生物起飞的曲子。**v0.3.2** 更新游戏内「模组」详情页的介绍，支持《朋友的酒》0.1.19 和 0.2.0。

## 下载与安装

从 [v0.3.2 Release](https://github.com/jiaotan798/shaoxinpipaqu/releases/tag/v0.3.2) 下载与下表匹配的 `.jar`。

| Minecraft | 加载器 | 安装位置 | 前置模组 |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.23 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0 |
| 1.20.1 | Fabric Loader 0.16.10 或更高 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0、Fabric API 0.92.9+1.20.1 或更高 |
| 1.21.1 | NeoForge 21.1.252 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0 |
| 1.21.1 | Fabric Loader 0.16.10 或更高 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0、Fabric API 0.116.7+1.21.1 或更高 |
| 26.1.2 | NeoForge 26.1.2.94 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0 |
| 26.1.2 | Fabric Loader 0.18.4 或更高 | 客户端及服务端 | 《朋友的酒》0.1.19 或 0.2.0、Fabric API 0.149.0+26.1.2 或更高 |

把附属 JAR、同一目标的《朋友的酒》和所需 Fabric API 放进对应实例的 `mods` 目录。联机时两端选同一个前置版本。 [朋友的酒 0.2.0](https://github.com/jiaotan798/friendswine/releases/tag/v0.2.0) · [朋友的酒 0.1.19](https://github.com/jiaotan798/friendswine/releases/tag/v0.1.19) · [安装指南](https://github.com/jiaotan798/shaoxinpipaqu/releases/download/v0.3.2/INSTALL.txt)

## 玩法

- **切歌与切档**。拿遥控器右键绑定玩偶，左键切换《朋友的酒》和《琵琶曲》，对空气右键切换停止和三个档位。
- **看生物飞行**。播放《琵琶曲》时，附近部分生物会飞起来。一、二档巡航，三档绕玩偶飞。歌曲唱到对应段落时会出现 MJ 文字和表情包。
- **兑换食物**。找成年傻子村民兑换忘情牛肉面和野生狗奶。吃面可以暂时阻止附近生物攻击，喝奶后双击跳跃即可起飞。

触发条件和道具效果见[完整玩法说明](docs/完整玩法说明.md)，配置方法见[模组设置](docs/模组设置.md)。各版本变更见[更新记录](CHANGELOG.md)。

## 构建源码

```powershell
.\mod\gradlew.bat -p mod build
```

1.21.1 NeoForge 版的产物在 `mod/build/libs/`。构建环境及其他版本和加载器的步骤见[构建说明](docs/构建说明.md)。

## 反馈

在 QQ 搜索群号 **873511838**，或扫描下方二维码加入「mc模组问题反馈群」。也可以在本仓库的 **[Issues](https://github.com/jiaotan798/shaoxinpipaqu/issues)** 提交问题。反馈时请附上模组版本、Minecraft 版本、加载器和复现步骤。

<img src=".github/feedback-qq.png" alt="mc模组问题反馈群二维码，群号 873511838" width="220">

## 作者、许可与素材

作者：jiaotan_。

- jiaotan_：[bilibili](https://space.bilibili.com/409729840?) · [抖音](https://v.douyin.com/KDs5ZxJh5V8/)

本模组的原创内容保留所有权利（**All Rights Reserved**），具体范围见[许可状态](LICENSE-STATUS.md)。上游模板、依赖和第三方素材适用各自的许可，见[第三方素材与依赖](THIRD_PARTY_NOTICES.md)。
