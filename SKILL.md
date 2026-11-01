---
name: rescue-old-android-tv
description: 拯救老安卓电视（Android 4.x 国产定制电视/盒子，如乐视、小米、创维、TCL 等）：用自带「无双ADB」一键看硬件信息、开 ADB、开局域网推送装包/卸载；按 minSdk 找兼容老系统的 App 版本；用手机版 App 绕过 TV 版限制；用自带「无双桌面」替换广告桌面；用 pm disable-user 去广告；并在不可行时给出电视盒子兜底方案。触发词：老电视、安卓电视、电视卡、电视去广告、换电视桌面、DS video、当贝市场、adb调试、乐视电视、刷机包、电视精简。
version: 1.3.0
author: B站-锤无双
user-invocable: true
argument-hint: "[电视IP]"
metadata:
  short-description: 老安卓电视抢救指南（无双ADB/无双桌面/兼容App/去广告）
  author: B站-锤无双
  version: V1.3
  qq-group: "170621740"
---

# 拯救老安卓电视

给 AI Agent 的操作手册：把一台 Android 4.x 的国产定制电视，从"什么都装不上、满屏广告"变成"能装兼容 App、有干净桌面、没有广告"的可用设备。本文档来自一次真实的乐视 X3-43（MStar 918 / Android 4.3 / EUI 6.0）抢救过程。

**本 skill 自带两个 APK（`assets/prebuilt/`），都不依赖当贝市场：**

| APK | 作用 |
|---|---|
| `WushuangADB.apk`（无双ADB） | 看 build.prop/IP/内核/root、一键开 ADB、开 HTTP 推送页（浏览器上传 APK 自动安装、网页卸载）、开机自启推送与自动 ADB；**有 root 静默装/卸载，无 root 自动切系统安装器让用户确认** |
| `WushuangDesktop.apk`（无双桌面） | 极简 HOME 桌面：统一图标、深色卡片、时钟、＋添加应用、长按移除 |

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
阶段0 装无双ADB看信息 → 阶段1 开ADB/推送 → 阶段2 建立控制 → 阶段3 找兼容App → 阶段4 换无双桌面 → 阶段5 去广告 → 阶段6 验证收尾
```

详细命令和踩坑见 `references/playbook.md`；ADB 命令速查见 `references/adb-cheatsheet.md`；找旧版 App 的方法见 `references/app-version-hunting.md`；乐视/MStar 的机型背景见 `references/letv-918-notes.md`。

## 阶段 0：装「无双ADB」，一眼看全信息

**推荐第一条命令就是让用户装无双ADB**（U 盘 / 电视自带文件管理器 / 当贝市场远程推送都行），打开后屏幕上直接显示：

- Android 版本 / API level / 型号 / 平台 / 内核
- 全部 IPv4 地址
- Root 状态
- 当前 ADB 端口

比让用户去翻设置、拍照 build.prop 快得多。装好并打开一次后，它还会自动尝试开 ADB、自动启动局域网推送服务（`http://<电视IP>:8555/`）。

同时仍建议收集：电视型号与系统信息页截图、桌面截图（看已装 App / 是否有 SuperSU）、刷机包目录（如果有）。

## 阶段 1：开 ADB（四条路，按顺序试）

### 路线 A：无双ADB（首选，只要电视能 root）

1. 把 `assets/prebuilt/WushuangADB.apk` 拷到 U 盘根目录，插电视，用电视的「文件管理」安装；或者用当贝市场远程推送（见路线 B）装进去。
2. 打开「无双ADB」→ SuperSU 点「允许」。
3. App 会自动执行 `setprop service.adb.tcp.port 5555; setprop service.adb.root 1; stop adbd; start adbd`，屏幕显示 `ADB 端口: 5555`。
4. 同一屏还会显示推送地址 `http://<电视IP>:8555/`——浏览器打开即可上传 APK 安装、点按钮卸载，**不再需要当贝市场**。

> 无双ADB **不是必须 root**：有 root → 静默装/卸载 + 一键开 ADB；无 root → 网页推送后在电视上点「安装」确认（ADB 仍需工程菜单开）。
> 开机自启：推送服务和自动开 ADB 由 `BOOT_COMPLETED` 触发，**装完必须先手动打开一次 App**（否则处于 stopped 状态收不到广播）。

