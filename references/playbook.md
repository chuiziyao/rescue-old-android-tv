# Playbook：从零到一台干净可用的老电视

本文是 `SKILL.md` 的展开版，按真实抢救顺序写，命令可直接复制。示例电视：乐视 X3-43（MStar 6A918，Android 4.3 / API 18，EUI 6.0.072N_0801，已 root，IP 192.168.2.163）。

---

## 0. 对用户"回忆版步骤"的纠错与补充

用户凭记忆列的 7 步方向正确，但有 5 处必须修正，否则会卡住：

| 用户记忆 | 实际情况 / 补充 |
|---|---|
| ① 拍型号、IP，告诉助手"精简桌面去广告" | 补充：还要拍**系统信息页全部内容**和**已装应用桌面**；最关键的是拿到 `ro.build.version.sdk`，不能只凭 EUI/系统版本号猜 Android 版本 |
| ② 按机型提示手动开 ADB | 多数情况对；但**精简 ROM 常把 ADB 开关删掉**（乐视 MStar 工程菜单里没有 ADB 项），所以必须有第③步兜底 |
| ③ 不行就用当贝市场远程安装，告诉 IP 和端口 | 补充：**端口每次会变**（实测 5002→5003），要重新在电视上打开"远程推送"页面再扫描；接口是 `POST /upload` 的 `file1` 字段 |
| ④ 推送"ADB 开关 APK"就有 ADB | 补充：该 APK **需要 root**，且部分固件不认 `persist.adb.tcp.port`，**重启后 ADB 会再次关闭**，要告诉用户每次重启后手动点一下 |
| ⑤ 助手自己找适合老电视的 App 版本 | 关键补充：判据是 **minSdk ≤ 电视 API**；最新版通常 23~26，要专门找历史版本；TV 版优先，手机版降级兜底 |
| ⑥ DS video 装手机版、助手帮登录 | 基本正确；补充：电视实际是 **Android 4.3（不是 4.2）**；手机版界面用 `input tap` + `input text` 填地址/账号/密码并勾选"记住登录"；播放交给系统播放器（选"始终"） |
| ⑦ 让助手做简洁桌面，图标一样大 | 补充：Android 4.3 没有 `pm set-home-activity`，替换 HOME 的正确姿势是**装自制桌面 → 停用原桌面 → 重启**；并且要防止"卸载自制桌面=没有桌面" |

另外有三条用户没提、但决定成败的补充：

- **控制通道**：ADB 通了不等于能用，很多 ROM 禁止 `adb install`、`am start`，必须用 `su -c` 绕过；
- **截图驱动**：老电视上 uiautomator 常不可用，要用 `su -c screencap` 截图 + 坐标点击来操作手机版 App；
- **兜底方案**：当成本超过买盒子时，直接建议外接 Android 9+ 电视盒子，不要试图给老 SoC 移植新 Android。

---

## 1. 信息收集（先拍照，再动手）

让用户提供：

1. 电视正面/背面铭牌照片（型号、SN）；
2. 设置 → 关于/系统信息 的每一页（尤其 Android 版本、内核版本、EUI/系统版本）；
3. 网络信息页（IP、有线/无线 MAC）；
4. 电视桌面截图（看已装 App、是否有 SuperSU / Root Explorer）；
5. 如果刷过机：刷机包目录截图（原厂包/第三方包文件名与大小）。

**终极确认 Android 版本**：让用户用 Root Explorer 打开 `/system/build.prop`，拍这两行：

```properties
ro.build.version.release=4.3
ro.build.version.sdk=18
```

拿到 `sdk` 后记下来，后面所有 App 选择都以它为准。

---

## 2. 开 ADB

### 2.0 无双ADB（首选，一个 APK 全搞定）

1. U 盘拷 `assets/prebuilt/WushuangADB.apk` → 电视「文件管理」安装（没 U 盘就先用 2.2 的当贝推送把它推进去）；
2. 打开「无双ADB」→ SuperSU 允许；
3. App 自动开 ADB 并启动推送服务，屏幕显示 Android 版本 / API / 型号 / 平台 / 内核 / IP / Root / ADB 端口，以及 `推送地址: http://<电视IP>:8555/`；
4. 电脑浏览器打开推送地址：上传 APK 安装、网页按钮卸载（有 root 静默；无 root 自动弹系统安装器，让用户在电视上点确认）。

> root 不是必须：很多刷机 ROM 自带 `su`（不需要另装 SuperSU）；完全没 root 时也能用"无 root 模式"装包/卸载，但**开 ADB 必须 root 或工程菜单**。

> 装完必须先打开一次，开机广播（自动 ADB + 推送服务）才会生效。推送页和 ADB 都是明文无鉴权，用完点「停止推送」「关闭 ADB」。

### 2.1 先试工程菜单

- 乐视：`信号源 → 2580 → 其他选项 → ADB 调试 → 开启 → 重启`。
- 其它品牌见 `references/adb-cheatsheet.md`。
- 如果菜单里根本没有 ADB（精简 ROM 常见），进下一节。

### 2.2 当贝市场远程推送（无 ADB、也无 U 盘时的通道）

1. 电视打开 **当贝市场 → 我的 → 远程推送**，记下屏幕上的 `IP:端口`。
2. PC 上扫描并确认页面：

```bash
# 端口可能变，先扫 4995-5011
python scripts/adb.py find-push 192.168.2.163
```

3. 上传自带的 WushuangADB：

