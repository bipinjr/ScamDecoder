import app.scamdecoder.Rules;

/** Laptop self-check for the on-phone rules. Run by android/build.sh before every build. */
public class RulesCheck {
    static void ok(boolean c, String msg) { if (!c) throw new AssertionError(msg); }

    public static void main(String[] a) {
        String scam = "Dear SBI customer, your account will be BLOCKED today. Update KYC: http://sbi-kyc-update.xyz/verify or call 9876543210";
        ok(Rules.score(scam) >= 65, "classic KYC scam should score high: " + Rules.score(scam) + " " + Rules.flags(scam));
        ok(Rules.flags(scam).contains("risky-tld") && Rules.flags(scam).contains("lookalike"), "link checks missed: " + Rules.flags(scam));

        String otp = "483920 is your OTP for login to HDFC Bank NetBanking. Do not share this OTP with anyone. -HDFC Bank";
        ok(Rules.score(otp) < Rules.SUSPICIOUS, "a genuine OTP must stay on the phone: " + Rules.score(otp));

        String chat = "Hi, are we still meeting at 6 for dinner? Bring the book.";
        ok(Rules.score(chat) == 0, "ordinary chat flagged: " + Rules.flags(chat));

        String hindi = "प्रिय उपभोक्ता, आपका बिजली कनेक्शन आज रात काट दिया जाएगा। तुरंत संपर्क करें 8123456789";
        ok(Rules.score(hindi) >= Rules.SUSPICIOUS, "Hindi electricity scam missed: " + Rules.score(hindi));

        String masked = Rules.mask("Call 9876543210, OTP 483920, a/c 1234 5678 9012 at sbi-kyc.xyz");
        ok(!masked.matches(".*\\d{4,}.*"), "digits leaked: " + masked);
        ok(masked.contains("sbi-kyc.xyz"), "scam link must survive masking: " + masked);
        System.out.println("rules checks passed");
    }
}
