package app.scamdecoder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Judges a tapped link before it opens: a port of the browser extension's offline guard (extension/guard.js).
 * Pure Java so it can be checked on a laptop (RulesCheck).
 */
public final class LinkGuard {
    public static final class Reason {
        public final String label; public final int weight;
        Reason(String label, int weight) { this.label = label; this.weight = weight; }
    }

    private static final List<String> SHORTENERS = Arrays.asList("bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd", "rb.gy", "shorturl.at", "tiny.cc", "ow.ly", "rebrand.ly", "t.ly", "s.id", "v.gd");
    private static final List<String> RISKY_TLDS = Arrays.asList("xyz", "top", "click", "live", "icu", "buzz", "shop", "online", "site", "rest", "monster", "cyou", "cfd", "sbs", "lol", "work", "support", "vip");
    private static final Map<String, String[]> BRANDS = new LinkedHashMap<>();
    static {
        BRANDS.put("sbi", new String[]{"sbi.co.in", "onlinesbi.sbi", "sbi.bank.in"});
        BRANDS.put("hdfc", new String[]{"hdfcbank.com", "hdfc.bank.in"});
        BRANDS.put("icici", new String[]{"icicibank.com", "icici.bank.in"});
        BRANDS.put("axis", new String[]{"axisbank.com", "axis.bank.in"});
        BRANDS.put("kotak", new String[]{"kotak.com", "kotak.bank.in"});
        BRANDS.put("paytm", new String[]{"paytm.com"});
        BRANDS.put("phonepe", new String[]{"phonepe.com"});
        BRANDS.put("amazon", new String[]{"amazon.in", "amazon.com", "amazon.co.uk", "amazonaws.com"});
        BRANDS.put("flipkart", new String[]{"flipkart.com"});
        BRANDS.put("indiapost", new String[]{"indiapost.gov.in"});
        BRANDS.put("incometax", new String[]{"incometax.gov.in"});
        BRANDS.put("uidai", new String[]{"uidai.gov.in"});
        BRANDS.put("aadhaar", new String[]{"uidai.gov.in"});
        BRANDS.put("paypal", new String[]{"paypal.com"});
        BRANDS.put("netflix", new String[]{"netflix.com"});
        BRANDS.put("whatsapp", new String[]{"whatsapp.com", "whatsapp.net"});
        BRANDS.put("google", new String[]{"google.com", "google.co.in", "googleusercontent.com", "googleapis.com", "gstatic.com"});
        BRANDS.put("microsoft", new String[]{"microsoft.com", "live.com", "office.com", "microsoftonline.com"});
        BRANDS.put("apple", new String[]{"apple.com", "icloud.com"});
    }
    private static final Pattern LOCAL = Pattern.compile("^(localhost|127\\..*|10\\..*|192\\.168\\..*|172\\.(1[6-9]|2\\d|3[01])\\..*|.*\\.local)$");
    private static final Pattern IP = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern LOGIN_PATH = Pattern.compile("(log-?in|sign-?in|verify|kyc|otp|password|account|wallet|bank)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOKALIKE = Pattern.compile("(sb1|amaz0n|amazom|paytrn|paypa1|g00gle|micros0ft|app1e|faceb00k|netf1ix|whatsap\\b|flipkrt)");
    private static final Pattern SPOOFED = Pattern.compile("(-kyc|kyc-|secure-?login|verify-?account|update-?kyc|account-?verify|login-?verify)");

    private LinkGuard() {}

    private static boolean endsWith(String host, String d) { return host.equals(d) || host.endsWith("." + d); }

    /** Every reason this link looks dangerous; empty means nothing suspicious. */
    public static List<Reason> reasons(String raw) {
        List<Reason> out = new ArrayList<>();
        URI u;
        try { u = new URI(raw.trim()); } catch (Exception e) { return out; }
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https") || u.getHost() == null) return out;
        String host = u.getHost().toLowerCase().replaceFirst("^www\\.", "");
        if (LOCAL.matcher(host).matches()) return out; // never interfere with home routers or dev servers
        String tld = host.substring(host.lastIndexOf('.') + 1);
        boolean rawIp = IP.matcher(host).matches(), risky = RISKY_TLDS.contains(tld);
        String pathQuery = (u.getRawPath() == null ? "" : u.getRawPath()) + (u.getRawQuery() == null ? "" : "?" + u.getRawQuery());
        if (rawIp) out.add(new Reason("Link to a raw IP address instead of a website name", 20));
        if (risky) out.add(new Reason("Throwaway domain (." + tld + ")", 12));
        if ((rawIp || risky) && LOGIN_PATH.matcher(pathQuery).find()) out.add(new Reason("Asks you to log in or verify on an untrusted address", 12));
        if (LOOKALIKE.matcher(host).find()) out.add(new Reason("Fake look-alike of a well-known brand name", 30));
        else if (SPOOFED.matcher(host).find()) out.add(new Reason("Lookalike / spoofed domain", 22));
        if (host.contains("xn--")) out.add(new Reason("Disguised characters in the address (punycode)", 18));
        for (Map.Entry<String, String[]> b : BRANDS.entrySet()) {
            if (!host.contains(b.getKey())) continue;
            boolean official = false;
            for (String d : b.getValue()) if (endsWith(host, d)) official = true;
            if (!official) {
                String n = b.getKey().toUpperCase();
                out.add(new Reason("Uses the name \"" + n + "\" but is not " + n + "'s official site", 30));
                break;
            }
        }
        if (u.getRawUserInfo() != null) out.add(new Reason("Hides the real address behind an @ sign", 20));
        for (String s : SHORTENERS) if (endsWith(host, s)) { out.add(new Reason("Shortened link that hides where it goes", 10)); break; }
        return out;
    }

    /** Block only when the evidence is strong enough; a shortener alone never blocks. */
    public static boolean shouldBlock(List<Reason> reasons) {
        int total = 0;
        for (Reason r : reasons) total += r.weight;
        return total >= 30;
    }

    public static int score(List<Reason> reasons) {
        int total = 0;
        for (Reason r : reasons) total += r.weight;
        return Math.min(100, total);
    }
}
