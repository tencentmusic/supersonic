package com.tencent.supersonic.chat.api.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/** the entity info about the model */
@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class Entity {

    /** uniquely identifies an entity */
    private Long entityId;

    /** entity name list */
    private List<String> names;
}
