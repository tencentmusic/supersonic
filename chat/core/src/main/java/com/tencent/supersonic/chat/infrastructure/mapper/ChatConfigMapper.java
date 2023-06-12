package com.tencent.supersonic.chat.infrastructure.mapper;

import com.tencent.supersonic.chat.domain.dataobject.ChatConfigDO;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilterInternal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConfigMapper {

    Long addConfig(ChatConfigDO chaConfigPO);

    Long editConfig(ChatConfigDO chaConfigPO);

    List<ChatConfigDO> search(ChatConfigFilterInternal filterInternal);

    ChatConfigDO fetchConfigByDomainId(Long domainId);
}
