# 预编译 APK 说明

| 文件 | 包名 | 版本 | minSdk | SHA-256 |
|---|---|---|---|---|
| `ADB-Switch.apk` | `com.dsh.adbhelper` | 1.2 (vc 3) | **16**（Android 4.1+） | `E878AA205317245CED433579D37598B26AC4C33FFDF8663C49EAFB10A1D349B9` |
| `SimpleHome.apk` | `com.dsh.simplehome` | 9.0 (vc 9) | **16**（Android 4.1+） | `AEB512F8E3C5E96C4F3BA6946DFF1D88D46C4AD27D9533F7974F8606B17B87E7` |

- 两者均用 **debug keystore** 签名，仅供个人/自用设备，不要用于任何商店分发。
- `targetSdk` 均为 18；如果电视 API < 16（Android 4.0 及以下），用 `scripts/build_assets.ps1 -MinSdk 14` 重编。
- **ADB-Switch 必须已 root**：它用 `su -c "setprop service.adb.tcp.port 5555; stop adbd; start adbd"` 打开网络 ADB；未 root 的电视只能走厂商工程菜单开 ADB。
- **SimpleHome 安装不需要 root**，但要让系统把它当默认 HOME，通常需要"停用原桌面 + 重启"（要 root）；部分电视允许按 HOME 键后在弹窗里选"始终"，那就无需 root。
- 两者升级时 `versionCode` 递增，可直接覆盖安装（同 debug 签名）。
- 源码分别在 `assets/adb-switch/`、`assets/simple-home/`，改完用 `scripts/build_assets.ps1` 重编。
