package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import java.util.List;

/**
 * @author yannsu
 */

public interface CollectService {

    Boolean createCollectionIndicators(User user, CollectDO collectDO);

    Boolean deleteCollectionIndicators(User user, Long id);

    Boolean deleteCollectionIndicators(User user, CollectDO collectDO);

    List<CollectDO> getCollectList(String username);

    List<CollectDO> getCollectList(String username, TypeEnums typeEnums);
}
