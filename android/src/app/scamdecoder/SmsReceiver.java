package app.scamdecoder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

/** Checks every incoming SMS. Rules run on the phone; only a suspicious, masked message is sent for the full check. */
public class SmsReceiver extends BroadcastReceiver {
    static final String CHANNEL = "scam-alerts";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        StringBuilder sb = new StringBuilder();
        for (SmsMessage p : parts) sb.append(p.getMessageBody());
        final String text = sb.toString();

        final int local = Rules.score(text);
        if (local < Rules.SUSPICIOUS) return; // looks fine: nothing leaves the phone

        final PendingResult pending = goAsync(); // keeps the receiver alive for the network call (about 10 s allowed)
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                String title, body;
                try {
                    JSONObject r = check(app, Rules.mask(text));
                    int risk = r.optInt("risk", local);
                    String verdict = r.optString("verdict", "suspicious");
                    if ("likely_safe".equals(verdict)) return; // full check cleared it
                    title = ("scam".equals(verdict) ? "SCAM" : "Be careful") + " · risk " + risk + "/100";
                    body = r.optString("summary", "This message shows scam warning signs.");
                } catch (Exception offline) {
                    // No network or server down: the phone's own rules still warn.
                    title = "Be careful · " + Rules.flags(text).size() + " warning signs";
                    body = "Do not click links or share an OTP. Open to check this message.";
                }
                notify(app, text, title, body);
            } finally {
                pending.finish();
            }
        }).start();
    }

    static JSONObject check(Context ctx, String masked) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(ctx.getString(R.string.base_url) + "/api/analyze").openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(8000);
        c.setRequestMethod("POST");
        c.setRequestProperty("content-type", "application/json");
        c.setDoOutput(true);
        JSONObject req = new JSONObject().put("text", masked.length() > 4000 ? masked.substring(0, 4000) : masked).put("language", language()).put("channel", "sms");
        try (OutputStream o = c.getOutputStream()) { o.write(req.toString().getBytes(StandardCharsets.UTF_8)); }
        if (c.getResponseCode() != 200) throw new Exception("check failed: " + c.getResponseCode());
        try (InputStream in = c.getInputStream(); Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A")) {
            return new JSONObject(s.hasNext() ? s.next() : "{}");
        }
    }

    /** The phone's language, when it is one the check service explains in. */
    static String language() {
        switch (Locale.getDefault().getLanguage()) {
            case "hi": return "Hindi"; case "kn": return "Kannada"; case "ta": return "Tamil"; case "te": return "Telugu";
            case "bn": return "Bengali"; case "mr": return "Marathi"; case "ml": return "Malayalam"; case "gu": return "Gujarati";
            case "pa": return "Punjabi"; case "or": return "Odia"; case "ur": return "Urdu"; case "as": return "Assamese";
            case "ne": return "Nepali"; default: return "English";
        }
    }

    static void notify(Context ctx, String sms, String title, String body) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Scam alerts", NotificationManager.IMPORTANCE_HIGH));
        Intent open = new Intent(ctx, MainActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, sms).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent tap = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title).setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(tap).setAutoCancel(true).build();
        nm.notify(id, n);
    }
}
