package com.wushuang.adb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/** 读取 build.prop、内核、IP、root 状态。 */
public final class DeviceInfo {
    private DeviceInfo() {}

    public static String getProp(String key) {
        return Shell.su("getprop " + key).trim();
    }

    public static String ipAddresses() {
        List<String> out = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
                    if (ia instanceof Inet4Address && !ia.isLoopbackAddress()) {
                        out.add(ia.getHostAddress() + " (" + ni.getName() + ")");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (out.isEmpty()) out.add("未获取到 IP");
        return out.toString();
    }

    public static String kernel() {
        try {
            BufferedReader r = new BufferedReader(new FileReader("/proc/version"));
            String s = r.readLine();
            r.close();
            return s == null ? "" : s.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Android 版本 : ").append(getProp("ro.build.version.release")).append('\n');
        sb.append("API level    : ").append(getProp("ro.build.version.sdk")).append('\n');
        sb.append("型号         : ").append(getProp("ro.product.model")).append('\n');
        sb.append("平台         : ").append(getProp("ro.board.platform")).append(" / ").append(getProp("ro.product.device")).append('\n');
        sb.append("内核         : ").append(kernel()).append('\n');
        sb.append("IP           : ").append(ipAddresses()).append('\n');
        sb.append("Root         : ").append(Shell.hasRoot() ? "已授权" : "无（或未授权）").append('\n');
        sb.append("ADB 端口     : ").append(getProp("service.adb.tcp.port")).append('\n');
        return sb.toString();
    }
}
