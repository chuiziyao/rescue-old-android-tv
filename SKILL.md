---
name: rescue-old-android-tv
description: 拯救老安卓电视（Android 4.x 国产定制电视/盒子，如乐视、小米、创维、TCL 等）：核对真实 Android/API 版本，开启 ADB（含当贝市场远程安装兜底），按 minSdk 找兼容老系统的 App 版本，用手机版 App 绕过 TV 版限制，自制极简桌面替换广告桌面，用 pm disable-user 去广告，并在不可行时给出电视盒子兜底方案。触发词：老电视、安卓电视、电视卡、电视去广告、换电视桌面、DS video、当贝市场、adb调试、乐视电视、刷机包、电视精简。
version: 1.0.0
author: B站-锤无双
user-invocable: true
argument-hint: "[电视IP]"
metadata:
  short-description: 老安卓电视抢救指南（ADB/兼容App/极简桌面/去广告）
  author: B站-锤无双
  version: V1.0
  qq-group: "170621740"
---

# 拯救老安卓电视

给 AI Agent 的操作手册：把一台 Android 4.x 的国产定制电视，从"什么都装不上、满屏广告"变成"能装兼容 App、有干净桌面、没有广告"的可用设备。本文档来自一次真实的乐视 X3-43（MStar 918 / Android 4.3 / EUI 6.0）抢救过程。

## 先读：三条铁律

1. **一切以 `build.prop` 为准，不要相信厂商版本号。**
   - `ro.build.version.release` = Android 版本（4.3 / 5.0…）
   - `ro.build.version.sdk` = API level（18 / 21…），**装 App 只看这个**
   - 反例：乐视 EUI 6.0 / 8.0 听起来像 Android 6/8，实际一直是 **Android 4.3（API 18）**。
2. **能不能装某个 App，由 APK 的 `minSdk` 决定**，与"内核版本"无关。内核旧不是问题，`sdkVersion` 大于电视的 API 才是。
3. **不要幻想"做个刷机包升级 Android/内核"**：老电视 SoC（MStar/Amlogic/瑞芯微等）的 kernel、U-Boot、显示/音频/HDMI/DTV 驱动都是闭源 BSP，没有公开源码和 device tree，U-Boot 有签名强刷，也没有第三方 recovery。连厂商自己都只换皮不升 Android。想跑现代 App，唯一正解是外接 Android 9+ 电视盒子。

## 什么时候用 / 不用

- 用：用户说电视老、装不上 App、广告多、桌面乱、想开 ADB、想精简/换桌面。
- 不用：Samsung Tizen / LG webOS / Roku / VIDAA 等非 Android 电视（没有 ADB），或者 Android 8+ 的 Android TV（可直接用 CleanYourTV 一类项目）。

## 总流程

```
阶段0 收集信息 → 阶段1 开ADB → 阶段2 建立控制 → 阶段3 找兼容App → 阶段4 换桌面 → 阶段5 去广告 → 阶段6 验证收尾
```

详细命令和踩坑见 `references/playbook.md`；ADB 命令速查见 `references/adb-cheatsheet.md`；找旧版 App 的方法见 `references/app-version-hunting.md`；乐视/MStar 的机型背景见 `references/letv-918-notes.md`。

## 阶段 0：收集信息（让用户拍照/截图）

1. **电视型号与系统信息页**（设置→关于/系统信息，多翻几页）：设备名称、产品型号、EUI/系统版本、硬件版本、**Android 版本/内核版本**。
2. **网络信息页**：IP 地址（如 192.168.2.163）、MAC（判断有线/无线）。
3. **桌面截图**：确认已装哪些 App、是不是已 root（SuperSU/KingRoot 图标）。
4. **刷机包目录**（如果有）：原厂包、第三方包各多大，文件名（如 `LetvUpgrade.bin`），用于判断平台和是否刷过机。

拿不到 Android 版本时，最可靠的办法是让用户用 Root Explorer 打开 `/system/build.prop` 拍照，看 `ro.build.version.release` 和 `ro.build.version.sdk`。其次可以让用户装一个系统信息类 App；再次是"试装"（见阶段 1 的 ADB 开关 APK 或阶段 3 的 APK 测试）。

