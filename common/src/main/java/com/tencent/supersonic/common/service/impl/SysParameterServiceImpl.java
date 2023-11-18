package com.tencent.supersonic.common.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.persistence.dataobject.SysParameterDO;
import com.tencent.supersonic.common.persistence.mapper.SysParameterMapper;
import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.service.SysParameterService;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SysParameterServiceImpl
        extends ServiceImpl<SysParameterMapper, SysParameterDO> implements SysParameterService {

    @Override
    public SysParameter getSysParameter() {
        List<SysParameterDO> list = list();
        if (CollectionUtils.isEmpty(list)) {
            SysParameter sysParameter = new SysParameter();
            sysParameter.setId(1);
            sysParameter.init();
            save(sysParameter);
            return sysParameter;
        }
        return convert(list.iterator().next());
    }

    @Override
    public void save(SysParameter sysParameter) {
        SysParameterDO sysParameterDO = convert(sysParameter);
        saveOrUpdate(sysParameterDO);
    }

    private SysParameter convert(SysParameterDO sysParameterDO) {
        SysParameter sysParameter = new SysParameter();
        sysParameter.setId(sysParameterDO.getId());
        List<Parameter> parameters = JsonUtil.toObject(sysParameterDO.getParameters(),
                new TypeReference<List<Parameter>>() {
                });
        sysParameter.setParameters(parameters);
        sysParameter.setAdminList(sysParameterDO.getAdmin());
        return sysParameter;
    }

    private SysParameterDO convert(SysParameter sysParameter) {
        SysParameterDO sysParameterDO = new SysParameterDO();
        sysParameterDO.setId(sysParameter.getId());
        sysParameterDO.setParameters(JSONObject.toJSONString(sysParameter.getParameters()));
        sysParameterDO.setAdmin(sysParameter.getAdmin());
        return sysParameterDO;
    }

}
