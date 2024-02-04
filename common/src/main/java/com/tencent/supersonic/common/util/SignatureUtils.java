package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.pojo.Pair;
import org.apache.commons.codec.digest.DigestUtils;

public class SignatureUtils {

    private static final long TIME_OUT = 60 * 1000 * 30;

    public static String generateSignature(String appSecret, long timestamp) {
        String psw = timestamp + appSecret + timestamp;
        return DigestUtils.sha1Hex(psw);
    }

    public static Pair<Boolean, String> isValidSignature(String appKey, String appSecret,
                                                         long timestamp, String signatureToCheck) {
        long currentTimeMillis = System.currentTimeMillis();

        if (currentTimeMillis < timestamp) {
            return new Pair<>(false, "Timestamp is in the future");
        }

        if (currentTimeMillis - timestamp > TIME_OUT) {
            return new Pair<>(false, "Timestamp is too old");
        }

        String generatedSignature = generateSignature(appSecret, timestamp);

        if (generatedSignature.equals(signatureToCheck)) {
            return new Pair<>(true, "Signature is valid");
        } else {
            return new Pair<>(false, "Invalid signature");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        //生成的密钥
        String appSecret = "38f2857c-d9ee-4c3a-bcc2-2cdb62fda5aa";
        long timestamp = 1706504908126L;
        System.out.println("timeStamp:" + timestamp);
        //生成的签名
        String serverSignature = generateSignature(appSecret, timestamp);
        System.out.println("Server Signature: " + serverSignature);
        //用户需要的入参
        Pair<Boolean, String> isValid = isValidSignature("1", appSecret, timestamp, serverSignature);
        System.out.println("Is Signature Valid? " + isValid.first);
    }

}
