package com.tencent.supersonic.headless.server.web.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.ClassReq;
import com.tencent.supersonic.headless.api.pojo.response.ClassResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ClassDO;
import com.tencent.supersonic.headless.server.persistence.repository.ClassRepository;
import com.tencent.supersonic.headless.server.pojo.ClassFilter;
import com.tencent.supersonic.headless.server.web.service.ClassService;
import com.tencent.supersonic.headless.server.utils.ClassConverter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassConverter converter;

    public ClassServiceImpl(ClassRepository classRepository, ClassConverter converter) {
        this.classRepository = classRepository;
        this.converter = converter;
    }

    @Override
    public ClassResp create(ClassReq classReq, User user) {

        ClassDO classDO = converter.convert(classReq);
        classDO.setId(null);
        Date date = new Date();
        classDO.setCreatedBy(user.getName());
        classDO.setCreatedAt(date);
        classDO.setUpdatedBy(user.getName());
        classDO.setUpdatedAt(date);
        classDO.setStatus(StatusEnum.ONLINE.getCode());

        classRepository.create(classDO);
        ClassDO classDOById = classRepository.getClassById(classDO.getId());

        return converter.convert2Resp(classDOById);
    }

    @Override
    public ClassResp update(ClassReq classReq, User user) {
        ClassDO classDO = classRepository.getClassById(classReq.getId());
        BeanMapper.mapper(classReq, classDO);
        classDO.setUpdatedAt(new Date());
        classDO.setUpdatedBy(user.getName());
        classRepository.update(classDO);
        return converter.convert2Resp(classRepository.getClassById(classReq.getId()));
    }

    @Override
    public Boolean delete(Long id, Boolean force, User user) throws Exception {
        ClassDO classDO = classRepository.getClassById(id);
        checkDeletePermission(classDO, user);
        checkDeleteValid(classDO, force);
        classRepository.delete(new ArrayList<>(Arrays.asList(id)));

        if (force) {
            // 删除子分类
            List<ClassDO> classDOList = classRepository.getAllClassDOList();
            Set<Long> deleteClassList = extractSubClass(id, classDOList);
            classRepository.delete(new ArrayList<>(deleteClassList));
        }
        return true;
    }

    private Set<Long> extractSubClass(Long id, List<ClassDO> classDOList) {
        Set<Long> classIdSet = new HashSet<>();
        for (ClassDO classDO : classDOList) {
            if (id.equals(classDO.getParentId())) {
                classIdSet.add(classDO.getId());
                classIdSet.addAll(extractSubClass(classDO.getId(), classDOList));
            }
        }
        return classIdSet;
    }

    private void checkDeleteValid(ClassDO classDelete, Boolean force) {
        List<ClassDO> classDOList = classRepository.getAllClassDOList();
        for (ClassDO classDO : classDOList) {
            if (classDO.getParentId().equals(classDelete.getId()) && !force) {
                throw new RuntimeException("该分类下还存在子分类, 暂不能删除, 请确认");
            }
        }
    }

    private void checkDeletePermission(ClassDO classDO, User user) throws Exception {
        if (user.getName().equalsIgnoreCase(classDO.getCreatedBy()) || user.isSuperAdmin()) {
            return;
        }
        throw new Exception("delete operation is not supported at the moment. Please contact the admin.");
    }

    @Override
    public List<ClassResp> getClassList(ClassFilter filter, User user) {
        List<ClassDO> classDOList = classRepository.getClassDOList(filter);
        return converter.convert2RespList(classDOList);
    }

}