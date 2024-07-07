package com.tencent.supersonic.common.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.persistence.dataobject.SystemConfigDO;
import com.tencent.supersonic.common.persistence.mapper.SystemConfigMapper;
import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Service
public class SystemConfigServiceImpl
        extends ServiceImpl<SystemConfigMapper, SystemConfigDO> implements SystemConfigService {

    @Autowired
    private Environment environment;

    @Override
    public SystemConfig getSystemConfig() {
        List<SystemConfigDO> list = list();
        if (CollectionUtils.isEmpty(list)) {
            SystemConfig systemConfig = new SystemConfig();
            systemConfig.setId(1);
            systemConfig.init();
            // use system property to initialize system parameter
            systemConfig.getParameters().stream().forEach(p -> {
                if (environment.containsProperty(p.getName())) {
                    p.setValue(environment.getProperty(p.getName()));
                }
            });
            save(systemConfig);
            return systemConfig;
        }

        return convert(list.iterator().next());
    }

    @Override
    public void save(SystemConfig sysConfig) {
        SystemConfigDO systemConfigDO = convert(sysConfig);
        saveOrUpdate(systemConfigDO);
    }

    private SystemConfig convert(SystemConfigDO systemConfigDO) {
        SystemConfig sysParameter = new SystemConfig();
        sysParameter.setId(systemConfigDO.getId());
        List<Parameter> parameters = JsonUtil.toObject(systemConfigDO.getParameters(),
                new TypeReference<List<Parameter>>() {
                });
        sysParameter.setParameters(parameters);
        sysParameter.setAdminList(systemConfigDO.getAdmin());
        return sysParameter;
    }

    private SystemConfigDO convert(SystemConfig sysParameter) {
        SystemConfigDO sysParameterDO = new SystemConfigDO();
        sysParameterDO.setId(sysParameter.getId());
        sysParameterDO.setParameters(JSONObject.toJSONString(sysParameter.getParameters()));
        sysParameterDO.setAdmin(sysParameter.getAdmin());
        return sysParameterDO;
    }

}
