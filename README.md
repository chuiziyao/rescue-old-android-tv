# 拯救老安卓电视 · rescue-old-android-tv

> 一个给 AI Agent（以及折腾老电视的人）用的操作手册 + 工具包：把 Android 4.x 的老国产电视从"什么都装不上、满屏广告"救回可用状态。

[![Android](https://img.shields.io/badge/Android-4.x-brightgreen)](#)
[![minSdk](https://img.shields.io/badge/APK%20minSdk-16%2B-blue)](#)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

> **版本 V1.0** ｜ 作者：**B站-锤无双** ｜ 交流 QQ 群：**170621740**

来自一次真实抢救：**乐视 X3-43（MStar 6A918 / Android 4.3 / API 18 / EUI 6.0 / 已 root）**。最终结果：DS video 可用、爱奇艺 TV 版（银河奇异果）可用、腾讯 TV 版（云视听极光）可用、自制极简桌面替代广告桌面、广告服务全部停用、ADB 可控。

![极简桌面](docs/screenshots/home.png)

## 它解决什么问题

- 电视提示"解析包错误 / 应用未安装"——根因是 APK 的 `minSdk` 高于电视 API，不是"内核太老"；
- 设置里找不到 ADB 开关——用当贝市场远程推送 + 自写「ADB 开关」App 兜底；
- 满屏开机广告、桌面广告、预装购物/直播——用 `pm disable-user` 全部停用（可逆）；
- 官方桌面太乱——用 17KB 的自制 HOME App 替换，图标统一、无广告、可自定义添加/移除；
- 冷门 App（如群晖 DS video）没有兼容 TV 版——用手机版历史版本 + ADB 自动登录绕过。

## 三条铁律

1. **看 `ro.build.version.sdk`，别看厂商版本号**：EUI 6.0/8.0 实际都是 Android 4.3（API 18）。
2. **能不能装只看 APK 的 minSdk**：`minSdk ≤ 电视 API` 才能装；与内核新旧无关。
3. **别幻想刷机升级 Android**：老 SoC 的 BSP 闭源、U-Boot 签名、无 recovery；想跑现代 App 就外接 Android 9+ 盒子。

## 目录结构

```text
rescue-old-android-tv/
├── SKILL.md                    # Agent 主文档（DSH/Claude Skills 格式）
├── references/
│   ├── playbook.md             # 完整抢救流程 + 对"用户回忆版步骤"的纠错
│   ├── adb-cheatsheet.md       # 各品牌开 ADB 路径、命令、按键码、logcat
│   ├── app-version-hunting.md  # 按 minSdk 找旧版 App（含实测兼容表）
│   ├── letv-918-notes.md       # 乐视 MStar 918 平台背景与刷机风险
│   └── troubleshooting.md      # 症状 → 原因 → 处理
├── scripts/
│   ├── adb.py                  # TV 控制助手（connect/cap/key/tap/text/install/disable…）
│   ├── check_apk.py            # 纯 Python 读 APK 的 minSdk/targetSdk/versionName/ABI
│   ├── pardl.py                # 多线程分段下载 APK（绕开单连接限速）
│   ├── build_assets.ps1        # 重新编译两个自带 APK
│   └── axml_patch.py           # 高级：硬改 minSdk（风险操作）
├── assets/
│   ├── prebuilt/ADB-Switch.apk # 一键开 ADB（需 root）
│   ├── prebuilt/SimpleHome.apk # 极简电视桌面（minSdk 16）
│   ├── adb-switch/             # ADB 开关 App 源码
│   └── simple-home/            # 极简桌面源码
└── README.md
```

## 快速开始

1. 把电视型号、系统信息页、IP、桌面截图发给你的 AI 助手；
2. 按 `references/adb-cheatsheet.md` 开 ADB；开不了就打开当贝市场「远程推送」；
3. 电脑上：

```bash
python scripts/adb.py connect 192.168.2.163
python scripts/adb.py props
python scripts/adb.py cap home.png
```

4. 装 App 前先验证兼容性：

```bash
python scripts/check_apk.py some.apk
```

5. 按 `references/playbook.md` 换桌面、去广告、收尾。

## 为什么要自带两个 APK（而不是让 AI 现场编译）

不是每个 AI 运行环境都有 JDK 17-22 + Android SDK 34/35，但**任何能跑 adb 的环境**都能安装现成 APK。所以本仓库直接提供编译并签名好的：

| APK | 版本 | minSdk | 用途 |
|---|---|---|---|
| `assets/prebuilt/ADB-Switch.apk` | 1.2 | 16（Android 4.1+） | 一键开/关网络 ADB（需 root） |
| `assets/prebuilt/SimpleHome.apk` | 9.0 | 16（Android 4.1+） | 极简 HOME 桌面 |

只有需要改默认卡片/图标/配色，或电视低于 Android 4.1 时，才需要用 `scripts/build_assets.ps1` 重新编译（源码在 `assets/` 下）。

## 自带工具说明

| 脚本 | 用途 |
|---|---|
| `adb.py cap/key/tap/text/launch` | 截图驱动 UI，替代不可用的 uiautomator |
| `adb.py install-app` | 自动处理 `adb install` 被拒（回退 `su -c pm install`） |
| `adb.py upload` | 向当贝市场远程推送页面 POST APK（字段 `file1`） |
| `adb.py find-push` | 扫描 4995-5011 找当贝推送端口 |
| `check_apk.py` | 不装 Android SDK 也能读 minSdk/ABI |
| `pardl.py` | 8 线程分段下载，老 CDN 限速时提速 10 倍 |
| `build_assets.ps1` | 改完源码一键重编两个 APK |

## 实测可用 App（Android 4.3 / API 18）

| App | 包名 | 版本 | minSdk |
|---|---|---|---|
| 银河奇异果（爱奇艺 TV） | `com.gitvdemo.video` | 16.10.0 | 16 |
| 云视听极光（腾讯 TV） | `com.ktcp.video` | 13.5.0.1010 | 老固件自带 |
| 云视听小电视（B 站 TV） | `com.xiaodianshi.tv.yst` | 老固件自带 | — |
| DS video 手机版 | `com.synology.dsvideo` | **3.3.3** | 16 |
| 哔哩哔哩手机版 | `tv.danmaku.bili` | 5.2.0 | 15 |
| 爱奇艺手机版 | `com.qiyi.video` | 8.0 | 14 |
| 当贝播放器 | `com.dangbei.lerad.videoposter` | 1.3.5 | 16 |

更多见 `references/app-version-hunting.md`。

## 作者

- 版本：**V1.0**
- 作者：**B站-锤无双**
- 交流 QQ 群：**170621740**
- 欢迎补充新机型 / 新 App 版本，提 Issue 或 PR。

## 免责声明

- 只应在**你自己的设备**上使用；停用系统服务、替换桌面、改 build.prop 都有风险，请先备份并能接受回滚。
- 本项目不包含任何厂商固件、签名密钥或破解工具，全部依赖设备自身开放的调试能力（开发者选项 / ADB / root）。
- 按 MIT 协议开源，不提供任何明示或暗示担保。

## License

MIT © 2026 B站-锤无双 · 交流 QQ 群 170621740
