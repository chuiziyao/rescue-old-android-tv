# ADB 速查表（老安卓电视/盒子）

## 1. 各品牌开 ADB 入口（先试这些）

| 品牌 | 路径 |
|---|---|
| 乐视 / LeTV | 信号源 → 输入 `2580` → 其他选项 → ADB 调试 → 开启 → **重启**（部分精简 ROM 无此项） |
| 小米 / 红米 | 设置 → 账号与安全 → ADB 调试；或 关于 → 产品型号连点 5~7 次 → 账号与安全 → ADB 调试 |
| 创维 / 酷开 | 设置 → 本机信息 → 遥控器依次按 `上上下下左右左右` → 工厂菜单 → ADB 开关（部分机型在"通用设置"） |
| 海信 | 菜单 → 声音设置 → 平衡 → 输入 `0532` → 工厂模式 → ADB；或 设置 → 关于 → 连点版本号 |
| 长虹 | 菜单 → 设置 → 关于电视 → 连点版本号 7 次 → 开发者选项 → USB 调试 |
| 康佳 | 按菜单键 → 输入 `2008` → Factory Menu → ADB |
| TCL / 雷鸟 | 我的应用 → 设置 → 关于本机 → 本机信息 → 按 `上下左右` → ADB 开关 |
| 海尔 | 信源界面连续按 信源键 + 左左右右 |
| 华为智慧屏 | 设置 → 关于本机 → 连点版本号 7 次 → 系统和更新 → 开发人员选项 → USB 调试 + 启用 ADB |
| 索尼 Android TV | 设置 → 关于 → 连点版本号 → 开发者选项 → ADB 调试 |
| 微鲸 | 设置 → 安全 → ADB 调试 |
| 通用 Android TV / Google TV | 设置 → 系统 → 关于 → 连点版本号 7 次 → 开发者选项 → USB/网络调试 |

> 开完必须**重启**，5555 才会监听。PC 验证：`test-netconnection <IP> -Port 5555` 或 `nc -vz <IP> 5555`。

## 2. 连接与探测

```bash
adb connect 192.168.2.163:5555
adb devices -l
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.board.platform
adb shell cat /proc/version
adb shell wm size            # 分辨率（部分老 ROM 不支持）
```

连不上/offline 时：

```bash
adb kill-server && adb start-server
adb disconnect && adb connect <IP>:5555
# 仍不行：确认电视 ADB 开关仍开、电视与 PC 同网段、路由器未开 AP 隔离
```

## 3. 权限受限时的绕过

| 目标 | 命令 |
|---|---|
| 安装 APK | `adb push x.apk /data/local/tmp/ && adb shell su -c "pm install -r /data/local/tmp/x.apk"` |
| 卸载 | `adb shell su -c "pm uninstall com.pkg"` |
| 启动 App | `adb shell su -c "am start -n com.pkg/.MainActivity"` 或 `adb shell su -c "monkey -p com.pkg -c android.intent.category.LAUNCHER 1"` |
| 停用 | `adb shell su -c "pm disable-user --user 0 com.pkg"` |
| 启用 | `adb shell su -c "pm enable com.pkg"` |
| 强制停止 | `adb shell su -c "am force-stop com.pkg"` |
| 清数据 | `adb shell su -c "pm clear com.pkg"` |
| 截图 | `adb shell su -c "screencap -p /sdcard/s.png" && adb pull /sdcard/s.png` |
| 录屏 | 老 ROM 基本不支持 `screenrecord`，别浪费时间 |
| 读取 build.prop | `adb shell su -c "cat /system/build.prop" \| grep -E "version|board"` |
| 当前前台 | `adb shell su -c "dumpsys window windows" \| grep mCurrentFocus` |

## 4. 模拟遥控器/触摸

| 动作 | 命令 |
|---|---|
| HOME | `adb shell su -c "input keyevent 3"` |
| BACK | `input keyevent 4` |
| OK/确认 | `input keyevent 23`（DPAD_CENTER）或 `66`（ENTER，个别 App 只认 ENTER） |
| 方向 | `19` 上 / `20` 下 / `21` 左 / `22` 右 |
| 菜单 | `82` |
| 音量/电源 | `24/25`、`26` |
| 点击坐标 | `input tap X Y` |
| 滑动 | `input swipe X1 Y1 X2 Y2 300` |
| 输入 ASCII | `input text 192.168.2.207`（空格用 `%s`；中文不支持） |
| 输入中文 | 让用户用遥控器/输入法，或装"蓝牙遥控/空鼠"类 App |

> 坐标换算：截图实际分辨率（如 1920×1080）÷ 预览尺寸（如 1708×961）≈ 1.124，预览坐标乘该系数即设备坐标。

## 5. 判定 API 与 ABI

```bash
aapt dump badging app.apk | grep -E "package:|sdkVersion|targetSdkVersion|native-code|application-label"
```

- `sdkVersion` ≤ 电视 API 才能安装；
- `native-code` 需包含 `armeabi-v7a` 或 `armeabi`（老电视都是 32 位；纯 `arm64-v8a` 不可用）；
- 用 `scripts/check_apk.py` 可以一次看全。

## 6. 常用 logcat 过滤

```bash
adb logcat -d -v brief | grep -E "AndroidRuntime|FATAL|ANR"        # 崩溃
adb logcat -d -v brief | grep -E "ActivityManager.*(START|Displayed)"
adb logcat -d -v brief | grep -iE "MediaPlayer|OMX|codec"          # 播放器问题
adb logcat -c                                                       # 清空再看新日志
```

崩溃分析关键词：`NoSuchMethodError`（调用了更高 API）、`NoClassDefFoundError`（缺少类/依赖）、`INSTALL_FAILED_OLDER_SDK`（minSdk 太高）、`INSTALL_FAILED_NO_MATCHING_ABIS`（ABI 不符）。
