# tono-app

先读 README.md。

- Android 包名 `com.tono.android` 与签名密钥沿用旧 Tono，versionCode 由 tag 推导且必须 > 12，不要手写版本号。
- 订阅页与额度说明的文案以服务端 `/api/app-config` 为准（`tono-api/src/paywall.ts`），本地 strings 只是兜底；客户端任何文案不得出现费率或每日额度数字。
- chat 的 UI 与协议全在 chatbase-kmp，宿主只实现 `ChatHost`（`shared/.../chat/TonoChatHost.kt`）。要改 chat 行为先改库、发版、bump `libs.versions.toml`。
- 刷新 token 只能走 `AuthManager.refreshAccessToken`，不要自己请求 `/auth/refresh`（并发刷新会烧掉服务端救活配额）。
- 加载指示统一用 M3 Expressive 的 `LoadingIndicator`，不用 `CircularProgressIndicator`。
- 打 tag 前必须跑 `scripts/release-smoke.sh` 并看到 PASS，FAIL 时禁止发版。

## 写更新说明

每次发正式版都要写，CI 不自动生成，没写或 `version` 不等于 tag 时发版直接失败：

1. 确定本次 tag `<版本>`，取上个正式版到 HEAD 的 commit：`git log "$(git tag --sort=-v:refname | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | grep -v '^<版本>$' | head -n 1)"..HEAD --no-merges --oneline`
2. 按面向普通用户的口径起草中英双语：滤掉构建、CI、依赖、重构、测试、文档，以及埋点、监控、日志等用户无感的改动；每条一句话，中文 3–6 条，英文对应翻译。交用户逐条定稿。
3. 写入 `shared/src/commonMain/composeResources/files/whatsnew.json`：`{"version": "<版本>", "zh": [...], "en": [...]}`，`version` 必须等于 tag。
4. 要单独定 Play 发版说明时，同一个 commit 里写 `fastlane/metadata/android/{zh-CN,en-US}/changelogs/<versionCode>.txt`（每条前加 `• `，单文件 ≤ 500 字符，versionCode 规则见 `androidApp/build.gradle.kts`）；不写则 CI 从 whatsnew.json 生成。
5. commit 后再打 tag，让 tag 指向含本版内容的 commit。
