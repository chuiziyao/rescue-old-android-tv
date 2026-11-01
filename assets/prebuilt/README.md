# 预编译 APK 说明

| 文件 | 包名 | 版本 | minSdk | SHA-256 |
|---|---|---|---|---|
| `WushuangADB.apk` | `com.wushuang.adb` | 1.0 (vc 1) | **16**（Android 4.1+） | `F790CCE59D77E11CD721E558055CDC1F2C091F4E09D4C726DBEF79495C3C3698` |
| `WushuangDesktop.apk` | `com.wushuang.desktop` | 11.0 (vc 11) | **16**（Android 4.1+） | `0494AD46A0E7A0A5686C61FE6BE17FC2B91C0417F22AD8F3AF8ECF17C63C2238` |

- 签名密钥：`assets/signing/debug.keystore`（别名 `androiddebugkey`，口令 `android`，证书 SHA-256 `30C8583C815E818EC74F2C16BC22737CE713ED25532A9D9C500531A08AAD6EE5`）。公开的 debug 密钥，只用于自用，**不要商店分发**。
- `targetSdk` 均为 18；电视 API 低于 16 时用 `scripts/build_assets.ps1 -MinSdk 14` 重编。
- **无双ADB**：需要 root（开 ADB / 静默装包卸载都走 `su`）。装完**必须先打开一次**，否则收不到 `BOOT_COMPLETED`，开机自启/自动 ADB 不生效。推送页 `http://<电视IP>:8555/` 无密码，只在家用局域网临时开。
- **无双桌面**：安装不需要 root；要成为默认 HOME 需要停用原桌面（root）或系统允许 HOME 键弹窗选「始终」。**不要在它作为唯一 HOME 时卸载/覆盖安装**——部分老 ROM（乐视 4.3 实测）会触发 system_server 重启。正确顺序：先装新桌面 → 再停旧桌面 → 重启。
- 源码分别在 `assets/wushuang-adb/`、`assets/wushuang-desktop/`；改完用 `scripts/build_assets.ps1` 重编（默认使用上面这把密钥，保证后续版本可覆盖升级）。
