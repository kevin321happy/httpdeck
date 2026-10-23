# HttpDeck — 应用内 HTTP 工作台设计

| 字段 | 值 |
|---|---|
| 状态 | Draft for review |
| 日期 | 2026-10-23 |
| 作者 | kevin321happy |
| License | Apache-2.0 |
| 仓库 | `kevin321happy/httpdeck` |
| Android 包名 | `io.github.kevin321happy.httpdeck` |
| Flutter 包名 | `httpdeck_flutter`（目录 `httpdeck-flutter`） |

## 1. 摘要

HttpDeck 是一个应用内 HTTP 工作台：在 debug 包内捕获经过 OkHttp / Dio 的请求和响应，提供一个漂亮的悬浮面板，用于查看、筛选、分享和重新请求。它同时支持纯 Android、纯 Flutter、Android + Flutter Hybrid，不依赖 root、VPN 或 MITM 证书。

差异点：**Hybrid 项目只开一个面板，同时看原生和 Flutter 的请求**。现有开源工具通常只覆盖单一技术栈。

## 2. 目标与非目标

### 2.1 目标

- 纯 Android 项目只依赖 `httpdeck-android` 即可开箱即用。
- 纯 Flutter 项目只依赖 `httpdeck_flutter` 即可开箱即用，不引入 Android 代码。
- Hybrid 项目同时依赖两者，通过 bridge 把 Flutter 记录送到原生面板。
- 默认零配置：自动挂桌面宠物入口、自动 debug-only、release 自动 no-op。
- 界面达到可对外展示的精致程度，交互流畅（60/120fps）。
- 通过 JitPack / pub.dev 远程依赖，接入成本低于 10 分钟。
- 通过接口预留存储、脱敏、导出、重新请求、主题、入口的扩展点。

### 2.2 非目标

- 不做 Wireshark 类底层 TCP/TLS 抓包。
- 不做 MITM、证书安装、VPNService。
- 不捕获不经过 OkHttp / Dio 的流量（WebView、第三方 SDK 自建网络栈、Flutter 引擎内部非 Dio 请求）。
- 默认 release 不启用、不暴露调试面板；可选 release-enabled 构建必须显式开启，且只用于受控分发。
- 不默认把数据上传到远端。
- v0.1 不做 Mock、WebSocket、请求对比、磁盘持久化。

## 3. 用户与场景

| 用户 | 场景 | 价值 |
|---|---|---|
| Android 开发 | 联调 Retrofit/OkHttp 接口 | 手机内直接看请求/响应，不用连电脑 |
| Flutter 开发 | 联调 Dio 接口 | 不需要 Flutter DevTools/桌面代理 |
| Hybrid 开发 | 原生页面与 Flutter 页面互相跳转 | 一个面板看完整链路 |
| 测试/实施 | 现场复现问题 | 桌面宠物入口即开即用，分享给开发 |

## 4. 命名与产物

| 产物 | 目录 | 远程坐标 | 说明 |
|---|---|---|---|
| Core | `httpdeck-core` | 内部依赖 | Kotlin 模型/存储/脱敏/导出/重新请求策略 |
| Android | `httpdeck-android` | JitPack `com.github.kevin321happy.HttpDeck:httpdeck-android:<tag>`；1.0 起 Maven Central `io.github.kevin321happy:httpdeck-android` | OkHttp + Compose UI + 悬浮入口 |
| Android no-op | `httpdeck-android-noop` | 同上 `-noop` | release 空实现 |
| Flutter | `httpdeck-flutter` | pub.dev `httpdeck_flutter` | Dio + Material3 UI + Overlay |
| Samples | `samples/android-demo`、`samples/flutter-demo`、`samples/hybrid-demo` | 不发布 | 接入验证 |

Dart/pub 包名不允许连字符，因此目录叫 `httpdeck-flutter`，pub 包名必须是 `httpdeck_flutter`。

## 5. 架构

```text
┌───────────────────────────────┐
│        httpdeck-core          │
│ model / store / redact /      │
│ export / replay policy        │
└──────────────┬────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
┌────▼─────────┐   ┌─────▼────────────┐
│httpdeck-     │   │httpdeck-flutter  │
│android       │   │(Dart, standalone)│
│OkHttp + UI   │   │Dio + UI          │
└────┬─────────┘   └─────┬────────────┘
     │  MethodChannel    │
     │  dev.httpdeck/    │
     │  bridge           │
     └─────────┬─────────┘
               │
        Hybrid: 原生面板统一展示
```

