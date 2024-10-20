package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface ModelRelaService {

    void save(ModelRela modelRela, User user);

    void update(ModelRela modelRela, User user);

    List<ModelRela> getModelRelaList(Long domainId);

    List<ModelRela> getModelRela(List<Long> modelIds);

    void delete(Long id);
}
