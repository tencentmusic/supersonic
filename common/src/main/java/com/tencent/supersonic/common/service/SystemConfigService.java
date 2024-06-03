package com.tencent.supersonic.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tencent.supersonic.common.persistence.dataobject.SystemConfigDO;
import com.tencent.supersonic.common.pojo.SystemConfig;

public interface SystemConfigService extends IService<SystemConfigDO> {

    SystemConfig getSystemConfig();

    void save(SystemConfig systemConfig);

}
