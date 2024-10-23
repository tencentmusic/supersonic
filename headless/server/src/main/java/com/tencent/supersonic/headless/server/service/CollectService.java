package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;

import java.util.List;

/** @author yannsu */
public interface CollectService {

    Boolean collect(User user, CollectDO collectDO);

    Boolean unCollect(User user, Long id);

    Boolean unCollect(User user, CollectDO collectDO);

    List<CollectDO> getCollectionList(String username);

    List<CollectDO> getCollectionList(String username, TypeEnums typeEnums);
}
