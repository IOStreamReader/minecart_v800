# Minecart V800 · NeoForge 开发环境

Author: **GPT-6 Astra**. This mod was written entirely by generative artificial intelligence.

License: **unlicensed**.

三个独立的 Gradle 项目分别面向以下版本，每个版本产出自己的 JAR：

| Minecraft | NeoForge（固定版本） | Java |
| --- | --- | --- |
| 1.21.11 | 21.11.45 | 21 |
| 26.1.2 | 26.1.2.114 | 25 |
| 26.2 | 26.2.0.88 | 25 |

全部使用官方 ModDevGradle 2.0.148 和 Gradle Wrapper 9.2.1。
模组名称暂定 `Minecart V800`，ID 为 `minecart_v800`，包名为 `dev.seiriko.minecart`。
已实现普通载人矿车的刹车、蓄力显示、刹车损坏及损坏后强行刹车的惩罚。兼容性声明限定为每个项目对应的 Minecraft 版本。

## 常用命令

在本目录打开终端：

```bash
./dev all build                  # 依次构建全部三个版本
./dev all prepareClientRun prepareServerRun # 准备启动文件及游戏资源
./dev 1.21.11 build              # 仅构建指定版本
./dev 26.1.2 runClient           # 启动开发客户端
./dev 26.2 runClient
./dev 26.2 runServer             # 启动开发服务端
./dev 26.2 runData               # 运行数据生成器
./dev 26.2 tasks                # 查看 Gradle 任务
```

服务端首次运行可能提示 Minecraft EULA，请自行阅读并在同意后修改对应运行目录中的 `eula.txt`。
自动验证：

```bash
./dev all build                                    # 包含刹车状态机边界测试
./dev all runGameTestServer -PbrakeGameTests         # 真实服务端矿车生命周期测试
./dev all build                                    # 测试后重新生成不含测试代码的发布 JAR
```

`-PbrakeGameTests` 仅在开发测试时引入测试类和测试结构，不应带此参数发布。

产物位于 `versions/<版本>/build/libs/`，命名包含 Minecraft 版本，例如：
`minecart_v800-mc26.2-0.1.0.jar`。不同版本分别安装对应 JAR。

## 编辑与调试

使用支持 Java 25 的 IntelliJ IDEA，分别打开或关联 `versions/<版本>/settings.gradle`，选择 Gradle Wrapper。
1.21.11 的 Gradle JVM 选择 JDK 21；26.x 选择 JDK 25。同步后使用 NeoForge 提供的客户端、服务端运行配置调试。
若希望复用本项目已下载的依赖，在 IDE 中将 Gradle user home 指向本目录下的 `.dev/gradle-home`。

每个版本的 Java 入口：`versions/<版本>/src/main/java/dev/seiriko/minecart/MinecartMod.java`。
资源放在各项目的 `src/main/resources`，模组元数据在 `src/main/templates/META-INF/neoforge.mods.toml`。
名称、版本号及依赖版本在各项目的 `gradle.properties`。修改模组 ID 时也要同步修改入口常量和资源命名空间。

共享实现位于 `common/src/main`，状态机边界测试位于 `common/src/test`，游戏测试位于 `common/src/gameTest`。三个版本分别编译共享实现；界面可见性差异由各版本的 `HudVisibility` 适配。

## 刹车规则

- 乘坐普通矿车时按住“后退”（默认 S，支持游戏内改键）刹车。仅界面替换经验条，实际经验和等级不会改变。松开后剩余蓄力继续显示，冷却到零或下车后恢复经验条。
- 32 游戏刻蓄满；松开后每 20 刻下降半条，满条冷却到零需要 40 刻。时间以正常 20 TPS 为准。
- 每刻减速约为当前矿车速度上限的 1/100，充能铁轨不能抵消刹车。持续刹车 32 刻会先损坏，因此约 5 秒停车是刹车力度的标定值，需要间歇使用。
- 首次蓄满播放原版工具损坏音效、掉落 1 个铁锭，并保存实体布尔字段 `brake_broken:true`。损坏不可通过冷却修复。
- 损坏矿车被正常破坏时，掉落矿车在 `minecraft:custom_data` 中保留 `brake_broken:true`，名称显示“刹车损坏的矿车”或“Brake Broken Minecart”。放置（包括发射器放置）后仍然损坏；存档读写也保留状态。
- 已损坏矿车不响应玩家刹车减速；按住后退尝试刹车时立即持续冒黑烟，连续强行刹车 60 刻后播放爆炸音效、产生爆炸粒子并消失，不掉落额外矿车或铁锭。松开后这 60 刻计时清零。
- 矿车消失后，原乘客受到 25 点基础物理伤害（12.5 颗心）；护甲、保护等原版伤害规则仍有效，创造模式保留原版免伤。
- 死亡信息：`%s死于车祸` / `%s died in car accident`（`%s` 替换为玩家名称）。
- 客户端和服务端都需要安装对应版本模组。

## 本机 Java 与网络

`dev` 自动为 1.21.11 选择本机 Java 21，为 26.x 选择项目内的 Temurin Java 25。
也可用 `JAVA21_HOME` / `JAVA25_HOME` 指定自己的 JDK。脚本不会修改系统默认 Java。
本次下载的 JDK 25.0.4.1+1 位于 `.dev/jdks/jdk-25.0.4.1+1/Contents/Home`，下载包已经过 Adoptium 官方 SHA-256 校验。

`.dev` 保存本机工具、缓存和验证日志，已排除版本管理。新机器需要安装 Java 21 和 Java 25，并设置上述变量（macOS 也可自动发现系统安装的 JDK）。
Windows 可在各版本目录使用 `gradlew.bat`，并选择对应 JDK。

此机器通过本地代理下载依赖，代理设置仅保存于 `.dev/gradle-home/gradle.properties`；代理更改后在该文件调整 `systemProp.http(s).proxyHost/proxyPort`。
在其他网络环境中可删除这些代理配置。所有下载保持 HTTPS 证书验证开启。

## 来源

- [NeoForge 官方开发模板](https://github.com/NeoForgeMDKs)
- [1.21.11 模板](https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle)
- [26.1.2 模板](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle)
- [26.2 模板](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle)
- [NeoForge 文档](https://docs.neoforged.net/)
- [Java 25 迁移说明](https://neoforged.net/news/26.1release/)

各项目保留 `TEMPLATE_LICENSE.txt`。模组自身的许可证标识为 `unlicensed`。
