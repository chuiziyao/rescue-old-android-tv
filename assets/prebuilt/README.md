# 预编译 APK 说明

| 文件 | 包名 | 版本 | minSdk | SHA-256 |
|---|---|---|---|---|
| `ADB-Switch.apk` | `com.dsh.adbhelper` | 1.3 (vc 4) | **16**（Android 4.1+） | `C0C5A3AFD870F810E77F66B8EF22B5008CFFC7CE923E86C17A7DECA35014A98C` |
| `SimpleHome.apk` | `com.dsh.simplehome` | 10.0 (vc 10) | **16**（Android 4.1+） | `9F15DAA141FDE52392AD8BB3AE357535508F06F117C2050C6604C46F3182872D` |

- 签名密钥：`assets/signing/debug.keystore`（别名 `androiddebugkey`，口令 `android`，证书 SHA-256 `30C8583C815E818EC74F2C16BC22737CE713ED25532A9D9C500531A08AAD6EE5`）。这是公开的 debug 密钥，只用于自用，**不要用于商店分发**。
- `targetSdk` 均为 18；电视 API 低于 16（Android 4.0 及以下）时用 `scripts/build_assets.ps1 -MinSdk 14` 重编。
- **ADB-Switch 必须已 root**：它用 `su -c "setprop service.adb.tcp.port 5555; stop adbd; start adbd"` 打开网络 ADB；未 root 的电视只能走厂商工程菜单开 ADB。
- **SimpleHome 安装不需要 root**，但要让系统把它当默认 HOME，通常需要"停用原桌面 + 重启"（要 root）；部分电视允许按 HOME 键后在弹窗里选"始终"，那就无需 root。
- **升级提示**：从"不同签名"的旧版升级会报签名冲突，必须先卸载旧版再装。且**不要在它正作为唯一桌面运行时卸载/覆盖它**——部分老 ROM（实测乐视 4.3）会因此触发 system_server 重启、黑屏、ADB 掉线；较稳的顺序是：原桌面还在时先装好新桌面 → 确认能启动 → 再停用原桌面并重启。
- 已在 Android 4.3 乐视 X3-43 上实测：minSdk 16 构建可安装并正常作为桌面运行；ADB-Switch 1.3 可正常开/关 ADB。
- 源码分别在 `assets/adb-switch/`、`assets/simple-home/`，改完用 `scripts/build_assets.ps1` 重编（默认就用上面这把密钥，保证后续版本可覆盖升级）。