## 阶段 1：开启 ADB（三条路，按顺序试）

### 路线 A：厂商工程菜单（最快，但常被精简固件阉割）

- 乐视：遥控器 **信号源 → 输入 2580 → 其他选项 → 拉到底 → ADB 调试 → 开启 → 重启电视**。
- 小米/创维/海信/长虹/海尔等：见 `references/adb-cheatsheet.md` 的"开 ADB 速查表"。
- 注意：很多"精简 ROM"把工程菜单里的 ADB 项删了；MStar 工厂菜单（Design Menu / Factory Menu）里通常**没有** ADB 开关，只有 Uart Enable，不要在这里浪费时间。

### 路线 B：当贝市场"远程推送"（没有 ADB 时的救命通道）

前提：电视上已装当贝市场（国产电视/盒子大多有）。

1. 电视上打开 **当贝市场 → 我的 → 远程推送**，屏幕会显示 `IP:端口`（端口每次都可能变，见过 5002、5003）。
2. PC 先扫端口确认页面：`scripts/adb.py find-push <TV_IP>`，或 GET `http://<IP>:<PORT>/` 看是不是"当贝市场…互传文件"页面。
3. PC 上传 APK：

```bash
curl -F "file1=@ADB-Switch.apk;filename=ADB-Switch.apk;type=application/vnd.android.package-archive" \
     http://<TV_IP>:<PORT>/upload
# 返回 ok 即成功；电视端会弹安装框，用户点安装
```

4. 装好后打开 **「ADB开关」**，点「一键开启 ADB (5555)」，SuperSU 弹窗点「允许」（可勾选记住）。

> 本 skill 自带 `assets/prebuilt/ADB-Switch.apk`（minSdk 16 / Android 4.1+，源码在 `assets/adb-switch/`）。它的原理是用 root 执行 `setprop service.adb.tcp.port 5555; stop adbd; start adbd`。如果电视未 root，这条路走不通，改用路线 C。

### 路线 C：root 手段（已 root 的电视）

- 装一个终端类 App（或自写一个调用 `su` 的 APK），执行：
  ```
  su
  setprop service.adb.tcp.port 5555
  stop adbd
  start adbd
  ```
- 或改 `/system/build.prop` 加 `persist.adb.tcp.port=5555`（**危险，改坏会开不了机，必须先备份**）。
- 实测注意：部分老固件的 adbd **不认 `persist.adb.tcp.port`**，重启后 5555 还是会关；别承诺"开机自启"，让用户重启后手动点一次 ADB 开关 App。

### 开好后的验证

```bash
adb connect <TV_IP>:5555
adb devices          # 应显示 <TV_IP>:5555  device（不是 offline / unauthorized）
adb shell getprop ro.build.version.sdk
```

## 阶段 2：建立控制通道（能截图、能按键、能装包）

老电视的 ADB 往往"看起来能用，实际到处受限"，先做能力探测：

```bash
adb shell su -c id                          # 是否拿到 uid=0。拿不到先让用户在 SuperSU 里设"默认访问=授予"
adb shell getprop ro.build.version.sdk      # 记录 API level
adb shell screencap -p /sdcard/t.png        # 非 root 可能生成 0 字节文件；用 su -c 再试
adb shell dumpsys window windows | grep mCurrentFocus   # 当前前台窗口
```

已封装的命令直接用 `scripts/adb.py`（自动拼接 adb 路径，支持 root）：

```bash
python scripts/adb.py connect 192.168.2.163
python scripts/adb.py cap  home.png          # root 截图并拉回
python scripts/adb.py key 3                  # HOME
python scripts/adb.py tap 961 950            # 坐标点击（配合截图）
python scripts/adb.py text 192.168.2.207     # 只支持 ASCII；中文输入要改用系统输入法/鼠标
python scripts/adb.py launch com.synology.dsvideo
python scripts/adb.py install-app app.apk    # 优先 adb install，失败自动回退 su -c pm install
python scripts/adb.py disable com.ktcp.video # pm disable-user
python scripts/adb.py enable  com.ktcp.video
```

**三个常见封锁与绕过：**

