package com.tencent.supersonic.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tencent.supersonic.common.persistence.dataobject.SysParameterDO;
import com.tencent.supersonic.common.pojo.SystemConfig;

public interface SystemConfigService extends IService<SysParameterDO> {

    SystemConfig getSysParameter();

    void save(SystemConfig sysConfig);

}
