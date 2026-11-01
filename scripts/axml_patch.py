#!/usr/bin/env python3
"""Patch minSdkVersion in an APK's binary AndroidManifest.xml (AXML).

Usage:
  python axml_patch.py inspect <apk>
  python axml_patch.py patch <in.apk> <out.apk> <new_min_sdk>

Parses the AXML binary format, locates the <uses-sdk> element's
"minSdkVersion" attribute and rewrites its integer value. Everything else in
the APK is copied byte-for-byte (same compression type per entry).
"""
import struct
import sys
import zipfile

RES_STRING_POOL_TYPE = 0x0001
RES_XML_TYPE = 0x0003
RES_XML_START_ELEMENT_TYPE = 0x0102
TYPE_INT_DEC = 0x10
TYPE_INT_HEX = 0x11
MANIFEST = "AndroidManifest.xml"


def read_string_pool(data, off):
    """Return list of strings for the string pool chunk at off."""
    _type, header_size, size = struct.unpack_from("<HHI", data, off)
    string_count, style_count, flags, strings_start, styles_start = struct.unpack_from(
        "<IIIII", data, off + 8
    )
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
            out.append(data[p : p + blen].decode("utf-8", "replace"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p)[0]
                p += 2
            out.append(data[p : p + 2 * n].decode("utf-16-le", "replace"))
    return out


def iter_chunks(data):
    """Yield (type, offset, header_size, size) for top-level chunks."""
    off = 8  # skip the file header (RES_XML_TYPE)
    total = len(data)
    while off + 8 <= total:
        ctype, hsize, csize = struct.unpack_from("<HHI", data, off)
        if csize == 0:
            break
        yield ctype, off, hsize, csize
        off += csize


def find_min_sdk(data):
    """Locate the minSdkVersion attribute. Return its value + its data offset."""
    strings = None
    result = None
    for ctype, off, hsize, csize in iter_chunks(data):
        if ctype == RES_STRING_POOL_TYPE:
            strings = read_string_pool(data, off)
            continue
        if ctype != RES_XML_START_ELEMENT_TYPE or strings is None:
            continue
        # ResXMLTree_node: header(8) + lineNumber(4) + comment(4) = 16
        # ResXMLTree_attrExt starts at off+16
        ns_idx, name_idx = struct.unpack_from("<II", data, off + 16)
        attr_start, attr_size, attr_count = struct.unpack_from("<HHH", data, off + 24)
        if name_idx >= len(strings):
            continue
        elem = strings[name_idx]
        if elem != "uses-sdk":
            continue
        attrs_off = off + 16 + attr_start
        for i in range(attr_count):
            a = attrs_off + i * attr_size
            a_ns, a_name, a_raw = struct.unpack_from("<III", data, a)
            t_size, t_res0, t_type = struct.unpack_from("<HBB", data, a + 12)
            t_data = struct.unpack_from("<I", data, a + 16)[0]
            aname = strings[a_name] if a_name < len(strings) else ""
            if aname == "minSdkVersion":
                result = (t_data, a + 12, t_type)  # data offset points at typedValue
                break
        if result:
            break
    return result


def command_inspect(apk):
    with zipfile.ZipFile(apk) as z:
        data = z.read(MANIFEST)
    found = find_min_sdk(data)
    print("apk:", apk)
    print("manifest bytes:", len(data))
    if not found:
        print("minSdkVersion attribute: NOT FOUND")
        return 1
    value, typed_off, dtype = found
    print("minSdkVersion =", value, "dataType=0x%02x" % dtype, "typedValue offset=", typed_off)
    # Also print all uses-sdk attribute names for context
    return 0


def command_patch(apk_in, apk_out, new_value):
    with zipfile.ZipFile(apk_in) as z:
        infos = z.infolist()
        blobs = {i.filename: z.read(i.filename) for i in infos}
    data = bytearray(blobs[MANIFEST])
    found = find_min_sdk(bytes(data))
    if not found:
        print("minSdkVersion attribute: NOT FOUND")
        return 1
    old, typed_off, dtype = found
    print("patching minSdkVersion %d -> %d at offset %d" % (old, new_value, typed_off))
    struct.pack_into("<I", data, typed_off + 4, new_value)
    # sanity: re-parse
    found2 = find_min_sdk(bytes(data))
    if not found2 or found2[0] != new_value:
        print("patch verification failed:", found2)
        return 1
    blobs[MANIFEST] = bytes(data)

    with zipfile.ZipFile(apk_out, "w", allowZip64=True) as z:
        for info in infos:
            zi = zipfile.ZipInfo(info.filename, info.date_time)
            zi.compress_type = info.compress_type
            zi.external_attr = info.external_attr
            zi.create_system = info.create_system
            zi.internal_attr = info.internal_attr
            z.writestr(zi, blobs[info.filename])
    print("wrote", apk_out)
    return 0


def main(argv):
    if len(argv) == 3 and argv[1] == "inspect":
        return command_inspect(argv[2])
    if len(argv) == 5 and argv[1] == "patch":
        return command_patch(argv[2], argv[3], int(argv[4]))
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
