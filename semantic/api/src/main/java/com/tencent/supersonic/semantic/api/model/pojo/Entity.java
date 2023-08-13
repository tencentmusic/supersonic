package com.tencent.supersonic.semantic.api.model.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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