package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class RetryUtils {

    private static final int RETRY_NUM = 3;

    public static <T> T exec(Supplier<T> supplier) {
        return exec(supplier, RETRY_NUM);
    }

    public static <T> T exec(Supplier<T> supplier, int retryNum) {
        T result = null;
        for (int index = 1; index <= retryNum; index++) {
            try {
                result = supplier.get();
            } catch (Exception ex) {
                if (index < retryNum) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        log.error("e", e);
                    }
                    log.warn("Retry exec {}, {}", index, ex.getMessage());
                    continue;
                }
                log.warn("Retry {} times all fail, err: {}", retryNum, ex.getMessage());
                throw ex;
            }
            break;
        }

        return result;
    }
}
