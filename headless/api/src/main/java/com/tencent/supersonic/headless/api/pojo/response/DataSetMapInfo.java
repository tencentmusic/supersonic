package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import lombok.Data;
import java.util.List;

@Data
public class DataSetMapInfo {

    private String name;

    private String description;

    private List<SchemaElementMatch> mapFields;

    private List<SchemaElementMatch> topFields;

}
