package com.tencent.supersonic.headless.server.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.repository.DictRepository;
import com.tencent.supersonic.headless.server.service.DictConfService;
import com.tencent.supersonic.headless.server.utils.DictUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictConfServiceImpl implements DictConfService {

    private final DictRepository dictRepository;
    private final DictUtils dictConverter;

    public DictConfServiceImpl(DictRepository dictRepository,
                               DictUtils dictConverter) {
        this.dictRepository = dictRepository;
        this.dictConverter = dictConverter;
    }

    @Override
    public Long addDictConf(DictItemReq itemValueReq, User user) {
        DictConfDO dictConfDO = dictConverter.generateDictConfDO(itemValueReq, user);
        return dictRepository.addDictConf(dictConfDO);
    }

    @Override
    public Long editDictConf(DictItemReq itemValueReq, User user) {
        DictConfDO dictConfDO = dictConverter.generateDictConfDO(itemValueReq, user);
        dictRepository.editDictConf(dictConfDO);
        if (StatusEnum.DELETED.equals(itemValueReq.getStatus())) {
            // todo delete dict file and refresh

        }
        return itemValueReq.getItemId();
    }

    @Override
    public List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter, User user) {
        return dictRepository.queryDictConf(dictItemFilter);
    }
}