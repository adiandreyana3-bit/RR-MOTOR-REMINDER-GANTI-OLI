package com.rrmotor.remindergantioli;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "RR_MOTOR";
    private static final String PREFS = "RR_MOTOR_ALARM";

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

        Log.d(TAG, "Memulihkan jadwal reminder...");

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        Map<String, ?> semua = prefs.getAll();

        int jumlah = 0;

        for (Map.Entry<String, ?> entry : semua.entrySet()) {

            String key = entry.getKey();

            if (!key.startsWith("reminder_")) {
                continue;
            }

            if (!(entry.getValue() instanceof String)) {
                continue;
            }

            try {
                JSONObject data = new JSONObject(
                        (String) entry.getValue()
                );

                String id = data.optString("id", "");
                long waktu = data.optLong("waktu", 0L);

                if (id.isEmpty() || waktu <= 0L) {
                    continue;
                }

                if (prefs.getBoolean("done_" + id, false)) {
                    continue;
                }

                ReminderReceiver.jadwalkan(
                        context,
                        id,
                        data.optString("nama", ""),
                        data.optString("nopol", ""),
                        data.optString("whatsapp", ""),
                        data.optString("nomorMesin", ""),
                        data.optString("kmTerakhir", ""),
                        data.optString("interval", ""),
                        data.optString("jenisMotor", ""),
                        data.optString("tanggalInput", ""),
                        data.optString("jatuhTempo", ""),
                        waktu
                );

                jumlah++;

            } catch (Exception e) {
                Log.e(
                        TAG,
                        "Gagal memulihkan jadwal: " + key,
                        e
                );
            }
        }

        Log.d(TAG, "Jadwal berhasil diproses: " + jumlah);
    }
}
