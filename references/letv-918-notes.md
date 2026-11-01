# 乐视 918 平台（X3-43 / 超3 系列）背景笔记

> 来自一次真实抢救：LeTV X3-43（产品名 X3-43，硬件版本 H5000，EUI 6.0.072N_0801，已 root/SuperSU，IP 192.168.2.163）。

## 1. 硬件与系统真相

| 项目 | 值 |
|---|---|
| 平台 | MStar 6A918（`ro.board.platform=napoli`，`ro.product.device=mstarnapoli`），4×Cortex-A9，Mali-450 MP4 |
| 内存/存储 | 2GB DDR3 + 8GB eMMC |
| 系统 | EUI 6.0.072N_0801，**Android 4.3 / API 18**（`ro.build.version.sdk=18`） |
| 内核 | MStar BSP 3.x（闭源） |
| 已 root | SuperSU + Root Explorer |

**关键认知**：

- EUI 5.5 → 6.0 → 8.0 一路升级，Android 底包始终是 **4.3**。厂商版号 ≠ Android 版本，别被"6.0/8.0"骗了。
- 918 平台是 2013–2014 年的方案，官方从未升级 Android 大版本；社区也没有 LineageOS/AOSP/Android TV 移植（BSP 闭源、U-Boot 签名、无 recovery）。
- 所以"做个刷机包升级内核/安卓"不可行；能刷的第三方包（蓝同学 beta、080S_0724 精简版等）只是换皮 + 去广告 + 解锁安装，Android 还是 4.3。

## 2. 刷机与救砖（仅作背景，实操需谨慎）

- 918 平台强刷文件名：`LetvUpgrade.bin`（928 是 `LetvUpgrade928.bin`，938 是 `LetvUpgrade938.bin`）；刷错平台 = 花屏/黑屏。
- U 盘要求：单分区 FAT32；`.bin` 放根目录；插 USB2.0 口；常见强刷键序：39 键遥控 `3→6→9→5→开机`，超级遥控 `设置→音量+→音量-→频道-→频道+→开机`。
- 救砖：同型号强刷包重刷；更严重需 MBOOT/UART/编程器。
- 刷机风险：签名校验失败卡 logo、断电成黑板砖、刷后不能恢复出厂（主页键失灵）等。
- 第三方包来源（仅供参考，注意 MD5/机型匹配）：wangtongxue.cn/p/31、zntvrom、51tvrom、ZNDS 乐视版块。

## 3. ADB 的特殊性

- 乐视官方路径：`信号源 → 2580 → 工程界面 → 其他选项 → ADB 调试 → 开启 → 重启`。
- 但本次机器的 2580 进入的是 **MStar 工厂菜单（Design Menu）**，里面有 `Uart Enable` 却没有 ADB 开关 —— 精简固件阉割了。
- 实际解法：U 盘安装「无双ADB」（root 执行 `setprop service.adb.tcp.port 5555; stop adbd; start adbd`）；没 U 盘时用第三方市场远程推送把它推进去。
- 无双ADB 支持开机自动尝试开 ADB + 自动启动 8555 推送页，但**装完必须先打开一次 App**（否则收不到开机广播）。
- 实测该固件 **不认 `persist.adb.tcp.port`**；如果自启失效，手动再点一次「一键开启 ADB」。

## 4. 该机型实测可用的去广告清单

```text
com.stv.reportService       # 统计上报
com.stv.bootadmanager       # 开机广告
com.amap.android.location    # 高德定位
com.system.wcrash            # 崩溃上报
com.le.lepay.tv              # 乐视支付
com.stv.lap.alitvbuy         # 阿里购物
com.android.browser          # 自带浏览器
sina.mobile.tianqitongstv    # 天气
com.android.gallery3d        # 相册（按需）
com.ktcp.video               # 腾讯视频（需要时再 enable）
```

原桌面：`com.stv.launcher`（停用后由自制桌面接管 HOME）。系统播放器：`com.stv.videoplayer`（DS video 播片就交给它，硬解最稳）。

## 5. 该机型的现实结论

- 能跑：DS video 手机版 3.3.3、银河奇异果 16.10.0、云视听极光 13.5.0、云视听小电视、第三方应用市场、自写无双桌面。
- 不能跑：DS video TV 版（需 5.0）、爱奇艺/B 站最新手机版（需 8.0+）、任何现代 64 位 App。
- 卡死风险：Kodi 16 这类重型 App 在这台 2GB 机器上会把 system_server 拖死，遥控器无响应，只能断电重启。推荐轻量组合（DS video + 系统播放器）。
- 想要现代 App：外接 Android 9+ 电视盒子，电视只当显示器 —— 这是唯一"真正解决"的路线。
