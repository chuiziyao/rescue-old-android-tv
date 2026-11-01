package com.wushuang.adb;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/** 普通 shell / su 执行工具；读信息用普通 shell（不触发 SuperSU），只有开 ADB 等才用 su。 */
public final class Shell {
    private Shell() {}

    private static volatile Boolean rootCache = null;

    public static String exec(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            StringBuilder sb = new StringBuilder();
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = out.readLine()) != null) sb.append(line).append('\n');
            while ((line = err.readLine()) != null) sb.append(line).append('\n');
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "ERROR: " + e.toString();
        }
    }

    public static String su(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            StringBuilder sb = new StringBuilder();
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = out.readLine()) != null) sb.append(line).append('\n');
            while ((line = err.readLine()) != null) sb.append(line).append('\n');
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "ERROR: " + e.toString();
        }
    }

    /** 是否存在 su 二进制（不代表已授权，只是有可能 root）。 */
    public static boolean hasSuBinary() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/su/bin/su", "/system/sbin/su"};
        for (String p : paths) {
            try {
                if (new File(p).exists()) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean hasRoot() {
        Boolean c = rootCache;
        if (c != null) return c;
        boolean ok = su("id").contains("uid=0");
        rootCache = ok;
        return ok;
    }

    public static void setRootStatus(boolean ok) {
        rootCache = ok;
    }

    public static void resetRootStatus() {
        rootCache = null;
    }

    public static String enableAdb() {
        return su("setprop service.adb.tcp.port 5555; setprop service.adb.root 1; stop adbd; start adbd; sleep 1; getprop service.adb.tcp.port");
    }

    public static String disableAdb() {
        return su("setprop service.adb.tcp.port -1; setprop service.adb.root 0; stop adbd; start adbd");
    }
}
