package com.tencent.supersonic.common.calcite;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class SqlParserInfo implements Serializable {

    private String tableName;

    private List<String> selectFields = new ArrayList<>();

    private List<String> allFields = new ArrayList<>();
}
