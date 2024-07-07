package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExemplar {

    private String question;

    private String dbSchema;

    private String sql;

}