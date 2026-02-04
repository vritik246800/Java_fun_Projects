package crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class CryptoUtils {

    private static final byte[] KEY =
            Arrays.copyOf("RemoteDesktopAES".getBytes(), 16);

    public static Cipher cipher(int mode) throws Exception {
        SecretKeySpec key = new SecretKeySpec(KEY, "AES");
        Cipher c = Cipher.getInstance("AES");
        c.init(mode, key);
        return c;
    }
}
