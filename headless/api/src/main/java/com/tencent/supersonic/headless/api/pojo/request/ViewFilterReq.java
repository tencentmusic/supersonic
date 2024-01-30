package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
@NoArgsConstructor
public class ViewFilterReq {

    protected boolean defaultAll = true;

    protected List<Long> viewIds = Lists.newArrayList();

    public ViewFilterReq(Long viewId) {
        addView(viewId);
    }

    public List<Long> getViewIds() {
        if (CollectionUtils.isEmpty(viewIds) && !defaultAll) {
            return Lists.newArrayList(-1L);
        }
        return viewIds;
    }

    public void addView(Long viewId) {
        viewIds.add(viewId);
    }

}