### 路线 B：当贝市场"远程推送"（没有 U 盘/文件管理器时）

1. 电视打开 **当贝市场 → 我的 → 远程推送**，记下 `IP:端口`（端口会变：见过 5002、5003）。
2. PC 扫端口：`python scripts/adb.py find-push <TV_IP>`，或浏览器看 `http://<IP>:<PORT>/` 是不是当贝页面。
3. 上传无双ADB：
   ```bash
   python scripts/adb.py upload http://<IP>:<PORT> assets/prebuilt/WushuangADB.apk
   ```
4. 电视上点安装 → 打开无双ADB → 允许 root，ADB 就开了。

### 路线 C：厂商工程菜单（部分 ROM 可用）

- 乐视：遥控器 **信号源 → 输入 2580 → 其他选项 → 拉到底 → ADB 调试 → 开启 → 重启电视**。
- 其它品牌：见 `references/adb-cheatsheet.md` 的"开 ADB 速查表"。
- 注意：很多"精简 ROM"把工程菜单里的 ADB 项删了；MStar 工厂菜单（Design Menu / Factory Menu）里通常**没有** ADB 开关，只有 Uart Enable。

### 路线 D：root 终端兜底

```bash
su
setprop service.adb.tcp.port 5555
stop adbd
start adbd
```

> 实测：部分老固件不认 `persist.adb.tcp.port`，重启后 ADB 会关；让用户把「无双ADB」留在桌面，重启后点一下即可。

### 开好后验证 + 找 IP

**电视重启后 IP 可能会变**（DHCP），永远不要假设还是上次那个 IP：

```bash
python scripts/adb.py discover        # 扫描本网段，找开 5555/8555 的设备
adb connect <发现的IP>:5555
adb shell getprop ro.build.version.sdk
```

## 阶段 2：建立控制通道（能截图、能按键、能装包）

老电视的 ADB 往往"看起来能用，实际到处受限"，先做能力探测：

```bash
adb shell su -c id                          # 是否拿到 uid=0
adb shell getprop ro.build.version.sdk
adb shell screencap -p /sdcard/t.png        # 非 root 可能是 0 字节，用 su -c 再试
adb shell dumpsys window windows | grep mCurrentFocus
```

已封装的命令直接用 `scripts/adb.py`（自动拼接 adb 路径，支持 root）：

```bash
python scripts/adb.py connect 192.168.2.121
python scripts/adb.py discover                # 重启后 IP 变了，先扫
python scripts/adb.py cap  home.png
python scripts/adb.py key 3                   # HOME
python scripts/adb.py tap 961 950
python scripts/adb.py text 192.168.2.207      # 仅 ASCII
python scripts/adb.py launch com.synology.dsvideo
python scripts/adb.py install-app app.apk     # 失败自动回退 su -c pm install
python scripts/adb.py disable com.ktcp.video
python scripts/adb.py enable  com.ktcp.video
python scripts/adb.py find-push <IP>          # 找当贝远程推送端口
python scripts/adb.py upload http://<IP>:8555 app.apk   # 无双ADB 推送页
```

**三个常见封锁与绕过：**

| 现象 | 原因 | 绕过 |
|---|---|---|
| `adb install` 报 `INSTALL_PACKAGES` 权限拒绝 | 定制 ROM 不给 shell 安装权限 | `adb push` 到 `/data/local/tmp`，再 `su -c "pm install -r ..."` |
| `am start` 报 `INTERACT_ACROSS_USERS_FULL` / NPE | 厂商改坏了 am | `su -c am start ...` 或 `monkey -p <pkg> -c android.intent.category.LAUNCHER 1` |
| `screencap` 0 字节 / `uiautomator dump` 失败 | 权限或 ROM 阉割 | `su -c 'screencap -p /sdcard/x.png'`；改用截图 + 坐标点击 |

**用截图驱动 UI**：截屏 → 换算坐标（预览 1708×961 对 1920×1080 约 ×1.124）→ `input tap x y`；`input text` 只支持 ASCII。

