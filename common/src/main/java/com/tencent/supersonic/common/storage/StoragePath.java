package com.tencent.supersonic.common.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class StoragePath {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StoragePath() {}

    public static String forTenant(String prefix, Long tenantId, Long groupId, String fileName) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for storage key");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("%s/%d/%s/%d/%s", prefix, tenantId, date,
                groupId == null ? 0 : groupId, fileName);
    }

    public static Long extractTenantId(String key) {
        if (key == null) {
            return null;
        }
        String[] parts = key.split("/");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
