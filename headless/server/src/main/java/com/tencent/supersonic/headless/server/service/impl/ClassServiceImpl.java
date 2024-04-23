package com.tencent.supersonic.headless.server.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.api.pojo.request.ClassReq;
import com.tencent.supersonic.headless.api.pojo.response.ClassResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ClassDO;
import com.tencent.supersonic.headless.server.persistence.repository.ClassRepository;
import com.tencent.supersonic.headless.server.pojo.ClassFilter;
import com.tencent.supersonic.headless.server.service.ClassService;
import com.tencent.supersonic.headless.server.utils.ClassConverter;

import java.util.Date;
import java.util.List;

public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;

    public ClassServiceImpl(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @Override
    public ClassResp create(ClassReq classReq, User user) {

        ClassDO classDO = ClassConverter.convert(classReq);
        classDO.setId(null);
        Date date = new Date();
        classDO.setCreatedBy(user.getName());
        classDO.setCreatedAt(date);
        classDO.setUpdatedBy(user.getName());
        classDO.setUpdatedAt(date);
        classDO.setStatus(StatusEnum.ONLINE.getCode());

        classRepository.create(classDO);
        ClassDO classDOById = classRepository.getClassById(classDO.getId());

        return ClassConverter.convert2Resp(classDOById);
    }

    @Override
    public ClassResp update(ClassReq classReq, User user) {
        return null;
    }

    @Override
    public Boolean delete(Long id, User user) {
        return null;
    }

    @Override
    public List<ClassResp> getClassList(ClassFilter filter, User user) {
        return null;
    }
}