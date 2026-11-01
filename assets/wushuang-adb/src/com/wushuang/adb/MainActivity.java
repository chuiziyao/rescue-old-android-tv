package com.wushuang.adb;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 无双ADB：设备信息 + 一键开ADB + 局域网推送安装/卸载。 */
public class MainActivity extends Activity {

    private TextView info;
    private TextView status;
    private Button pushBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(14));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("无双ADB");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("老电视自救工具：看信息 / 开ADB / 局域网装包卸载");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        sub.setPadding(0, 0, 0, dp(12));
        root.addView(sub);

        info = new TextView(this);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        info.setText("正在读取信息...（首次会请求 root）");
        info.setPadding(0, dp(8), 0, dp(12));
        root.addView(info);

        LinearLayout row1 = row();
        row1.addView(btn("刷新信息", new View.OnClickListener() {
            public void onClick(View v) {
                refresh();
            }
        }));
        row1.addView(btn("一键开启 ADB", new View.OnClickListener() {
            public void onClick(View v) {
                String r = Shell.enableAdb();
                setStatus("ADB 开启命令已执行:\n" + r);
                refresh();
            }
        }));
        row1.addView(btn("关闭 ADB", new View.OnClickListener() {
            public void onClick(View v) {
                String r = Shell.disableAdb();
                setStatus("ADB 关闭命令已执行:\n" + r);
                refresh();
            }
        }));
        root.addView(row1);

        LinearLayout row2 = row();
        pushBtn = btn("", new View.OnClickListener() {
            public void onClick(View v) {
                startService(new Intent(MainActivity.this, PushService.class));
                try {
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
                refresh();
            }
        });
        row2.addView(pushBtn);
        row2.addView(btn("停止推送", new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, PushService.class));
                refresh();
            }
        }));
        row2.addView(btn("复制推送地址", new View.OnClickListener() {
            public void onClick(View v) {
                String url = pushUrl();
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("url", url));
                    Toast.makeText(MainActivity.this, "已复制: " + url, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, url, Toast.LENGTH_LONG).show();
                }
            }
        }));
        root.addView(row2);

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        status.setPadding(0, dp(12), 0, 0);
        root.addView(status);

        TextView tip = new TextView(this);
        tip.setText("\n提示：\n"
                + "1. 首次运行会请求 root（SuperSU 点允许），并自动尝试开启 ADB(5555)。\n"
                + "2. 推送服务无密码，只在自家局域网用，用完请点“停止推送”。\n"
                + "3. 没有 root 时只能查看信息，安装/卸载会失败。\n"
                + "4. 重启后推送服务会自动启动，但 ADB 需要再点一次“一键开启 ADB”。");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tip.setPadding(0, dp(16), 0, 0);
        root.addView(tip);

        setContentView(scroll);

        // 首次进入自动启动推送服务 + 尝试开 ADB
        startService(new Intent(this, PushService.class));
        new Thread(new Runnable() {
            public void run() {
                if (Shell.hasRoot()) {
                    Shell.enableAdb();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        refresh();
                    }
                });
            }
        }).start();

        refresh();
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(6), 0, dp(6));
        return r;
    }

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void refresh() {
        new Thread(new Runnable() {
            public void run() {
                final String s = DeviceInfo.summary();
                final boolean run = PushService.running;
                final String url = pushUrl();
                runOnUiThread(new Runnable() {
                    public void run() {
                        info.setText(s);
                        pushBtn.setText(run ? "推送已开启" : "开启推送");
                        status.setText("推送服务: " + (run ? "运行中" : "已停止")
                                + "\n推送地址: " + (run ? url : "（先点“开启推送”）"));
                    }
                });
            }
        }).start();
    }

    private String pushUrl() {
        String ips = DeviceInfo.ipAddresses().replace("[", "").replace("]", "");
        int sp = ips.indexOf(' ');
        String ip = sp > 0 ? ips.substring(0, sp) : ips;
        return "http://" + ip + ":" + PushService.PORT + "/";
    }

    private void setStatus(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                status.setText(text);
            }
        });
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
