# Ender Pearl Log

服务端末影珍珠只读日志模组，用于排查气泡柱珍珠存储、区块卸载与持久化过程中的异常消失。

模组不会阻止珍珠传送、碰撞、死亡清除或区块卸载，也不会修改世界数据。所有记录由独立线程异步写入，避免阻塞服务端 tick。

[源代码](https://github.com/cbtc-59/ender-pearl-log) · [问题反馈](https://github.com/cbtc-59/ender-pearl-log/issues) · [MIT License](LICENSE)

## 功能

- 服务端启动和停止
- 每 100 tick（正常约 5 秒）扫描已加载珍珠及其区块
- 珍珠碰撞对象、方块和命中坐标
- 珍珠永久移除与临时卸载原因
- 区块保存前后珍珠数量
- 玩家上线、断线、死亡及其名下珍珠数量
- 约每分钟一次珍珠数量快照
- 世界中同一 UUID 多颗珍珠的重复持久化异常
- 玩家珍珠集合中同一 UUID 重复注册

## 兼容性

| 项目 | 要求 |
|---|---|
| Minecraft | 26.1 至 26.1.x，不含 26.2 |
| 加载器 | Fabric Loader 0.18.4 或更高 |
| Java | 25 或更高 |
| 前置模组 | Fabric API |
| 安装位置 | 服务端 |

模组是纯服务端模组，客户端不需要安装。

## 安装

1. 从 [Releases](https://github.com/cbtc-59/ender-pearl-log/releases) 页面下载文件名不以 `-sources` 结尾的 JAR。
2. 删除服务器 `mods` 目录中的旧版本。
3. 将 JAR 放入服务器的 `mods` 目录。
4. 正常重启服务器。

启动成功后，服务端控制台应出现：

```text
[EnderPearlLog] 末影珍珠只读日志已加载
```

`-sources.jar` 仅供开发和调试，不要放入 `mods` 目录。

## 日志

日志位置：

```text
logs/ender-pearl.log
```

日志使用 UTF-8 编码并持续追加，目前不会自动轮转。长期运行时应定期检查文件大小，在停服后归档或轮转日志。

日志包含玩家名称、玩家 UUID、珍珠 UUID、维度、坐标和区块等诊断信息。公开分享前应先脱敏。

## 判断方式

- `临时卸载` 不等于丢失，珍珠可能随后从区块加载并重新出现。
- `永久移除` 表示服务端确实删除了实体，后面的 `原因=` 是关键。
- `UNLOADED_WITH_PLAYER` 的珍珠由玩家数据负责保存，`会保存=false` 不能单独作为丢失依据。
- `碰撞` 表示珍珠已经命中方块或实体，之后通常会传送玩家或被丢弃。
- `异常缺失` 表示对象尚未标记为移除，却已不在服务端实体表中，值得优先排查。
- `重复UUID` 表示世界实体表中同时存在相同 UUID 的多颗珍珠。
- `玩家珍珠重复注册` 表示玩家珍珠集合中出现相同 UUID 的不同对象，不等于世界中一定存在多颗珍珠。
- `日志丢弃` 表示异步队列已满，当前日志不完整。

玩家上下线和死亡事件中的珍珠数量会即时扫描世界实体表，不使用周期扫描缓存。

正常停服会等待日志队列排空。服务器被强制终止时，仍在队列中的最后少量事件可能丢失。

## 构建

构建需要 JDK 25 和 Gradle 9.5.1。仓库当前不包含 Gradle Wrapper，请先在本机安装对应版本。

```powershell
$env:JAVA_HOME = '<JDK 25 安装目录>'
gradle build
```

产物位于：

```text
build/libs/ender-pearl-log-fabric-mc26.1.2-v0.2.0.jar
```

首次构建需要联网下载 Minecraft、Fabric Loader、Fabric API 和构建插件。

## 开发测试

启动开发服务端：

```powershell
gradle runServer
```

`build.gradle` 会为 `runServer` 自动添加 `-Dowo.debug=false`，避免 owo 在开发环境中注册正式客户端不存在的调试命令参数类型。

项目已在以下环境完成启动、玩家上下线、死亡、断线重连、区块卸载与重新加载、保存和日志排空测试：

- Minecraft 26.1.2
- Fabric Loader 0.20.1
- Fabric API 0.155.3+26.1.2
- 21 个对照模组

GitHub Actions 会使用 JDK 25 和 Gradle 9.5.1 执行构建，并上传不含源码包的模组 JAR。

## 已知限制

- 日志文件不会自动轮转。
- `玩家珍珠重复注册` 可能因原版定期刷新珍珠区块票据而重复记录，应结合 `重复数量` 和 `重复UUID` 判断。
- 强制终止服务器可能丢失队列末尾的少量事件。

## 许可证

本项目使用 [MIT License](LICENSE)，Copyright (c) 2026 cbtc-59。

## AI 编写声明

本 mod 为纯添加无自然的纯 vibe coding 的 AI 编写 mod，如有不足还望体谅。
