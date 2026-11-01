#!/usr/bin/env python3
"""check_apk.py - 纯 Python 读取 APK 的 package / minSdk / targetSdk / versionName / ABI

不需要安装 Android SDK（不依赖 aapt）。
用法:
  python check_apk.py app.apk
  python check_apk.py app.apk --json
退出码: 0 正常；2 参数错误；3 不是 APK / 解析失败
"""
import json
import struct
import sys
import zipfile

RES_STRING_POOL_TYPE = 0x0001
RES_XML_START_ELEMENT_TYPE = 0x0102
TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10
TYPE_INT_HEX = 0x11


def read_string_pool(data, off):
    _type, header_size, _size = struct.unpack_from("<HHI", data, off)
    string_count, _style_count, flags, strings_start, _styles_start = struct.unpack_from(
        "<IIIII", data, off + 8)
    offs = list(struct.unpack_from("<%dI" % string_count, data, off + header_size))
    utf8 = bool(flags & 0x100)
    base = off + strings_start
    out = []
    for o in offs:
        p = base + o
        if utf8:
            n = data[p]
            p += 1
            if n & 0x80:
                n = ((n & 0x7F) << 8) | data[p]
                p += 1
            blen = data[p]
            p += 1
            if blen & 0x80:
                blen = ((blen & 0x7F) << 8) | data[p]
                p += 1
            out.append(data[p:p + blen].decode("utf-8", "replace"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p)[0]
                p += 2
            out.append(data[p:p + 2 * n].decode("utf-16-le", "replace"))
    return out


def iter_chunks(data):
    off = 8
    total = len(data)
    while off + 8 <= total:
        ctype, hsize, csize = struct.unpack_from("<HHI", data, off)
        if csize == 0:
            break
        yield ctype, off, hsize, csize
        off += csize


def parse_manifest(data):
    strings = None
    result = {"package": None, "versionName": None, "versionCode": None,
              "minSdk": None, "targetSdk": None}
    for ctype, off, hsize, csize in iter_chunks(data):
        if ctype == RES_STRING_POOL_TYPE:
            strings = read_string_pool(data, off)
            continue
        if ctype != RES_XML_START_ELEMENT_TYPE or strings is None:
            continue
        ns_idx, name_idx = struct.unpack_from("<II", data, off + 16)
        attr_start, attr_size, attr_count = struct.unpack_from("<HHH", data, off + 24)
        elem = strings[name_idx] if name_idx < len(strings) else ""
        if elem not in ("manifest", "uses-sdk"):
            continue
        attrs_off = off + 16 + attr_start
        for i in range(attr_count):
            a = attrs_off + i * attr_size
            _a_ns, a_name, a_raw = struct.unpack_from("<III", data, a)
            _t_size, _t_res0, t_type = struct.unpack_from("<HBB", data, a + 12)
            t_data = struct.unpack_from("<I", data, a + 16)[0]
            aname = strings[a_name] if a_name < len(strings) else ""
            value = None
            if t_type == TYPE_STRING:
                if t_data < len(strings):
                    value = strings[t_data]
            elif t_type in (TYPE_INT_DEC, TYPE_INT_HEX):
                value = t_data
            if elem == "manifest":
                if aname == "package":
                    result["package"] = value
                elif aname == "versionName":
                    result["versionName"] = value
                elif aname == "versionCode":
                    result["versionCode"] = value
            else:
                if aname == "minSdkVersion":
                    result["minSdk"] = value
                elif aname == "targetSdkVersion":
                    result["targetSdk"] = value
    return result


def apk_info(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        if "AndroidManifest.xml" not in names:
            raise ValueError("不是 APK：缺少 AndroidManifest.xml")
        data = z.read("AndroidManifest.xml")
        abis = set()
        for n in names:
            if n.startswith("lib/") and n.count("/") >= 2:
                abis.add(n.split("/")[1])
        info = parse_manifest(data)
        info["abis"] = sorted(abis)
        info["file"] = path
        return info


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    path = argv[1]
    try:
        info = apk_info(path)
    except Exception as e:
        print("解析失败:", e)
        return 3
    if "--json" in argv:
        print(json.dumps(info, ensure_ascii=False, indent=2))
    else:
        print("file        :", info["file"])
        print("package     :", info["package"])
        print("versionName :", info["versionName"], "(versionCode %s)" % info["versionCode"])
        print("minSdk      :", info["minSdk"])
        print("targetSdk   :", info["targetSdk"])
        print("abis        :", ", ".join(info["abis"]) or "(no native libs)")
        if isinstance(info["minSdk"], int):
            print("\n兼容提示: 电视 API >= %d 才能安装（API 18=Android 4.3, 19=4.4, 21=5.0）"
                  % info["minSdk"])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
