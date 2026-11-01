# 预编译 APK 说明

| 文件 | 包名 | 版本 | minSdk | SHA-256 |
|---|---|---|---|---|
| `WushuangADB.apk` | `com.wushuang.adb` | 1.3 (vc 4) | **16**（Android 4.1+） | `ECD689C0EC97D2964D8F38C33E0C52BD6BF64A171CCF91841D263A4212728310` |
| `WushuangDesktop.apk` | `com.wushuang.desktop` | 13.0 (vc 13) | **16**（Android 4.1+） | `A4FDDCE9681895DFE660F8A20B1416E99FBCC044C223C4249A07A43905D1E220` |

- 签名密钥：`assets/signing/debug.keystore`（别名 `androiddebugkey`，口令 `android`，证书 SHA-256 `30C8583C815E818EC74F2C16BC22737CE713ED25532A9D9C500531A08AAD6EE5`）。公开的 debug 密钥，只用于自用，**不要商店分发**。
- `targetSdk` 均为 18；电视 API 低于 16 时用 `scripts/build_assets.ps1 -MinSdk 14` 重编。
- **无双ADB 两种模式**：  - 有 root：网页上传后 `su -c pm install` 静默安装、`pm uninstall` 静默卸载；
  - 无 root：网页勾选"无 root 模式"（或自动检测到无 root），APK 推到 `/sdcard/Download/wushuang/`，电视弹出系统安装器，用户点「安装」；卸载走系统卸载确认框。
  - **ADB 按钮仍需要 root**（`setprop` + 重启 adbd 是系统权限）；没有 root 的电视只能用厂商工程菜单开 ADB。
- 无双ADB 装完**必须先打开一次**，否则收不到 `BOOT_COMPLETED`，开机自启/自动 ADB 不生效。推送页 `http://<电视IP>:8555/` 无密码，只在家用局域网临时开。
- **无双桌面**：安装不需要 root；要成为默认 HOME 需要停用原桌面（root）或系统允许 HOME 键弹窗选「始终」。**不要在它作为唯一 HOME 时卸载/覆盖安装**——部分老 ROM（乐视 4.3 实测）会触发 system_server 重启。正确顺序：先装新桌面 → 再停旧桌面 → 重启。
- 源码分别在 `assets/wushuang-adb/`、`assets/wushuang-desktop/`；改完用 `scripts/build_assets.ps1` 重编（默认使用上面这把密钥，保证后续版本可覆盖升级）。
