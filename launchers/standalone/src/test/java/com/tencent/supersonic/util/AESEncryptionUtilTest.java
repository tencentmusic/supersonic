package com.tencent.supersonic.util;

import com.tencent.supersonic.common.util.AESEncryptionUtil;

public class AESEncryptionUtilTest {

    public static boolean areByteArraysEqual(byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        byte[] salt = AESEncryptionUtil.generateSalt("zhangsan1234");
        System.out.println("salt: " + salt);
        String encodeSalt = AESEncryptionUtil.getStringFromBytes(salt);
        System.out.println("encodeSalt: " + encodeSalt);
        byte[] decodeSalt = AESEncryptionUtil.getBytesFromString(encodeSalt);
        System.out.println("decodeSalt: " + decodeSalt);

        System.out.println("areByteArraysEqual: " + areByteArraysEqual(salt, decodeSalt));

        String password = AESEncryptionUtil.encrypt("zhangsan1234", salt);
        System.out.println("password: " + password);
        String password2 = AESEncryptionUtil.encrypt("zhangsan1234", decodeSalt);
        System.out.println("password2: " + password2);

        String content = "123";
        System.out.println("before AES/CBC encrypt：" + content);
        String encrypt = AESEncryptionUtil.aesEncryptCBC(content);
        System.out.println("after AES/CBC encrypt：" + encrypt);
        String decrypt = AESEncryptionUtil.aesDecryptCBC(encrypt);
        System.out.println("after AES/CBC decrypt：" + decrypt);

        String str = "123";
        System.out.println("before AES/ECB encrypt：" + str);
        String encryptStr = AESEncryptionUtil.aesEncryptECB(str);
        System.out.println("after AES/ECB encrypt：" + encryptStr);
        String decryptStr = AESEncryptionUtil.aesDecryptECB(encryptStr);
        System.out.println("after AES/ECB decrypt：" + decryptStr);
    }
}
