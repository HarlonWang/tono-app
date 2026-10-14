#!/usr/bin/env bash
# 发布前冒烟：先查线上服务端与官网（客户端写死的接口和外链），再构建 play 渠道 release 包（R8 minify 与线上一致），
# 装设备验证启动、聊天输入框渲染、进设置页不崩。打 tag 前必须跑一次并看到 PASS。用法：scripts/release-smoke.sh
# 不发任何聊天消息，不消耗额度。
# 设备：ANDROID_SERIAL 指定的那台；否则唯一在线的设备；都没有时启动 SMOKE_AVD，
# 未设 SMOKE_AVD 且本机只有一个 AVD 就启动它。多台在线或多个 AVD 时不猜，报错列出供选。
# 本机无正式签名时 release 回落 debug 签名（见 androidApp/build.gradle.kts），只用于冒烟，不能上传。
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="com.tono.android"
APK="androidApp/build/outputs/apk/play/release/androidApp-play-release.apk"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$SDK/platform-tools:$PATH"
API="https://api.tonote.app"
SITE="https://tonote.app"

fail() { echo "FAIL: $*"; exit 1; }

echo "==> 线上服务端"
[ "$(curl -s -m 15 "$API/health")" = '{"status":"ok"}' ] || fail "$API/health 不是 ok"
echo "  ok: /health"

# 契约：default 必须在目录里且对免费档开放，否则免费用户每条消息都失败（chatbase 0.2.0 之前踩过）
curl -s -m 30 "$API/api/chat/models" | python3 -c '
import sys, json
d = json.load(sys.stdin)
default, models = d["default"], d["models"]
entry = next((m for m in models if m["id"] == default), None)
if entry is None: sys.exit(f"default {default} 不在模型目录里")
if entry["minTier"] != "user": sys.exit(f"default {default} 不是免费档模型")
print(f"  ok: /api/chat/models（default={default}，共 {len(models)} 个模型）")
' || fail "模型目录不符合契约"

curl -s -m 15 "$API/api/billing/prices" | python3 -c '
import sys, json
d = json.load(sys.stdin)
annual, monthly = d.get("annual", {}).get("formatted"), d.get("monthly", {}).get("formatted")
if not (annual and monthly): sys.exit("年付或月付价格缺失")
print(f"  ok: /api/billing/prices（{annual} / {monthly}）")
' || fail "订阅价格不可用，订阅页会显示不出价格"

curl -s -m 15 "$API/api/app-config" | python3 -c '
import sys, json
d = json.load(sys.stdin)
missing = {"pro_paywall", "quota_help"} - d.keys()
if missing: sys.exit(f"缺少 {sorted(missing)}")
print("  ok: /api/app-config")
' || fail "app-config 不完整"

echo "==> 官网外链（客户端设置页、生成图举报，Play 后台）"
for p in privacy terms report account/delete; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -m 15 "$SITE/$p")
  [ "$code" = 200 ] || fail "$SITE/$p 返回 $code"
  echo "  ok: /$p"
done

echo "==> 构建 play release"
./gradlew :androidApp:assemblePlayRelease -q
[ -f "$APK" ] || fail "未找到产物 $APK"

online() { adb devices | awk 'NR>1 && $2=="device" {print $1}'; }

