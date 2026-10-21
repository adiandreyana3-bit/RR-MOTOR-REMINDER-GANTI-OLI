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

        String id = ambil(intent, "id");
        if (id.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

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

        String pesan = buatPesan(
                nama, nopol, mesin, km, interval, jenisMotor,
                tanggalInput, jatuhTempo);

        String nomor = normalisasiNomor(whatsapp);
        Intent bukaWA;

        if (!nomor.isEmpty()) {
            String url = "https://wa.me/" + nomor
                    + "?text=" + Uri.encode(pesan);

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

        // Menandai notifikasi telah ditampilkan.
        // Tidak berarti pesan WhatsApp telah terkirim.
        prefs.edit()
                .putBoolean("done_" + id, true)
                .apply();

        Log.d(TAG, "Notifikasi ditampilkan untuk ID: " + id);
    }

    private static String buatPesan(
            String nama,
            String nopol,
            String mesin,
            String km,
            String interval,
            String jenisMotor,
            String tanggalInput,
            String jatuhTempo) {

        StringBuilder pesan = new StringBuilder();

        pesan.append("🏍️ *RR MOTOR*\n\n");

        if (!kosong(nama)) {
            pesan.append("Halo Bapak/Ibu ")
                    .append(nama.trim())
                    .append(".\n\n");
        } else {
            pesan.append("Halo Bapak/Ibu.\n\n");
        }

        if (!kosong(interval)) {
            pesan.append("Sudah ")
                    .append(interval.trim().toLowerCase())
                    .append(" sejak terakhir ganti oli di RR MOTOR.\n\n");
        } else {
            pesan.append(
                    "Kami mengingatkan bahwa sudah waktunya "
                            + "melakukan pengecekan atau penggantian oli.\n\n");
        }

        tambahBaris(pesan, "Nomor polisi", nopol);
        tambahBaris(pesan, "Nomor mesin", mesin);
        tambahBaris(pesan, "KM terakhir", km);
        tambahBaris(pesan, "Jenis motor", jenisMotor);
        tambahBaris(pesan, "Tanggal input", tanggalInput);
        tambahBaris(pesan, "Jatuh tempo", jatuhTempo);

        pesan.append("\nSilakan melakukan pengecekan atau penggantian oli.\n\n");
        pesan.append("Pengecekan oli GRATIS.\n\n");
        pesan.append(
                "Terima kasih telah mempercayakan perawatan "
                        + "motor Anda kepada *RR MOTOR*.");

        return pesan.toString();
    }

    private static boolean kosong(String nilai) {
        return nilai == null
                || nilai.trim().isEmpty()
                || "-".equals(nilai.trim());
    }

    private static String ambil(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private static void tambahBaris(
            StringBuilder pesan, String label, String nilai) {

        if (!kosong(nilai)) {
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
                "", "", "", "", "", "", waktu);
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

        // Jadwal lewat: munculkan dalam sekitar 60 detik.
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
                    // Menghapus tanda selesai agar jadwal ini bisa aktif lagi.
                    .remove("done_" + id)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Gagal menyimpan jadwal.", e);
            return;
        }

        Intent alarmAction = new Intent(context, ReminderReceiver.class);
        alarmAction.putExtra("id", id);
        alarmAction.putExtra("nama", aman(nama));
        alarmAction.putExtra("nopol", aman(nopol));
        alarmAction.putExtra("whatsapp", aman(whatsapp));
        alarmAction.putExtra("nomorMesin", aman(nomorMesin));
        alarmAction.putExtra("kmTerakhir", aman(kmTerakhir));
        alarmAction.putExtra("interval", aman(interval));
        alarmAction.putExtra("jenisMotor", aman(jenisMotor));
        alarmAction.putExtra("tanggalInput", aman(tanggalInput));
        alarmAction.putExtra("jatuhTempo", aman(jatuhTempo));

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                alarmAction,
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
                    AlarmManager.RTC_WAKEUP, waktu, alarmIntent);
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP, waktu, alarmIntent);
        }

        Log.d(TAG, "Jadwal reminder dibuat: " + id
                + ", waktu: " + waktu);
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
