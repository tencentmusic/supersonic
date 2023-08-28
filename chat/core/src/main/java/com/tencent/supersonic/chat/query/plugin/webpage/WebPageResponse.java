package com.tencent.supersonic.chat.query.plugin.webpage;

import com.tencent.supersonic.chat.query.plugin.WebBaseResult;
import java.util.List;
import lombok.Data;

@Data
public class WebPageResponse {

    private Long pluginId;

    private String pluginType;

    private String name;

    private String description;

    private WebBaseResult webPage;

    private List<WebBaseResult> moreWebPage;

}
