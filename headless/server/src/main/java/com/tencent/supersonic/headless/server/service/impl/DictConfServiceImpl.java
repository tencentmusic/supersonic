package com.tencent.supersonic.headless.server.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.repository.DictRepository;
import com.tencent.supersonic.headless.server.service.DictConfService;
import com.tencent.supersonic.headless.server.utils.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DictConfServiceImpl implements DictConfService {

    private final DictRepository dictRepository;
    private final DictUtils dictConverter;

    public DictConfServiceImpl(DictRepository dictRepository,
                               DictUtils dictConverter) {
        this.dictRepository = dictRepository;
        this.dictConverter = dictConverter;
    }

    @Override
    public DictItemResp addDictConf(DictItemReq itemValueReq, User user) {
        DictConfDO dictConfDO = dictConverter.generateDictConfDO(itemValueReq, user);
        Boolean exist = checkConfExist(itemValueReq, user);
        if (exist) {
            throw new RuntimeException("dictConf is existed");
        }
        Long id = dictRepository.addDictConf(dictConfDO);
        log.debug("dictConfDO:{}", dictConfDO);

        DictItemFilter filter = DictItemFilter.builder()
                .id(id)
                .status(itemValueReq.getStatus())
                .build();
        Optional<DictItemResp> dictItemResp = queryDictConf(filter, user).stream().findFirst();
        if (dictItemResp.isPresent()) {
            return dictItemResp.get();
        }
        return null;
    }

    private Boolean checkConfExist(DictItemReq itemValueReq, User user) {
        DictItemFilter filter = DictItemFilter.builder().build();
        BeanUtils.copyProperties(itemValueReq, filter);
        filter.setStatus(null);
        Optional<DictItemResp> dictItemResp = queryDictConf(filter, user).stream()
                .findFirst();
        if (dictItemResp.isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    public DictItemResp editDictConf(DictItemReq itemValueReq, User user) {
        DictConfDO dictConfDO = dictConverter.generateDictConfDO(itemValueReq, user);
        dictRepository.editDictConf(dictConfDO);
        DictItemFilter filter = DictItemFilter.builder().build();
        BeanUtils.copyProperties(itemValueReq, filter);
        Optional<DictItemResp> dictItemResp = queryDictConf(filter, user).stream().findFirst();
        if (dictItemResp.isPresent()) {
            return dictItemResp.get();
        }
        return null;
    }

    @Override
    public List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter, User user) {
        return dictRepository.queryDictConf(dictItemFilter);
    }
}