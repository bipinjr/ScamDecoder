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
    private Uri waitingImage; // a shared photo we could not open yet: read again once photo access is allowed

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        base = getString(R.string.base_url);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setTextZoom(100); // the page sizes its own text; the phone's font scale on top made buttons spill and crowd
        web.getSettings().setOffscreenPreRaster(true); // paint tiles just off screen too, so a fling never scrolls into blank or half-drawn content
        web.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
        if (Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        web.setBackgroundColor(0xff050505);
        getWindow().getDecorView().setBackgroundColor(0xff050505);
        // Android 15 draws apps edge to edge: keep the page clear of the status bar, the gesture bar and the keyboard.
        android.widget.FrameLayout root = new android.widget.FrameLayout(this); // padding goes on a frame: a WebView draws its page under its own padding
        root.setBackgroundColor(0xff050505);
        root.addView(web);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets s = ins.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime() | android.view.WindowInsets.Type.displayCutout());
                v.setPadding(s.left, s.top, s.right, s.bottom);
            } else v.setPadding(ins.getSystemWindowInsetLeft(), ins.getSystemWindowInsetTop(), ins.getSystemWindowInsetRight(), ins.getSystemWindowInsetBottom());
            return ins;
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (r.getUrl().toString().startsWith(base)) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); // tel:1930, WhatsApp share, outside links
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                android.util.Log.i("DECODR", "page finished: " + url + " toCheck=" + (toCheck != null) + " shared=" + (shared != null));
                if (toCheck != null) {
                    // Hand the text over and signal the page; reloading here raced the page's own start-up and lost the check.
                    String js = "sessionStorage.setItem('scamdecoder-prefill'," + JSONObject.quote(toCheck) + ");dispatchEvent(new Event('decodr-prefill'));";
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
            public boolean onConsoleMessage(android.webkit.ConsoleMessage m) {
                android.util.Log.i("DECODR", "page console: " + m.message() + " @" + m.lineNumber());
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePick != null) filePick.onReceiveValue(null);
                filePick = cb;
                try { startActivityForResult(p.createIntent(), 1); } catch (Exception e) { filePick = null; return false; }
                return true;
            }
        });
        setContentView(root);
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
        if (i != null && (Intent.ACTION_SEND.equals(i.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction()))
                && i.getType() != null && i.getType().startsWith("image/")) {
            Uri img = sharedImage(i);
            android.util.Log.i("DECODR", "shared image: " + img);
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

    /** Gallery apps put the image in EXTRA_STREAM, in ClipData, or both; take the first one found. */
    private static Uri sharedImage(Intent i) {
        Object extra = i.getExtras() == null ? null : i.getExtras().get(Intent.EXTRA_STREAM);
        if (extra instanceof Uri) return (Uri) extra;
        if (extra instanceof java.util.List && !((java.util.List<?>) extra).isEmpty() && ((java.util.List<?>) extra).get(0) instanceof Uri)
            return (Uri) ((java.util.List<?>) extra).get(0);
        if (i.getClipData() != null && i.getClipData().getItemCount() > 0) return i.getClipData().getItemAt(0).getUri();
        return null;
    }

    /** A shared screenshot: shrink it on the phone, have the server read its text, then check that text as a message. */
    private void readImage(Uri img) {
        web.loadDataWithBaseURL(base, "<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>"
            + "<body style='margin:0;min-height:100vh;display:grid;place-items:center;background:#050505;color:#f5f5f5;font:600 22px system-ui;text-align:center'>"
            + "<div><div style='font-size:15px;color:#d7dbe0;letter-spacing:.08em'>DECODR</div><p>Reading your screenshot…</p>"
            + "<p style='font:400 15px system-ui;color:#9a9a9a'>The verdict appears in a few seconds.</p></div>", "text/html", "utf-8", null);
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
                    android.util.Log.i("DECODR", "read-image HTTP " + c.getResponseCode() + " text chars=" + (text == null ? -1 : text.length()) + " error=" + (text == null ? error : "-"));
                }
            } catch (SecurityException e) {
                android.util.Log.e("DECODR", "no access to shared image", e);
                runOnUiThread(() -> askPhotoAccess(img));
                return;
            } catch (Exception e) {
                android.util.Log.e("DECODR", "reading shared image failed", e);
                error = "No internet connection, or the image could not be opened.";
            }
            final String t = text, err = error;
            runOnUiThread(() -> {
                // Could not read it: say why, and open the check page so the screenshot can be picked there instead.
                if (t == null) { Toast.makeText(this, err + " Pick the screenshot here instead.", Toast.LENGTH_LONG).show(); web.loadUrl(base + "/check"); return; }
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

    /** The gallery shared a photo without granting access: ask for photo access once, then read it again. */
    private void askPhotoAccess(Uri img) {
        String perm = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Could not open that photo. Pick the screenshot here instead.", Toast.LENGTH_LONG).show();
            web.loadUrl(base + "/check");
            return;
        }
        waitingImage = img;
        requestPermissions(new String[] { perm }, 3);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (req != 3 || waitingImage == null) return;
        Uri img = waitingImage;
        waitingImage = null;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) readImage(img);
        else { Toast.makeText(this, "Allow photo access to check shared screenshots, or pick it here.", Toast.LENGTH_LONG).show(); web.loadUrl(base + "/check"); }
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
