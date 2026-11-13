package app.scamdecoder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * DECODR as the phone's browser: every link tapped in SMS, WhatsApp, email or any app comes here first.
 * Safe links go straight on to a real browser; risky ones are stopped with the reasons, like the browser extension.
 */
public class LinkActivity extends Activity {
    private static final int BG = 0xFF070A14, CARD = 0xFF0E1426, INK = 0xFFF2F5FF, SOFT = 0xFF9AA6C7, RED = 0xFFFF5364, TEAL = 0xFF22E6C3;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Uri uri = getIntent() == null ? null : getIntent().getData();
        if (uri == null) { finish(); return; }
        List<LinkGuard.Reason> reasons = LinkGuard.reasons(uri.toString());
        if (!LinkGuard.shouldBlock(reasons)) { openInBrowser(uri); return; }
        showWarning(uri, reasons);
    }

    /** Hand the link to a real browser, never back to ourselves (that would loop). */
    private void openInBrowser(Uri uri) {
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE);
        List<ResolveInfo> browsers = getPackageManager().queryIntentActivities(probe, PackageManager.MATCH_ALL);
        // Prefer the browser the user had before DECODR took over, then Chrome, then any other.
        String saved = getSharedPreferences("decodr", MODE_PRIVATE).getString("browser", null), pick = null, chrome = null;
        for (ResolveInfo r : browsers) {
            String p = r.activityInfo.packageName;
            if (p.equals(getPackageName())) continue;
            if (p.equals(saved)) { pick = p; break; }
            if (p.equals("com.android.chrome")) chrome = p;
            else if (pick == null) pick = p;
        }
        if (!java.util.Objects.equals(pick, saved) && chrome != null) pick = chrome;
        if (pick == null) {
            Toast.makeText(this, "No browser found to open this link.", Toast.LENGTH_LONG).show();
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, uri).setPackage(pick).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
    }

    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    private TextView text(String s, int sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sizeSp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    private Button button(String s, int bg, int fg) {
        Button btn = new Button(this);
        btn.setText(s); btn.setAllCaps(false); btn.setTextSize(17); btn.setTextColor(fg);
        GradientDrawable d = new GradientDrawable(); d.setColor(bg); d.setCornerRadius(dp(14));
        if (bg == Color.TRANSPARENT) d.setStroke(dp(1), SOFT);
        btn.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lp.topMargin = dp(12);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void showWarning(Uri uri, List<LinkGuard.Reason> reasons) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(48), dp(24), dp(32));
        col.setBackgroundColor(BG);

        col.addView(text("DECODR", 16, TEAL, true));
        col.addView(text("We stopped this link. It looks like a scam.", 28, INK, true));
        col.addView(text("Pages like this steal passwords, OTPs and UPI PINs. Nothing from it has opened.", 16, SOFT, false));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable cd = new GradientDrawable(); cd.setColor(CARD); cd.setCornerRadius(dp(16)); cd.setStroke(dp(1), 0x55FF5364);
        card.setBackground(cd);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(18);
        card.setLayoutParams(clp);
        card.addView(text("The address", 13, SOFT, false));
        TextView host = text(uri.getHost() == null ? uri.toString() : uri.getHost(), 20, INK, true);
        host.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        card.addView(host);
        card.addView(text("Risk " + LinkGuard.score(reasons) + " / 100", 15, RED, true));
        card.addView(text("Why we stopped it", 13, SOFT, false));
        for (LinkGuard.Reason r : reasons) card.addView(text("•  " + r.label, 16, INK, false));
        col.addView(card);

        Button back = button("Go back to safety", TEAL, BG);
        back.setOnClickListener(v -> finish());
        col.addView(back);

        Button check = button("Check it in DECODR", Color.TRANSPARENT, INK);
        check.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, uri.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
        col.addView(check);

        Button open = button("I know this site, open it anyway", Color.TRANSPARENT, RED);
        open.setOnClickListener(v -> openInBrowser(uri));
        col.addView(open);

        TextView help = text("Lost money? Call 1930 straight away.", 15, SOFT, false);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, dp(18), 0, 0);
        col.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(col);
        setContentView(scroll);
    }
}
