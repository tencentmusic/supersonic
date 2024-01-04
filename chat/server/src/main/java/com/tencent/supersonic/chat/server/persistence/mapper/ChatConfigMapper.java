package com.tencent.supersonic.chat.server.persistence.mapper;

import com.tencent.supersonic.chat.server.config.ChatConfigFilterInternal;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConfigMapper {

    Long addConfig(ChatConfigDO chaConfigPO);

    Long editConfig(ChatConfigDO chaConfigPO);

    List<ChatConfigDO> search(ChatConfigFilterInternal filterInternal);

    ChatConfigDO fetchConfigByModelId(Long modelId);
}
