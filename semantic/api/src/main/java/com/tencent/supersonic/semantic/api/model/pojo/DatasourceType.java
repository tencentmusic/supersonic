package com.tencent.supersonic.semantic.api.model.pojo;

import com.tencent.supersonic.common.pojo.Constants;
import java.util.List;
import lombok.Data;

@Data
public class DatasourceType {

    private String name;
    private String prefix;
    private List<String> versions;

    public DatasourceType(String name, List<String> versions) {
        this.name = name;
        this.prefix = String.format(Constants.JDBC_PREFIX_FORMATTER, name);
        this.versions = versions;
    }
}