- 纯 Android：`httpdeck-core` + `httpdeck-android`。
- 纯 Flutter：只有 `httpdeck_flutter`；Dart 侧有独立的 store/UI，不依赖 Android。
- Hybrid：两者都依赖；Flutter 侧 `bridge: true`，记录通过 channel 进入原生 store，原生 UI 统一展示。
- 两端共用**记录 JSON 协议**和**设计 token**，不共享代码。
- bridge 协议带 `version`，后续可兼容演进。

## 6. 对外 API

### 6.1 Android

```kotlin
debugImplementation("com.github.kevin321happy.HttpDeck:httpdeck-android:0.1.0")
releaseImplementation("com.github.kevin321happy.HttpDeck:httpdeck-android-noop:0.1.0")

HttpDeck.install(
    application = application,
    clientBuilder = okHttpClientBuilder,
    config = HttpDeckConfig(
        enabled = BuildConfig.DEBUG,
        maxRecords = 300,
        maxBodyBytes = 64 * 1024,
        entryPoint = EntryPoint.FloatingBubble,
        theme = HttpDeckTheme.System,
    ),
)
```

- `install` 注册 ActivityLifecycleCallbacks，debug 下自动挂桌面宠物入口。
- 手动打开：`HttpDeck.show(activity)`。
- 只想要拦截器：`HttpDeck.interceptor()` 加到自己的 OkHttpClient。
- 默认 release 使用 `-noop`，所有 API 为空操作；即使误用也不会编译进 UI。
- 仅内部/现场 release 构建可改为依赖 `httpdeck-android` 并设置 `enableInRelease = true`，同时显式关闭 `-noop` 依赖；不要同时依赖两个 artifact。

### 6.2 Flutter

```dart
dependencies:
  httpdeck_flutter: ^0.1.0

void main() {
  final dio = Dio();
  HttpDeckFlutter.install(dio);
  runApp(HttpDeckFlutter.wrap(const App()));
}
```

- `install` 添加 Dio interceptor。
- `wrap` 在 MaterialApp 外层挂 Overlay 桌面宠物。
- 手动打开：`HttpDeckFlutter.show(context)`。
- 纯 Flutter 不依赖任何 Android 代码。
- v0.1 支持 Dio 5.x；Dio 4 适配作为 v0.1.1 候补。

### 6.3 Hybrid

```kotlin
HttpDeck.install(
    application = application,
    clientBuilder = okHttpClientBuilder,
    bridge = true,
)
```

```dart
HttpDeckFlutter.install(dio, bridge: true);
```

- `bridge: true` 时 Flutter 不显示自己的面板，记录转发给原生。
- 原生面板的 `source` 字段区分 `android` / `flutter`。
- 纯 Flutter 使用 `bridge: false`（默认）。

### 6.4 扩展点

| 接口 | 作用 |
|---|---|
| `HttpDeckConfig` | 开关、上限、入口、主题、过滤、body 限制 |
| `RecordStore` | 内存（默认）/ 后续 Room/SQLite |
| `Redactor` | Header/Body/URL 自定义脱敏 |
| `Exporter` | 文本 / cURL（v0.1）；HAR/JSON（v0.2） |
| `ReplayHandler` | 业务签名在 HTTP 客户端之外生成时接管重新请求 |
| `EntryPoint` | 桌面宠物 / 通知 / 自定义 |
| `HttpDeckTheme` | System / Dark / Light / 自定义 token |
| `HttpDeckAdapter` | OkHttp/Dio（v0.1），Ktor/http（v0.2） |

## 7. 捕获与数据模型

### 7.1 生命周期

1. 请求进入 interceptor：记录 method/url/headers/body 快照、起始时间。
2. 响应返回：记录 status/headers/body/耗时；错误记录 error。
3. 组装 `HttpDeckRecord`，写入 `RecordStore`。
4. UI 通过 Flow/LiveData/ValueNotifier 订阅；列表增量更新。
5. 环形缓冲按条数和总字节淘汰；不落盘。

