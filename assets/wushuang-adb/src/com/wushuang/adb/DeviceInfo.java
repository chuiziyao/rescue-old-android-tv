package com.wushuang.adb;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 设备信息读取。读信息走普通 shell（getprop/cat 都不需要 root，不触发 SuperSU）；
 * 只有 enableAdb=true 时才用一次 su（顺便完成开 ADB）。
 */
public final class DeviceInfo {
    private DeviceInfo() {}

    private static String cachedSummary = "";
    private static long cachedAt = 0;

    public static synchronized void refresh() {
        refresh(false);
    }

    public static synchronized void refresh(boolean enableAdb) {
        String info = ""
                + "echo release=$(getprop ro.build.version.release); "
                + "echo sdk=$(getprop ro.build.version.sdk); "
                + "echo model=$(getprop ro.product.model); "
                + "echo platform=$(getprop ro.board.platform); "
                + "echo device=$(getprop ro.product.device); "
                + "echo kernel=$(cat /proc/version); "
                + "echo id=$(id); "
                + "echo adbport=$(getprop service.adb.tcp.port)";

        String out;
        boolean suUsed = false;
        if (enableAdb && Shell.hasSuBinary()) {
            suUsed = true;
            out = Shell.su("setprop service.adb.tcp.port 5555; setprop service.adb.root 1; "
                    + "stop adbd; start adbd; sleep 1; " + info);
        } else {
            out = Shell.exec(info);
        }

        String release = "", sdk = "", model = "", platform = "", device = "";
        String kernel = "", id = "", adbport = "";
        String[] lines = out.split("\n");
        for (String l : lines) {
            int i = l.indexOf('=');
            if (i <= 0) continue;
            String k = l.substring(0, i).trim(), v = l.substring(i + 1).trim();
            if (k.equals("release")) release = v;
            else if (k.equals("sdk")) sdk = v;
            else if (k.equals("model")) model = v;
            else if (k.equals("platform")) platform = v;
            else if (k.equals("device")) device = v;
            else if (k.equals("kernel")) kernel = v;
            else if (k.equals("id")) id = v;
            else if (k.equals("adbport")) adbport = v;
        }
        if (suUsed) Shell.setRootStatus(id.contains("uid=0"));

        String rootText;
        if (suUsed) {
            boolean r = id.contains("uid=0");
            Shell.setRootStatus(r);
            rootText = r ? "已授权" : "su 存在但被拒绝";
        } else if (Shell.hasSuBinary()) {
            rootText = "有 su（开 ADB 时授权）";
        } else {
            rootText = "无 root";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Android 版本 : ").append(release).append('\n');
        sb.append("API level    : ").append(sdk).append('\n');
        sb.append("型号         : ").append(model).append('\n');
        sb.append("平台         : ").append(platform).append(" / ").append(device).append('\n');
        sb.append("内核         : ").append(kernel).append('\n');
        sb.append("IP           : ").append(ipAddresses()).append('\n');
        sb.append("Root         : ").append(rootText).append('\n');
        sb.append("ADB 端口     : ").append(adbport).append('\n');
        cachedSummary = sb.toString();
        cachedAt = System.currentTimeMillis();
    }

    public static synchronized String summary() {
        if (cachedSummary.length() == 0) refresh();
        return cachedSummary;
    }

    public static boolean isStale(long ms) {
        return cachedSummary.length() == 0 || System.currentTimeMillis() - cachedAt > ms;
    }

    public static String getProp(String key) {
        return Shell.exec("getprop " + key).trim();
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
        return Shell.exec("cat /proc/version").trim();
    }
}
