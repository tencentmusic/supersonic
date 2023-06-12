package com.tencent.supersonic.semantic.core.domain.dataobject;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDimValueDictBatchDO {

    private List<Long> itemIdList = new ArrayList<>();
    private String rules;
}