echo "==> 选择设备"
serial="${ANDROID_SERIAL:-}"
if [ -z "$serial" ]; then
  devices=$(online)
  count=$(printf '%s' "$devices" | grep -c . || true)
  if [ "$count" -gt 1 ]; then
    echo "FAIL: 有 $count 台设备在线，用 ANDROID_SERIAL 指定一台："
    printf '%s\n' "$devices" | sed 's/^/    /'
    exit 1
  elif [ "$count" -eq 1 ]; then
    serial="$devices"
  else
    avd="${SMOKE_AVD:-}"
    if [ -z "$avd" ]; then
      avds=$("$SDK/emulator/emulator" -list-avds 2>/dev/null | grep -v '^INFO' || true)
      if [ "$(printf '%s' "$avds" | grep -c . || true)" -ne 1 ]; then
        echo "FAIL: 没有设备在线，且无法确定启动哪个 AVD；先开一台，或用 SMOKE_AVD 指定："
        printf '%s\n' "${avds:-（本机没有 AVD）}" | sed 's/^/    /'
        exit 1
      fi
      avd="$avds"
    fi
    echo "==> 没有设备在线，启动 $avd"
    "$SDK/emulator/emulator" -avd "$avd" -no-snapshot-save -no-boot-anim >/dev/null 2>&1 &
    for _ in $(seq 1 60); do
      serial=$(online | head -1)
      [ -n "$serial" ] && break
      sleep 3
    done
    [ -n "$serial" ] || fail "$avd 启动超时"
  fi
fi
adb -s "$serial" wait-for-device
adb -s "$serial" shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
echo "    使用设备 ${serial}（$(adb -s "$serial" shell getprop ro.product.model | tr -d '\r')）"

echo "==> 安装（覆盖升级路径；签名不兼容时回退全新安装）"
# release 包的埋点 is_debug=0，冒烟产生的事件会混进线上看板：退出时（含 FAIL）清应用数据，丢掉尚未上报的队列
trap 'adb -s "$serial" shell pm clear "$PKG" >/dev/null 2>&1 || true' EXIT
adb -s "$serial" install -r "$APK" >/dev/null 2>&1 || {
  adb -s "$serial" uninstall "$PKG" >/dev/null 2>&1 || true
  adb -s "$serial" install "$APK" >/dev/null
}

check_alive() {
  stage="$1"
  crash=$(adb -s "$serial" logcat -d -b crash -v brief 2>/dev/null | grep -c "$PKG" || true)
  pid=$(adb -s "$serial" shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)
  if [ "$crash" -gt 0 ] || [ -z "$pid" ]; then
    echo "FAIL: ${stage}未通过（crash 日志行数=${crash}, pid=[${pid}]）"
    adb -s "$serial" logcat -d -b crash -v time | tail -40
    exit 1
  fi
  echo "  ok: ${stage}（pid=${pid}）"
}

has_edit_text() {
  adb -s "$serial" shell uiautomator dump /sdcard/smoke_ui.xml >/dev/null 2>&1 || true
  adb -s "$serial" shell cat /sdcard/smoke_ui.xml 2>/dev/null | grep -q "EditText"
}

echo "==> 启动并观察 10 秒"
adb -s "$serial" logcat -c
adb -s "$serial" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 10
check_alive "启动冒烟"

# 首页就是聊天页：material3 与 CMP 版本脱节时，编译、单测、启动全绿，渲染输入框即 AbstractMethodError
has_edit_text || fail "首页没有输入框——聊天页没渲染出来"
echo "  ok: 聊天输入框已渲染"

echo "==> 进设置页（左上角齿轮）"
size=$(adb -s "$serial" shell wm size 2>/dev/null | sed -n 's/.*: *\([0-9]*\)x\([0-9]*\).*/\1 \2/p')
w=$(echo "$size" | cut -d' ' -f1)
h=$(echo "$size" | cut -d' ' -f2)
[ -n "$w" ] && [ -n "$h" ] || fail "取不到屏幕尺寸，无法定位设置入口"
adb -s "$serial" shell input tap $((w * 7 / 100)) $((h * 93 / 1000))
sleep 4
check_alive "设置页渲染"
# 设置页没有输入框：还看得到 EditText 说明没点中入口，这一步等于没验
has_edit_text && fail "没进到设置页——齿轮坐标可能已失效，这一步等于没验"
adb -s "$serial" shell input keyevent 4
adb -s "$serial" shell rm -f /sdcard/smoke_ui.xml >/dev/null 2>&1 || true

echo "PASS: 线上接口与外链正常，release 包启动、聊天页与设置页均无崩溃。可以打 tag 发布。"
