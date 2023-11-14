package com.tencent.supersonic.common.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {
    private String name;
    private String value;
    private String comment;
    private String dataType;
}
