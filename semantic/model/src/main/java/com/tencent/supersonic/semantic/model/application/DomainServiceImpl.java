package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.request.DomainReq;
import com.tencent.supersonic.semantic.api.model.request.DomainUpdateReq;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Domain;
import com.tencent.supersonic.semantic.model.domain.repository.DomainRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DomainConvert;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Sets;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;
    private final ModelService modelService;
    private final UserService userService;


    public DomainServiceImpl(DomainRepository domainRepository,
                             @Lazy ModelService modelService,
                             UserService userService) {
        this.domainRepository = domainRepository;
        this.modelService = modelService;
        this.userService = userService;
    }

    @Override
    public void createDomain(DomainReq domainReq, User user) {
        log.info("[create domain] cmd : {}", JSONObject.toJSONString(domainReq));
        Domain domain = DomainConvert.convert(domainReq);
        log.info("[create domain] object:{}", JSONObject.toJSONString(domainReq));
        saveDomain(domain, user);
    }

    @Override
    public void updateDomain(DomainUpdateReq domainUpdateReq, User user) {
        DomainDO domainDO = getDomainDO(domainUpdateReq.getId());
        domainDO.setUpdatedAt(new Date());
        domainDO.setUpdatedBy(user.getName());
        BeanMapper.mapper(domainUpdateReq, domainDO);
        domainDO.setAdmin(String.join(",", domainUpdateReq.getAdmins()));
        domainDO.setAdminOrg(String.join(",", domainUpdateReq.getAdminOrgs()));
        domainDO.setViewer(String.join(",", domainUpdateReq.getViewers()));
        domainDO.setViewOrg(String.join(",", domainUpdateReq.getViewOrgs()));
        domainRepository.updateDomain(domainDO);
    }

    @Override
    public void deleteDomain(Long id) {
        domainRepository.deleteDomain(id);
    }

    @Override
    public List<DomainResp> getDomainList() {
        return convertList(domainRepository.getDomainList());
    }

    @Override
    public List<DomainResp> getDomainList(List<Long> domainIds) {
        return getDomainList().stream()
                .filter(domainDO -> domainIds.contains(domainDO.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainResp> getDomainListWithAdminAuth(User user) {
        Set<DomainResp> domainWithAuthAll = getDomainAuthSet(user.getName(), AuthType.ADMIN);
        if (!CollectionUtils.isEmpty(domainWithAuthAll)) {
            List<Long> domainIds = domainWithAuthAll.stream().map(DomainResp::getId).collect(Collectors.toList());
            domainWithAuthAll.addAll(getParentDomain(domainIds));
        }
        List<ModelResp> modelResps = modelService.getModelAuthList(user.getName(), AuthType.ADMIN);
        if (!CollectionUtils.isEmpty(modelResps)) {
            List<Long> domainIds = modelResps.stream().map(ModelResp::getDomainId).collect(Collectors.toList());
            domainWithAuthAll.addAll(getParentDomain(domainIds));
        }
        return new ArrayList<>(domainWithAuthAll).stream()
                .sorted(Comparator.comparingLong(DomainResp::getId)).collect(Collectors.toList());
    }

    @Override
    public Set<DomainResp> getDomainAuthSet(String userName, AuthType authTypeEnum) {
        List<DomainResp> domainResps = getDomainList();
        Set<String> orgIds = userService.getUserAllOrgId(userName);
        List<DomainResp> domainWithAuth = Lists.newArrayList();
        if (authTypeEnum.equals(AuthType.ADMIN)) {
            domainWithAuth = domainResps.stream()
                    .filter(domainResp -> checkAdminPermission(orgIds, userName, domainResp))
                    .collect(Collectors.toList());
        }
        if (authTypeEnum.equals(AuthType.VISIBLE)) {
            domainWithAuth = domainResps.stream()
                    .filter(domainResp -> checkViewerPermission(orgIds, userName, domainResp))
                    .collect(Collectors.toList());
        }
        List<Long> domainIds = domainWithAuth.stream().map(DomainResp::getId)
                .collect(Collectors.toList());
        //get all child domain
        return getDomainChildren(domainIds);
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

    //保存并获取自增ID
    private void saveDomain(Domain domain, User user) {
        DomainDO domainDO = DomainConvert.convert(domain, user);
        domainRepository.createDomain(domainDO);
        domain.setId(domainDO.getId());
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
        return getDomainList().stream().collect(Collectors.toMap(DomainResp::getId, a -> a, (k1, k2) -> k1));
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


    private boolean checkAdminPermission(Set<String> orgIds, String userName, DomainResp domainResp) {

        List<String> admins = domainResp.getAdmins();
        List<String> adminOrgs = domainResp.getAdminOrgs();
        if (admins.contains(userName) || domainResp.getCreatedBy().equals(userName)) {
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

    private boolean checkViewerPermission(Set<String> orgIds, String userName, DomainResp domainDesc) {
        List<String> admins = domainDesc.getAdmins();
        List<String> viewers = domainDesc.getViewers();
        List<String> adminOrgs = domainDesc.getAdminOrgs();
        List<String> viewOrgs = domainDesc.getViewOrgs();
        if (admins.contains(userName) || viewers.contains(userName) || domainDesc.getCreatedBy().equals(userName)) {
            return true;
        }
        if (CollectionUtils.isEmpty(adminOrgs) && CollectionUtils.isEmpty(viewOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (adminOrgs.contains(orgId)) {
                return true;
            }
        }
        for (String orgId : orgIds) {
            if (viewOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }
}
