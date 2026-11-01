# 排错手册（症状 → 原因 → 处理）

## ADB 连不上

| 症状 | 原因 | 处理 |
|---|---|---|
| `connection refused` | 电视 ADB 开关没开 / 重启后关闭 | 让用户重新开（工程菜单或 ADB 开关 App），再 `adb connect` |
| `offline` | Wi-Fi 不稳 / 电视负载高 / adb 服务卡住 | `adb kill-server`→`adb start-server`→重连；让电视空闲几秒；仍不行重启电视 |
| `unauthorized` | 未在电视上确认授权 | 电视上点"允许 USB 调试"；或删 `~/.android/adbkey` 重连 |
| 端口扫描通但 shell 无响应 | 系统繁忙/ANR | 等 1~2 分钟；`adb shell echo ok` 测试；必要时重启 |

## 安装/启动受限

| 症状 | 原因 | 处理 |
|---|---|---|
| `INSTALL_FAILED_OLDER_SDK` / 解析包错误 | APK minSdk > 电视 API | 换低 minSdk 版本（见 app-version-hunting.md） |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | APK 只有 arm64 | 换 32 位（armeabi-v7a/armeabi）版本 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 同包名不同签名 | 先卸载旧包：`su -c pm uninstall <pkg>` |
| `Neither user 2000 ... INSTALL_PACKAGES` | 定制 ROM 不给 adb shell 安装权限 | `adb push` 到 `/data/local/tmp` 后 `su -c pm install -r` |
| `am start` 报 `INTERACT_ACROSS_USERS_FULL` / NPE | 厂商改坏 am | `su -c am start`；或 `monkey -p <pkg> -c android.intent.category.LAUNCHER 1` |
| `error: closed` 后进程消失 | 被系统/省电策略杀 | 关掉省电限制；`am force-stop` 后重开 |

## 截图/输入

| 症状 | 原因 | 处理 |
|---|---|---|
| screencap 生成 0 字节 | 非 root 或 ROM 限制 | `su -c 'screencap -p /sdcard/x.png'` |
| uiautomator dump 权限错误 | ROM 未给 shell RETRIEVE_WINDOW_CONTENT | 放弃 uiautomator，改用截图 + 坐标点击 |
| `input text` 输不了中文 | input 只支持 ASCII | 让用户用输入法；或临时用英文/数字账号 |
| 点击坐标错位 | 预览图与实际分辨率不一致 | 坐标 = 截图坐标；预览显示尺寸需按 1920/预览宽 比例换算 |

## App 运行问题

| 症状 | 原因 | 处理 |
|---|---|---|
| 启动即闪退 | `NoSuchMethodError`（调用了高 API 方法）/ `NoClassDefFoundError` | 看 `logcat`；换更低 minSdk 的版本 |
| 播放时弹"选择应用" | App 把播放交给外部播放器 | 选系统自带播放器 + 「始终」 |
| App 界面只能触屏 | 手机版 UI | 接 USB 鼠标/飞鼠，或用 adb `input tap` 帮用户操作 |
| 老 App 打开空白/暂无数据 | 服务端 API 已变更 | 换 TV 版或更新的可装版本；实在不行用 TV 版/盒子 |

## 系统与桌面

| 症状 | 原因 | 处理 |
|---|---|---|
| 换桌面后开机无桌面 | 自制桌面被卸载/崩溃，原桌面被停用 | 恢复原桌面：`su -c pm enable <原桌面包名>`，重启 |
| 覆盖/卸载唯一桌面时黑屏、ADB 掉线、网络断 | 部分老 ROM（乐视 4.3 实测）替换运行中的唯一 HOME 会触发 system_server 重启 | 等系统自己重启完（桌面通常恢复）；不行断电重启；确实没桌面了就 adb 恢复原桌面或重装自制桌面。预防：原桌面还在时先装新桌面、确认能启动，再停原桌面 |
| 遥控器全部无响应、画面卡死 | system_server 被重型 App 拖死 | 先 `adb reboot`，不行 `su -c reboot`，再不行拔电源 10 秒（死机断电不会变砖） |
| 遥控器主页键按了没反应（注入 HOME 也不走） | 老 ROM 的 HOME 键/默认偏好仍指向被停用的旧桌面 | 重启一次（开机只剩一个 HOME 时系统会设为默认）；或让用户按实体主页键从弹窗选「始终」 |
| 换桌面后重启，IP 变了连不上 | DHCP 重新分配 | `python scripts/adb.py discover` 扫描本网段 5555/8555，再 connect |
| 开机后 ADB 没自动开 | 无双ADB 装完没打开过（处于 stopped，收不到 BOOT_COMPLETED） | 打开一次无双ADB；或手动点「一键开启 ADB」 |
| 开机仍有广告 | 广告服务/开机视频未处理 | 停 `bootadmanager` 等；检查 `/tvconfig/video.ts` |
| 停用后某项功能坏了 | 停了系统依赖服务 | `su -c pm enable <包名>` 恢复；只停"应用类"包，别停 systemui/providers/packageinstaller |

## 无双ADB / 局域网推送

| 症状 | 原因 | 处理 |
|---|---|---|
| 浏览器打不开 `http://IP:8555/` | 推送服务没开 / App 被系统杀掉 / IP 变了 | 打开一次无双ADB 点「开启推送」；用 `adb.py discover` 重新确认 IP |
| 上传后提示"安装失败" | 未 root / APK minSdk 过高 / 包名冲突 | 确认 su 已授权；用 `check_apk.py` 看 minSdk；签名冲突先卸载旧版 |
| 网页卸载失败 | 系统应用/无 root | 只卸载第三方应用；系统应用用 `pm disable-user` |
| 上传后页面 404/无响应 | 端口被别的服务占用或服务已停 | 重启无双ADB；确认 8555 没被当贝/其他 App 占用 |

## 当贝市场远程推送

| 症状 | 原因 | 处理 |
|---|---|---|
| 端口连不上 | 页面已关闭 / 端口变了 | 电视重新打开"远程推送"；`scripts/adb.py find-push <IP>` 扫 4995-5011 |
| 上传返回 ok 但电视没反应 | 安装框被系统弹窗挡住/被拦截 | 电视端确认安装弹窗；检查"未知来源"是否允许 |
| 上传失败 | 字段名/格式不对 | 必须是 multipart 的 `file1` 字段，且 filename 以 `.apk` 结尾 |

## 止损线

- 电视无 root、当贝通道也开不了 ADB → 停止 root 方案，改"用户自己在电视商店装 TV 版 App"。
- 用户要的是最新版微信/爱奇艺/游戏 → 直接建议外接 Android 9+ 盒子，别在老 SoC 上硬撑。
