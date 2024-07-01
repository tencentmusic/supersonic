package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String ENCODE = "UTF-8";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String KEY = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    // TODO 固定IV，确保每次加密时使用相同的IV,该值应该安全保管
    private static final String IV = "supersonic@bicom";

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
            byte[] iv = IV.getBytes(ENCODE);
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

    public static String aesDecryptCBC(String encryptStr) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptStr);
            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] encryptedData = Arrays.copyOfRange(combined, 16, combined.length);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(KEY), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);

            return new String(decryptedBytes, ENCODE);
        } catch (Exception e) {
            log.warn("encryptStr decrypt failed:{}", encryptStr);
            return encryptStr;
        }
    }

    public static String aesEncryptCBC(String content) throws Exception {
        byte[] iv = IV.getBytes(ENCODE);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(KEY), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encryptEncode = cipher.doFinal(content.getBytes(ENCODE));
        byte[] combined = new byte[iv.length + encryptEncode.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptEncode, 0, combined, iv.length, encryptEncode.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String aesEncryptECB(String content) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(KEY), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptEncode = cipher.doFinal(content.getBytes(ENCODE));
        return getStringFromBytes(encryptEncode);
    }

    public static String aesDecryptECB(String encryptStr) {
        try {
            byte[] encryptBytes = getBytesFromString(encryptStr);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(KEY), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher.doFinal(encryptBytes);
            return new String(decryptedBytes, ENCODE);
        } catch (Exception e) {
            log.warn("encryptStr decrypt failed:{}", encryptStr);
            return encryptStr;
        }
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    public static String getStringFromBytes(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] getBytesFromString(String str) {
        return Base64.getDecoder().decode(str);
    }

}