| 现象 | 原因 | 绕过 |
|---|---|---|
| `adb install` 报 `INSTALL_PACKAGES` 权限拒绝 | 定制 ROM 不给 shell 安装权限 | `adb push` 到 `/data/local/tmp`，再 `adb shell su -c "pm install -r /data/local/tmp/x.apk"` |
| `am start` 报 `INTERACT_ACROSS_USERS_FULL` 或 NullPointerException | 厂商改坏了 am | 用 `su -c am start ...`，或用 `monkey -p <pkg> -c android.intent.category.LAUNCHER 1` |
| `screencap` 得到 0 字节 / `uiautomator dump` 失败 | 权限或 ROM 阉割 | `su -c 'screencap -p /sdcard/x.png'`；uiautomator 不可用就纯截图 + 坐标点击 |

**用截图驱动 UI 的基本功**：截屏 → 用图片坐标换算出设备坐标（预览图 1708×961 对应 1920×1080 时约 ×1.124）→ `input tap x y`；文本输入用 `input text`（ASCII），中文/密码可以让用户自己在电视上输，或用系统输入法。

## 阶段 3：找兼容老系统的 App（最关键的活）

**判断标准只看 `minSdk`**（用 SDK 的 `aapt dump badging x.apk | grep sdkVersion`，或 `scripts/check_apk.py x.apk`）：

- 电视 API 18（Android 4.3）→ 只能装 `sdkVersion ≤ 18` 的 APK；
- 电视 API 19（4.4）→ ≤19；API 21（5.0）→ ≤21。

**最新版通常装不上**（微信/爱奇艺/B站/群晖手机版最新都要 API 23~26）。策略：

1. 先试 **TV 版**：TV 版（如银河奇异果、云视听极光、云视听小电视）对老系统更宽容，minSdk 常是 16~18。
2. TV 版不行/没有 → 找**手机版的历史版本**，按 `minSdk` 挑：
   - 豌豆荚历史版本页：`https://www.wandoujia.com/apps/<id>/history`，每个版本页里的下载直链带 `minSDK=` 参数，可直接筛；
   - k73 等国产下载站（`k.197947.com` 直链，国内速度尚可）；
   - 下载慢用 `scripts/pardl.py <url> <out> 8` 多线程分段下载（Cloudflare 单连接约 80KB/s，8 线程可到数 MB/s）。
3. 装之前先 `check_apk.py` 确认 minSdk 和 ABI（老电视基本是 `armeabi-v7a`，`armeabi` 也兼容；**纯 arm64** 的包装不上/跑不了）。
4. 装完立刻启动看截图与 logcat；闪退就 `adb logcat -d | grep -E "AndroidRuntime|FATAL"` 找原因（常见是调用了更高 API 的方法，如 `ActivityOptions.makeSceneTransitionAnimation`、`KeyPairGeneratorSpec.Builder.setKeySize`）。
5. 登录类 App（DS video 等）：手机版界面可以用 `input tap` + `input text` 帮用户把**地址/账号/密码**填好，勾选"记住登录状态"，之后用户点图标就能直接用；播放一般会交给系统播放器（弹出"选择应用"），选 TV 自带播放器并在弹窗点「始终」。

DS video 的实测兼容表（可作为"手机版降级"范例）：

| 版本 | minSdk | 结论 |
|---|---|---|
| DS video TV 1.1.8 / 1.1.7 | 21 | Android 5.0+，4.3 装不上（硬改 minSdk 后能装但登录崩：`KeyPairGeneratorSpec.Builder.setKeySize` 是 API 19） |
| DS video 手机版 3.4.7 / 3.4.5 | 26 / 23 | 装不上 |
| DS video 手机版 **3.3.3** | **16** | ✅ 4.3 可用，手机界面 + 外部播放器 |

更多来源、API 对照与下载技巧见 `references/app-version-hunting.md`。

## 阶段 4：自制极简桌面（换掉广告桌面）

**为什么不直接装第三方桌面**：当贝桌面等也带推荐位/广告；老机器上还容易卡。最干净的是自己写一个几十 KB 的 HOME App。

本 skill 自带预编译 `assets/prebuilt/SimpleHome.apk`（minSdk 16）和源码 `assets/simple-home/`（纯 Java，无网络权限）：

