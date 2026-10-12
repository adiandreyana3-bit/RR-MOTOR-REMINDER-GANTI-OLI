package com.rrmotor.remindergantioli;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "rr_motor_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        String nama = intent.getStringExtra("nama");
        String nopol = intent.getStringExtra("nopol");

        if (id == null) return;

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        "RR_MOTOR_ALARM", Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) {
            return;
        }

        String judul = "🏍️ RR MOTOR - Waktunya Ganti Oli";
        String isi = "Pelanggan: " +
                (nama == null || nama.isEmpty() ? "-" : nama);

        if (nopol != null && !nopol.isEmpty()) {
            isi += "\nPlat motor: " + nopol;
        }

        buatChannel(context);

        Intent buka = new Intent(context, MainActivity.class);
        buka.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent bukaApp = PendingIntent.getActivity(
                context,
                id.hashCode(),
                buka,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(judul)
                .setContentText(isi)
                .setStyle(new Notification.BigTextStyle().bigText(isi))
                .setContentIntent(bukaApp)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(id.hashCode(), notification);
        }

        prefs.edit().putBoolean("done_" + id, true).apply();
    }

    public static void buatChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Ganti Oli RR MOTOR",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(
                    "Notifikasi pengingat ganti oli pelanggan RR MOTOR");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
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

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        "RR_MOTOR_ALARM", Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) {
            return;
        }

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
            intent.putExtra("id", id);
            intent.putExtra("nama", nama);
            intent.putExtra("nopol", nopol);

            PendingIntent pending = PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE);

            if (alarmManager != null) {
                long sekarang = System.currentTimeMillis();

                // Jika sudah lewat, jadwalkan segera.
                long waktuAlarm = waktu <= sekarang
                        ? sekarang + 60_000L
                        : waktu;

                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        waktuAlarm,
                        pending);
            }

        } catch (Exception e) {
            android.util.Log.e(
                    "RR_MOTOR", "Gagal menjadwalkan reminder", e);
        }
    }

    public static void batalkan(Context context, String id) {
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        AlarmManager manager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (manager != null) {
            manager.cancel(pending);
        }

        context.getSharedPreferences(
                        "RR_MOTOR_ALARM", Context.MODE_PRIVATE)
                .edit()
                .remove("reminder_" + id)
                .remove("done_" + id)
                .apply();
    }
}
