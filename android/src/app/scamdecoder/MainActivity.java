package app.scamdecoder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

/** The web app in a full-screen view, plus "Share to DECODR" from any app. */
public class MainActivity extends Activity {
    private WebView web;
    private String base;
    private String shared; // text waiting to be handed to the page
    private String toCheck; // text read from a shared screenshot, checked as a message
    private ValueCallback<Uri[]> filePick;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        base = getString(R.string.base_url);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (r.getUrl().toString().startsWith(base)) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); // tel:1930, WhatsApp share, outside links
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                if (toCheck != null) {
                    String js = "sessionStorage.setItem('scamdecoder-prefill'," + JSONObject.quote(toCheck) + ");location.replace('/check');";
                    toCheck = null;
                    v.evaluateJavascript(js, null);
                    return;
                }
                if (shared == null) return;
                // Hand the text over through sessionStorage, never the URL, so it cannot end up in server logs.
                String js = "sessionStorage.setItem('scamdecoder-share'," + JSONObject.quote(shared) + ");location.replace('/share?shared=2');";
                shared = null;
                v.evaluateJavascript(js, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePick != null) filePick.onReceiveValue(null);
                filePick = cb;
                try { startActivityForResult(p.createIntent(), 1); } catch (Exception e) { filePick = null; return false; }
                return true;
            }
        });
        setContentView(web);
        askPermissions();
        rememberBrowser();
        open(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        open(i);
    }

    private void open(Intent i) {
        if (i != null && Intent.ACTION_SEND.equals(i.getAction()) && i.getType() != null && i.getType().startsWith("image/")) {
            Uri img = i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (img != null) { readImage(img); return; }
        }
        String text = null;
        if (i != null && Intent.ACTION_SEND.equals(i.getAction())) text = i.getStringExtra(Intent.EXTRA_TEXT);
        else if (i != null && Intent.ACTION_PROCESS_TEXT.equals(i.getAction())) {
            CharSequence picked = i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            text = picked == null ? null : picked.toString();
        }
        shared = text != null && !text.trim().isEmpty() ? (text.length() > 4000 ? text.substring(0, 4000) : text) : null;
        web.loadUrl(base + "/share");
    }

    /** A shared screenshot: shrink it on the phone, have the server read its text, then check that text as a message. */
    private void readImage(Uri img) {
        web.loadUrl(base + "/share?reading=1");
        Toast.makeText(this, "Reading the screenshot…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String text = null, error = "Could not read that image.";
            try {
                byte[] jpeg = shrink(img);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(base + "/api/read-image").openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(30000); c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("content-type", "image/jpeg");
                try (java.io.OutputStream o = c.getOutputStream()) { o.write(jpeg); }
                java.io.InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
                try (java.util.Scanner s = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    JSONObject j = new JSONObject(s.hasNext() ? s.next() : "{}");
                    if (j.has("text")) text = j.getString("text"); else error = j.optString("error", error);
                }
            } catch (Exception e) {
                error = "No internet connection, or the image could not be opened.";
            }
            final String t = text, err = error;
            runOnUiThread(() -> {
                if (t == null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
                toCheck = t;
                web.loadUrl(base + "/check");
            });
        }).start();
    }

    /** Longest side at most 1600 px, JPEG: keeps uploads small and well under the server limit. */
    private byte[] shrink(Uri img) throws Exception {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (java.io.InputStream in = getContentResolver().openInputStream(img)) { android.graphics.BitmapFactory.decodeStream(in, null, o); }
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > 1600) sample *= 2;
        o = new android.graphics.BitmapFactory.Options();
        o.inSampleSize = sample;
        android.graphics.Bitmap bmp;
        try (java.io.InputStream in = getContentResolver().openInputStream(img)) { bmp = android.graphics.BitmapFactory.decodeStream(in, null, o); }
        if (bmp == null) throw new Exception("not an image");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out);
        return out.toByteArray();
    }

    /** While DECODR is not yet the default browser, note which app is, so the link guard can hand safe links back to it. */
    private void rememberBrowser() {
        android.content.pm.ResolveInfo r = getPackageManager().resolveActivity(
            new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE), PackageManager.MATCH_DEFAULT_ONLY);
        String p = r == null || r.activityInfo == null ? null : r.activityInfo.packageName;
        if (p != null && !p.equals(getPackageName()) && !p.equals("android"))
            getSharedPreferences("decodr", MODE_PRIVATE).edit().putString("browser", p).apply();
    }

    private void askPermissions() {
        java.util.List<String> need = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 2);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req != 1 || filePick == null) return;
        filePick.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
        filePick = null;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
