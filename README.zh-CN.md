<div align="right">
  <a href="README.md">English</a> | <a href="README.zh-CN.md">简体中文</a>
</div>

# HttpDeck

> **Android / Flutter 应用内抓包调试台。** 不用 root、不用 VPN、不用装证书，直接抓 OkHttp 和 Dio 请求。
>
> 原生 Android、纯 Flutter、原生 + Flutter 混合项目共用一套抓包面板。

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Maven](https://img.shields.io/badge/Maven-0.1.0--alpha.3-blue)](https://kevin321happy.github.io/httpdeck/maven)
[![Flutter](https://img.shields.io/badge/Flutter-0.1.0--alpha.1-02569B)](docs/quickstart-flutter.md)
[![Android](https://img.shields.io/badge/Android-minSdk%2023-3DDC84)](docs/quickstart-android.md)

![HttpDeck Android 抓包面板](docs/images/android-alpha.png)

## 目录

- [为什么用 HttpDeck](#为什么用-httpdeck)
- [三种接入方式](#三种接入方式)
- [纯原生 Android 接入](#一纯原生-android-接入)
- [纯 Flutter 接入](#二纯-flutter-接入android-宿主)
- [原生 + Flutter 混合接入](#三原生--flutter-混合接入)
- [工作原理](#工作原理)
- [对比](#对比)
- [Release 构建](#release-构建)
- [API](#api)
- [从源码构建](#从源码构建)
- [状态](#状态)
- [安全](#安全)
- [参与](#参与)

## 为什么用 HttpDeck

市面上的应用内抓包工具，大多只支持原生 Android；Flutter 工具又只看 Dio。
混合项目只能装两套工具，截图、分享、字段口径都不统一。

HttpDeck 把两类请求放进**同一个记录仓库、同一个面板**：

- **免 root / 免 VPN / 免证书**：在 App 自己的 HTTP 客户端里抓；
- **原生 + Flutter**：OkHttp 和 Dio 共用列表、详情、分享；
- **默认 debug-only**：正式包默认 no-op，不自动带抓包能力；
- **默认脱敏**：Authorization、Cookie、token、手机号、签名字段展示/分享前脱敏；
- **重新请求**：原生请求走同一个 OkHttp client，重新补 token/签名；
- **不落盘**：v0.1 只在内存环形缓冲里保存。

## 三种接入方式

| 你的项目 | 需要加什么 | 得到什么 |
|---|---|---|
| 纯原生 Android | `httpdeck-android` + `httpdeck-android-noop` | 原生 OkHttp 请求进面板 |
| 纯 Flutter（Android 宿主） | `httpdeck_flutter` + 一段 Android bridge | Flutter Dio 请求进同一个面板 |
| 原生 + Flutter 混合 | 两者都加 | OkHttp + Dio 在一个面板里 |

> v0.1 不支持 iOS；面板是 Android 原生 UI。

## 一、纯原生 Android 接入

### Gradle

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://kevin321happy.github.io/httpdeck/maven' }
    }
}
```

```groovy
// app/build.gradle
dependencies {
    debugImplementation 'io.github.kevin321happy:httpdeck-android:0.1.0-alpha.3'
    releaseImplementation 'io.github.kevin321happy:httpdeck-android-noop:0.1.0-alpha.3'
}
```

### 初始化

```kotlin
val builder = OkHttpClient.Builder()
HttpDeck.install(application, builder)
val client = builder.build()
```

必须在 `builder.build()` 之前调用；Retrofit 使用同一个 `client`。

### 打开面板

```kotlin
HttpDeck.show(activity)
```

建议放在自己的 debug 入口，例如长按版本号。公开 alpha 不会强制挂悬浮球。

完整说明：[Android Quickstart](docs/quickstart-android.md)。

## 二、纯 Flutter 接入（Android 宿主）

Flutter 请求通过 MethodChannel 转发给原生面板；面板仍然是原生 Android UI。

### Flutter 依赖

```yaml
dependencies:
  httpdeck_flutter:
    git:
      url: https://github.com/kevin321happy/httpdeck.git
      ref: v0.1.0-alpha.3
      path: httpdeck-flutter
```

```dart
import 'package:httpdeck_flutter/httpdeck_flutter.dart';

await HttpDeckFlutter.install(dio: dio);
```

debug 默认抓；release 默认关闭。

### Android 宿主 bridge

```kotlin
class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(engine: FlutterEngine) {
        super.configureFlutterEngine(engine)

        HttpDeck.install(application, OkHttpClient.Builder())

        MethodChannel(
            engine.dartExecutor.binaryMessenger,
            "io.github.kevin321happy.httpdeck/network"
        ).setMethodCallHandler { call, result ->
            if (call.method == "debugNetworkEvent") {
                HttpDeck.recordFlutterEvent(
                    call.arguments as? Map<*, *> ?: emptyMap<Any, Any>()
                )
                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }
}
```

纯 Flutter 项目传空 `OkHttpClient.Builder` 即可：只创建共享记录仓库/面板，
不抓原生流量。

打开面板可以加一个隐藏 control channel：

```kotlin
MethodChannel(
    engine.dartExecutor.binaryMessenger,
    "io.github.kevin321happy.httpdeck/control"
).setMethodCallHandler { call, result ->
    if (call.method == "show") {
        HttpDeck.show(this)
        result.success(true)
    } else {
        result.notImplemented()
    }
}
```

```dart
GestureDetector(
  onLongPress: () => const MethodChannel(
    'io.github.kevin321happy.httpdeck/control',
  ).invokeMethod('show'),
  child: const Text('version'),
)
```

完整说明：[Flutter Quickstart](docs/quickstart-flutter.md)。

## 三、原生 + Flutter 混合接入

1. 原生 App 依赖 `httpdeck-android`，给 OkHttp 加拦截器；
2. Flutter 模块依赖 `httpdeck_flutter`，在 Dio 上 `install`；
3. 在现有 Flutter engine 注册**一个**
   `io.github.kevin321happy.httpdeck/network` handler；
4. 用原有原生 debug 入口打开面板。

两个来源会进同一个列表：

```text
source=android  POST /v1/orders        200  120ms
source=flutter  GET  /v1/site/analysis 200   86ms
```

## 工作原理

```text
Android OkHttp ──HttpDeckInterceptor──┐
                                      ├── MemoryRecordStore ── 原生面板
Flutter Dio ──HttpDeckFlutter─────────┘
                 │
                 └── MethodChannel: io.github.kevin321happy.httpdeck/network
```

- `httpdeck-core`：纯 Kotlin/JVM，模型、环形缓冲、脱敏、OkHttp 拦截器、重新请求；
- `httpdeck-android`：原生面板 + `recordFlutterEvent`；
- `httpdeck-flutter`：轻量 Dio 适配，不重复做 UI。

## 对比

| | Chucker | Flipper | Charles | **HttpDeck** |
|---|---|---|---|---|
| 免 root / VPN / 证书 | ✅ | ✅ | ❌ | ✅ |
| 原生 OkHttp | ✅ | ✅ | ✅ | ✅ |
| Flutter Dio | ❌ | 部分 | ✅ | ✅ |
| 混合项目单面板 | ❌ | 部分 | ✅ | ✅ |
| 应用内分享 / cURL | 部分 | ❌ | ❌ | ✅ |
| 原生重新请求 | ✅ | ✅ | ✅ | ✅ |
| HAR / WebSocket / Mock | 部分 | ✅ | ✅ | 路线图 |

HttpDeck 还是 alpha，功能比 Charles/Flipper 少；目标是把移动端**应用内**
抓包、分享、重新请求这条高频链路做快、做好看。

## 捕获范围

- OkHttp：method、URL、状态、耗时、请求/响应 Header、文本 Body；
- Dio：通过 `HttpDeckFlutter` 上报同样字段；
- Body 默认 64KB 截断；二进制、multipart、Stream 只显示占位，不消费业务流；
- 列表只显示 path，详情显示完整 URL。

## Release 构建

默认 release 用 `-noop`，没有入口、没有拦截器。

受控内部包才使用真实 artifact 并显式打开：

```kotlin
HttpDeck.install(
    application = application,
    builder = builder,
    config = HttpDeckConfig(enableInRelease = true)
)
```

release 模式会加 `FLAG_SECURE`。本地隐藏开关**不是安全边界**，公开商店包
不要开启；生产环境需要服务端白名单/kill switch。

## API

| API | 用途 |
|---|---|
| `HttpDeck.install(application, builder, config)` | 安装 OkHttp 拦截器 |
| `HttpDeck.show(activity)` | 打开面板 |
| `HttpDeck.records()` | 当前记录 |
| `HttpDeck.recordFlutterEvent(arguments)` | 接收 Flutter Dio 事件 |
| `HttpDeck.replay(activity, record, callback)` | 原生 GET/POST 重新请求 |
| `HttpDeck.curl(record)` / `shareText(record)` | 脱敏导出 |
| `HttpDeckFlutter.install(dio: dio)` | 安装 Dio 拦截器 |
| `HttpDeckFlutter.captureEnabled` | debug 运行时开关 |

## 从源码构建

```bash
./gradlew :httpdeck-core:test
./gradlew :sample-android:assembleDebug
```

Flutter 包：

```bash
cd httpdeck-flutter
flutter pub get
```

目录：

```text
httpdeck-core/          Kotlin/JVM 抓包内核
httpdeck-android/       Android 面板 + OkHttp 拦截器
httpdeck-android-noop/  Release 空实现
httpdeck-flutter/       Flutter Dio 适配
sample-android/         原生示例
docs/                   接入文档、设计 spec、截图
```

## 状态

`v0.1.0-alpha.3`：

- 原生 OkHttp 抓包、环形缓冲、脱敏、面板、分享、cURL、重新请求；
- Flutter Dio 适配 + 原生 bridge；
- Release no-op + 显式 `FLAG_SECURE`；
- Core 单测。

路线图：宠物/Compose UI、iOS、HAR、WebSocket、Mock/对比、Dio 5。

## 安全

- 默认 debug-only；不上传、不落盘；
- Header/query/JSON 敏感字段展示、分享、cURL 前脱敏；
- 开启 release 抓包前先读 [SECURITY.md](SECURITY.md)。

## 参与

欢迎 issue/PR，先读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## License

Apache-2.0，见 [LICENSE](LICENSE)。

---

<div align="center">
  <a href="#httpdeck">回到顶部</a> · <a href="README.md">English</a>
  <br />
  如果 HttpDeck 帮你节省了时间，点一个 ⭐ 能让更多移动端开发者找到它。
</div>
<!-- footer -->
