package top.niunaijun.blackbox.fake.spoof;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

/** Bir profile (sanal kullanıcıya) ait sabit kimlik değerleri. */
public final class SpoofConfig {
    public static final int VERSION = 1;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SERIAL_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    public final String androidId; // 16 hex, küçük harf
    public final String imei;      // 15 hane, Luhn geçerli
    public final String imei2;
    public final String meid;      // 14 hex, büyük harf
    public final String serial;

    private SpoofConfig(String androidId, String imei, String imei2, String meid, String serial) {
        this.androidId = androidId;
        this.imei = imei;
        this.imei2 = imei2;
        this.meid = meid;
        this.serial = serial;
    }

    public static SpoofConfig generate() {
        return new SpoofConfig(
                randomHex(8, false),
                randomImei(),
                randomImei(),
                randomHex(7, true),
                randomSerial(10));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("version", VERSION);
        o.put("androidId", androidId);
        o.put("imei", imei);
        o.put("imei2", imei2);
        o.put("meid", meid);
        o.put("serial", serial);
        return o;
    }

    /** Alan eksik ya da bozuksa null döner, çağıran yeniden üretir. */
    public static SpoofConfig fromJson(JSONObject o) {
        String a = o.optString("androidId", "");
        String i1 = o.optString("imei", "");
        String i2 = o.optString("imei2", "");
        String m = o.optString("meid", "");
        String s = o.optString("serial", "");
        if (a.length() != 16 || i1.length() != 15 || i2.length() != 15
                || m.length() != 14 || s.isEmpty()) {
            return null;
        }
        return new SpoofConfig(a, i1, i2, m, s);
    }

    private static String randomHex(int bytes, boolean upper) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format(upper ? "%02X" : "%02x", x));
        return sb.toString();
    }

    private static String randomSerial(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(SERIAL_CHARS.charAt(RANDOM.nextInt(SERIAL_CHARS.length())));
        return sb.toString();
    }

    // Not: TAC (ilk 8 hane) şimdilik rastgele; gerçek cihaz şablonu gelince şablona bağlanacak.
    private static String randomImei() {
        StringBuilder sb = new StringBuilder("35");
        for (int i = 0; i < 12; i++) sb.append(RANDOM.nextInt(10));
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    private static char luhnCheckDigit(String payload) {
        int sum = 0;
        boolean dbl = true;
        for (int i = payload.length() - 1; i >= 0; i--) {
            int d = payload.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return (char) ('0' + (10 - sum % 10) % 10);
    }
}