- 深色背景 + 圆角卡片 + 统一 64dp 图标 + 焦点蓝框放大 + 右上角 24 小时时钟/日期；
- 默认 7 个卡片（可在 `DEFAULT_TILES` 改），**「＋」打开已安装应用列表添加**，**长按卡片移除**，配置存 SharedPreferences；
- 应用未安装时自动隐藏对应卡片（升级/卸载不会留下死图标）。

**替换成默认桌面的正确顺序（Android 4.3 没有 `pm set-home-activity`）：**

1. `adb install` 或 `su -c pm install` 装好自制桌面；
2. `su -c am start -n com.dsh.simplehome/.MainActivity` 先手动启动，确认不闪退；
3. 停用原桌面：`su -c pm disable-user --user 0 <原桌面包名>`（乐视为 `com.stv.launcher`），再 `am force-stop`；
4. **重启电视**；因为没有其他 HOME 了，系统会自动把自制桌面当默认（实测 4.3 上重启后生效）。
5. 验证：`dumpsys window windows | grep mCurrentFocus` 应显示自制桌面。

**风险控制**：不要把自制桌面的安装包卸载（否则开机没有桌面）；提前把原桌面包名记下来，恢复命令是 `su -c pm enable <原桌面包名>`。自制桌面要能处理 `onBackPressed`（Home 键回到自己），并做"应用不存在就跳过分组"的健壮性处理，避免老机器上 ANR。

> **实测警告**：部分老 ROM（乐视 4.3 已验证）在**替换/卸载正在运行的唯一 HOME** 时会触发 system_server 重启 → 黑屏 + ADB 掉线 + 网络断开，安装被中断。稳妥顺序：原桌面还在时先装好新桌面并确认能启动 → 再停用原桌面 → 重启；**永远不要让自己处于"只有自制桌面、还要覆盖安装它"的状态**。真遇到了：等系统自己重启完（桌面通常还在），或断电重启；若确实没有桌面了，用 `pm enable <原桌面>` / 重装自制桌面恢复。

## 阶段 5：去广告 / 精简（`pm disable-user`，可逆）

原则：**只停应用、不删系统文件、不做危险 mount**。先备份：

```bash
adb shell pm list packages > packages-before.txt
adb shell pm list packages -d > disabled-before.txt
```

按"先广告/统计、后预装娱乐、再系统服务"的顺序小批量停用，每批之后重启验证。乐视 4.3 实测可用清单（包名可能因固件不同）：

```text
com.stv.reportService           # 统计上报
com.stv.bootadmanager           # 开机广告
com.amap.android.location       # 高德定位（广告用）
com.system.wcrash               # 崩溃上报
com.le.lepay.tv                 # 乐视支付
com.stv.lap.alitvbuy            # 阿里购物
com.android.browser             # 自带浏览器
com.ktcp.video                  # 腾讯视频（要装 TV 版时再 enable）
sina.mobile.tianqitongstv       # 天气
com.android.gallery3d           # 相册（不用可停）
```

- 开机视频：`/tvconfig/video.ts`（有的 ROM 已删；删前先 `ls` 确认，改名为 `.bak` 更稳）。
- 系统更新：停 `SystemUpgrade`（包名以固件为准），避免被强制升级。
- 恢复：`su -c pm enable <包名>`；或按 `disabled-before.txt` 批量恢复。
- **别停**：`com.android.systemui`、`com.android.providers.*`、`com.android.packageinstaller`、输入法、设置、SuperSU、以及负责遥控/HDMI/信号源的厂商服务。

## 阶段 6：验证与收尾

- 重启电视，验证：桌面正常、App 能打开、遥控器 HOME/返回正常、无开机广告。
- 安全：网络 ADB 是明文 5555，折腾完让用户在 ADB 开关 App 里点"关闭 ADB"，或 `su -c "setprop service.adb.tcp.port -1; stop adbd; start adbd"`。
- 把备份文件（包列表、停用列表、原桌面包名、App 版本号）留在电脑上，告诉用户恢复方法。
- 如果折腾成本已经超过买盒子：直接建议外接 Android 9+ 盒子，电视当显示器。

## 常见坑（都是实测踩过的）

