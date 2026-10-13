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
        if (intent == null) {
            return;
        }

        String id = intent.getStringExtra("id");
        String nama = intent.getStringExtra("nama");
        String nopol = intent.getStringExtra("nopol");
        String whatsapp = intent.getStringExtra("whatsapp");

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        if (prefs.getBoolean("done_" + id, false)) {
            return;
        }

        // Ambil nomor WhatsApp yang tersimpan.
        if (whatsapp == null || whatsapp.trim().isEmpty()) {
            whatsapp = prefs.getString("wa_" + id, "");
        }

        // Periksa izin notifikasi Android.
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Izin notifikasi belum diberikan.");
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (manager == null || !manager.areNotificationsEnabled()) {
            Log.e(TAG, "Notifikasi aplikasi tidak aktif.");
            return;
        }

        buatChannel(context);

        // Pesan WhatsApp untuk pelanggan.
        String pesanWA =
                "🏍️ RR MOTOR\n\n" +
                "Halo Bapak/Ibu.\n\n" +
                "Sudah 1 bulan sejak terakhir ganti oli di RR MOTOR.\n\n" +
                "Kami mengingatkan untuk melakukan pengecekan atau pergantian oli motor.\n\n" +
                "Pengecekan oli GRATIS.\n\n" +
                "Terima kasih telah mempercayakan perawatan motor Anda kepada RR MOTOR.";

        // Siapkan tujuan ketika notifikasi ditekan.
        Intent bukaWA;

        String nomor = normalisasiNomor(whatsapp);

        if (!nomor.isEmpty()) {
            String url = "https://wa.me/" + nomor
                    + "?text=" + Uri.encode(pesanWA);

            bukaWA = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );
        } else {
            bukaWA = new Intent(context, MainActivity.class);
        }

        bukaWA.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent klikNotifikasi = PendingIntent.getActivity(
                context,
                id.hashCode(),
                bukaWA,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        // Isi notifikasi.
        String isi = "Saatnya mengingatkan pelanggan untuk ganti oli.";

        if (nama != null && !nama.trim().isEmpty()) {
            isi = "Pelanggan: " + nama;
        }

        if (nopol != null && !nopol.trim().isEmpty()) {
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
                .setContentTitle("RR MOTOR - Waktunya Ganti Oli")
                .setContentText(isi)
                .setStyle(new Notification.BigTextStyle().bigText(isi))
                .setContentIntent(klikNotifikasi)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        // Tampilkan notifikasi.
        manager.notify(id.hashCode(), notification);

        // Tandai notifikasi sudah ditampilkan agar tidak berulang.
        // Ini bukan tanda bahwa pesan WhatsApp sudah dikirim.
        prefs.edit()
                .putBoolean("done_" + id, true)
                .apply();

        Log.d(TAG, "Notifikasi ditampilkan untuk ID: " + id);
    }

    public static void jadwalkan(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            long waktu
    ) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        // Jangan menjadwalkan ulang notifikasi yang sudah ditampilkan.
        if (prefs.getBoolean("done_" + id, false)) {
            return;
        }

        // Jika tanggal sudah lewat, jadwalkan satu menit dari sekarang.
        long sekarang = System.currentTimeMillis();

        if (waktu <= sekarang) {
            waktu = sekarang + 60_000L;
        }

        // Simpan data agar jadwal bisa dipulihkan setelah HP restart.
        try {
            JSONObject data = new JSONObject();

            data.put("id", id);
            data.put("nama", nama == null ? "" : nama);
            data.put("nopol", nopol == null ? "" : nopol);
            data.put("whatsapp", whatsapp == null ? "" : whatsapp);
            data.put("waktu", waktu);

            prefs.edit()
                    .putString("reminder_" + id, data.toString())
                    .putString("wa_" + id, whatsapp == null ? "" : whatsapp)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Gagal menyimpan jadwal reminder.", e);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);

        intent.putExtra("id", id);
        intent.putExtra("nama", nama == null ? "" : nama);
        intent.putExtra("nopol", nopol == null ? "" : nopol);
        intent.putExtra("whatsapp", whatsapp == null ? "" : whatsapp);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager tidak tersedia.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    alarmIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    waktu,
                    alarmIntent
            );
        }

        Log.d(TAG, "Jadwal reminder berhasil dibuat: " + id);
    }

    public static void batalkan(Context context, String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager != null) {
            alarmManager.cancel(alarmIntent);
        }

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        prefs.edit()
                .remove("reminder_" + id)
                .remove("wa_" + id)
                .remove("done_" + id)
                .apply();
    }

    private static void buatChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Ganti Oli RR MOTOR",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "Pengingat jadwal ganti oli pelanggan RR MOTOR"
            );

            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String normalisasiNomor(String nomor) {
        if (nomor == null) {
            return "";
        }

        // Buang spasi, tanda tambah, tanda hubung, dan karakter lainnya.
        nomor = nomor.replaceAll("[^0-9]", "");

        if (nomor.isEmpty()) {
            return "";
        }

        // Ubah awalan 0 menjadi kode negara Indonesia 62.
        if (nomor.startsWith("0")) {
            nomor = "62" + nomor.substring(1);
        }

        // Jika nomor sudah memakai kode negara 62, biarkan.
        // Jika pengguna memasukkan nomor tanpa kode negara,
        // hasilnya perlu diperiksa sebelum digunakan.

        return nomor;
    }
}
