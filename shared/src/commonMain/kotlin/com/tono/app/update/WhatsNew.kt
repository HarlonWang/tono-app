package com.tono.app.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 随包内置的新版本更新说明（composeResources/files/whatsnew.json）。
 * 仓库里提交的是 version=0.0.0 的占位文件，CI 打 tag 构建前用 AI 生成或本地手写的真实内容覆盖（见 README「发版」）。
 */
@Serializable
data class WhatsNewInfo(
    val version: String = "",
    val zh: List<String> = emptyList(),
    val en: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = zh.isEmpty() && en.isEmpty()
}

private val whatsNewJson = Json { ignoreUnknownKeys = true }

fun parseWhatsNew(raw: String): WhatsNewInfo? =
    runCatching { whatsNewJson.decodeFromString<WhatsNewInfo>(raw) }.getOrNull()

/** 按语言从中英双列表里选一份展示：zh 取 zh、缺失回退 en，其他语言反之。 */
fun pickByLanguage(lang: String, zh: List<String>, en: List<String>): List<String> =
    if (lang.startsWith("zh")) zh.ifEmpty { en } else en.ifEmpty { zh }

/**
 * 升级后首启弹一次。首次安装（lastSeen 为空，调用方静默写入当前版本）与
 * 内置文件版本不匹配（dev 构建 / CI 生成失败的占位文件）都不弹。
 */
fun shouldShowWhatsNew(
    currentVersion: String,
    bundledVersion: String,
    lastSeenVersion: String?,
): Boolean =
    lastSeenVersion != null &&
        lastSeenVersion != currentVersion &&
        bundledVersion == currentVersion
