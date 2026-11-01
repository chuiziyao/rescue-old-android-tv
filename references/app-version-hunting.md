# 找"能装在老电视上"的 App 版本

老电视装不上 App 的根因几乎只有一个：**APK 的 `minSdkVersion` 高于电视的 API level**。所以核心动作是：确认电视 API → 找 minSdk 达标的 APK → 验证 ABI → 安装 → 启动看日志。

## 1. 先确认电视 API

```bash
adb shell getprop ro.build.version.sdk
# 18=Android 4.3  19=4.4  21=5.0  22=5.1  23=6.0  24=7.0  26=8.0  28=9.0
```

## 2. 检查 APK 的 minSdk / ABI

```bash
python scripts/check_apk.py app.apk
# 输出示例：package=com.qiyi.video versionName=8.0 minSdk=14 abis=armeabi
```

或：

```bash
aapt dump badging app.apk | grep -E "package:|sdkVersion|native-code"
```

判定：`minSdk ≤ 电视 API`，且 ABI 含 `armeabi`/`armeabi-v7a`（老电视没有 arm64 运行时）。

## 3. 优先顺序

1. **TV 版**（对老系统支持最久）：
   - 银河奇异果（爱奇艺 TV）：`com.gitvdemo.video`，实测 16.10.0 仍 `minSdk=16`；
   - 云视听极光（腾讯 TV）：`com.ktcp.video`，老固件自带版本 13.5.0 可用；
   - 云视听小电视（B 站 TV）：`com.xiaodianshi.tv.yst`；
   - 当贝市场：`com.dangbeimarket`；
   - DS video Android TV：`com.synology.dsvideo` 的 TV 版（minSdk 21，4.3 不行，见下）。
2. **手机版历史版本**（TV 版没有或太新时）。
3. **同功能替代**（如 Kodi 16、当贝播放器 1.3.5、VLC 旧版）。

## 4. 手机版历史版本去哪找

### 4.1 豌豆荚（推荐，页面带 minSDK 参数）

- App 历史版本页：`https://www.wandoujia.com/apps/<appid>/history`
  - 爱奇艺：appid 28855；哔哩哔哩：appid 281291。
- 每个版本页（`/history_vXXXX`）里的下载直链形如：
  `https://android-apps.pp.cn/...apk?...&minSDK=21&...`
  → 从 URL 里直接读出 `minSDK`，挑 ≤ 电视 API 的版本。
- 注意：豌豆荚只保留最近几十个版本；太老的（2015 年前）可能已经查不到。

### 4.2 国产下载站（k73 等）

- k73 页面会给出 `https://k.197947.com/kdx8/android/xxx.apk` 直链（2018 年前后的老版本常见）。
- 这些 CDN 单连接约 80KB/s，用 `scripts/pardl.py <url> <out> 8` 多线程分段下载（实测可到数 MB/s）。
- 下载后**一定先 check_apk.py 验证**，别直接装。

### 4.3 官方/商店旧版接口

- 当贝市场 TV 端会按设备兼容性推荐版本，是最省事的"设备感知"渠道（让用户直接在电视上搜安装）。
- 群晖官方归档站保留全部历史 APK：`https://archive.synology.com/download/Mobile/AndroidTV-DSvideo` 与 `.../Android-DSvideo`（每个版本目录下是 APK）。
- 群晖 CDN（国内快）：`https://cndl.synology.cn/download/Mobile/<产品>/<版本>/<文件名>.apk`。

## 5. 实测兼容表（可直接抄）

### DS video

| 版本 | minSdk | 电视 API 18（4.3） | 备注 |
|---|---|---|---|
| TV 1.1.8 / 1.1.7 / 1.1.5 / 1.1.0 | 21 | ❌ 装不上 | 硬改 minSdk 到 18 可装，但登录崩 `KeyPairGeneratorSpec$Builder.setKeySize`（API 19），推荐页还会崩 `ActivityOptions.makeSceneTransitionAnimation`（API 21） |
| 手机版 3.4.7 | 26 | ❌ | |
| 手机版 3.4.5 | 23 | ❌ | |
| 手机版 3.4.3 | 19 | ❌（4.3） | 4.4 可用 |
| **手机版 3.3.3** | **16** | ✅ | 手机/平板界面；登录后播放交给系统播放器 |
| 手机版 3.0 / 2.6.2 | 14 | ✅ | 更老，功能少 |

### 其它常见

| App | 包名 | 可用版本参考 |
|---|---|---|
| 银河奇异果（爱奇艺 TV） | `com.gitvdemo.video` | 16.10.0，minSdk 16 ✅ |
| 云视听极光（腾讯 TV） | `com.ktcp.video` | 13.5.0.1010（固件自带）✅ |
| 云视听小电视（B 站 TV） | `com.xiaodianshi.tv.yst` | 固件自带 ✅ |
| 哔哩哔哩手机版 | `tv.danmaku.bili` | 5.2.0，minSdk 15 ✅（能刷推荐流） |
| 爱奇艺手机版 | `com.qiyi.video` | 8.0，minSdk 14 ✅（能开、能播预告片） |
| 当贝播放器 | `com.dangbei.lerad.videoposter` | 1.3.5，minSdk 16 ✅（老电视唯一能装的版本，建议留备份） |
| Kodi | `org.xbmc.kodi` | 16.1 可用但**太重，2GB 老电视容易把系统拖死**，不推荐 |

## 6. 硬改 minSdk 的风险（不推荐常规使用）

有时只能找到 minSdk 只高 1~3 的 APK，可以改二进制 AndroidManifest 的 minSdk 再重签名（`scripts/axml_patch.py`）：

```bash
python scripts/axml_patch.py inspect app.apk
python scripts/axml_patch.py patch app.apk app-sdk18.apk 18
zipalign -f 4 app-sdk18.apk aligned.apk
apksigner sign --ks debug.keystore --out signed.apk aligned.apk
```

**风险**：minSdk 是被开发者显式抬高的，通常意味着代码里直接调用了更高 API 的方法（`NoSuchMethodError`）或依赖了 multidex/native 库。实测 DS video TV 版就是这样崩的。只在"有 logcat 可验证、可回退"时尝试，别把它当常规方案。

## 7. 装完的验证清单

1. `check_apk.py` 确认 minSdk/ABI；
2. 安装成功（`su -c pm install`）；
3. 启动后 `dumpsys window windows | grep mCurrentFocus` 是目标 App；
4. 截图看界面是否正常渲染；
5. `adb logcat -d | grep -E "AndroidRuntime|FATAL"` 无崩溃；
6. 关键功能（播放/登录）点一遍。

## 8. 登录类 App 的自动化

- 截图定位输入框 → `input tap x y` → `input text <ASCII>`；
- 勾选"记住登录状态"（坐标点击复选框）；
- 中文用户名/密码：让用户用电视输入法输入，或临时切换到英文账号；
- 播放器选择弹窗：选系统自带播放器并点「始终」，避免每次询问。
