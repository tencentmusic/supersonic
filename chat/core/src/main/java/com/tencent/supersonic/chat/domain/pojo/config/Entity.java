package com.tencent.supersonic.chat.domain.pojo.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * the entity info about the domain
 */
@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class Entity {

    /**
     * uniquely identifies an entity
     */
    private List<Long> entityIds;

    /**
     * entity name list
     */
    private List<String> names;

    /**
     * query entity default information
     */
    private EntityDetailData detailData;

}