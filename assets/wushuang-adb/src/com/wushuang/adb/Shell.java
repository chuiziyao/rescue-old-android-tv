package com.wushuang.adb;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** 用 su 执行 shell 命令的小工具。 */
public final class Shell {
    private Shell() {}

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

    public static boolean hasRoot() {
        return su("id").contains("uid=0");
    }

    public static String enableAdb() {
        String a = su("setprop service.adb.tcp.port 5555; setprop service.adb.root 1; stop adbd; start adbd; sleep 1; getprop service.adb.tcp.port");
        return a;
    }

    public static String disableAdb() {
        return su("setprop service.adb.tcp.port -1; setprop service.adb.root 0; stop adbd; start adbd");
    }
}
