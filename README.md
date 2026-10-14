# tono-app

Tono 客户端：Android + iOS，Kotlin Multiplatform + Compose Multiplatform。产品定义见 `~/TonoChatProjects/product-design.md`。

## 模块

| 模块 | 职责 |
|---|---|
| `shared` | 全部业务代码：根 `App()`、导航、登录面板、设置页、订阅页、`ChatHost` 实现、API 层、主题 |
| `androidApp` | Android 壳：`TonoApplication`（装 ChatHost）、`MainActivity`（初始化 loginbase）、图标与签名 |
| `iosApp` | Xcode 工程，`ContentView` 挂 `MainViewController()` |

共享库：chat 由 `wang.harlon:chatbase-kmp` 提供整页 UI 与协议，身份由 `wang.harlon:loginbase-kmp` 提供。本仓只做壳层：导航、设置、账户、付费。

## 服务端

`https://api.tonote.app`（仓库 `HarlonWang/tono-api`）：`/auth/*` loginbase、`/api/chat/*` 与 `/api/quota` chatbase、`/api/me`、`/api/billing/*`（Paddle）、`/api/app-config`。

## 构建

- Android：`./gradlew :androidApp:assembleGithubDebug`。渠道 flavor：github / play / fdroid，仅 `BuildConfig.CHANNEL` 不同。
- iOS：Xcode 打开 `iosApp/iosApp.xcodeproj`，build phase 会调 `:shared:embedAndSignAppleFrameworkForXcode`。
- 版本号从 git tag 推导（`MAJOR.MINOR.PATCH` → versionCode `MAJOR*10000 + MINOR*100 + PATCH`），正式包必须在 tag 上构建。
- 共享库双轨：CI 只认 `gradle/libs.versions.toml` 的 Maven 版本；本机在 `local.properties` 写 `chatbase-kmp.dir=` / `loginbase-kmp.dir=` 即走 composite build 吃本地源码。改了库必须发版并 bump toml。

## 发布

**打 tag 前必须跑 `scripts/release-smoke.sh` 并看到 PASS。** 它先查线上服务端（模型目录 default 契约、订阅价格、app-config）与官网外链，再构建 play 渠道 release 包（R8 minify 与线上一致）装到设备上，验证启动、聊天输入框、设置页不崩。debug 包不混淆，R8 裁剪类问题只有 release 包能暴露。不发聊天消息、不耗额度。

推 tag 触发 `.github/workflows/release.yml`：构建 github 渠道 APK 与 play 渠道 AAB，建 GitHub Release。需要 secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`（旧 Tono 的签名密钥，Play 更新必须同一把）。

### 版本更新说明（whatsnew）

App 升级后首启弹一次「新版本更新说明」，内容来自 `shared/src/commonMain/composeResources/files/whatsnew.json`（随包打进去）。**只走手写，不用 AI 自动生成**：发版前把本版内容写进去，`version` 必须等于 tag。同一份内容还拼成 GitHub Release 正文，并生成 Play 本版发版说明 `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`（仓库里已有该文件则沿用）。

CI 的 Check What's New 步骤校验 `version` 与 tag 一致且有内容：正式版不符直接失败，预发布只警告（本版不弹窗）。首次安装不弹；内置文件版本与 App 版本不一致（dev 构建、占位文件）也不弹。写法见 `CLAUDE.md`「写更新说明」。
