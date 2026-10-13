import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        implementation(projects.shared)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.appcompat)
        implementation(libs.compose.uiToolingPreview)
        debugImplementation(libs.compose.uiTooling)
    }
}

// 版本号从 git tag 推导，apk / play / fdroid 三渠道一致且可复现（F-Droid 从 tag 源码自建，拿不到 CI 变量）。
// CI 注入的 VERSION_NAME（= tag 名）优先；缺失时从 git 读取。providers.exec 对 configuration cache 安全。
fun gitDescribe(vararg extra: String): String? =
    try {
        providers.exec {
            commandLine(listOf("git", "describe", "--tags") + extra.toList())
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim().ifEmpty { null }
    } catch (e: Exception) {
        null
    }

// 「MAJOR.MINOR.PATCH[-后缀]」→ MAJOR*10000 + MINOR*100 + PATCH，首个 tag 1.0.0 → 10000。
// 旧 Tono（待办）最后一版 versionCode 为 12，Play 要求新包严格大于它，0.5.0（versionCode 500）起步即满足；
// 无 tag 时回落 13 只为本地安装能覆盖旧版，正式包必须在 tag 上构建。
fun versionCodeFromTag(tag: String?): Int {
    val trimmed = tag?.trim().takeUnless { it.isNullOrEmpty() } ?: return 13
    val match = Regex("""(\d+)\.(\d+)\.(\d+)(?:[-+].*)?""").matchEntire(trimmed)
        ?: error("非法版本 tag，必须为 MAJOR.MINOR.PATCH[-后缀]，当前 tag=$tag")
    val (major, minor, patch) = match.destructured
    val mi = minor.toInt()
    val pa = patch.toInt()
    require(mi in 0..99 && pa in 0..99) { "versionCode 编码要求 minor/patch ≤ 99，当前 tag=$tag" }
    return major.toInt() * 10000 + mi * 100 + pa
}

val ciVersionName: String? = providers.environmentVariable("VERSION_NAME").orNull?.takeIf { it.isNotBlank() }
val appVersionCode: Int = versionCodeFromTag(ciVersionName ?: gitDescribe("--abbrev=0"))
val appVersionName: String = ciVersionName ?: gitDescribe() ?: "1.0.0-dev"

android {
    namespace = "com.tono.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // 沿用旧 Tono 的包名与签名，在 Google Play 原上架记录上更新
        applicationId = "com.tono.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        manifestPlaceholders["appName"] = "Tono"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // 渠道只影响 BuildConfig.CHANNEL（UA 打标）；applicationId / 版本 / 签名完全一致，可互相覆盖升级
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "CHANNEL", "\"github\"")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "CHANNEL", "\"play\"")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("String", "CHANNEL", "\"fdroid\"")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // F-Droid 的 check apk 不接受 AGP 的 Dependency metadata 签名块；AAB 保留供 Play Console 用
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            // findByName：F-Droid 构建会剥掉 release 签名定义，此时回落 debug 占位签名由 F-Droid 重签
            val releaseConfig = signingConfigs.findByName("release")
            signingConfig = if (releaseConfig?.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appName"] = "Tono (D)"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
