package com.tencent.supersonic.util;

import com.tencent.supersonic.auth.authentication.utils.AESEncryptionUtil;

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

    }
}
