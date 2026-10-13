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
        String nama = intent.getStringExtra("nama");
        String nopol = intent.getStringExtra("nopol");
        String whatsapp = intent.getStringExtra("whatsapp");

        if (id == null || id.trim().isEmpty()) return;

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("done_" + id, false)) return;

        buatChannel(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Izin notifikasi belum diberikan.");
            return;
        }

        if (!manager.areNotificationsEnabled()) {
            Log.e(TAG, "Notifikasi aplikasi dinonaktifkan.");
            return;
        }

        // Ambil nomor WhatsApp dari data pelanggan.
        if (whatsapp == null || whatsapp.trim().isEmpty()) {
            whatsapp = prefs.getString("wa_" + id, "");
        }

        String nomor = normalisasiNomor(whatsapp);

        String pesanWA =
                "🏍️ RR MOTOR\n\n" +
                "Halo Bapak/Ibu.\n\n" +
                "Sudah 1 bulan sejak terakhir ganti oli di RR MOTOR.\n\n" +
                "Kami mengingatkan untuk melakukan pengecekan atau pergantian oli motor.\n\n" +
                "Pengecekan oli GRATIS.\n\n" +
                "Terima kasih telah mempercayakan perawatan motor Anda kepada RR MOTOR.";

        // Ketika notifikasi diketuk, buka chat WhatsApp pelanggan.
        Intent bukaWA;

        if (!nomor.isEmpty()) {
            String url = "https://wa.me/" + nomor
                    + "?text=" + Uri.encode(pesanWA);

            bukaWA = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url));
        } else {
            // Jika nomor tidak tersedia, buka aplikasi utama.
            bukaWA = new Intent(context, MainActivity.class);
        }

        bukaWA.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent klikNotifikasi = PendingIntent.getActivity(
                context,
                id.hashCode(),
                bukaWA,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        String isi = "Saatnya mengingatkan pelanggan untuk ganti oli.";

        if (nama != null && !nama.trim().isEmpty()) {
            isi = "Pelanggan: " + nama;
        }

        if (nopol != null && !nopol.trim().isEmpty()) {
            isi += " | Plat: " + nopol;
        }

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🏍️ RR MOTOR - Waktunya Ganti Oli")
                    .setContentText(isi)
                    .setStyle(new
