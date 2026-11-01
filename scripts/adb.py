#!/usr/bin/env python3
"""adb.py - 老安卓电视控制助手（纯标准库，Python 3.7+）

用法:
  python adb.py connect 192.168.2.163
  python adb.py devices
  python adb.py props
  python adb.py cap home.png              # root 截图并拉回当前目录
  python adb.py key 3                     # HOME；23=OK；4=BACK；19/20/21/22=方向
  python adb.py tap 961 950
  python adb.py text 192.168.2.207        # 仅 ASCII，空格自动转 %s
  python adb.py launch com.synology.dsvideo
  python adb.py install-app app.apk       # 先试 adb install，失败回退 su -c pm install
  python adb.py disable com.ktcp.video
  python adb.py enable com.ktcp.video
  python adb.py find-push 192.168.2.163   # 扫描第三方市场远程推送端口(4995-5011)
python adb.py discover                  # 扫描本网段 5555/8555，电视重启换 IP 后用
python adb.py upload http://192.168.2.163:8555 app.apk   # 无双ADB 推送页安装
  python adb.py upload http://192.168.2.163:5002 ADB-Switch.apk
  python adb.py shell "getprop ro.build.version.sdk"

环境变量:
  ADB       adb 可执行文件路径（可选）
  TV_SERIAL 设备序列号/IP:5555（可选，默认取第一台在线设备）
"""
import json
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request

SERIAL = os.environ.get("TV_SERIAL", "")


def find_adb():
    cands = []
    if os.environ.get("ADB"):
        cands.append(os.environ["ADB"])
    w = shutil.which("adb")
    if w:
        cands.append(w)
    for root in (
        os.environ.get("ANDROID_HOME", ""),
        os.environ.get("ANDROID_SDK_ROOT", ""),
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk"),
        os.path.expanduser("~/Android/Sdk"),
        os.path.expanduser("~/Library/Android/sdk"),
    ):
        if root:
            cands.append(os.path.join(root, "platform-tools", "adb.exe" if os.name == "nt" else "adb"))
    for c in cands:
        if c and os.path.isfile(c):
            return c
    return None


ADB = find_adb()


def _tmpdir():
    """有些受限环境里 adb 无法写 %TEMP%\\adb.log，换一个可写目录。"""
    for d in (tempfile.gettempdir(), os.path.expanduser("~"), os.getcwd()):
        try:
            p = os.path.join(d, ".adb-tmp")
            os.makedirs(p, exist_ok=True)
            test = os.path.join(p, "w.txt")
            with open(test, "w") as f:
                f.write("ok")
            os.remove(test)
            return p
        except Exception:
            continue
    return None


TMP = _tmpdir()


def env():
    e = dict(os.environ)
    if TMP:
        e["TEMP"] = TMP
        e["TMP"] = TMP
    return e


def run(args, check=False, stdin=None):
    if not ADB:
        sys.exit("找不到 adb：请安装 Android platform-tools，或设置环境变量 ADB=<adb路径>")
    cmd = [ADB]
    if SERIAL:
        cmd += ["-s", SERIAL]
    cmd += args
    p = subprocess.run(cmd, env=env(), capture_output=True, text=False)
    out = (p.stdout or b"").decode("utf-8", "replace")
    err = (p.stderr or b"").decode("utf-8", "replace")
    if check and p.returncode != 0:
        sys.exit((out + err).strip() or "adb 命令失败: %s" % " ".join(cmd))
    return p.returncode, out, err


def shell(cmd, root=False):
    if root:
        return run(["shell", "su", "-c", cmd])
    return run(["shell", cmd])


def ensure_device():
    rc, out, err = run(["get-state"])
    if rc != 0 or "device" not in out:
        sys.exit("设备未连接。先运行: python adb.py connect <TV_IP>（并确认电视 ADB 已开）")


def cmd_connect(ip):
    if ":" not in ip:
        ip += ":5555"
    run(["connect", ip])
    time.sleep(1.5)
    print(run(["devices"])[1])


def cmd_cap(name):
    ensure_device()
    remote = "/sdcard/%s" % name
    rc, out, err = shell("screencap -p %s" % remote, root=True)
    if rc != 0 or "No such" in out + err:
        shell("screencap -p %s" % remote, root=False)
    time.sleep(0.6)
    rc, out, err = run(["pull", remote, name])
    print(out.strip() or err.strip())
    if os.path.exists(name):
        print("saved:", os.path.abspath(name), os.path.getsize(name), "bytes")
    else:
        sys.exit("截图失败：尝试 su -c screencap，确认已 root")


def cmd_install(path):
    ensure_device()
    path = os.path.abspath(path)
    if not os.path.exists(path):
        sys.exit("文件不存在: " + path)
    rc, out, err = run(["install", "-r", path])
    text = out + err
    if rc == 0 and "Success" in text:
        print("installed (adb install)")
        return
    print("adb install 失败，回退 su -c pm install ...")
    remote = "/data/local/tmp/%s" % os.path.basename(path).replace(" ", "_")
    run(["push", path, remote], check=True)
    rc, out, err = shell("pm install -r %s" % remote, root=True)
    print((out + err).strip())
    if "Success" not in out + err:
        sys.exit("安装失败")


