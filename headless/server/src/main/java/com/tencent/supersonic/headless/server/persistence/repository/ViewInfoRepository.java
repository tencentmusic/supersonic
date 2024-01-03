package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.ViewInfoDO;

import java.util.List;

public interface ViewInfoRepository {

    List<ViewInfoDO> getViewInfoList(Long domainId);

    ViewInfoDO getViewInfoById(Long id);

    void deleteViewInfo(Long id);

    void createViewInfo(ViewInfoDO viewInfoDO);

    void updateViewInfo(ViewInfoDO viewInfoDO);
}