### 7.2 记录协议

```json
{
  "version": 1,
  "id": "01J...",
  "source": "android",
  "method": "POST",
  "url": "https://api.example.com/v1/orders",
  "status": 200,
  "durationMs": 825,
  "timestamp": 1792732525227,
  "requestHeaders": {"content-type": "application/json"},
  "requestBody": "{\"pageNum\":1}",
  "responseHeaders": {"content-type": "application/json"},
  "responseBody": "{\"success\":true}",
  "error": null,
  "replayed": false
}
```

- body 以字符串保存；二进制/超大 body 存占位摘要。
- 单 body 默认上限 64KB；超出截断并标记。
- 默认内存 300 条 / 12MB；超出淘汰最旧。
- 展示时脱敏；原始值只存在内存。

### 7.3 线程与性能

- 拦截器运行在 OkHttp/Dio 工作线程；只做轻量快照，不阻塞主线程。
- JSON pretty/tree 解析在 UI 打开详情时按需进行，LRU 缓存。
- 面板打开目标 <100ms；列表滚动 60fps；不引入模糊/大图。

## 8. 重新请求

按钮文案统一为「重新请求」，不再使用「重放」。

- 默认行为：使用抓包时的 method/url/body；去掉旧 `Authorization/_t/_s/Content-Type` 等，交给同一个 HTTP 客户端重新签名/补 token。
- 不是重新执行业务方法，因此 `traceId/time/pin` 等 body 字段保持抓包时值。
- `ReplayHandler` 允许接入方接管：如果签名在客户端之外生成，可实现自己的重建逻辑。
- 鉴权接口禁止重新请求；写接口二次确认；结果以新记录展示并标记 `replayed=true`。
- v0.1 不做编辑 body；可编辑重新请求列入 v0.2。

## 9. 安全与合规

- 默认仅 debug：Android 使用 `debugImplementation` + `-noop`；Flutter 用 `kDebugMode` 守卫，release 不注册入口。
- 默认不落盘、不上传、不申请系统悬浮窗权限。
- 默认脱敏：`Authorization`、`Cookie`、`Set-Cookie`、`token`、`password`、`phone`、`sign`、`_s` 等；URL query 中的敏感参数同样处理。
- 分享只发脱敏文本；复制原始 cURL 需二次确认并提示可能过期/含鉴权。
- 文档明确：面板只用于调试，不要放进公开分发包；不要把抓包内容发到公开渠道。
- 开源版 clean-room：不包含任何公司内部包名、接口、账号、测试数据或业务字段。

### 9.1 Release 抓包模式（显式开启）

- 默认 `enableInRelease = false`；release 依赖 `-noop`，没有入口、没有拦截器。
- 需要 release 抓包时，仅在内部/现场/受控分发构建中：
  - 依赖真实的 `httpdeck-android`（不是 `-noop`）；
  - 设置 `enableInRelease = true`；
  - Android POC 使用构建参数 `-PhttpdeckReleaseEnabled=true`。
- 开启后必须启用：隐藏入口、`FLAG_SECURE` 防截屏/录屏、内存-only、关闭即清空、默认脱敏、原始 cURL 二次确认。
- **本地隐藏开关不是安全边界**：拿到 APK 的人仍可能解包/开启；正式商店包不要开启。生产环境如需临时支持，应在 v0.2 加服务端设备/账号白名单、TTL、远程 kill。

## 10. UI / UX

### 10.1 设计语言

- 名称：HttpDeck；气质：开发者工作台，不是玩具。
- 深色优先：背景 `#0B0F14`，卡片 `#121821`，主色 `#4CC2FF`，辅助 `#5EEAD4`，警告 `#F5B942`，错误 `#FF6B6B`。
- 浅色主题同 token 映射；支持跟随系统。
- 数据区使用等宽字体；正文使用系统字体，避免额外字体包。
- 圆角 16–20dp，细边框，低对比阴影，不使用大面积 blur。

### 10.2 桌面宠物式入口

入口不是一个常驻展开的按钮，而是可收起的“流量宠物”：