## 阶段 3：找兼容老系统的 App（最关键的活）

**判断标准只看 `minSdk`**（`python scripts/check_apk.py x.apk` 或 `aapt dump badging`）：

- API 18（Android 4.3）→ 只能装 `sdkVersion ≤ 18`；API 19 → ≤19；API 21 → ≤21。

策略：

1. 先试 **TV 版**（银河奇异果 `com.gitvdemo.video`、云视听极光 `com.ktcp.video`、云视听小电视 `com.xiaodianshi.tv.yst` 等，minSdk 常 16~18）。
2. 没有/太新 → 找**手机版历史版本**：
   - 豌豆荚历史页 `https://www.wandoujia.com/apps/<id>/history`，版本直链里带 `minSDK=`；
   - k73 等国产站的 `k.197947.com` 直链；
   - 慢时用 `python scripts/pardl.py <url> out.apk 8` 多线程下载。
3. 装前确认 minSdk 和 ABI（老电视是 `armeabi-v7a`/`armeabi`，纯 arm64 不行）。
4. 装完立刻启动 + `logcat -d | grep -E "AndroidRuntime|FATAL"` 看崩溃；常见是高 API 调用（`ActivityOptions.makeSceneTransitionAnimation`、`KeyPairGeneratorSpec.Builder.setKeySize`）。
5. 登录类 App：截图 + `input tap` + `input text` 帮用户填地址/账号/密码，勾"记住登录"；播放交给系统播放器并点「始终」。

DS video 实测兼容表：

| 版本 | minSdk | 结论 |
|---|---|---|
| TV 1.1.8 / 1.1.7 | 21 | 4.3 装不上；硬改 minSdk 后登录崩（API 19 方法） |
| 手机版 3.4.7 / 3.4.5 | 26 / 23 | 装不上 |
| 手机版 **3.3.3** | **16** | ✅ 4.3 可用，手机界面 + 外部播放器 |

更多见 `references/app-version-hunting.md`。

## 阶段 4：换成「无双桌面」

为什么不用第三方桌面：当贝桌面等也有推荐位/广告。自制桌面只有 17KB、纯 Java、无网络权限。

`assets/prebuilt/WushuangDesktop.apk`（包名 `com.wushuang.desktop`，minSdk 16）特性：

- 深色渐变 + 圆角卡片 + 统一 64dp 图标 + 焦点蓝框放大 + 右上角 24 小时时钟/日期；
- 左上角标题「无双桌面」；
- 默认卡片：DS video、银河奇异果、云视听极光、云视听小电视、当贝市场、设置、**无双ADB**；
- 「＋」从已安装应用里添加、长按卡片移除、应用卸载后自动隐藏卡片。

**切换默认桌面的正确顺序（Android 4.3 没有 `pm set-home-activity`）：**

1. 先装好 `WushuangDesktop.apk`（新包名，不影响当前桌面）；
2. 启动一次确认不闪退：`su -c am start -n com.wushuang.desktop/.MainActivity`；
3. 启用原桌面当兜底：`su -c pm enable com.stv.launcher`；
4. 停用旧桌面：`su -c pm disable com.dsh.simplehome`（或旧版桌面包名）；
5. 再停用原桌面：`su -c pm disable-user com.stv.launcher`；
6. **重启电视**（让系统重建默认 HOME 偏好）；重启后唯一的 HOME 就是无双桌面；
7. 验证：`dumpsys window windows | grep mCurrentFocus` 应显示 `com.wushuang.desktop`。

**HOME 键为什么不灵（乐视实测）**：乐视在 `system_server` 里塞了 `letv.desktop.DesktopManager`，`PhoneWindowManager.launchHomeFromHotKey()` 不走标准 HOME 解析，而是直接 `startActivity(com.stv.launcher/.activity.Launcher)`。原桌面被停用后它抛 `NullPointerException`（logcat: `Error while switching to desktop`），HOME 键就"空按"；按返回键退出应用则正常回到无双桌面（任务栈行为）。

