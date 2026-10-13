package com.rrmotor.remindergantioli;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "rr_motor_reminder";
    private static final String PREFS = "RR_MOTOR_ALARM";
    private static final String TAG = "RR_MOTOR";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String id = intent.getStringExtra("id");
        String nama = intent.getStringExtra("nama");
        String nopol = intent.getStringExtra("nopol");

        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) return;

        // Buat channel terlebih dahulu.
        buatChannel(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.e(TAG, "NotificationManager tidak tersedia.");
            return;
        }

        // Jangan tandai selesai jika izin notifikasi belum diberikan.
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Izin POST_NOTIFICATIONS belum diberikan.");
            return;
        }

        if (!manager.areNotificationsEnabled()) {
            Log.e(TAG, "Notifikasi aplikasi sedang dinonaktifkan.");
            return;
        }

        String judul = "RR MOTOR - Waktunya Ganti Oli";
        String isi = "Pelanggan: "
                + (nama == null || nama.trim().isEmpty() ? "-" : nama);

        if (nopol != null && !nopol.trim().isEmpty()) {
            isi += "\nPlat motor: " + nopol;
        }

        Intent buka = new Intent(context, MainActivity.class);
        buka.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent bukaApp = PendingIntent.getActivity(
                context,
                id.hashCode(),
                buka,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(judul)
                    .setContentText(isi)
                    .setStyle(new Notification.BigTextStyle().bigText(isi))
                    .setContentIntent(bukaApp)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .build();
        } else {
            notification = new Notification.Builder(context)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(judul)
                    .setContentText(isi)
                    .setStyle(new Notification.BigTextStyle().bigText(isi))
                    .setContentIntent(bukaApp)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build();
        }

        try {
            manager.notify(id.hashCode(), notification);

            // Tandai selesai hanya setelah pemanggilan notify berhasil.
            // Jika izin tidak ada, kode di atas tidak akan mencapai bagian ini.
            prefs.edit().putBoolean("done_" + id, true).apply();

            // Pengingat sudah ditampilkan. Hapus payload alarm.
            prefs.edit().remove("reminder_" + id).apply();

            Log.i(TAG, "Notifikasi ditampilkan untuk ID: " + id);

        } catch (Exception e) {
            Log.e(TAG, "Gagal menampilkan notifikasi.", e);
        }
    }

    public static void buatChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Pengingat Ganti Oli RR MOTOR",
                        NotificationManager.IMPORTANCE_HIGH);

                channel.setDescription(
                        "Pengingat jatuh tempo ganti oli pelanggan.");

                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void jadwalkan(
            Context context,
            String id,
            String nama,
            String nopol,
            long waktu) {

        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) return;

        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("nama", nama == null ? "" : nama);
            data.put("nopol", nopol == null ? "" : nopol);
            data.put("waktu", waktu);

            prefs.edit()
                    .putString("reminder_" + id, data.toString())
                    .apply();

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.setAction("com.rrmotor.remindergantioli.REMINDER");
            intent.putExtra("id", id);
            intent.putExtra("nama", nama == null ? "" : nama);
            intent.putExtra("nopol", nopol == null ? "" : nopol);

            PendingIntent pending = PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE);

            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager tidak tersedia.");
                return;
            }

            long sekarang = System.currentTimeMillis();

            // Untuk data jatuh tempo lama, beri waktu satu menit.
            long waktuAlarm = waktu <= sekarang
                    ? sekarang + 60_000L
                    : waktu;

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktuAlarm,
                    pending);

            Log.i(TAG, "Alarm dijadwalkan untuk " + id
                    + " pada " + waktuAlarm);

        } catch (Exception e) {
            Log.e(TAG, "Gagal menjadwalkan reminder.", e);
        }
    }

    public static void batalkan(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.rrmotor.remindergantioli.REMINDER");

        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager manager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (manager != null) {
            manager.cancel(pending);
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove("reminder_" + id)
                .remove("done_" + id)
                .apply();
    }
}
