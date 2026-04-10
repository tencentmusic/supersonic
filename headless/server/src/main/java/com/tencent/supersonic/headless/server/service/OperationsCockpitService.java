package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.pojo.OperationsCockpitVO;

public interface OperationsCockpitService {

    OperationsCockpitVO getCockpit(User user);
}