可选处理：
- **A（推荐）**：接受"按遥控器返回键回桌面"，文档/桌面提示都写清楚；
- **B**：保留乐视桌面做 HOME 中转，加一个后台服务检测到乐视桌面就把无双桌面拉到前台（桌面会闪现广告、需要常驻服务，不推荐）；
- **C**：换支持标准 HOME 的机型/盒子。
其它品牌若 HOME 不灵，先重启；仍不灵说明该 ROM 也劫持了 HOME 键。

**风险控制**：
- 不要把无双桌面卸载/覆盖安装（它是唯一 HOME 时会导致黑屏）。
- 原桌面包名先记下来，恢复命令 `su -c pm enable <原桌面包名>`。
- 实测：**替换/卸载正在运行的唯一 HOME** 可能触发 system_server 重启（黑屏 + ADB 掉线 + 网络断），所以按上面"先装新桌面 → 再停旧桌面 → 重启"的顺序做。

## 阶段 5：去广告 / 精简（`pm disable-user`，可逆）

先备份：

```bash
adb shell pm list packages > packages-before.txt
adb shell pm list packages -d > disabled-before.txt
```

小批量停用，每批重启验证。乐视 4.3 实测清单：

```text
com.stv.reportService           # 统计上报
com.stv.bootadmanager           # 开机广告
com.amap.android.location       # 高德定位
com.system.wcrash               # 崩溃上报
com.le.lepay.tv                 # 乐视支付
com.stv.lap.alitvbuy            # 阿里购物
com.android.browser             # 自带浏览器
com.ktcp.video                  # 腾讯视频（要装时再 enable）
sina.mobile.tianqitongstv       # 天气
com.android.gallery3d           # 相册（按需）
```

- 开机视频：`/tvconfig/video.ts`（有的 ROM 已删；改名 `.bak` 更稳）。
- 恢复：`su -c pm enable <包名>`，或按 `disabled-before.txt` 批量恢复。
- **别停**：`com.android.systemui`、`com.android.providers.*`、`com.android.packageinstaller`、输入法、设置、SuperSU、遥控/HDMI/信号源相关厂商服务。

## 阶段 6：验证与收尾

- **重启电视验证**：桌面正常、App 能开、遥控主页/返回正常、无开机广告；重启后先用 `adb.py discover` 重新确认电视 IP（会变）。
- 无双ADB 的推送服务/自动 ADB 依赖开机广播：**装完必须打开过一次 App**；验证：重启后浏览器打开 `http://<新IP>:8555/` 能否出页面。
- 安全：网络 ADB 和无双ADB 推送页都是**明文、无密码**，只在家用局域网开；折腾完在无双ADB里点「关闭 ADB」「停止推送」。
- 把备份（包列表、停用列表、原桌面包名、App 版本号、电视 IP）留给用户。
- 如果折腾成本超过买盒子：建议外接 Android 9+ 盒子，电视当显示器。

## 常见坑（实测）

1. **EUI 版本 ≠ Android 版本**；`ro.build.version.sdk` 才是真相。
2. **工程菜单可能没有 ADB 开关**（MStar 工厂菜单只有 Uart Enable）→ 用无双ADB。
3. **当贝远程推送端口会变**（5002→5003）→ `adb.py find-push`，或直接用无双ADB 的 8555。
4. **`adb install` 被拒** → `su -c pm install`。
5. **`am start` 被改坏** → `su -c am start` 或 `monkey`。
6. **非 root `screencap` 0 字节 / uiautomator 权限错**。
7. **`input text` 不支持中文**。
8. **老机器别装重型 App**：Kodi 16 能把 system_server 拖死。
9. **电视重启后 IP 会变**：每次重连前 `adb.py discover`，不要硬编码旧 IP。
10. **无双ADB 装完要先打开一次**，否则开机广播收不到（推送服务/自动 ADB 不生效）。
11. **替换唯一 HOME 可能触发系统服务重启**：先装新桌面再停旧桌面，必要时重启。
12. **乐视 EUI 的 HOME 键被系统服务劫持**：停用原桌面后 HOME 键失效是必然（见阶段 4），用返回键回桌面。
13. **SuperSU 反复弹窗**：读设备信息别用 `su`（`getprop`/`cat` 不需要 root），只在开 ADB 时一次 `su`；仍嫌烦可在 SuperSU 设置里关通知。
14. **刷机包不要乱刷**：第三方精简包只换皮；刷错平台/签名会变砖。