def cmd_upload(base, path):
    """向第三方市场远程推送页面 POST /upload，字段名 file1。"""
    base = base.rstrip("/")
    boundary = "----dshupload%d" % int(time.time())
    fname = os.path.basename(path)
    with open(path, "rb") as f:
        data = f.read()
    body = b""
    body += ("--%s\r\n" % boundary).encode()
    body += ('Content-Disposition: form-data; name="file1"; filename="%s"\r\n' % fname).encode()
    body += b"Content-Type: application/vnd.android.package-archive\r\n\r\n"
    body += data + b"\r\n"
    body += ("--%s--\r\n" % boundary).encode()
    req = urllib.request.Request(
        base + "/upload",
        data=body,
        headers={"Content-Type": "multipart/form-data; boundary=%s" % boundary},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            print(r.status, r.read(200).decode("utf-8", "replace"))
    except Exception as e:
        sys.exit("上传失败: %s" % e)


def cmd_discover():
    """扫描本机所在 /24 网段，找开着 5555（ADB）或 8555（无双ADB推送）的电视。"""
    import concurrent.futures
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        local = s.getsockname()[0]
    except Exception:
        local = "192.168.1.10"
    finally:
        s.close()
    prefix = local.rsplit(".", 1)[0] + "."
    print("本机网段:", prefix + "0/24")

    def chk(args):
        ip, port = args
        sock = socket.socket()
        sock.settimeout(0.35)
        try:
            sock.connect((ip, port))
            return (ip, port)
        except Exception:
            return None
        finally:
            sock.close()

    targets = [(prefix + str(i), p) for i in range(1, 255) for p in (5555, 8555)]
    found = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=400) as ex:
        for r in ex.map(chk, targets):
            if r:
                found.append(r)
    if not found:
        print("没找到开 5555/8555 的设备；确认电视已开机、ADB/推送已开")
        return
    by_ip = {}
    for ip, port in sorted(found):
        by_ip.setdefault(ip, []).append(port)
    for ip, ports in by_ip.items():
        print("%-15s ports=%s" % (ip, ports))


def cmd_find_push(ip):
    ports = []
    for p in range(4995, 5012):
        s = socket.socket()
        s.settimeout(0.35)
        try:
            s.connect((ip, p))
            ports.append(p)
        except Exception:
            pass
        finally:
            s.close()
    print("open ports:", ports)
    for p in ports:
        try:
            with urllib.request.urlopen("http://%s:%d/" % (ip, p), timeout=3) as r:
                head = r.read(600).decode("utf-8", "replace")
            if "upload" in head or "WebServer" in head or "Web" in head:
                print("push page: http://%s:%d/  (POST /upload, field file1)" % (ip, p))
        except Exception:
            pass


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 1
    c = argv[1]
    a = argv[2:]
    if c == "connect":
        cmd_connect(a[0])
    elif c == "devices":
        print(run(["devices", "-l"])[1])
    elif c == "props":
        ensure_device()
        for k in ("ro.build.version.release", "ro.build.version.sdk", "ro.product.model",
                  "ro.product.device", "ro.board.platform", "ro.build.version.incremental"):
            rc, out, _ = shell("getprop " + k, root=False)
            print("%-32s %s" % (k, out.strip()))
    elif c == "cap":
        cmd_cap(a[0] if a else "screenshot.png")
    elif c == "key":
        ensure_device()
        print(shell("input keyevent %s" % a[0], root=True)[1].strip())
    elif c == "tap":
        ensure_device()
        print(shell("input tap %s %s" % (a[0], a[1]), root=True)[1].strip())
    elif c == "swipe":
        ensure_device()
        print(shell("input swipe %s %s %s %s %s" % tuple(a[:5]), root=True)[1].strip())
    elif c == "text":
        ensure_device()
        txt = " ".join(a).replace(" ", "%s")
        print(shell("input text %s" % txt, root=True)[1].strip())
    elif c == "launch":
        ensure_device()
        pkg = a[0]
        rc, out, err = shell("am start -n %s/.MainActivity" % pkg, root=True)
        if rc != 0 or "Error" in out + err:
            rc, out, err = shell("monkey -p %s -c android.intent.category.LAUNCHER 1" % pkg, root=True)
        print((out + err).strip())
    elif c == "install-app":
        cmd_install(a[0])
    elif c in ("disable", "enable", "uninstall", "clear", "force-stop"):
        ensure_device()
        pkg = a[0]
        if c == "disable":
            cmd = "pm disable-user --user 0 %s" % pkg
        elif c == "enable":
            cmd = "pm enable %s" % pkg
        elif c == "uninstall":
            cmd = "pm uninstall %s" % pkg
        elif c == "clear":
            cmd = "pm clear %s" % pkg
        else:
            cmd = "am force-stop %s" % pkg
        rc, out, err = shell(cmd, root=True)
        print((out + err).strip())
    elif c == "packages":
        ensure_device()
        print(shell("pm list packages " + (" ".join(a) if a else ""), root=False)[1])
    elif c == "find-push":
        cmd_find_push(a[0])
    elif c == "discover":
        cmd_discover()
    elif c == "upload":
        cmd_upload(a[0], a[1])
    elif c == "shell":
        ensure_device()
        print(shell(" ".join(a), root=True)[1])
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
