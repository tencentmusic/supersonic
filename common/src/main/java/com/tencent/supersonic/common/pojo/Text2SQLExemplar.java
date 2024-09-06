package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Text2SQLExemplar {

    public static final String PROPERTY_KEY = "sql_exemplar";

    private String question;

    private String sideInfo;

    private String dbSchema;

    private String sql;
}
