package com.tencent.supersonic.chat.query.plugin.webpage;

import com.tencent.supersonic.chat.query.plugin.WebBaseResult;
import lombok.Data;
import java.util.List;

@Data
public class WebPageResponse {

    private Long pluginId;

    private String pluginType;

    private String name;

    private String description;

    private WebBaseResult webPage;

    private List<WebBaseResult> moreWebPage;

}
