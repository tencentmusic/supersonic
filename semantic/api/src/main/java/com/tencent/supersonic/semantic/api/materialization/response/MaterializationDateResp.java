package com.tencent.supersonic.semantic.api.materialization.response;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MaterializationDateResp {

    private Long modelId;
    private TypeEnums elementType;
    private Long elementId;
    private String elementName;
    private String dateFormat;
    private String startDate;
    private String endDate;
    private List<String> unavailableDateList = new ArrayList<>();
}