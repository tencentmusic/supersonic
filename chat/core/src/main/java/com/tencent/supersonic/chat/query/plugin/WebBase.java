package com.tencent.supersonic.chat.query.plugin;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;

@Data
public class WebBase {

    private String url;

    private List<ParamOption> paramOptions = Lists.newArrayList();

}
