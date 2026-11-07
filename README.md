# 烧心琵琶曲

《朋友的酒》的独立附属模组。1.20.1 和 26.1.2 的当前版本为 **0.2.9**，1.21.1 的源码版本为 **0.2.8**。四个 0.2.9 成品见 [Release](https://github.com/jiaotan798/shaoxinpipaqu/releases/tag/v0.2.9)。

## 安装与玩法

客户端和专服都要安装匹配 Minecraft 版本、加载器的附属及[《朋友的酒》0.1.19](https://github.com/jiaotan798/friendswine/releases/tag/v0.1.19)。Fabric 还需安装表中的 Fabric API。前置和可选模组分别安装。

| Minecraft | 加载器 | 附属版本 | 游戏 Java | Fabric API | 构建目录 |
| --- | --- | --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.23 | 0.2.9 | 17 | — | `ports/forge-1.20.1` |
| 1.20.1 | Fabric | 0.2.9 | 17 | 0.92.9+1.20.1 | `ports/fabric-1.20.1` |
| 1.21.1 | NeoForge 21.1.252 | 0.2.8 | 21 | — | `mod` |
| 1.21.1 | Fabric | 0.2.8 | 21 | 0.116.7+1.21.1 | `ports/fabric-1.21.1` |
| 26.1.2 | NeoForge 26.1.2.94 | 0.2.9 | 25 | — | `ports/neoforge-26.1.2` |
| 26.1.2 | Fabric | 0.2.9 | 25 | 0.149.0+26.1.2 | `ports/fabric-26.1.2` |

- 遥控器右键玩偶绑定，左键切换「朋友的酒／琵琶曲」，对空气右键切换停止和三个档位。切歌保留档位。
- 琵琶曲从 50 格内抽取约 20% 的非玩家生物起飞，少量生物保底一只。一二档巡航，三档环绕，结束后安全下降。MJ 与三张表情包在歌曲对应卡点出现。
- 忘情牛肉面提供 180 秒、随身 50 格的攻击保护。野生狗奶提供 60 秒飞行，双击跳跃起飞，跳跃或潜行控制升降。成年傻子村民可兑换两种食物，牛肉面所需的碗从背包额外扣除。

## 构建

将对应的前置 JAR 按 Release 原文件名放在仓库根目录的 `local-deps/`。编译 NeoForge 1.21.1 另需 `ysm-2.6.5-neoforge+mc1.21.1-release.jar` 和 `touhoulittlemaid-1.5.3-neoforge+mc1.21.1.jar`，同样放在该目录。这两个模组是该端口的可选运行兼容依赖。

使用目标目录自带的 Gradle Wrapper。Forge 1.20.1 用 JDK 17，两个 1.21.1 目标用 JDK 21，26.1.2 用 JDK 25。Fabric 1.20.1 用 JDK 21 启动 Gradle，运行开发游戏时还需要 Gradle 能找到 JDK 17。首次构建需联网下载 Gradle 和公开依赖。

```powershell
./ports/forge-1.20.1/gradlew.bat -p ./ports/forge-1.20.1 build
./mod/gradlew.bat -p ./mod build
python ./scripts/verify-build.py --matrix
```

其余目标使用表中目录替换命令路径。成品在相应目录的 `build/libs/`。校验矩阵前先构建六个目标，脚本要求 Python 3.11 或更新版本。重新导出素材时需要 FFmpeg，在 PATH 可用或将 `FFMPEG` 指向其可执行文件，再运行 `python ./scripts/prepare-resources.py`。

验收探针保留在各端口源码中。NeoForge 26.1.2 服务端 GameTest 使用 `-PgameTests`，需要将匹配 0.1.19 的《朋友的酒》源码放入 `local-deps/friendswine-source/` 并先编译其 NeoForge 26.1.2 工程。实际覆盖见[验证记录](VALIDATION.md)。

## 反馈

在 QQ 搜索群号 **873511838**，或扫描下方二维码加入「mc模组问题反馈群」。也可以在本仓库的 **Issues** 提交问题。反馈时请附上模组版本、Minecraft 版本、加载器和复现步骤。

<img src=".github/feedback-qq.png" alt="mc模组问题反馈群二维码，群号 873511838" width="220">

## 作者、许可与素材

作者：jiaotan_。

- jiaotan_：[bilibili](https://space.bilibili.com/409729840?) · [抖音](https://v.douyin.com/KDs5ZxJh5V8/)

项目采用现行模组元数据中的 All Rights Reserved 声明。见[许可状态](LICENSE-STATUS.md)、[第三方素材与依赖](THIRD_PARTY_NOTICES.md)和[版本记录](CHANGELOG.md)。
