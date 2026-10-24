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

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "rr_motor_reminder";

    private static final String PREFS = "RR_MOTOR_ALARM";
    private static final String TAG = "RR_MOTOR";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.MY_PACKAGE_REPLACED"
                .equals(action)) {

            pulihkanSemuaJadwal(context);
            return;
        }

        String id = ambil(intent, "id");

        if (id.isEmpty()) {
            Log.e(TAG, "ID reminder kosong.");
            return;
        }

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

        String isi = kosong(nama)
                ? "Saatnya mengingatkan pelanggan untuk ganti oli."
                : "Pelanggan: " + nama;

        if (!kosong(nopol)) {
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
                    context,
                    MainActivity.class
            );
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
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RR MOTOR - Reminder Ganti Oli")
                .setContentText(isi)
                .setStyle(new Notification.BigTextStyle().bigText(isi))
                .setContentIntent(klik)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();

        try {
            manager.notify(id.hashCode(), notification);

            Log.d(TAG, "Notifikasi ditampilkan. ID=" + id);

            // Status diperbarui hanya setelah permintaan tampil
            // notifikasi berhasil dijalankan.
            perbaruiStatusFirebase(id);

            // Jangan jadwalkan ulang alarm yang sudah berjalan.
            SharedPreferences prefs =
                    context.getSharedPreferences(
                            PREFS, Context.MODE_PRIVATE);

            prefs.edit()
                    .remove("reminder_" + id)
                    .remove("wa_" + id)
                    .putBoolean("done_" + id, true)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Gagal menampilkan notifikasi.", e);
        }
    }

    private static void perbaruiStatusFirebase(String id) {

        FirebaseFirestore.getInstance()
                .collection("reminders")
                .document(id)
                .update("status", "SUDAH DIINGATKAN")
                .addOnSuccessListener(unused ->
                        Log.d(TAG,
                                "Status SUDAH DIINGATKAN: " + id))
                .addOnFailureListener(error ->
                        Log.e(TAG,
                                "Gagal memperbarui status Firebase: " + id,
                                error));
    }

    public static boolean tampilkanNotifikasiTes(Context context) {

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null || !manager.areNotificationsEnabled()) {
            return false;
        }

        buatChannel(context);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
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
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("TES NOTIFIKASI RR MOTOR")
                .setContentText("Notifikasi HP berfungsi dengan baik.")
                .setContentIntent(klik)
                .setAutoCancel(true)
                .build();

        try {
            manager.notify(778899, notifikasi);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Gagal menampilkan notifikasi tes.", e);
            return false;
        }
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

        if (kosong(nama)) {
            pesan.append("Halo Bapak/Ibu.\n\n");
        } else {
            pesan.append("Halo Bapak/Ibu ")
                    .append(nama.trim())
                    .append(".\n\n");
        }

        if (kosong(interval)) {
            pesan.append(
                    "Kami mengingatkan untuk melakukan pengecekan " +
                    "atau penggantian oli.\n\n");
        } else {
            pesan.append("Sudah ")
                    .append(interval.toLowerCase())
                    .append(" sejak terakhir ganti oli di RR MOTOR.\n\n");
        }

        if (!kosong(tanggalInput)) {
            pesan.append("Tanggal terakhir ganti oli: ")
                    .append(tanggalInput)
                    .append("\n\n");
        }

        if (!kosong(km)) {
            try {
                long kmTerakhir = Long.parseLong(km.trim());
                long kmKembali;
                long kmMaksimal;

                if ("Motor ojek".equalsIgnoreCase(jenisMotor)) {
                    kmKembali =
