package com.wushuang.desktop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "home";
    private static final String KEY_TILES = "tiles";
    private static final String[] DEFAULT_TILES = new String[]{
            "com.synology.dsvideo",
            "com.gitvdemo.video",
            "com.ktcp.video",
            "com.xiaodianshi.tv.yst",
            "com.stv.t2.globalsetting",
            "com.wushuang.adb"
    };

    private float density;
    private LinearLayout gridBox;
    private final List<String> tiles = new ArrayList<String>();
    private final Handler handler = new Handler();
    private TextView clockView;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA);

    private final Runnable clockTick = new Runnable() {
        public void run() {
            updateClock();
            handler.postDelayed(this, 20000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        loadTiles();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.home_bg);
        root.setPadding(dp(44), dp(26), dp(44), dp(18));

        root.addView(buildHeader());

        gridBox = new LinearLayout(this);
        gridBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        gridBox.setLayoutParams(gp);
        root.addView(gridBox);

        TextView hint = new TextView(this);
        hint.setText("按遥控器 OK 打开应用 · 长按卡片可移除 · 点「＋」添加");
        hint.setTextColor(0xFF4A5866);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setPadding(0, dp(6), 0, 0);
        root.addView(hint);

        setContentView(root);
        rebuildGrid();
        handler.post(clockTick);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    private void updateClock() {
        if (clockView != null) {
            clockView.setText(timeFmt.format(new Date()));
        }
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("无双桌面");
        title.setTextColor(0xFF7B8A99);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tp);
        header.addView(title);

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);

        clockView = new TextView(this);
        clockView.setTextColor(0xFFF2F5F9);
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        clockView.setText(timeFmt.format(new Date()));
        clockBox.addView(clockView);

        TextView date = new TextView(this);
        date.setText(dateFmt.format(new Date()));
        date.setTextColor(0xFF6C7A89);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        date.setGravity(Gravity.END);
        clockBox.addView(date);

        header.addView(clockBox);
        return header;
    }

    private void rebuildGrid() {
        gridBox.removeAllViews();
        List<View> cards = new ArrayList<View>();
        for (String pkg : tiles) {
            if (isInstalled(pkg)) {
                cards.add(buildCard(pkg));
            }
        }
        cards.add(buildAddCard());

        int columns = 4;
        for (int i = 0; i < cards.size(); i += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            row.setLayoutParams(rp);
            for (int j = 0; j < columns; j++) {
                int idx = i + j;
                if (idx < cards.size()) {
                    row.addView(cards.get(idx));
                } else {
                    View spacer = new View(this);
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                    sp.setMargins(dp(8), dp(8), dp(8), dp(8));
                    spacer.setLayoutParams(sp);
                    row.addView(spacer);
                }
            }
            gridBox.addView(row);
        }

        final View first = cards.get(0);
        first.post(new Runnable() {
            public void run() {
                first.requestFocus();
            }
        });
    }

    private View buildCard(final String pkg) {
        LinearLayout card = baseCard();
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(64), dp(64));
        icon.setLayoutParams(ip);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        TextView label = new TextView(this);
        label.setTextColor(0xFFDCE6F2);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        label.setMaxLines(2);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpp.topMargin = dp(12);
        label.setLayoutParams(lpp);
        card.addView(icon);
        card.addView(label);

        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence l = pm.getApplicationLabel(ai);
            if (l != null && l.length() > 0) {
                label.setText(l.toString());
            }
            Drawable d = pm.getApplicationIcon(ai);
            if (d != null) {
                icon.setImageDrawable(d);
            } else {
                icon.setImageResource(android.R.drawable.ic_menu_manage);
            }
        } catch (Exception e) {
            label.setText(pkg);
            icon.setImageResource(android.R.drawable.ic_menu_manage);
        }

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launch(pkg);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                askRemove(pkg);
                return true;
            }
        });
        return card;
    }

    private View buildAddCard() {
        LinearLayout card = baseCard();
        TextView plus = new TextView(this);
        plus.setText("＋");
        plus.setTextColor(0xFF4C8DFF);
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
        plus.setGravity(Gravity.CENTER);
        TextView label = new TextView(this);
        label.setText("添加应用");
        label.setTextColor(0xFF8FA3B8);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpp.topMargin = dp(6);
        label.setLayoutParams(lpp);
        card.addView(plus);
        card.addView(label);
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAddDialog();
            }
        });
        return card;
    }

    private LinearLayout baseCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setBackgroundResource(R.drawable.card_bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(8), dp(8), dp(8), dp(8));
        card.setLayoutParams(lp);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                v.animate().scaleX(hasFocus ? 1.04f : 1f)
                        .scaleY(hasFocus ? 1.04f : 1f)
                        .setDuration(120).start();
            }
        });
        return card;
    }

    private void showAddDialog() {
        final List<String> pkgs = new ArrayList<String>();
        final List<String> labels = new ArrayList<String>();
        try {
            PackageManager pm = getPackageManager();
            Intent it = new Intent(Intent.ACTION_MAIN, null);
            it.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> ris = pm.queryIntentActivities(it, 0);
            Collections.sort(ris, new Comparator<ResolveInfo>() {
                public int compare(ResolveInfo a, ResolveInfo b) {
                    String la = a.loadLabel(getPackageManager()).toString();
                    String lb = b.loadLabel(getPackageManager()).toString();
                    return la.compareToIgnoreCase(lb);
                }
            });
            for (ResolveInfo ri : ris) {
                String p = ri.activityInfo.packageName;
                if (p == null || p.equals(getPackageName())) {
                    continue;
                }
                if (tiles.contains(p) || pkgs.contains(p)) {
                    continue;
                }
                pkgs.add(p);
                labels.add(ri.loadLabel(pm).toString());
            }
        } catch (Exception ignored) {
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "没有可添加的应用了", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = labels.toArray(new String[0]);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("添加应用到首页");
        b.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                String pkg = pkgs.get(which);
                if (!tiles.contains(pkg)) {
                    tiles.add(pkg);
                    saveTiles();
                    rebuildGrid();
                }
            }
        });
        b.setNegativeButton("取消", null);
        b.show();
    }

    private void askRemove(final String pkg) {
        String name = pkg;
        try {
            name = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) {
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("从首页移除");
        b.setMessage("「" + name + "」要从首页移除吗？（不会卸载）");
        b.setPositiveButton("移除", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
                tiles.remove(pkg);
                saveTiles();
                rebuildGrid();
            }
        });
        b.setNegativeButton("取消", null);
        b.show();
    }

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void launch(String pkg) {
        try {
            Intent it = getPackageManager().getLaunchIntentForPackage(pkg);
            if (it == null) {
                Toast.makeText(this, "应用未安装：" + pkg, Toast.LENGTH_SHORT).show();
                return;
            }
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
        } catch (Exception e) {
            Toast.makeText(this, "启动失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTiles() {        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = sp.getString(KEY_TILES, null);
        tiles.clear();
        if (saved == null) {
            Collections.addAll(tiles, DEFAULT_TILES);
        } else if (saved.length() > 0) {
            for (String p : saved.split(",")) {
                if (p.length() > 0) {
                    tiles.add(p);
                }
            }
        }
    }

    private void saveTiles() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putString(KEY_TILES, TextUtils.join(",", tiles)).commit();
    }

    @Override
    public void onBackPressed() {
        // Home app: swallow BACK.
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }
}
