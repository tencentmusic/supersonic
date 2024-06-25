package com.tencent.supersonic.auth.authentication.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;

@Slf4j
public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String ENCODE = "UTF-8";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static byte[] generateSalt(String username) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(username.getBytes(ENCODE));
        byte[] hash = md.digest();
        // 通常只需要使用盐的一部分作为盐值，例如16字节
        byte[] salt = new byte[16];
        System.arraycopy(hash, 0, salt, 0, salt.length);
        return salt;
    }

    public static String encrypt(String password, byte[] salt) throws Exception {
        try {
            // TODO 固定IV，确保每次加密时使用相同的IV,该值应该安全保管
            byte[] iv = "supersonic@bicom".getBytes(ENCODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(password.getBytes(ENCODE));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Throwable e) {
            log.error("encrypt", e);
            throw e;
        }
    }

    public static String getStringFromBytes(byte[] salt) {
        return Base64.getEncoder().encodeToString(salt);
    }

    public static byte[] getBytesFromString(String encodeSalt) {
        return Base64.getDecoder().decode(encodeSalt);
    }

}