package com.wushuang.adb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机自动启动局域网推送服务，并尝试自动开启 ADB。 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        try {
            context.startService(new Intent(context, PushService.class));
        } catch (Exception ignored) {
        }
        new Thread(new Runnable() {
            public void run() {
                if (Shell.hasRoot()) {
                    Shell.enableAdb();
                }
            }
        }).start();
    }
}