## 复用预编译 APK（推荐：AI 没有 Android SDK/JDK 时）

`assets/prebuilt/` 里是编译并签名好的 APK，直接装，不需要现场编译：

| APK | 版本 | minSdk | 说明 |
|---|---|---|---|
| `WushuangADB.apk` | 1.0 | 16（Android 4.1+） | 信息/开ADB/推送装包卸载；需 root |
| `WushuangDesktop.apk` | 11.0 | 16（Android 4.1+） | 无双桌面 HOME App；装它不需要 root，替换默认桌面需要 |

推荐流程：U 盘/文件管理器装无双ADB → 打开允许 root → 浏览器 `http://<IP>:8555/` 装其余 APK → 装无双桌面 → 停旧桌面 → 重启。

只有这些情况才改源码重编（`scripts/build_assets.ps1`）：

1. 电视 API **< 16**：`-MinSdk 14` 重编；
2. 改默认卡片/图标/配色：改 `assets/wushuang-desktop/.../MainActivity.java` 的 `DEFAULT_TILES`；
3. 改 ADB 命令、推送端口等：改 `assets/wushuang-adb/` 源码。

> **不要默认"现场编译 APK"**：不是每个 AI 环境都有 JDK 17-22 + Android SDK；预编译 APK 让 skill 在只有 adb + curl 的环境也能跑完整流程。

## 自带资产与脚本

| 路径 | 用途 |
|---|---|
| `assets/prebuilt/WushuangADB.apk` | 无双ADB 1.3（信息/开ADB/局域网装包卸载，minSdk 16，有 root 静默/无 root 确认） |
| `assets/prebuilt/WushuangDesktop.apk` | 无双桌面（minSdk 16） |
| `assets/wushuang-adb/`、`assets/wushuang-desktop/` | 两个 App 的完整源码 |
| `assets/signing/debug.keystore` | 固定签名密钥（别名 androiddebugkey / 口令 android），保证重编可覆盖升级 |
| `scripts/adb.py` | TV 控制助手（connect/discover/cap/key/tap/text/launch/install/disable/enable/find-push/upload） |
| `scripts/check_apk.py` | 纯 Python 读 package / minSdk / versionName / ABI |
| `scripts/pardl.py` | 多线程分段下载 APK |
| `scripts/build_assets.ps1` | 用 Android SDK 重编两个 APK |
| `references/` | playbook、ADB 速查、找旧版方法、乐视 918 背景、排错 |

## 作者与版本

- 版本：**V1.2**
- 作者：**B站-锤无双**
- 交流 QQ 群：**170621740**
- V1.3 修复：无双ADB 读信息不再走 su（SuperSU 只在开 ADB 时弹一次）；确认乐视 EUI 的 HOME 键由系统 DesktopManager 硬编码劫持，给出返回键方案。
- V1.2 新增：无双ADB 无 root 模式（网页推送 → 系统安装器确认；卸载走系统确认框），解释"为什么不能自动 root"，文档区分"装包/桌面（可无 root）"与"开 ADB（需 root 或工程菜单）"。
- V1.1 新增：无双ADB（信息/开ADB/局域网推送装包卸载/开机自启）、无双桌面改名、固定签名密钥、IP 变化自动发现、HOME 键坑位说明。
- 欢迎补充新机型与新 App 版本（提 Issue / PR 即可）。

## 安全与免责

- 只在用户自己的设备上操作，并事先说明风险（停用系统服务、替换桌面、断电重启）。
- 无双ADB 的 8555 推送页和 ADB 5555 都是明文无鉴权，只在家用局域网临时开启，用完关闭。
- 不删系统分区、不刷不明来源的 bin、不 root 新设备（用户机器已 root 才用 root 方案）。
- 不收集/不外传用户账号密码；帮用户登录时只在电视本地输入，不写入任何文件。
- 本项目无厂商固件/签名密钥，全部依赖设备本身开放的调试能力。
