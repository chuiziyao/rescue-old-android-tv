package com.dsh.adbhelper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(48, 48, 48, 48);
        tv = new TextView(this);
        tv.setTextSize(20);
        tv.setText("本工具需要 root（SuperSU 授权）。\n点击按钮开启网络 ADB（5555 端口）。");
        Button btn = new Button(this);
        btn.setTextSize(22);
        btn.setText("一键开启 ADB (5555)");
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run();
            }
        });
        Button close = new Button(this);
        close.setTextSize(18);
        close.setText("关闭 ADB（折腾完用）");
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                runCmd("setprop service.adb.tcp.port -1; stop adbd; start adbd; echo closed");
            }
        });
        ll.addView(tv);
        ll.addView(btn);
        ll.addView(close);
        ScrollView sv = new ScrollView(this);
        sv.addView(ll);
        setContentView(sv);
    }

    private void run() {
        runCmd("setprop service.adb.tcp.port 5555; stop adbd; start adbd; "
                + "sleep 2; getprop service.adb.tcp.port; ip addr show wlan0 2>/dev/null | grep 'inet '");
    }

    private void runCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            p.waitFor();
            tv.setText("结果：\n" + sb.toString() + "\n（上面有 5555 就说明开了）");
        } catch (Exception e) {
            tv.setText("失败：" + e.toString() + "\n请确认已 root 并允许了 SuperSU 请求");
        }
    }
}
