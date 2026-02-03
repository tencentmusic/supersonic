package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Tenant entity representing a tenant in the multi-tenant system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    private Long id;

    private String name;

    private String code;

    private String description;

    private String status;

    private String contactEmail;

    private String contactName;

    private String contactPhone;

    private String logoUrl;

    private String settings;

    /**
     * Enriched at query time, not persisted.
     */
    private String subscriptionPlanName;

    private Timestamp createdAt;

    private String createdBy;

    private Timestamp updatedAt;

    private String updatedBy;

}
