package com.tencent.supersonic.chat.query.plugin;

import com.google.common.collect.Lists;
import lombok.Data;
import java.util.List;

@Data
public class WebBaseResult {

    private String url;

    private List<ParamOption> params = Lists.newArrayList();

}
