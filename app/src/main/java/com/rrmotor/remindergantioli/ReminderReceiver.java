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

import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "rr_motor_reminder";

    private static final String PREFS = "RR_MOTOR_ALARM";
    private static final String TAG = "RR_MOTOR";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        // Pulihkan jadwal setelah HP dinyalakan kembali.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.MY_PACKAGE_REPLACED"
                .equals(action)) {

            pulihkanSemuaJadwal(context);
            return;
        }

        // Tampilkan notifikasi pengingat pelanggan.
        String id = ambil(intent, "id");

        if (id.isEmpty()) {
            Log.e(TAG, "ID reminder kosong.");
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS, Context.MODE_PRIVATE);

        tampilkanNotifikasi(
                context,
                id,
                ambil(intent, "nama"),
                ambil(intent, "nopol"),
                ambil(intent, "whatsapp"),
                ambil(intent, "nomorMesin"),
                ambil(intent, "kmTerakhir"),
                ambil(intent, "interval"),
                ambil(intent, "jenisMotor"),
                ambil(intent, "tanggalInput"),
                ambil(intent, "jatuhTempo")
        );
    }

    // =====================================================
    // NOTIFIKASI PELANGGAN
    // =====================================================

    private static void tampilkanNotifikasi(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            String nomorMesin,
            String km,
            String interval,
            String jenisMotor,
            String tanggalInput,
            String jatuhTempo) {

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Izin POST_NOTIFICATIONS belum diberikan.");
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.e(TAG, "NotificationManager tidak tersedia.");
            return;
        }

        if (!manager.areNotificationsEnabled()) {
            Log.e(TAG, "Notifikasi aplikasi dinonaktifkan.");
            return;
        }

        buatChannel(context);

        String isi = nama == null || nama.trim().isEmpty()
                ? "Saatnya mengingatkan pelanggan untuk ganti oli."
                : "Pelanggan: " + nama;

        if (nopol != null && !nopol.trim().isEmpty()) {
            isi += " | Plat: " + nopol;
        }

        String pesanWA = buatPesan(
                nama,
                nopol,
                nomorMesin,
                km,
                interval,
                jenisMotor,
                tanggalInput,
                jatuhTempo
        );

        String nomor = normalisasiNomor(whatsapp);

        Intent bukaAplikasi;

        if (!nomor.isEmpty()) {
            String url = "https://wa.me/"
                    + nomor
                    + "?text="
                    + Uri.encode(pesanWA);

            bukaAplikasi = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );
        } else {
            bukaAplikasi = new Intent(
                    context, MainActivity.class);
        }

        bukaAplikasi.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent klik = PendingIntent.getActivity(
                context,
                id.hashCode(),
                bukaAplikasi,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info)
                .setContentTitle(
                        "RR MOTOR - Reminder Ganti Oli")
                .setContentText(isi)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(isi))
                .setContentIntent(klik)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();

        manager.notify(id.hashCode(), notification);

        Log.d(TAG, "Notifikasi berhasil ditampilkan. ID=" + id);
    }

    // =====================================================
    // TES NOTIFIKASI
    // =====================================================

    public static boolean tampilkanNotifikasiTes(
            Context context) {

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Izin notifikasi tes belum diberikan.");
            return false;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null
                || !manager.areNotificationsEnabled()) {

            Log.e(TAG, "Notifikasi tes tidak dapat ditampilkan.");
            return false;
        }

        buatChannel(context);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Intent buka = new Intent(context, MainActivity.class);

        PendingIntent klik = PendingIntent.getActivity(
                context,
                778899,
                buka,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notifikasi = builder
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info)
                .setContentTitle("TES NOTIFIKASI RR MOTOR")
                .setContentText(
                        "Notifikasi HP berfungsi dengan baik.")
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(
                                        "Tes berhasil. " +
                                        "Selanjutnya kita periksa jadwal " +
                                        "reminder pelanggan."))
                .setContentIntent(klik)
                .setAutoCancel(true)
                .build();

        manager.notify(778899, notifikasi);

        Log.d(TAG, "Notifikasi tes berhasil dikirim.");
        return true;
    }

    // =====================================================
    // PESAN WHATSAPP
    // =====================================================

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

        if (kosong(nama)) {
            pesan.append("Halo Bapak/Ibu.\n\n");
        } else {
            pesan.append("Halo Bapak/Ibu ")
                    .append(nama.trim())
                    .append(".\n\n");
        }

        if (kosong(interval)) {
            pesan.append(
                    "Kami mengingatkan untuk melakukan " +
                    "pengecekan atau penggantian oli.\n\n");
        } else {
            pesan.append("Sudah ")
                    .append(interval.trim().toLowerCase())
                    .append(" sejak terakhir ganti oli di RR MOTOR.\n\n");

            pesan.append(
                    "Kami mengingatkan untuk melakukan " +
                    "pengecekan atau penggantian oli.\n\n");
        }

        // Informasi tambahan hanya dicantumkan jika diisi.
        tambahBaris(pesan, "Nomor polisi", nopol);
        tambahBaris(pesan, "Nomor mesin", mesin);

        if (!kosong(km)) {
            tambahBaris(pesan, "KM terakhir", km);
        }

        // Jenis motor, tanggal input dan jatuh tempo tidak
        // ditambahkan ke pesan pelanggan.

        pesan.append("\nPengecekan oli GRATIS.\n\n");

        pesan.append(
                "Terima kasih telah mempercayakan perawatan " +
                "motor Anda kepada *RR MOTOR*.");

        return pesan.toString();
    }

    private static void tambahBaris(
            StringBuilder pesan,
            String label,
            String nilai) {

        if (!kosong(nilai)) {
            pesan.append(label)
                    .append(": ")
                    .append(nilai.trim())
                    .append("\n");
        }
    }

    private static boolean kosong(String nilai) {
        return nilai == null
                || nilai.trim().isEmpty()
                || "-".equals(nilai.trim());
    }

    private static String ambil(
            Intent intent,
            String key) {

        String nilai = intent.getStringExtra(key);

        return nilai == null ? "" : nilai.trim();
    }

    private static String normalisasiNomor(String nomor) {

        if (nomor == null) {
            return "";
        }

        nomor = nomor.replaceAll("[^0-9]", "");

        if (nomor.startsWith("0")) {
            nomor = "62" + nomor.substring(1);
        }

        return nomor;
    }

    // =====================================================
    // MENJADWALKAN ALARM
    // =====================================================

    public static void jadwalkan(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            long waktu) {

        jadwalkan(
                context,
                id,
                nama,
                nopol,
                whatsapp,
                "", "", "", "", "", "", waktu
        );
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

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        // Jadwal yang sudah lewat dimunculkan sekitar 1 menit
        // setelah pemeriksaan jadwal.
        if (waktu <= System.currentTimeMillis()) {
            waktu = System.currentTimeMillis() + 60_000L;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS, Context.MODE_PRIVATE);

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
                    .remove("done_" + id)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Gagal menyimpan jadwal.", e);
            return;
        }

        buatAlarm(
                context,
                id,
                aman(nama),
                aman(nopol),
                aman(whatsapp),
                aman(nomorMesin),
                aman(kmTerakhir),
                aman(interval),
                aman(jenisMotor),
                aman(tanggalInput),
                aman(jatuhTempo),
                waktu
        );
    }

    private static void buatAlarm(
            Context context,
            String id,
            String nama,
            String nopol,
            String whatsapp,
            String mesin,
            String km,
            String interval,
            String jenisMotor,
            String tanggalInput,
            String jatuhTempo,
            long waktu) {

        Intent alarmAction = new Intent(
                context, ReminderReceiver.class);

        alarmAction.putExtra("id", id);
        alarmAction.putExtra("nama", nama);
        alarmAction.putExtra("nopol", nopol);
        alarmAction.putExtra("whatsapp", whatsapp);
        alarmAction.putExtra("nomorMesin", mesin);
        alarmAction.putExtra("kmTerakhir", km);
        alarmAction.putExtra("interval", interval);
        alarmAction.putExtra("jenisMotor", jenisMotor);
        alarmAction.putExtra("tanggalInput", tanggalInput);
        alarmAction.putExtra("jatuhTempo", jatuhTempo);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                alarmAction,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager tidak tersedia.");
            return;
        }

        try {
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

            Log.d(TAG, "Alarm dijadwalkan untuk " + id
                    + " pada " + waktu);

        } catch (SecurityException e) {
            Log.e(TAG, "Izin menjadwalkan alarm ditolak.", e);
        }
    }

    // =====================================================
    // PULIHKAN ALARM SETELAH HP MENYALA KEMBALI
    // =====================================================

    public static void pulihkanSemuaJadwal(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS, Context.MODE_PRIVATE);

        Map<String, ?> semua = prefs.getAll();

        int jumlah = 0;

        for (Map.Entry<String, ?> entry : semua.entrySet()) {

            String kunci = entry.getKey();

            if (!kunci.startsWith("reminder_")) {
                continue;
            }

            Object nilai = entry.getValue();

            if (!(nilai instanceof String)) {
                continue;
            }

            try {
                JSONObject data = new JSONObject((String) nilai);

                String id = data.optString("id", "");
                long waktu = data.optLong("waktu", 0L);

                if (id.isEmpty() || waktu <= 0L) {
                    continue;
                }

                buatAlarm(
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
                Log.e(TAG, "Gagal memulihkan jadwal: " + kunci, e);
            }
        }

        Log.d(TAG, "Jumlah jadwal yang dipulihkan: " + jumlah);
    }

    // =====================================================
    // MEMBATALKAN ALARM
    // =====================================================

    public static void batalkan(
            Context context,
            String id) {

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(
                context, ReminderReceiver.class);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(alarmIntent);
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
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

        Log.d(TAG, "Alarm dibatalkan: " + id);
    }

    // =====================================================
    // CHANNEL NOTIFIKASI
    // =====================================================

    public static void buatChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Reminder Ganti Oli RR MOTOR",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Pengingat jadwal ganti oli pelanggan RR MOTOR");

            channel.enableVibration(true);
            channel.setLockscreenVisibility(
                    Notification.VISIBILITY_PUBLIC);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String aman(String teks) {
        return teks == null ? "" : teks.trim();
    }
}
