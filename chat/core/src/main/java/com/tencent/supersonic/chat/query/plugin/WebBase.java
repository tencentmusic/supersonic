package com.tencent.supersonic.chat.query.plugin;

import com.google.common.collect.Lists;
import lombok.Data;
import java.util.List;

@Data
public class WebBase {

    private String url;

    private List<ParamOption> paramOptions = Lists.newArrayList();

}
