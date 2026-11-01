package com.wushuang.adb;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 局域网推送服务：TV 上开一个 HTTP 服务，PC 浏览器上传 APK 并静默安装/卸载。
 * 需要 root（pm install / pm uninstall 走 su）。
 */
public class PushService extends Service {

    public static final int PORT = 8555;
    public static volatile boolean running = false;

    private ServerSocket server;
    private Thread acceptThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    public synchronized void startServer() {
        if (running) return;
        try {
            server = new ServerSocket(PORT);
            running = true;
            acceptThread = new Thread(new Runnable() {
                public void run() {
                    acceptLoop();
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (Exception e) {
            running = false;
        }
    }

    public synchronized void stopServer() {
        running = false;
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {
        }
        server = null;
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket s = server.accept();
                new Thread(new Runnable() {
                    public void run() {
                        handle(s);
                    }
                }).start();
            } catch (Exception e) {
                if (!running) break;
            }
        }
    }

    private void handle(Socket socket) {
        try {
            socket.setSoTimeout(30000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null) {
                socket.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "GET";
            String path = parts.length > 1 ? parts[1] : "/";

            Map<String, String> headers = new LinkedHashMap<String, String>();
            String line;
            while ((line = readLine(in)) != null && line.length() > 0) {
                int i = line.indexOf(':');
                if (i > 0) {
                    headers.put(line.substring(0, i).trim().toLowerCase(),
                            line.substring(i + 1).trim());
                }
            }

            byte[] body = new byte[0];
            String cl = headers.get("content-length");
            if (cl != null) {
                try {
                    body = readFully(in, Integer.parseInt(cl));
                } catch (Exception ignored) {
                }
            }

            String resp;
            if ("GET".equals(method) && ("/".equals(path) || path.startsWith("/?"))) {
                resp = "text/html; charset=utf-8|" + htmlPage();
            } else if ("GET".equals(method) && path.startsWith("/info")) {
                resp = "text/plain; charset=utf-8|"
                        + DeviceInfo.summary() + "\n"
                        + "推送地址: " + pushUrl() + "\n"
                        + "服务状态: " + (running ? "运行中" : "已停止") + "\n";
            } else if ("POST".equals(method) && path.startsWith("/upload")) {
                resp = "text/html; charset=utf-8|" + upload(headers, body);
            } else if ("POST".equals(method) && path.startsWith("/uninstall")) {
                String pkg = formValue(new String(body, "UTF-8"), "pkg");
                resp = "text/html; charset=utf-8|" + uninstall(pkg);
            } else {
                resp = "text/plain; charset=utf-8|404 Not Found";
            }

            int sep = resp.indexOf('|');
            String contentType = resp.substring(0, sep);
            byte[] payload = resp.substring(sep + 1).getBytes("UTF-8");
            String head = "HTTP/1.1 200 OK\r\nContent-Type: " + contentType
                    + "\r\nContent-Length: " + payload.length
                    + "\r\nConnection: close\r\n\r\n";
            out.write(head.getBytes("UTF-8"));
            out.write(payload);
            out.flush();
            socket.close();
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String upload(Map<String, String> headers, byte[] body) {
        try {
            String ct = headers.get("content-type");
            if (ct == null || ct.indexOf("boundary=") < 0) {
                return "<p>上传失败：不是 multipart 请求</p>" + backLink();
            }
            String boundary = ct.substring(ct.indexOf("boundary=") + 9).trim();
            if (boundary.startsWith("\"")) boundary = boundary.substring(1, boundary.length() - 1);
            byte[] delim = ("--" + boundary).getBytes("UTF-8");

            int first = indexOf(body, delim, 0);
            if (first < 0) return "<p>上传失败：找不到 multipart 边界</p>" + backLink();
            int partStart = first + delim.length;
            if (partStart + 1 < body.length && body[partStart] == '\r' && body[partStart + 1] == '\n') {
                partStart += 2;
            }
            int headerEnd = indexOf(body, "\r\n\r\n".getBytes("UTF-8"), partStart);
            if (headerEnd < 0) return "<p>上传失败：multipart 头不完整</p>" + backLink();
            String partHeaders = new String(body, partStart, headerEnd - partStart, "UTF-8");
            int dataStart = headerEnd + 4;
            int next = indexOf(body, ("\r\n" + "--" + boundary).getBytes("UTF-8"), dataStart);
            if (next < 0) next = body.length;

            String filename = "upload.apk";
            for (String h : partHeaders.split("\r\n")) {
                if (h.toLowerCase().startsWith("content-disposition")) {
                    int fi = h.indexOf("filename=\"");
                    if (fi >= 0) {
                        int fe = h.indexOf('"', fi + 10);
                        if (fe > fi) filename = h.substring(fi + 10, fe);
                    }
                }
            }
            filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!filename.toLowerCase().endsWith(".apk")) filename = filename + ".apk";

            File dir = new File(getFilesDir(), "upload");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(body, dataStart, next - dataStart);
            fos.close();

            String result = Shell.su("cp \"" + f.getAbsolutePath() + "\" /data/local/tmp/wushuang_upload.apk"
                    + " && chmod 644 /data/local/tmp/wushuang_upload.apk"
                    + " && pm install -r /data/local/tmp/wushuang_upload.apk");
            String ok = result.contains("Success") ? "✅ 安装成功" : "❌ 安装失败";
            return "<h3>" + ok + "</h3><pre>" + esc(result) + "</pre>"
                    + "<p>文件: " + esc(f.getAbsolutePath()) + "</p>" + backLink();
        } catch (Exception e) {
            return "<p>上传异常: " + esc(e.toString()) + "</p>" + backLink();
        }
    }

    private String uninstall(String pkg) {
        if (pkg == null || pkg.trim().length() == 0) {
            return "<p>缺少包名</p>" + backLink();
        }
        String result = Shell.su("pm uninstall " + pkg.trim());
        String ok = result.contains("Success") ? "✅ 卸载成功" : "❌ 卸载失败";
        return "<h3>" + ok + " " + esc(pkg) + "</h3><pre>" + esc(result) + "</pre>" + backLink();
    }

    private String htmlPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'>")
                .append("<title>无双ADB</title><style>")
                .append("body{background:#0e1216;color:#dce6f2;font-family:sans-serif;padding:24px;max-width:900px;margin:auto}")
                .append("h1{color:#4c8dff}pre{background:#121922;padding:12px;border-radius:8px;white-space:pre-wrap}")
                .append("input[type=file]{color:#dce6f2}button,a.btn{background:#4c8dff;color:#fff;border:0;padding:8px 16px;border-radius:6px;cursor:pointer;text-decoration:none}")
                .append("table{width:100%;border-collapse:collapse}td{padding:6px;border-bottom:1px solid #1e2a38}")
                .append("small{color:#7b8a99}</style></head><body>");
        sb.append("<h1>无双ADB · 局域网推送</h1>");
        sb.append("<pre>").append(esc(DeviceInfo.summary()))
                .append("局域网推送: ").append(esc(pushUrl())).append("</pre>");
        sb.append("<h3>安装 APK</h3><form action='/upload' method='post' enctype='multipart/form-data'>")
                .append("<input type='file' name='file1' accept='.apk' required> ")
                .append("<button type='submit'>上传并安装</button></form>");
        sb.append("<small>需要电视已 root；上传后自动静默安装。</small>");
        sb.append("<h3>已安装第三方应用</h3><table>");
        String list = Shell.su("pm list packages -3");
        for (String l : list.split("\n")) {
            if (!l.startsWith("package:")) continue;
            final String pkg = l.substring(8).trim();
            if (pkg.length() == 0) continue;
            sb.append("<tr><td>").append(esc(pkg)).append("</td><td style='text-align:right'>")
                    .append("<form action='/uninstall' method='post' style='display:inline'>")
                    .append("<input type='hidden' name='pkg' value='").append(esc(pkg)).append("'>")
                    .append("<button type='submit'>卸载</button></form></td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String pushUrl() {
        String ips = DeviceInfo.ipAddresses();
        String first = ips.replace("[", "").replace("]", "");
        int sp = first.indexOf(' ');
        if (sp > 0) first = first.substring(0, sp);
        return "http://" + first + ":" + PORT + "/";
    }

    private String backLink() {
        return "<p><a class='btn' href='/'>返回</a></p>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formValue(String body, String key) {
        for (String kv : body.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0 && key.equals(kv.substring(0, i))) {
                try {
                    return java.net.URLDecoder.decode(kv.substring(i + 1), "UTF-8");
                } catch (Exception e) {
                    return kv.substring(i + 1);
                }
            }
        }
        return "";
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') bos.write(c);
        }
        if (c == -1 && bos.size() == 0) return null;
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static byte[] readFully(InputStream in, int len) throws Exception {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) break;
            off += n;
        }
        return buf;
    }

    private static int indexOf(byte[] data, byte[] pat, int from) {
        outer:
        for (int i = from; i <= data.length - pat.length; i++) {
            for (int j = 0; j < pat.length; j++) {
                if (data[i + j] != pat[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