```bash
python scripts/adb.py upload http://<IP>:<PORT> assets/prebuilt/WushuangADB.apk
```

4. 电视弹安装框 → 安装 → 打开「无双ADB」→ 点「一键开启 ADB」→ SuperSU 允许。
5. PC 验证：`python scripts/adb.py connect <IP>`，`adb devices` 出现 `device`。

### 2.3 root 终端兜底

```bash
su
setprop service.adb.tcp.port 5555
stop adbd
start adbd
```

> **重要实测结论**：某些老固件不读取 `persist.adb.tcp.port`，开机自启无效。稳妥做法是让用户把「无双ADB」App 留在桌面，每次重启后点一下。

---

## 3. 建立控制通道

```bash
python scripts/adb.py discover               # 电视重启过？IP 可能变了，先扫
python scripts/adb.py connect 192.168.2.121
adb shell getprop ro.build.version.sdk     # 记录 API
adb shell su -c id                          # 期望 uid=0
```

如果 `su -c id` 无输出/失败：让用户打开 SuperSU → 设置 → 默认访问 → 授予；或下次弹窗点允许。

能力探测与绕过（详见 `references/adb-cheatsheet.md`）：

```bash
# 安装（adb install 被拒时）
adb push app.apk /data/local/tmp/app.apk
adb shell su -c "pm install -r /data/local/tmp/app.apk"

# 启动（am start 被 ROM 改坏时）
adb shell su -c "am start -n com.pkg/.MainActivity"
adb shell su -c "monkey -p com.pkg -c android.intent.category.LAUNCHER 1"

# 截图（非 root 可能 0 字节）
adb shell su -c "screencap -p /sdcard/s.png"
adb pull /sdcard/s.png s.png
```

坐标点击换算：截图 1920×1080，若预览图显示为 1708×961，则实际坐标 = 预览坐标 × 1920/1708 ≈ ×1.124。

---

## 4. 找兼容 App（核心步骤）

1. **先查 minSdk**：

```bash
python scripts/check_apk.py some.apk
# 或
aapt dump badging some.apk | grep -E "package:|sdkVersion|native-code"
```

2. 规则：`sdkVersion <= 电视 API` 才能装。
3. 优先 TV 版；没有或太新 → 找手机版历史版本（`references/app-version-hunting.md` 有完整来源与下载方法）。
4. 下载慢用多线程：

```bash
python scripts/pardl.py "https://.../app.apk" app.apk 8
```

5. 安装后启动、截图、必要时看 logcat：

```bash
adb logcat -d -v brief | grep -E "AndroidRuntime|FATAL|ActivityManager.*START"
```

6. 登录类 App：截图 → 算坐标 → `input tap` 点输入框 → `input text` 填 ASCII 内容；中文/密码可让用户自己输；勾选"记住登录状态"。

**DS video 实战路径**：

- TV 版 1.1.x（minSdk 21）在 4.3 上装不上；硬改 minSdk 到 18 能装，但登录时崩 `KeyPairGeneratorSpec$Builder.setKeySize`（API 19）。
- 手机版 3.4.3（minSdk 19）仍装不上；**3.3.3（minSdk 16）可用**。
- 登录后播放会弹"选择应用"：建议选系统自带播放器并点「始终」，老电视硬解最稳。

---

## 5. 自制无双桌面

1. 源码在 `assets/wushuang-desktop/`，可改 `DEFAULT_TILES`、颜色、列数；
2. 用 `scripts/build_assets.ps1` 编译（需要 JDK 17-22 + Android SDK platform 34/35 + build-tools）；
3. 安装并手动启动确认不闪退；
4. 停用原桌面（乐视 `com.stv.launcher`）→ 重启；
5. 截图验证，按用户意见微调（图标统一大小、间距、焦点颜色）。

> 注意：自制桌面是唯一 HOME 时，**绝不能卸载它**；恢复原桌面：`su -c pm enable com.stv.launcher`。

---

## 6. 去广告 / 精简

```bash
adb shell pm list packages > packages-before.txt
adb shell pm list packages -d > disabled-before.txt

# 小批量停用（示例：乐视）
for p in com.stv.reportService com.stv.bootadmanager com.amap.android.location \
         com.system.wcrash com.le.lepay.tv com.stv.lap.alitvbuy com.android.browser; do
  adb shell su -c "pm disable-user --user 0 $p"
done
```

每批之后重启验证。停错了用 `su -c pm enable <包名>` 恢复。**不要停 systemui、providers、packageinstaller、输入法、设置、遥控/信号源相关服务。**

---

## 7. 收尾

- 重启验证桌面、App、遥控。
- 关闭网络 ADB（ADB 开关 App 的"关闭 ADB"，或 `setprop service.adb.tcp.port -1; stop adbd; start adbd`）。
- 把「包列表备份、停用列表、原桌面包名、可用 App 版本号」交给用户。
- 如果用户还想装现代 App：建议外接 Android 9+ 盒子。

---

## 8. 时间预算与止损

一次完整抢救（ADB + 3~5 个 App + 桌面 + 去广告）通常 1~3 小时，其中"找旧版 App 并验证"最耗时。若出现以下情况，及时止损改方案：

- 电视无 root 且当贝通道也打不开 ADB；
- 老固件 ADB 极不稳定（频繁 offline）；
- 用户目标是"用最新版微信/爱奇艺"这类现代 App —— 外接盒子是唯一正解。
