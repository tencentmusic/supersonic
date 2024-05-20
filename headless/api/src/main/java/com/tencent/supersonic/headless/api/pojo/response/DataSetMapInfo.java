package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import lombok.Data;
import java.util.List;

@Data
public class DataSetMapInfo {

    private String name;

    private String description;

    private List<SchemaElementMatch> mapFields = Lists.newArrayList();

    private List<SchemaElementMatch> topFields = Lists.newArrayList();

}
