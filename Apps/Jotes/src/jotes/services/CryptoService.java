package jotes.services;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

/**
 * Cifragem de backups com Bouncy Castle: AES-256-GCM, chave derivada da
 * palavra-passe com PBKDF2WithHmacSHA256 (200 000 iterações, sal de 16 bytes,
 * IV de 12 bytes).
 * <p>Formato do ficheiro: {@code "JOTESENC" (8 bytes) | versão (1) | sal (16) | IV (12) | criptograma}.
 */
public class CryptoService {
    public static final byte[] MAGIC = "JOTESENC".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int ITERATIONS = 200_000;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;
    private static final int HEADER_LEN = MAGIC.length + 1 + SALT_LEN + IV_LEN;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final SecureRandom random = new SecureRandom();

    /** Cifra os dados com a palavra-passe; devolve o ficheiro completo (cabeçalho + criptograma). */
    public byte[] encrypt(byte[] plain, char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(salt);
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plain);

        ByteArrayOutputStream out = new ByteArrayOutputStream(HEADER_LEN + ciphertext.length);
        out.writeBytes(MAGIC);
        out.write(VERSION);
        out.writeBytes(salt);
        out.writeBytes(iv);
        out.writeBytes(ciphertext);
        return out.toByteArray();
    }

    /** Decifra um ficheiro produzido por {@link #encrypt}; lança erro se a palavra-passe estiver errada. */
    public byte[] decrypt(byte[] blob, char[] password) throws GeneralSecurityException {
        if (blob == null || blob.length < HEADER_LEN
                || !Arrays.equals(Arrays.copyOf(blob, MAGIC.length), MAGIC)) {
            throw new IllegalArgumentException("O ficheiro não é um backup encriptado do Jotes.");
        }
        int version = blob[MAGIC.length] & 0xFF;
        if (version != VERSION) {
            throw new IllegalArgumentException("Versão de backup encriptado não suportada: " + version);
        }
        byte[] salt = Arrays.copyOfRange(blob, MAGIC.length + 1, MAGIC.length + 1 + SALT_LEN);
        byte[] iv = Arrays.copyOfRange(blob, MAGIC.length + 1 + SALT_LEN, HEADER_LEN);
        byte[] ciphertext = Arrays.copyOfRange(blob, HEADER_LEN, blob.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("Palavra-passe incorreta ou ficheiro corrompido.", e);
        }
    }

    private static SecretKeySpec deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256", BouncyCastleProvider.PROVIDER_NAME);
        byte[] key = factory.generateSecret(new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)).getEncoded();
        return new SecretKeySpec(key, "AES");
    }
}
