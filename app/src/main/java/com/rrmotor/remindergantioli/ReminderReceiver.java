package com.rrmotor.remindergantioli;

import android.Manifest;
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
import android.net.Uri;
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
        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) return;

        String nama = ambil(intent, "nama");
        String nopol = ambil(intent, "nopol");
        String whatsapp = ambil(intent, "whatsapp");
        String mesin = ambil(intent, "nomorMesin");
        String km = ambil(intent, "kmTerakhir");
        String interval = ambil(intent, "interval");
        String jenisMotor = ambil(intent, "jenisMotor");
        String tanggalInput = ambil(intent, "tanggalInput");
        String jatuhTempo = ambil(intent, "jatuhTempo");

        if (whatsapp.isEmpty()) {
            whatsapp = prefs.getString("wa_" + id, "");
        }

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Izin notifikasi belum diberikan.");
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null || !manager.areNotificationsEnabled()) {
            Log.e(TAG, "Notifikasi aplikasi tidak aktif.");
            return;
        }

        buatChannel(context);

        StringBuilder pesan = new StringBuilder();

        pesan.append("🏍️ RR MOTOR\n");
        pesan.append("REMINDER GANTI OLI\n\n");

        if (!nama.isEmpty()) {
            pesan.append("Halo Bapak/Ibu ")
                    .append(nama).append(".\n\n");
        } else {
            pesan.append("Halo Bapak/Ibu.\n\n");
        }

        pesan.append(
                "Kami mengingatkan bahwa sudah waktunya " +
                "melakukan pengecekan atau penggantian oli motor.\n\n");

        tambahBaris(pesan, "Nama pelanggan", nama);
        tambahBaris(pesan, "Nomor polisi", nopol);
        tambahBaris(pesan, "Nomor mesin", mesin);
        tambahBaris(pesan, "KM terakhir", km);
        tambahBaris(pesan, "Jenis motor", jenisMotor);
        tambahBaris(pesan, "Interval pengingat", interval);
        tambahBaris(pesan, "Tanggal input", tanggalInput);
        tambahBaris(pesan, "Jatuh tempo", jatuhTempo);

        pesan.append("\nPengecekan oli GRATIS.\n\n");
        pesan.append(
                "Terima kasih telah mempercayakan perawatan " +
                "motor Anda kepada RR MOTOR.");

        String nomor = normalisasiNomor(whatsapp);
        Intent bukaWA;

        if (!nomor.isEmpty()) {
            String url = "https://wa.me/" + nomor
                    + "?text=" + Uri.encode(pesan.toString());

            bukaWA = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        } else {
            bukaWA = new Intent(context, MainActivity.class);
        }

        bukaWA.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent klikNotifikasi = PendingIntent.getActivity(
                context,
                id.hashCode(),
                bukaWA,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        String isi = nama.isEmpty()
                ? "Saatnya mengingatkan pelanggan ganti oli."
                : "Pelanggan: " + nama;

        if (!nopol.isEmpty()) {
            isi += " | Plat: " + nopol;
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RR MOTOR - Reminder Ganti Oli")
                .setContentText(isi)
                .setStyle(new Notification.BigTextStyle().bigText(isi))
                .setContentIntent(klikNotifikasi)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        manager.notify(id.hashCode(), notification);

        // Notifikasi sudah ditampilkan.
        // Ini bukan tanda bahwa WhatsApp sudah terkirim.
        prefs.edit().putBoolean("done_" + id, true).apply();

        Log.d(TAG, "Notifikasi ditampilkan untuk ID: " + id);
    }

    private static String ambil(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private static void tambahBaris(
            StringBuilder pesan, String label, String nilai) {

        if (nilai != null
                && !nilai.trim().isEmpty()
                && !"-".equals(nilai.trim())) {
            pesan.append(label)
                    .append(": ")
                    .append(nilai.trim())
                    .append("\n");
        }
    }

    public static void jadwalkan(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            long waktu) {

        jadwalkan(
                context, id, nama, nopol, whatsapp,
                "", "", "", "", "", waktu);
    }

    public static void jadwalkan(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            String nomorMesin,
            String kmTerakhir,
            String interval,
            String jenisMotor,
            String tanggalInput,
            String jatuhTempo,
            long waktu) {

        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) return;

        if (waktu <= System.currentTimeMillis()) {
            waktu = System.currentTimeMillis() + 60_000L;
        }

        try {
            JSONObject data = new JSONObject();

            data.put("id", aman(id));
            data.put("nama", aman(nama));
            data.put("nopol", aman(nopol));
            data.put("whatsapp", aman(whatsapp));
            data.put("nomorMesin", aman(nomorMesin));
            data.put("kmTerakhir", aman(kmTerakhir));
            data.put("interval", aman(interval));
            data.put("jenisMotor", aman(jenisMotor));
            data.put("tanggalInput", aman(tanggalInput));
            data.put("jatuhTempo", aman(jatuhTempo));
            data.put("waktu", waktu);

            prefs.edit()
                    .putString("reminder_" + id, data.toString())
                    .putString("wa_" + id, aman(whatsapp))
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Gagal menyimpan jadwal.", e);
        }

        Intent alarmIntentAction =
                new Intent(context, ReminderReceiver.class);

        alarmIntentAction.putExtra("id", id);
        alarmIntentAction.putExtra("nama", aman(nama));
        alarmIntentAction.putExtra("nopol", aman(nopol));
        alarmIntentAction.putExtra("whatsapp", aman(whatsapp));
        alarmIntentAction.putExtra("nomorMesin", aman(nomorMesin));
        alarmIntentAction.putExtra("kmTerakhir", aman(kmTerakhir));
        alarmIntentAction.putExtra("interval", aman(interval));
        alarmIntentAction.putExtra("jenisMotor", aman(jenisMotor));
        alarmIntentAction.putExtra("tanggalInput", aman(tanggalInput));
        alarmIntentAction.putExtra("jatuhTempo", aman(jatuhTempo));

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                alarmIntentAction,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager tidak tersedia.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    alarmIntent);
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    alarmIntent);
        }

        Log.d(TAG, "Jadwal reminder dibuat: " + id);
    }

    public static void batalkan(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;

        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(alarmIntent);
        }

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        // Jangan menghapus done_ID di sini.
        // Status selesai perlu dikelola secara terpisah.
        prefs.edit()
                .remove("reminder_" + id)
                .remove("wa_" + id)
                .apply();

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.cancel(id.hashCode());
        }
    }

    public static void buatChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Ganti Oli RR MOTOR",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(
                    "Pengingat jadwal ganti oli pelanggan RR MOTOR");

            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String normalisasiNomor(String nomor) {
        if (nomor == null) return "";

        nomor = nomor.replaceAll("[^0-9]", "");

        if (nomor.isEmpty()) return "";

        if (nomor.startsWith("0")) {
            nomor = "62" + nomor.substring(1);
        }

        return nomor;
    }

    private static String aman(String teks) {
        return teks == null ? "" : teks.trim();
    }
}
