# 预编译 APK 说明

| 文件 | 包名 | 版本 | minSdk | 用途 |
|---|---|---|---|---|
| `ADB-Switch.apk` | `com.dsh.adbhelper` | 1.1 (vc 2) | 18 | 一键开/关网络 ADB（5555），需要 root |
| `SimpleHome.apk` | `com.dsh.simplehome` | 8.0 (vc 8) | 18 | 极简电视桌面（HOME App） |

- 两者均用 **debug keystore** 签名，仅用于个人/自用设备；不要用于任何商店分发。
- 源码分别在 `assets/adb-switch/`、`assets/simple-home/`，改完用 `scripts/build_assets.ps1` 重编。
- ADB-Switch 的原理：`su -c "setprop service.adb.tcp.port 5555; stop adbd; start adbd"`；点"关闭 ADB"则反向执行。
- SimpleHome 的原理：声明 `android.intent.category.HOME`，用 PackageManager 取应用图标/名称，SharedPreferences 保存卡片列表；要成为默认桌面，需要**停用原桌面并重启**（详见 SKILL.md 阶段 4）。