- **收起态**：52dp 流量球，中心显示当前请求数；默认收起，尽量不挡业务 UI。
- **展开态**：156×54dp 胶囊，深色半透明底，左侧流量球 +「调试控制台」+「N 条请求」+ 右侧收起按钮。
- **流量球**：径向渐变（青蓝→蓝）、白色计数、收到新请求时 700ms 脉冲环；请求越多计数越高。
- **交互**：
  - 点收起态流量球 → 展开（220ms Overshoot 动画 + 文本淡入）；
  - 点展开态主体 → 打开面板；
  - 点右侧「−」→ 收起（反向动画）；
  - 拖拽移动，松手后吸附最近屏幕边缘；
  - 轻触震动反馈，展开状态持久化。
- **不申请** `SYSTEM_ALERT_WINDOW`；只挂在当前 Activity / Flutter Overlay。
- Android Compose 与 Flutter 实现同一套状态机和视觉 token；默认收起，长按可临时隐藏，设置里可关闭入口。

### 10.3 面板

- 宽度 92%、高度 80%，圆角 20dp，深色卡片。
- 顶部：标题、数量、分享、清空、关闭。
- 搜索框 + 来源 chip（全部/Android/Flutter）+ 方法筛选 + 状态筛选。
- 列表项：方法徽标、path、状态码、耗时、大小、来源、时间；长按复制/分享。
- 下拉刷新不做（本地数据）；空状态给明确文案。

### 10.4 详情

五个 tab：

1. **Overview**：URL、状态、耗时、大小、来源、时间、错误。
2. **Request**：Header + Body；JSON 可折叠树、语法高亮、节点复制。
3. **Response**：Header + Body；大 body 虚拟滚动。
4. **Headers**：键值表，敏感值默认打码，可点按临时查看。
5. **cURL**：脱敏预览 + 复制原始（二次确认）。

底部动作：分享、复制 cURL、重新请求、关闭。

### 10.5 分享

- 使用系统 `ACTION_SEND` + `createChooser("分享到")`，微信/钉钉/QQ/邮件等已安装应用自动出现。
- 单条分享：完整脱敏请求/响应。
- 列表分享：当前筛选结果摘要（method/url/status/duration/source），最多 50 条。
- 文本上限 20KB，超出截断并提示。
- 不接入微信/钉钉 SDK，不申请额外权限。

### 10.6 动效与无障碍

- 面板 200ms spring 进入；列表 diff 动画；tab 切换 180ms。
- 触控目标 ≥44dp；TalkBack/contentDescription 完整；颜色对比度满足 WCAG AA。
- 支持横屏/折叠屏；面板尺寸按系统真实 displayMetrics 计算。

## 11. 性能预算

| 指标 | 目标 |
|---|---|
| 面板打开 | <100ms |
| 列表滚动 | 60fps（120Hz 设备不丢帧） |
| 单条记录内存 | body ≤64KB |
| 总内存 | ≤20MB（300 条） |
| 主线程 JSON 解析 | 单次 <8ms，必要时后台线程 |
| release 体积增量 | 0（no-op 依赖） |

## 12. 发布与远程依赖

- Android v0.1：JitPack；tag `v0.1.0` 后自动构建 `httpdeck-android` / `-noop`。
- Android 1.0：迁 Maven Central（Sonatype），group `io.github.kevin321happy`。
- Flutter：pub.dev `httpdeck_flutter`；发布前用 Git 依赖验证。
- SemVer；CHANGELOG；GitHub Release；sample 固定版本。
- 发布前检查：无公司代码、无凭据、LICENSE、NOTICE、SECURITY、CONTRIBUTING。

## 13. 测试与 CI

- Android：JUnit 测试 core 的脱敏/环形缓冲/重新请求构造；OkHttp MockWebServer 测拦截器；Compose UI 关键交互测试。
- Flutter：`flutter test` 覆盖 interceptor/store/share text/re-request；widget test 覆盖列表/详情/筛选。
- Hybrid：sample 里用 MockWebServer + MethodChannel 验证双向 bridge。
- CI：Gradle `check`、Flutter `analyze`/`test`、samples 构建；tag 触发 release workflow。
- 手工验收：真机安装三个 sample，验证桌面宠物、列表、详情、分享、重新请求、release 无入口。

## 14. 文档与示例

