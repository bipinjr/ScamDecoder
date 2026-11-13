package app.scamdecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-phone first pass for incoming SMS: a port of the web app's rule engine (src/lib/signals.ts).
 * Pure Java, no Android imports, so it can be tested on a laptop (RulesCheck).
 * An SMS that scores below SUSPICIOUS never leaves the phone.
 */
public final class Rules {
    // Lower than the web verdict line (35): the full check decides, this only picks what to send.
    // Still above a lone "OTP" hit (25), so a plain genuine OTP stays on the phone.
    public static final int SUSPICIOUS = 30;

    private static final int I = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Object[][] RULES = {
        {"urgency", 12, "\\b(urgent(ly)?|immediately|right now|today itself|within \\d+\\s*(hours?|hrs?|minutes?|mins?)|expire[sd]?|expiring|last (chance|date|warning)|final (notice|warning)|act now|before midnight|aaj hi|turant)\\b"},
        {"threat", 12, "\\b(block(ed)?|suspend(ed)?|deactivat(e|ed)|disconnect(ed)?|frozen|freeze|terminat(e|ed)|penalty|legal action|fine of|will be cut|band ho)\\b"},
        {"credentials", 25, "\\b(otp|one[- ]time password|upi pin|atm pin|pin|cvv|password|net ?banking (id|password)|card number|expiry date|share the code)\\b"},
        {"kyc", 15, "\\b(kyc|re-?kyc|pan (card )?(update|link)|aadhaa?r (update|link|verify)|update your (details|pan|kyc))\\b"},
        {"authority", 22, "\\b(cbi|police|cyber ?crime|customs|narcotics|ncb|enforcement directorate|ed officer|trai|rbi officer|income tax officer|arrest warrant|digital arrest|money laundering|drugs? (found|parcel)|illegal parcel|fir (registered|lodged)|court (notice|summons))\\b"},
        {"secrecy", 15, "\\b(do ?n[o']t (tell|inform|share with) (anyone|family|bank)|keep (this )?(confidential|secret)|stay on (the )?(video )?call|don't disconnect|kisi ko mat batana)\\b"},
        {"prize", 14, "\\b(you (have )?won|winner|lottery|lucky draw|prize|jackpot|cash ?back of|reward points? (expir|redeem)|free gift|claim (your|now)|congratulations)\\b"},
        {"job", 16, "\\b(work from home|part[- ]time job|earn (rs\\.?|₹|inr)?\\s?\\d[\\d,]*\\s*(per|/)\\s*(day|hour)|daily (income|payout)|like (youtube )?videos|rate (hotels|products)|prepaid task|telegram (group|channel))\\b"},
        {"payment", 14, "\\b(pay (now|rs|₹|inr)|processing fee|registration fee|refundable deposit|transfer (rs|₹|inr|the amount)|scan (this|the) qr|collect request|approve the request|security deposit)\\b"},
        {"apk", 22, "(\\.apk\\b|download (this|the) app|install (anydesk|teamviewer|quicksupport|rustdesk)|screen ?shar(e|ing))"},
        {"callback", 8, "\\b(call|contact|whatsapp)\\b[^.\\n]{0,25}(\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}\\b"},
        // Hindi (Devanagari): same ids, so a rule counts once whichever script matched.
        {"urgency", 12, "(तुरंत|आज ही|आज रात|अभी तुरंत|जल्दी करें)"},
        {"threat", 12, "(काट दिया जाएगा|बंद कर दिया|ब्लॉक|निलंबित|जुर्माना)"},
        {"credentials", 25, "(ओटीपी|पिन नंबर|पासवर्ड)"},
        {"kyc", 15, "(केवाईसी|पैन कार्ड अपडेट|आधार अपडेट|आधार लिंक)"},
        {"authority", 22, "(पुलिस|सीबीआई|गिरफ्तारी|वारंट|कस्टम विभाग)"},
        {"prize", 14, "(लॉटरी|इनाम|आपने जीत)"},
        {"callback", 8, "(संपर्क|कॉल)[^।\\n]{0,40}[6-9]\\d{9}"},
    };
    private static final Pattern[] COMPILED = new Pattern[RULES.length];
    static { for (int i = 0; i < RULES.length; i++) COMPILED[i] = Pattern.compile((String) RULES[i][2], I); }

    private static final List<String> SHORTENERS = Arrays.asList("bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd", "rb.gy", "shorturl.at", "tiny.cc", "ow.ly", "rebrand.ly", "t.ly", "s.id", "v.gd");
    private static final List<String> RISKY_TLDS = Arrays.asList("xyz", "top", "click", "live", "icu", "buzz", "shop", "online", "site", "rest", "monster", "cyou", "cfd", "sbs", "lol", "work", "support", "vip");
    private static final Pattern URL = Pattern.compile("\\b((?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s]*)?)", I);
    private static final Pattern LOOKALIKE = Pattern.compile("(sb1|amaz0n|amazom|paytrn|paypa1|g00gle|micros0ft|app1e|faceb00k|netf1ix|flipkrt|hdfc-|icici-|-kyc|kyc-|secure-?login|verify-?account|update-?kyc)");

    private Rules() {}

    static List<String> hosts(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String h = m.group(1).toLowerCase().replaceFirst("^https?://", "").replaceFirst("[/?#:].*$", "").replaceFirst("^www\\.", "");
            if (h.matches(".*\\.[a-z]{2,}$") && !h.matches("^\\d+\\.\\d+$")) out.add(h);
        }
        return new ArrayList<>(out);
    }

    /** Ids of every rule that fired, e.g. [threat, kyc, risky-tld]. */
    public static Set<String> flags(String text) {
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i < RULES.length; i++) if (COMPILED[i].matcher(text).find()) ids.add((String) RULES[i][0]);
        for (String h : hosts(text)) {
            for (String s : SHORTENERS) if (h.equals(s) || h.endsWith("." + s)) ids.add("shortener");
            if (RISKY_TLDS.contains(h.substring(h.lastIndexOf('.') + 1))) ids.add("risky-tld");
            if (LOOKALIKE.matcher(h).find()) ids.add("lookalike");
        }
        return ids;
    }

    public static int score(String text) {
        int total = 0;
        for (String id : flags(text)) total += weight(id);
        return Math.min(100, total);
    }

    private static int weight(String id) {
        switch (id) {
            case "shortener": return 15;
            case "risky-tld": return 12;
            case "lookalike": return 22;
            default: for (Object[] r : RULES) if (r[0].equals(id)) return (Integer) r[1];
        }
        return 0;
    }

    /** Hide phone numbers, OTPs and account numbers before anything is sent to the server. */
    public static String mask(String text) {
        return text
            .replaceAll("(\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}\\b", "[phone hidden]")
            .replaceAll("\\b\\d[\\d\\s-]{5,}\\d\\b", "[number hidden]")
            .replaceAll("\\b\\d{4,}\\b", "[number hidden]");
    }
}
