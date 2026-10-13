# tono-app

先读 README.md。

- Android 包名 `com.tono.android` 与签名密钥沿用旧 Tono，versionCode 由 tag 推导且必须 > 12，不要手写版本号。
- 订阅页与额度说明的文案以服务端 `/api/app-config` 为准（`tono-api/src/paywall.ts`），本地 strings 只是兜底；客户端任何文案不得出现费率或每日额度数字。
- chat 的 UI 与协议全在 chatbase-kmp，宿主只实现 `ChatHost`（`shared/.../chat/TonoChatHost.kt`）。要改 chat 行为先改库、发版、bump `libs.versions.toml`。
- 刷新 token 只能走 `AuthManager.refreshAccessToken`，不要自己请求 `/auth/refresh`（并发刷新会烧掉服务端救活配额）。
- 加载指示统一用 M3 Expressive 的 `LoadingIndicator`，不用 `CircularProgressIndicator`。
- 打 tag 前必须跑 `scripts/release-smoke.sh` 并看到 PASS，FAIL 时禁止发版。
