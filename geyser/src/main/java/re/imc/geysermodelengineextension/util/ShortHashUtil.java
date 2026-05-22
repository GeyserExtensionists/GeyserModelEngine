package re.imc.geysermodelengineextension.util;

import java.math.BigInteger;
import java.security.MessageDigest;

public class ShortHashUtil {
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyz_";
    private static final int SUFFIX_LEN = 6;
    private static final long RADIX = 37;
    private static final long SUFFIX_SPACE = (long) Math.pow(RADIX, SUFFIX_LEN);

    public static String hashModelId(String modelId) {
        return "gm_" + hash(modelId);
    }

    public static String hashTextureName(String modelId, String textureName) {
        return "gm_" + hash(modelId + textureName);
    }

    public static String hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));

            BigInteger num = new BigInteger(1, digest);
            BigInteger totalSpace = BigInteger.valueOf(26).multiply(BigInteger.valueOf(SUFFIX_SPACE));
            BigInteger val = num.mod(totalSpace);

            BigInteger[] divAndRem = val.divideAndRemainder(BigInteger.valueOf(SUFFIX_SPACE));
            int firstIdx = divAndRem[0].intValue();
            long remainder = divAndRem[1].longValue();
            char[] suffix = new char[SUFFIX_LEN];
            for (int i = SUFFIX_LEN - 1; i >= 0; i--) {
                suffix[i] = CHARS.charAt((int) (remainder % RADIX));
                remainder /= RADIX;
            }
            char firstChar = (char) ('a' + firstIdx);
            return firstChar + new String(suffix);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return input;
    }
}
