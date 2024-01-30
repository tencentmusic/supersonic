package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.ViewDetail;
import com.tencent.supersonic.headless.api.pojo.request.ViewReq;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ViewDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ViewDOMapper;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.ViewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ViewServiceImpl
        extends ServiceImpl<ViewDOMapper, ViewDO> implements ViewService {

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

}
