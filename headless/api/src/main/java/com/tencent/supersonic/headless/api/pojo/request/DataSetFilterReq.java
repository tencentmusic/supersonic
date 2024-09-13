package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
@NoArgsConstructor
public class DataSetFilterReq {

    protected boolean defaultAll = true;

    protected List<Long> dataSetIds = Lists.newArrayList();

    public DataSetFilterReq(Long dataSetId) {
        addDataSet(dataSetId);
    }

    public List<Long> getDataSetIds() {
        if (CollectionUtils.isEmpty(dataSetIds) && !defaultAll) {
            return Lists.newArrayList(-1L);
        }
        return dataSetIds;
    }

    public void addDataSet(Long dataSetId) {
        dataSetIds.add(dataSetId);
    }
}
