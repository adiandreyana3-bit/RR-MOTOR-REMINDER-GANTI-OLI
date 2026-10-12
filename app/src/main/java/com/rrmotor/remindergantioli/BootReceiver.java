package com.rrmotor.remindergantioli;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

import org.json.JSONObject;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        "RR_MOTOR_ALARM", Context.MODE_PRIVATE);

        Map<String, ?> semua = prefs.getAll();

        for (Map.Entry<String, ?> entry : semua.entrySet()) {

            String key = entry.getKey();

            if (!key.startsWith("reminder_")) {
                continue;
            }

            try {
                JSONObject data = new JSONObject(
                        String.valueOf(entry.getValue()));

                String id = data.getString("id");
                String nama = data.optString("nama", "");
                String nopol = data.optString("nopol", "");
                long waktu = data.optLong("waktu", 0L);

                if (prefs.getBoolean("done_" + id, false)) {
                    continue;
                }

                ReminderReceiver.jadwalkan(
                        context, id, nama, nopol, waktu);

            } catch (Exception e) {
                android.util.Log.e(
                        "RR_MOTOR",
                        "Gagal memulihkan jadwal reminder",
                        e);
            }
        }
    }
}
