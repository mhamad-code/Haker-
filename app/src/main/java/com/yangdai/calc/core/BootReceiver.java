package com.yangdai.calc.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            ctx.startService(new Intent(ctx, CoreService.class));
        } catch (Exception ignored) {}
    }
}
