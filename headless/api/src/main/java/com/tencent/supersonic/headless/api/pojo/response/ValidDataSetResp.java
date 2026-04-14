package com.tencent.supersonic.headless.api.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight response DTO for the valid-dataset-list API, used by report scheduling and similar
 * scenarios that only need basic dataset info plus the partition dimension name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidDataSetResp {

    private Long id;
    private String name;
    private Long domainId;

    /** Name of the partition dimension (e.g. "imp_date"), or null if the dataset has none. */
    private String partitionDimension;

    /** Dataset-configured DETAIL query row limit. */
    private Long detailLimit;

    /** Dataset-configured AGGREGATE query row limit. */
    private Long aggregateLimit;
}
