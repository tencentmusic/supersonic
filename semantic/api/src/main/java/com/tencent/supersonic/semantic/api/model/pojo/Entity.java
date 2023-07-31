package com.tencent.supersonic.semantic.api.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Entity {


    /**
     * uniquely identifies an entity
     */
    private Long entityId;

    /**
     * entity name list
     */
    private List<String> names;
}