- `README.md`（英文主）+ `README.zh-CN.md`：三张 GIF、对比表、30 秒接入、截图。
- `docs/quickstart-android.md`、`quickstart-flutter.md`、`quickstart-hybrid.md`。
- `docs/architecture.md`、`docs/security.md`、`docs/faq.md`。
- `CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`、issue/PR 模板。
- 三个 sample 直接可运行。

## 15. v0.1 范围与路线图

### v0.1

- Android OkHttp 捕获 + Compose UI + 桌面宠物入口（收起流量球/展开胶囊）+ 筛选/搜索 + JSON 树 + 分享 + cURL + 重新请求 + 脱敏 + 默认 no-op；可选 release-enabled 构建（显式开关 + 隐藏入口 + FLAG_SECURE）。
- Flutter Dio 5 捕获 + Material3 UI + 同款桌面宠物入口 + 同样功能；可选 release-enabled 构建。
- Hybrid bridge + sample。
- 三个 sample、README、CI、Apache-2.0。
- **不做**：HAR、WebSocket、Mock、对比、磁盘持久化、Ktor/http、远程上传。

### v0.2

- HAR/JSON 文件导出。
- Dio 4 适配、Ktor/http 适配。
- WebSocket 捕获。
- 请求对比、pin、Mock。
- 可选磁盘持久化（加密/自动清理）。

### v1.0

- Maven Central + pub.dev 稳定版。
- 插件 API 冻结、兼容性承诺。
- 主题市场/自定义入口示例。

## 16. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| 公司代码/IP 不能开源 | 项目无法发布 | clean-room 重写；发布前确认授权 |
| 双端 UI 维护成本 | 功能漂移 | 共享协议 + 设计 token + 同一套验收用例 |
| Compose 依赖较重 | 纯 View 项目接入犹豫 | 提供 `httpdeck-core` 无 UI 依赖；文档说明 |
| JitPack/pub 发布失败 | 无法远程依赖 | CI 预演；1.0 迁 Maven Central；pub 发布前 dry-run |
| 同质化竞争 | 难以热门 | 主打 Hybrid 单面板 + 精致 UI + 10 分钟接入 |
| 维护者精力 | issue 堆积 | 明确 v0.1 范围；自动化测试；贡献指南 |
| release 抓包数据泄露 | token/业务数据被截屏或分享 | 默认 no-op；显式 build flag；FLAG_SECURE；内存-only；关闭清空；文档警告 |

## 17. 成功指标

- 接入时间：纯 Android / 纯 Flutter <10 分钟；Hybrid <20 分钟。
- v0.1 发布时：3 个 sample 可运行、CI 绿、默认 release 无入口、release-enabled 构建验证隐藏入口与 FLAG_SECURE、README 有 GIF。
- 90 天目标（目标而非承诺）：GitHub 200+ stars、10+ 外部 issue/PR、至少 3 个真实项目接入反馈。
- 质量：无 P0 崩溃；面板打开 <100ms；列表 60fps。

## 18. 开放问题

无阻塞项。以下为实现阶段验证项：

- JitPack 多模块坐标的最终写法。
- Compose/Kotlin/AGP 版本组合对下游项目的最低要求。
- pub.dev 发布者验证与首发时间。

## 19. 附录

### 19.1 仓库树

```text
httpdeck/
  httpdeck-core/
  httpdeck-android/
  httpdeck-android-noop/
  httpdeck-flutter/
  samples/
    android-demo/
    flutter-demo/
    hybrid-demo/
  docs/
    superpowers/specs/
    quickstart-android.md
    quickstart-flutter.md
    quickstart-hybrid.md
  .github/workflows/
  README.md
  README.zh-CN.md
  LICENSE
```

### 19.2 版本约束（实现阶段以实际验证为准）

- Android：minSdk 23，compileSdk 35，Kotlin 1.9.x，OkHttp 4.12.x，Compose Material3。
- Flutter：Flutter 3.22+ / Dart 3.4+，Dio 5.x，Material 3。
- Java 17 构建。

### 19.3 Clean-room 声明

本设计只复用通用技术思路（HTTP 拦截、内存环形缓冲、脱敏、系统分享、同客户端重新请求），不复制任何公司项目的源码、包名、接口路径、账号信息或业务字段。所有示例使用 `api.example.com` 等中性占位。