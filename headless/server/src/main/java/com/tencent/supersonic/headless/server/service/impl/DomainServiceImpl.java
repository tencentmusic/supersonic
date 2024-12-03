package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainUpdateReq;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DomainDO;
import com.tencent.supersonic.headless.server.persistence.repository.DomainRepository;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.utils.DomainConvert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;
    private final ModelService modelService;
    private final UserService userService;

    public DomainServiceImpl(DomainRepository domainRepository, @Lazy ModelService modelService,
            UserService userService) {
        this.domainRepository = domainRepository;
        this.modelService = modelService;
        this.userService = userService;
    }

    @Override
    public DomainResp createDomain(DomainReq domainReq, User user) {
        DomainDO domainDO = DomainConvert.convert(domainReq, user);
        domainDO.setStatus(StatusEnum.ONLINE.getCode());
        domainRepository.createDomain(domainDO);
        return DomainConvert.convert(domainDO);
    }

    @Override
    public DomainResp updateDomain(DomainUpdateReq domainUpdateReq, User user) {
        domainUpdateReq.updatedBy(user.getName());
        DomainDO domainDO = getDomainDO(domainUpdateReq.getId());
        BeanMapper.mapper(domainUpdateReq, domainDO);
        domainRepository.updateDomain(domainDO);
        return DomainConvert.convert(domainDO);
    }

    @Override
    public void deleteDomain(Long id) {
        List<ModelResp> modelResps = modelService.getModelByDomainIds(Lists.newArrayList(id));
        if (!CollectionUtils.isEmpty(modelResps)) {
            throw new RuntimeException("该主题域下还存在模型, 暂不能删除, 请确认");
        }
        List<DomainResp> domainList = getDomainList();
        for (DomainResp domainResp : domainList) {
            if (id.equals(domainResp.getParentId())) {
                throw new RuntimeException("该主题域下还存在子主题域, 暂不能删除, 请确认");
            }
        }
        domainRepository.deleteDomain(id);
    }

    @Override
    public List<DomainResp> getDomainList() {
        return convertList(domainRepository.getDomainList());
    }

    @Override
    public List<DomainResp> getDomainList(List<Long> domainIds) {
        return getDomainList().stream().filter(domainDO -> domainIds.contains(domainDO.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainResp> getDomainListWithAdminAuth(User user) {
        Set<DomainResp> domainWithAuthAll = getDomainAuthSet(user, AuthType.ADMIN);
        if (!CollectionUtils.isEmpty(domainWithAuthAll)) {
            List<Long> domainIds =
                    domainWithAuthAll.stream().map(DomainResp::getId).collect(Collectors.toList());
            domainWithAuthAll.addAll(getParentDomain(domainIds));
        }
        List<ModelResp> modelResps = modelService.getModelAuthList(user, null, AuthType.ADMIN);
        List<Long> domainIdsFromModel =
                modelResps.stream().map(ModelResp::getDomainId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(modelResps)) {
            domainWithAuthAll.addAll(getParentDomain(domainIdsFromModel));
        }
        for (DomainResp domainResp : domainWithAuthAll) {
            if (domainIdsFromModel.contains(domainResp.getId())) {
                domainResp.setHasModel(true);
            }
        }
        return new ArrayList<>(domainWithAuthAll).stream()
                .sorted(Comparator.comparingLong(DomainResp::getId)).collect(Collectors.toList());
    }

    @Override
    public Set<DomainResp> getDomainAuthSet(User user, AuthType authTypeEnum) {
        List<DomainResp> domainResps = getDomainList();
        Set<String> orgIds = userService.getUserAllOrgId(user.getName());
        Set<DomainResp> domainWithAuth = Sets.newHashSet();
        if (authTypeEnum.equals(AuthType.ADMIN)) {
            domainWithAuth = domainResps.stream()
                    .filter(domainResp -> checkAdminPermission(orgIds, user, domainResp))
                    .collect(Collectors.toSet());
            return domainWithAuth.stream().peek(domainResp -> domainResp.setHasEditPermission(true))
                    .collect(Collectors.toSet());
        }
        if (authTypeEnum.equals(AuthType.VIEWER)) {
            domainWithAuth = domainResps.stream()
                    .filter(domainResp -> checkViewPermission(orgIds, user, domainResp))
                    .collect(Collectors.toSet());
        }

        return domainWithAuth;
    }

    private Set<DomainResp> getParentDomain(List<Long> ids) {
        Set<DomainResp> domainSet = new HashSet<>();
        if (CollectionUtils.isEmpty(ids)) {
            return Sets.newHashSet(domainSet);
        }
        Map<Long, DomainResp> domainRespMap = getDomainMap();
        for (Long domainId : ids) {
            DomainResp domainResp = domainRespMap.get(domainId);
            while (domainResp != null) {
                domainSet.add(domainResp);
                domainResp = domainRespMap.get(domainResp.getParentId());
            }
        }
        return domainSet;
    }

    @Override
    public DomainResp getDomain(Long id) {
        Map<Long, String> fullDomainPathMap = getDomainFullPathMap();
        return DomainConvert.convert(getDomainDO(id), fullDomainPathMap);
    }

    @Override
    public Map<Long, String> getDomainFullPath() {
        return getDomainFullPathMap();
    }

    private List<DomainResp> convertList(List<DomainDO> domainDOS) {
        List<DomainResp> domainDescs = Lists.newArrayList();
        if (CollectionUtils.isEmpty(domainDOS)) {
            return domainDescs;
        }
        Map<Long, String> fullDomainPathMap = getDomainFullPath();
        return domainDOS.stream()
                .map(domainDO -> DomainConvert.convert(domainDO, fullDomainPathMap))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, DomainResp> getDomainMap() {
        return getDomainList().stream()
                .collect(Collectors.toMap(DomainResp::getId, a -> a, (k1, k2) -> k1));
    }

    @Override
    public List<DomainDO> getDomainByBizName(String bizName) {
        return domainRepository.getDomainByBizName(bizName);
    }

    @Override
    public Set<DomainResp> getDomainChildren(List<Long> domainIds) {
        Set<DomainResp> childDomains = new HashSet<>();
        if (CollectionUtils.isEmpty(domainIds)) {
            return childDomains;
        }
        Map<Long, DomainResp> allDomainMap = getDomainMap();
        for (Long domainId : domainIds) {
            DomainResp domain = allDomainMap.get(domainId);
            if (domain != null) {
                childDomains.add(domain);
                Queue<DomainResp> queue = new LinkedList<>();
                queue.add(domain);
                while (!queue.isEmpty()) {
                    DomainResp currentDomain = queue.poll();
                    for (DomainResp child : allDomainMap.values()) {
                        if (Objects.equals(child.getParentId(), currentDomain.getId())) {
                            childDomains.add(child);
                            queue.add(child);
                        }
                    }
                }
            }
        }
        return childDomains;
    }

    public Map<Long, String> getDomainFullPathMap() {
        Map<Long, String> domainFullPathMap = new HashMap<>();
        List<DomainDO> domainDOList = domainRepository.getDomainList();
        Map<Long, DomainDO> domainDOMap = domainDOList.stream()
                .collect(Collectors.toMap(DomainDO::getId, a -> a, (k1, k2) -> k1));
        for (DomainDO domainDO : domainDOList) {
            final Long domainId = domainDO.getId();
            StringBuilder fullPath = new StringBuilder(domainDO.getBizName() + "/");
            Long parentId = domainDO.getParentId();
            while (parentId != null && parentId > 0) {
                domainDO = domainDOMap.get(parentId);
                if (domainDO == null) {
                    String message = String.format("get domain : %s failed", parentId);
                    throw new RuntimeException(message);
                }
                fullPath.insert(0, domainDO.getBizName() + "/");
                parentId = domainDO.getParentId();
            }
            domainFullPathMap.put(domainId, fullPath.toString());
        }
        return domainFullPathMap;
    }

    protected DomainDO getDomainDO(Long id) {
        return domainRepository.getDomainById(id);
    }

    private boolean checkAdminPermission(Set<String> orgIds, User user, DomainResp domainResp) {
        List<String> admins = domainResp.getAdmins();
        List<String> adminOrgs = domainResp.getAdminOrgs();
        if (user.isSuperAdmin()) {
            return true;
        }
        if (admins.contains(user.getName()) || domainResp.getCreatedBy().equals(user.getName())) {
            return true;
        }
        if (CollectionUtils.isEmpty(adminOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (adminOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkViewPermission(Set<String> orgIds, User user, DomainResp domainResp) {
        if (checkAdminPermission(orgIds, user, domainResp)) {
            return true;
        }
        List<String> viewers = domainResp.getViewers();
        List<String> viewOrgs = domainResp.getViewOrgs();
        if (domainResp.openToAll()) {
            return true;
        }
        if (viewers.contains(user.getName())) {
            return true;
        }
        if (CollectionUtils.isEmpty(viewOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (viewOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }
}
