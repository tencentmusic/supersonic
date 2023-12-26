package com.tencent.supersonic.headless.model.domain.repository;

import com.tencent.supersonic.headless.model.domain.dataobject.ViewInfoDO;
import java.util.List;

public interface ViewInfoRepository {

    List<ViewInfoDO> getViewInfoList(Long domainId);

    ViewInfoDO getViewInfoById(Long id);

    void deleteViewInfo(Long id);

    void createViewInfo(ViewInfoDO viewInfoDO);

    void updateViewInfo(ViewInfoDO viewInfoDO);
}
