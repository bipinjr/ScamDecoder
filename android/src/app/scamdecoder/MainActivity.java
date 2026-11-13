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

import org.json.JSONObject;

/** The web app in a full-screen view, plus "Share to DECODR" from any app. */
public class MainActivity extends Activity {
    private WebView web;
    private String base;
    private String shared; // text waiting to be handed to the page
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
        open(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        open(i);
    }

    private void open(Intent i) {
        String text = i != null && Intent.ACTION_SEND.equals(i.getAction()) ? i.getStringExtra(Intent.EXTRA_TEXT) : null;
        shared = text != null && !text.trim().isEmpty() ? (text.length() > 4000 ? text.substring(0, 4000) : text) : null;
        web.loadUrl(base + "/share");
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
