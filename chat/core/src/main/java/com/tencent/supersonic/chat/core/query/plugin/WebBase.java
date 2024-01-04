package com.tencent.supersonic.chat.core.query.plugin;

import com.google.common.collect.Lists;
import lombok.Data;
import java.util.List;

@Data
public class WebBase {

    private String url;

    private List<ParamOption> paramOptions = Lists.newArrayList();

    public List<ParamOption> getParams() {
        return paramOptions;
    }

}
