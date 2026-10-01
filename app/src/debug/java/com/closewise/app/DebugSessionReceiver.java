package com.closewise.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Collections;

public final class DebugSessionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String target = intent.getStringExtra("target");
        if (target != null && !target.isEmpty()) {
            ClosingSession.begin(context, Collections.singletonList(target));
        }
    }
}
