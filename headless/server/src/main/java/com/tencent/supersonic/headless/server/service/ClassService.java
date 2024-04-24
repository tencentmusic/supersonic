package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ClassReq;
import com.tencent.supersonic.headless.api.pojo.response.ClassResp;
import com.tencent.supersonic.headless.server.pojo.ClassFilter;

import java.util.List;

public interface ClassService {

    ClassResp create(ClassReq classReq, User user);

    ClassResp update(ClassReq classReq, User user);

    Boolean delete(Long id, Boolean force, User user) throws Exception;

    List<ClassResp> getClassList(ClassFilter filter, User user);
}