1. **EUI 版本 ≠ Android 版本**；`ro.build.version.sdk=18` 才是真相。
2. **工程菜单里没有 ADB 开关**：MStar 工厂菜单只有 Uart Enable；换当贝远程推送 + ADB 开关 APK。
3. **当贝远程推送端口会变**（5002→5003），且页面关闭后端口消失；每次重新打开页面后重新扫描。
4. **`adb install` 被拒**：走 `su -c pm install`。
5. **`am start` 被 ROM 改坏**：走 `su -c am start` 或 `monkey`。
6. **非 root `screencap` 出 0 字节**；`uiautomator dump` 可能直接报权限错。
7. **`input text` 不支持中文**；中文输入交给用户或用输入法。
8. **老机器别装重型 App**：Kodi 16 在这类 2GB/4 核 A9 电视上能把 system_server 拖死机。推荐"DS video + 系统播放器"这种轻组合。
9. **系统卡死时**：先 `adb reboot`，不行再 `su -c reboot`；都无响应就让用户拔电源 10 秒（死机断电不会变砖，别在刷机中断时拔）。
10. **刷机包不要乱刷**：第三方"精简 ROM"只能换皮，Android 版本不变；刷错平台/签名会变砖，救砖要 UART/编程器。

## 复用预编译 APK（推荐：AI 没有 Android SDK/JDK 时）

`assets/prebuilt/` 里已经放了编译好、签名好的两个 APK，**直接安装即可，不需要现场用 Android SDK 编译**：

| APK | 版本 | minSdk | 说明 |
|---|---|---|---|
| `ADB-Switch.apk` | 1.3 | 16（Android 4.1+） | 一键开/关网络 ADB；**必须已 root** |
| `SimpleHome.apk` | 10.0 | 16（Android 4.1+） | 极简 HOME 桌面；装 App 不需要 root，但"停用原桌面"这一步通常要 root |

推荐流程：`adb.py upload` / 当贝远程推送 → 电视上点安装 → ADB 开关 App 开 ADB → 装 SimpleHome → 停原桌面 → 重启。

只有这些情况才需要改源码重编（`scripts/build_assets.ps1`）：

1. 电视 API **低于 16**（Android 4.0 及以下，极少见）：`-MinSdk 14` 重编；
2. 想改默认卡片/图标/配色：改 `assets/simple-home/.../MainActivity.java` 的 `DEFAULT_TILES` 等常量；
3. 想改 ADB 开关执行的命令或端口。

> 关键点：**不要默认"现场编译 APK"**。不是每个 AI 环境都有 JDK 17-22 + Android SDK 34/35；预编译 APK 让 skill 在只有 adb + curl 的环境里也能跑完整流程。

## 自带资产与脚本

| 路径 | 用途 |
|---|---|
| `assets/prebuilt/ADB-Switch.apk` | 一键开 ADB（需 root，minSdk 16），当贝远程推送安装 |
| `assets/prebuilt/SimpleHome.apk` | 极简桌面（minSdk 16），可改源码重编译 |
| `assets/adb-switch/`、`assets/simple-home/` | 两个 App 的完整源码 |
| `scripts/adb.py` | TV 控制助手（connect/cap/key/tap/text/launch/install/disable/enable/find-push） |
| `scripts/check_apk.py` | 纯 Python 检查 APK 的 package / minSdk / versionName / ABI |
| `scripts/pardl.py` | 多线程分段下载 APK（绕开单连接限速） |
| `scripts/build_assets.ps1` | 用 Android SDK 重新编译两个 APK |
| `references/` | 详细 playbook、ADB 速查、找旧版方法、乐视 918 背景、排错 |

## 作者与版本

- 版本：**V1.0**
- 作者：**B站-锤无双**
- 交流 QQ 群：**170621740**
- 欢迎补充新机型与新 App 版本（提 Issue / PR 即可）。

## 安全与免责

- 只在用户自己的设备上操作，并事先说明风险（停用系统服务、替换桌面、断电重启）。
- 不删系统分区、不刷不明来源的 bin、不 root 新设备（用户机器已 root 才用 root 方案）。
- 不收集/不外传用户账号密码；帮用户登录时只在电视本地输入，不写入任何文件。
- 本项目无厂商固件/签名密钥，全部依赖设备本身开放的调试能力。
