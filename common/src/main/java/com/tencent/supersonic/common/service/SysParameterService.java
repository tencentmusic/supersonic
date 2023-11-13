package com.tencent.supersonic.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tencent.supersonic.common.persistence.dataobject.SysParameterDO;
import com.tencent.supersonic.common.pojo.SysParameter;

public interface SysParameterService extends IService<SysParameterDO> {

    SysParameter getSysParameter();

    void save(SysParameter sysParameter);

}
