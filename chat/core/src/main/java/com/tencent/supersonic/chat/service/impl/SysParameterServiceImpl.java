package com.tencent.supersonic.chat.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.persistence.dataobject.SysParameterDO;
import com.tencent.supersonic.common.persistence.mapper.SysParameterMapper;
import com.tencent.supersonic.common.service.SysParameterService;
import com.tencent.supersonic.common.util.BeanMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Service
public class SysParameterServiceImpl
        extends ServiceImpl<SysParameterMapper, SysParameterDO> implements SysParameterService {

    @Override
    public SysParameter getSysParameter() {
        List<SysParameterDO> list = list();
        if (CollectionUtils.isEmpty(list)) {
            return new SysParameter();
        }
        return convert(list.iterator().next());
    }

    @Override
    public void save(SysParameter sysParameter) {
        SysParameterDO chatParameterDO = convert(sysParameter);
        saveOrUpdate(chatParameterDO);
    }

    private SysParameter convert(SysParameterDO sysParameterDO) {
        SysParameter chatParameter = new SysParameter();
        BeanMapper.mapper(sysParameterDO, chatParameter);
        chatParameter.setParameters(JSONObject.parseObject(sysParameterDO.getParameters(), List.class));
        chatParameter.setAdmin(sysParameterDO.getAdmin());
        return chatParameter;
    }

    private SysParameterDO convert(SysParameter sysParameter) {
        SysParameterDO sysParameterDO = new SysParameterDO();
        BeanMapper.mapper(sysParameter, sysParameterDO);
        sysParameterDO.setParameters(JSONObject.toJSONString(sysParameter.getParameters()));
        sysParameterDO.setAdmin(sysParameter.getAdmin());
        return sysParameterDO;
    }

}
