package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.ViewDetail;
import com.tencent.supersonic.headless.api.pojo.request.ViewReq;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ViewDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ViewDOMapper;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.ViewService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ViewServiceImpl
        extends ServiceImpl<ViewDOMapper, ViewDO> implements ViewService {

    protected final Cache<MetaFilter, List<ViewResp>> viewSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Autowired
    private DomainService domainService;

    @Override
    public ViewResp save(ViewReq viewReq, User user) {
        viewReq.createdBy(user.getName());
        ViewDO viewDO = convert(viewReq);
        viewDO.setStatus(StatusEnum.ONLINE.getCode());
        save(viewDO);
        return convert(viewDO);
    }

    @Override
    public ViewResp update(ViewReq viewReq, User user) {
        viewReq.updatedBy(user.getName());
        ViewDO viewDO = convert(viewReq);
        updateById(viewDO);
        return convert(viewDO);
    }

    @Override
    public ViewResp getView(Long id) {
        ViewDO viewDO = getById(id);
        return convert(viewDO);
    }

    @Override
    public List<ViewResp> getViewList(MetaFilter metaFilter) {
        QueryWrapper<ViewDO> wrapper = new QueryWrapper<>();
        if (metaFilter.getDomainId() != null) {
            wrapper.lambda().eq(ViewDO::getDomainId, metaFilter.getDomainId());
        }
        if (!CollectionUtils.isEmpty(metaFilter.getIds())) {
            wrapper.lambda().in(ViewDO::getId, metaFilter.getIds());
        }
        wrapper.lambda().ne(ViewDO::getStatus, StatusEnum.DELETED.getCode());
        return list(wrapper).stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, User user) {
        ViewDO viewDO = getById(id);
        viewDO.setStatus(StatusEnum.DELETED.getCode());
        viewDO.setUpdatedBy(user.getName());
        viewDO.setUpdatedAt(new Date());
        updateById(viewDO);
    }

    @Override
    public List<ViewResp> getViews(User user) {
        List<ViewResp> viewResps = getViewList(new MetaFilter());
        return getViewFilterByAuth(viewResps, user);
    }

    @Override
    public List<ViewResp> getViewsInheritAuth(User user, Long domainId) {
        List<ViewResp> viewResps = getViewList(new MetaFilter());
        List<ViewResp> inheritAuthFormDomain = getViewFilterByDomainAuth(viewResps, user);
        Set<ViewResp> viewRespSet = new HashSet<>(inheritAuthFormDomain);
        List<ViewResp> viewFilterByAuth = getViewFilterByAuth(viewResps, user);
        viewRespSet.addAll(viewFilterByAuth);
        if (domainId != null && domainId > 0) {
            viewRespSet = viewRespSet.stream().filter(modelResp ->
                    modelResp.getDomainId().equals(domainId)).collect(Collectors.toSet());
        }
        return viewRespSet.stream().sorted(Comparator.comparingLong(ViewResp::getId))
                .collect(Collectors.toList());
    }

    private List<ViewResp> getViewFilterByAuth(List<ViewResp> viewResps, User user) {
        return viewResps.stream()
                .filter(viewResp -> checkAdminPermission(user, viewResp))
                .collect(Collectors.toList());
    }

    private List<ViewResp> getViewFilterByDomainAuth(List<ViewResp> viewResps, User user) {
        Set<DomainResp> domainResps = domainService.getDomainAuthSet(user, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(domainResps)) {
            return Lists.newArrayList();
        }
        Set<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toSet());
        return viewResps.stream().filter(viewResp ->
                domainIds.contains(viewResp.getDomainId())).collect(Collectors.toList());
    }

    private ViewResp convert(ViewDO viewDO) {
        ViewResp viewResp = new ViewResp();
        BeanMapper.mapper(viewDO, viewResp);
        viewResp.setViewDetail(JSONObject.parseObject(viewDO.getViewDetail(), ViewDetail.class));
        if (viewDO.getQueryConfig() != null) {
            viewResp.setQueryConfig(JSONObject.parseObject(viewDO.getQueryConfig(), QueryConfig.class));
        }
        viewResp.setAdmins(StringUtils.isBlank(viewDO.getAdmin())
                ? Lists.newArrayList() : Arrays.asList(viewDO.getAdmin().split(",")));
        viewResp.setAdminOrgs(StringUtils.isBlank(viewDO.getAdminOrg())
                ? Lists.newArrayList() : Arrays.asList(viewDO.getAdminOrg().split(",")));
        viewResp.setTypeEnum(TypeEnums.VIEW);
        return viewResp;
    }

    private ViewDO convert(ViewReq viewReq) {
        ViewDO viewDO = new ViewDO();
        BeanMapper.mapper(viewReq, viewDO);
        viewDO.setViewDetail(JSONObject.toJSONString(viewReq.getViewDetail()));
        viewDO.setQueryConfig(JSONObject.toJSONString(viewReq.getQueryConfig()));
        return viewDO;
    }

    public static boolean checkAdminPermission(User user, ViewResp viewResp) {
        List<String> admins = viewResp.getAdmins();
        if (user.isSuperAdmin()) {
            return true;
        }
        String userName = user.getName();
        return admins.contains(userName) || viewResp.getCreatedBy().equals(userName);
    }

    @Override
    public Map<Long, List<Long>> getModelIdToViewIds(List<Long> viewIds) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        metaFilter.setIds(viewIds);
        List<ViewResp> viewList = viewSchemaCache.getIfPresent(metaFilter);
        if (CollectionUtils.isEmpty(viewList)) {
            viewList = getViewList(metaFilter);
            viewSchemaCache.put(metaFilter, viewList);
        }
        return viewList.stream()
                .flatMap(
                        viewResp -> viewResp.getAllModels().stream().map(modelId -> Pair.of(modelId, viewResp.getId())))
                .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toList())));
    }
}
