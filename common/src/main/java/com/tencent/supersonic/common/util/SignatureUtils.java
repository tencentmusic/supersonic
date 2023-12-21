package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.pojo.Pair;
import org.apache.commons.codec.binary.Hex;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static java.lang.Thread.sleep;

public class SignatureUtils {

    private static final String ALGORITHM_HMAC_SHA256 = "HmacSHA256";

    private static final long TIME_OUT = 60 * 1000 * 30;

    public static String generateSignature(String appKey, String appSecret, long timestamp) {
        try {
            Mac sha256HMAC = Mac.getInstance(ALGORITHM_HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(), ALGORITHM_HMAC_SHA256);
            sha256HMAC.init(secretKey);

            String data = appKey + timestamp;
            byte[] hash = sha256HMAC.doFinal(data.getBytes());

            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
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

        String generatedSignature = generateSignature(appKey, appSecret, timestamp);

        if (generatedSignature.equals(signatureToCheck)) {
            return new Pair<>(true, "Signature is valid");
        } else {
            return new Pair<>(false, "Invalid signature");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // appkey为申请的接口id
        String appKey = "1";
        //生成的密钥
        String appSecret = "8fb44f17-f37d-4510-bb29-59b0e0b266d0";
        long timestamp = System.currentTimeMillis();
        System.out.println("timeStamp:" + timestamp);
        //生成的签名
        String serverSignature = generateSignature(appKey, appSecret, timestamp);
        System.out.println("Server Signature: " + serverSignature);

        sleep(4000);
        //用户需要的入参
        Pair<Boolean, String> isValid = isValidSignature(appKey, appSecret, timestamp, serverSignature);
        System.out.println("Is Signature Valid? " + isValid.first);
    }

}
